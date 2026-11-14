package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.example.ui.theme.AquaBlue
import com.example.ui.theme.CardSurface
import com.example.ui.theme.CardSurfaceElevated
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.FireOrange
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.PitchBlack
import com.example.ui.theme.TextGray
import com.example.ui.theme.TextWhite
import java.io.File
import java.io.FileOutputStream

enum class ShareCardType(val label: String) {
    WEEKLY("Weekly"),
    STREAK("Streak"),
    PERSONAL_BEST("Best"),
    CHALLENGE("Challenge")
}

@Composable
fun ShareProgressDialog(
    weeklyPushUps: Int,
    weeklyTarget: Int,
    streakDays: Int,
    personalBest: Int,
    lastSessionReps: Int? = null,
    initialType: ShareCardType = ShareCardType.WEEKLY,
    onShareTriggered: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedType by remember { mutableStateOf(initialType) }
    var selectedChallengeTarget by remember {
        mutableStateOf(
            (lastSessionReps ?: personalBest.coerceAtLeast(25))
        )
    }

    val accentColor = when (selectedType) {
        ShareCardType.WEEKLY -> NeonGreen
        ShareCardType.STREAK -> FireOrange
        ShareCardType.PERSONAL_BEST -> AquaBlue
        ShareCardType.CHALLENGE -> NeonGreen
    }

    val cardHeadline = when (selectedType) {
        ShareCardType.WEEKLY -> "WEEKLY PROGRESS"
        ShareCardType.STREAK -> "CONSISTENCY STREAK"
        ShareCardType.PERSONAL_BEST -> "PERSONAL RECORD"
        ShareCardType.CHALLENGE -> "FRIEND CHALLENGE"
    }

    val heroNumber = when (selectedType) {
        ShareCardType.WEEKLY -> "$weeklyPushUps"
        ShareCardType.STREAK -> "$streakDays"
        ShareCardType.PERSONAL_BEST -> "$personalBest"
        ShareCardType.CHALLENGE -> "$selectedChallengeTarget"
    }

    val heroUnit = when (selectedType) {
        ShareCardType.WEEKLY -> "PUSH-UPS THIS WEEK"
        ShareCardType.STREAK -> if (streakDays == 1) "DAY STREAK" else "DAYS IN A ROW"
        ShareCardType.PERSONAL_BEST -> "REPS IN ONE SESSION"
        ShareCardType.CHALLENGE -> "PUSH-UP CHALLENGE"
    }

    val cardSubtext = when (selectedType) {
        ShareCardType.WEEKLY -> {
            val pct = if (weeklyTarget > 0) ((weeklyPushUps * 100f) / weeklyTarget).toInt() else 0
            "$pct% of my $weeklyTarget-rep weekly goal"
        }
        ShareCardType.STREAK -> "Showing up day after day on Pushup Club"
        ShareCardType.PERSONAL_BEST -> "Single-session personal best unlocked"
        ShareCardType.CHALLENGE -> "Can you beat $selectedChallengeTarget push-ups today?"
    }

    val shareMessage = when (selectedType) {
        ShareCardType.WEEKLY -> "Logged $weeklyPushUps push-ups this week toward my $weeklyTarget-rep goal on Pushup Club!"
        ShareCardType.STREAK -> "On a $streakDays-day push-up streak with Pushup Club. Keeping the momentum going!"
        ShareCardType.PERSONAL_BEST -> "Hit a new personal best of $personalBest push-ups in one session on Pushup Club!"
        ShareCardType.CHALLENGE -> "Pushup Club Challenge: Can you knock out $selectedChallengeTarget push-ups today? Reply with your count!"
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = CardSurfaceElevated,
            border = BorderStroke(1.dp, accentColor.copy(0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SHARE PROGRESS",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = TextWhite,
                        letterSpacing = 0.8.sp
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close share dialog", tint = TextGray)
                    }
                }

                // Mode selector chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ShareCardType.entries.forEach { type ->
                        val active = selectedType == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) accentColor else DarkSurface)
                                .border(
                                    1.dp,
                                    if (active) accentColor else Color.White.copy(0.08f),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedType = type }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = type.label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (active) Color(0xFF003818) else TextGray
                            )
                        }
                    }
                }

                // Challenge quick presets when in Challenge mode
                if (selectedType == ShareCardType.CHALLENGE) {
                    val challengeTargets = remember(personalBest) {
                        listOf(25, 50, 100, personalBest.coerceAtLeast(30)).distinct().sorted()
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        challengeTargets.forEach { target ->
                            val active = selectedChallengeTarget == target
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (active) NeonGreen.copy(0.18f) else CardSurface)
                                    .border(
                                        1.dp,
                                        if (active) NeonGreen else Color.White.copy(0.1f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { selectedChallengeTarget = target }
                                    .padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$target reps",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) NeonGreen else TextWhite
                                )
                            }
                        }
                    }
                }

                // Visual Share Card Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(PitchBlack, DarkSurface, CardSurface)
                            )
                        )
                        .border(1.5.dp, accentColor.copy(0.45f), RoundedCornerShape(18.dp))
                        .padding(22.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = when (selectedType) {
                                    ShareCardType.WEEKLY -> Icons.Default.FitnessCenter
                                    ShareCardType.STREAK -> Icons.Default.LocalFireDepartment
                                    ShareCardType.PERSONAL_BEST -> Icons.Default.EmojiEvents
                                    ShareCardType.CHALLENGE -> Icons.Default.SportsScore
                                },
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = cardHeadline,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = accentColor,
                                letterSpacing = 1.2.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = heroNumber,
                            fontSize = 56.sp,
                            fontWeight = FontWeight.Black,
                            color = TextWhite,
                            lineHeight = 56.sp
                        )

                        Text(
                            text = heroUnit,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = cardSubtext,
                            fontSize = 12.sp,
                            color = TextGray,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Box(
                            modifier = Modifier
                                .background(Color.White.copy(0.06f), CircleShape)
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "PUSHUP CLUB",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = TextWhite.copy(0.85f),
                                letterSpacing = 1.2.sp
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        onShareTriggered()
                        shareCardAndText(
                            context = context,
                            headline = cardHeadline,
                            heroValue = heroNumber,
                            heroUnit = heroUnit,
                            subtext = cardSubtext,
                            accentColorArgb = accentColor.toArgb(),
                            shareText = shareMessage
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = Color(0xFF003818)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("confirm_share_button")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedType == ShareCardType.CHALLENGE) "SEND CHALLENGE" else "SHARE PROGRESS CARD",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

private fun shareCardAndText(
    context: Context,
    headline: String,
    heroValue: String,
    heroUnit: String,
    subtext: String,
    accentColorArgb: Int,
    shareText: String
) {
    val imageUri = try {
        val width = 1080
        val height = 1080
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background gradient
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                intArrayOf(0xFF0A0E17.toInt(), 0xFF131823.toInt(), 0xFF1C2232.toInt()),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Card border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 10f
            color = accentColorArgb
        }
        canvas.drawRoundRect(RectF(64f, 64f, width - 64f, height - 64f), 48f, 48f, borderPaint)

        val centerX = width / 2f

        // Headline
        val headlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColorArgb
            textSize = 44f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.12f
        }
        canvas.drawText(headline, centerX, 260f, headlinePaint)

        // Hero value
        val heroPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFF8FAFC.toInt()
            textSize = 230f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(heroValue, centerX, 550f, heroPaint)

        // Hero unit
        val unitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColorArgb
            textSize = 46f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.08f
        }
        canvas.drawText(heroUnit, centerX, 640f, unitPaint)

        // Subtext
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF94A3B8.toInt()
            textSize = 36f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(subtext, centerX, 750f, subPaint)

        // Brand footer
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFF8FAFC.toInt()
            textSize = 34f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.18f
        }
        canvas.drawText("PUSHUP CLUB", centerX, 920f, brandPaint)

        val cardsDir = File(context.cacheDir, "shared_cards").apply { mkdirs() }
        val file = File(cardsDir, "pushup_club_card.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        bitmap.recycle()

        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    } catch (_: Exception) {
        null
    }

    val shareIntent = if (imageUri != null) {
        Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            putExtra(Intent.EXTRA_TEXT, shareText)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    } else {
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
    }

    context.startActivity(Intent.createChooser(shareIntent, "Share your Pushup Club progress"))
}
