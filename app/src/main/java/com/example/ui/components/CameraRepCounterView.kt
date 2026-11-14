package com.example.ui.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.FireOrange
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextGray
import com.example.ui.theme.TextWhite
import java.util.concurrent.Executors

@Composable
fun CameraRepCounterView(
    isActive: Boolean,
    onRepDetected: () -> Unit,
    onCameraError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnRepDetected by rememberUpdatedState(onRepDetected)
    val currentOnCameraError by rememberUpdatedState(onCameraError)

    val detector = remember { OpticalRepDetector() }
    var isCalibrating by remember { mutableStateOf(true) }
    var isDownPosition by remember { mutableStateOf(false) }
    var depthProgress by remember { mutableFloatStateOf(0f) }

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
            isCalibrating = true
            isDownPosition = false
            depthProgress = 0f

            val providerFuture = ProcessCameraProvider.getInstance(context)
            providerFuture.addListener(
                {
                    if (disposed) return@addListener
                    try {
                        val provider = providerFuture.get()
                        cameraProvider = provider

                        val selector = when {
                            provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraSelector.DEFAULT_FRONT_CAMERA
                            provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.DEFAULT_BACK_CAMERA
                            else -> {
                                currentOnCameraError("No usable camera found on this device.")
                                return@addListener
                            }
                        }

                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                            try {
                                val luma = OpticalRepDetector.computeCenterRoiLuminance(imageProxy)
                                val result = detector.processFrameLuminance(luma, System.currentTimeMillis())
                                mainExecutor.execute {
                                    if (!disposed) {
                                        isCalibrating = result.isCalibrating
                                        isDownPosition = result.isDownPosition
                                        depthProgress = result.depthProgress
                                        if (result.repDetected) {
                                            currentOnRepDetected()
                                        }
                                    }
                                }
                            } finally {
                                imageProxy.close()
                            }
                        }

                        provider.unbindAll()
                        provider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
                    } catch (e: Exception) {
                        mainExecutor.execute {
                            if (!disposed) {
                                currentOnCameraError(e.localizedMessage ?: "Unable to start optical camera counter.")
                            }
                        }
                    }
                },
                mainExecutor
            )
        }

        onDispose {
            disposed = true
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
                isCalibrating -> AquaBlue.copy(0.6f)
                isDownPosition -> NeonGreen
                else -> NeonGreen.copy(0.35f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(74.dp)
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = when {
                                    isCalibrating -> AquaBlue
                                    isDownPosition -> NeonGreen
                                    else -> FireOrange
                                },
                                shape = CircleShape
                            )
                    )
                    Text(
                        text = when {
                            isCalibrating -> "CALIBRATING LIGHTING..."
                            isDownPosition -> "DIP DETECTED (REP LOCKED)"
                            else -> "OPTICAL COUNTER READY"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isDownPosition) NeonGreen else TextWhite
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (isCalibrating) {
                        "Hold plank position briefly so ceiling light baseline locks in."
                    } else {
                        "Place phone face-up under your chest. Chest shadow triggers each rep."
                    },
                    fontSize = 10.sp,
                    color = TextGray,
                    lineHeight = 13.sp
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
    }
}
