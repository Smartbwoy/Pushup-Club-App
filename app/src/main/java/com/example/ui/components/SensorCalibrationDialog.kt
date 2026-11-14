package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.AquaBlue
import com.example.ui.theme.CardSurface
import com.example.ui.theme.CardSurfaceElevated
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextGray
import com.example.ui.theme.TextWhite

@Composable
fun SensorCalibrationDialog(
    calibrationStep: Int,
    isSensorNear: Boolean,
    onDone: () -> Unit,
    onUseManualFallback: () -> Unit
) {
    val isCalibrated = calibrationStep >= 2
    val statusColor = when {
        isCalibrated -> NeonGreen
        isSensorNear -> AquaBlue
        else -> TextGray
    }

    val statusTitle = when (calibrationStep) {
        0 -> "Step 1 of 2: Move Close"
        1 -> "Step 2 of 2: Move Away"
        else -> "Sensor Calibrated!"
    }

    val statusInstruction = when (calibrationStep) {
        0 -> "Wave your hand within 2 inches of the top of your phone (near the earpiece)."
        1 -> "Detected! Now pull your hand back to complete the test cycle."
        else -> "Verified! Place your phone face-up under your chest. Each full down-and-up motion counts 1 rep without double-counting."
    }

    Dialog(onDismissRequest = onDone) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = CardSurfaceElevated,
            border = BorderStroke(1.dp, statusColor.copy(0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(statusColor.copy(0.15f), CircleShape)
                        .border(2.dp, statusColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCalibrated) Icons.Default.CheckCircle else Icons.Default.Sensors,
                        contentDescription = "Proximity sensor calibration status",
                        tint = statusColor,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "HANDS-FREE CALIBRATION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = statusColor,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = statusTitle,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = TextWhite,
                        textAlign = TextAlign.Center
                    )
                }

                // Live reading pill
                Row(
                    modifier = Modifier
                        .background(CardSurface, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(if (isSensorNear) NeonGreen else TextGray, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isSensorNear) "LIVE SENSOR: CLOSE" else "LIVE SENSOR: CLEAR",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSensorNear) NeonGreen else TextGray
                    )
                }

                Text(
                    text = statusInstruction,
                    fontSize = 12.sp,
                    color = TextGray,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onDone,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCalibrated) NeonGreen else AquaBlue,
                            contentColor = Color(0xFF003818)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("calibration_done_button")
                    ) {
                        Text(
                            text = if (isCalibrated) "READY TO WORK OUT" else "SKIP TEST & USE SENSOR",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                    }

                    OutlinedButton(
                        onClick = onUseManualFallback,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.White.copy(0.18f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("calibration_manual_fallback")
                    ) {
                        Icon(Icons.Default.TouchApp, contentDescription = null, tint = TextWhite, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Use Manual Tap Instead",
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
