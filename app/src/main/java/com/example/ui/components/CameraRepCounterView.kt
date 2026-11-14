package com.example.ui.components

import android.hardware.camera2.CaptureRequest
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.camera.OpticalRepDetector
import com.example.ui.theme.AquaBlue
import com.example.ui.theme.CardSurface
import com.example.ui.theme.CardSurfaceElevated
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.FireOrange
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextGray
import com.example.ui.theme.TextWhite
import java.util.concurrent.Executors

@Composable
fun CameraRepCounterView(
    isActive: Boolean,
    isPaused: Boolean = false,
    onRepDetected: () -> Unit,
    onCameraError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnRepDetected by rememberUpdatedState(onRepDetected)
    val currentOnCameraError by rememberUpdatedState(onCameraError)
    val currentIsPaused by rememberUpdatedState(isPaused)

    val detector = remember { OpticalRepDetector(cooldownMs = 550L, calibrationFramesRequired = 8, warmupMs = 2000L) }
    var isWarmingUp by remember { mutableStateOf(true) }
    var countdownSeconds by remember { mutableIntStateOf(2) }
    var isCalibrating by remember { mutableStateOf(true) }
    var isDownPosition by remember { mutableStateOf(false) }
    var isTooDark by remember { mutableStateOf(false) }
    var depthProgress by remember { mutableFloatStateOf(0f) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    DisposableEffect(isActive, lifecycleOwner) {
        val analysisExecutor = Executors.newSingleThreadExecutor()
        val mainExecutor = ContextCompat.getMainExecutor(context)
        var cameraProvider: ProcessCameraProvider? = null
        var disposed = false

        if (isActive) {
            detector.reset()
            isWarmingUp = true
            countdownSeconds = 2
            isCalibrating = true
            isDownPosition = false
            isTooDark = false
            depthProgress = 0f

            val providerFuture = ProcessCameraProvider.getInstance(context)
            providerFuture.addListener(
                {
                    if (disposed) return@addListener
                    try {
                        val provider = providerFuture.get()
                        cameraProvider = provider

                        // Strictly require front camera; never fall back silently to rear camera
                        if (!provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                            currentOnCameraError("No front camera found. Switch to Proximity Sensor or Tap Mode.")
                            return@addListener
                        }
                        val selector = CameraSelector.DEFAULT_FRONT_CAMERA

                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                            try {
                                if (currentIsPaused && detector.isCalibrated()) {
                                    // Keep baseline & locked exposure intact across rest intervals
                                    mainExecutor.execute {
                                        if (!disposed) {
                                            isDownPosition = false
                                            depthProgress = 0f
                                        }
                                    }
                                    return@setAnalyzer
                                }
                                val luma = OpticalRepDetector.computeCenterRoiLuminance(imageProxy)
                                val result = detector.processFrameLuminance(luma, System.currentTimeMillis())
                                if (result.calibrationJustCompleted) {
                                    setAutoExposureLocked(boundCamera, locked = true)
                                }
                                mainExecutor.execute {
                                    if (!disposed) {
                                        isWarmingUp = result.isWarmingUp
                                        countdownSeconds = result.countdownSecondsLeft
                                        isCalibrating = result.isCalibrating
                                        isDownPosition = result.isDownPosition
                                        isTooDark = result.isTooDark
                                        depthProgress = result.depthProgress
                                        if (result.repDetected && !currentIsPaused) {
                                            currentOnRepDetected()
                                        }
                                    }
                                }
                            } finally {
                                imageProxy.close()
                            }
                        }

                        provider.unbindAll()
                        val camera = provider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
                        boundCamera = camera
                        setAutoExposureLocked(camera, locked = false)
                    } catch (e: Exception) {
                        mainExecutor.execute {
                            if (!disposed) {
                                currentOnCameraError(e.localizedMessage ?: "Unable to start front camera counter.")
                            }
                        }
                    }
                },
                mainExecutor
            )
        }

        onDispose {
            disposed = true
            setAutoExposureLocked(boundCamera, locked = false)
            boundCamera = null
            try {
                cameraProvider?.unbindAll()
            } catch (_: Exception) {
            }
            analysisExecutor.shutdown()
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("camera_rep_counter_card"),
        color = CardSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.5.dp,
            when {
                isTooDark -> FireOrange.copy(0.75f)
                isPaused -> AquaBlue.copy(0.4f)
                isCalibrating -> AquaBlue.copy(0.6f)
                isDownPosition -> NeonGreen
                else -> NeonGreen.copy(0.35f)
            }
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurface)
                        .border(
                            BorderStroke(2.dp, if (isDownPosition) NeonGreen else AquaBlue.copy(0.5f)),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { previewView },
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .border(1.5.dp, NeonGreen.copy(0.8f), CircleShape)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        color = when {
                                            isPaused -> AquaBlue
                                            isCalibrating -> AquaBlue
                                            isDownPosition -> NeonGreen
                                            else -> FireOrange
                                        },
                                        shape = CircleShape
                                    )
                            )
                            Text(
                                text = when {
                                    isPaused -> "PAUSED FOR REST (LOCKED)"
                                    isWarmingUp -> "GET IN PLANK (${countdownSeconds}s)..."
                                    isCalibrating -> "LOCKING EXPOSURE..."
                                    isDownPosition -> "DIP DETECTED (REP LOCKED)"
                                    else -> "OPTICAL COUNTER READY"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isDownPosition) NeonGreen else TextWhite
                            )
                        }

                        if (!isWarmingUp && !isCalibrating) {
                            IconButton(
                                onClick = {
                                    setAutoExposureLocked(boundCamera, locked = false)
                                    detector.reset()
                                    isWarmingUp = true
                                    countdownSeconds = 2
                                    isCalibrating = true
                                    isDownPosition = false
                                    depthProgress = 0f
                                },
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(CardSurfaceElevated, CircleShape)
                                    .testTag("camera_recalibrate_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Recalibrate camera lighting",
                                    tint = AquaBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = when {
                            isPaused -> "Calibration preserved between sets. Ready as soon as rest ends."
                            isWarmingUp -> "Set phone face-up under chest and hold top plank position."
                            isCalibrating -> "Capturing ceiling brightness and locking exposure..."
                            else -> "Exposure locked. Lower chest over phone to trigger each rep."
                        },
                        fontSize = 11.sp,
                        color = TextGray,
                        lineHeight = 14.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { if (isCalibrating) 0.25f else depthProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (isDownPosition) NeonGreen else AquaBlue,
                        trackColor = Color.White.copy(0.12f)
                    )
                }
            }

            if (isTooDark) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(FireOrange.copy(0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = FireOrange,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Too dark for camera detection — turn on a room light or switch to Tap/Sensor.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FireOrange,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalCamera2Interop::class)
private fun setAutoExposureLocked(camera: Camera?, locked: Boolean) {
    camera ?: return
    try {
        val camera2Control = Camera2CameraControl.from(camera.cameraControl)
        val options = CaptureRequestOptions.Builder()
            .setCaptureRequestOption(CaptureRequest.CONTROL_AE_LOCK, locked)
            .build()
        camera2Control.setCaptureRequestOptions(options)
    } catch (_: Exception) {
    }
}
