package com.example.camera

import androidx.camera.core.ImageProxy

data class OpticalFrameResult(
    val isCalibrating: Boolean,
    val isDownPosition: Boolean,
    val repDetected: Boolean,
    val depthProgress: Float // 0f (top) .. 1f (bottom/shadow dip)
)

/**
 * Calibrates ambient ceiling brightness over the front camera while the user holds the top
 * plank position, then detects push-up dips when the chest/head shadows the central ROI
 * and recovers back toward baseline. Includes a 550ms refractory cooldown to prevent double counts.
 */
class OpticalRepDetector(
    private val cooldownMs: Long = 550L,
    private val calibrationFramesRequired: Int = 8
) {
    private var baselineLuma: Float = 0f
    private var calibrationSum: Float = 0f
    private var calibrationCount: Int = 0
    private var isDown: Boolean = false
    private var hasSeenUpAfterEnable: Boolean = false
    private var lastRepTimestampMs: Long? = null

    fun reset() {
        baselineLuma = 0f
        calibrationSum = 0f
        calibrationCount = 0
        isDown = false
        hasSeenUpAfterEnable = false
        lastRepTimestampMs = null
    }

    fun processFrameLuminance(luma: Float, timestampMs: Long): OpticalFrameResult {
        if (calibrationCount < calibrationFramesRequired) {
            calibrationSum += luma
            calibrationCount++
            if (calibrationCount == calibrationFramesRequired) {
                baselineLuma = (calibrationSum / calibrationFramesRequired).coerceAtLeast(20f)
                hasSeenUpAfterEnable = true
            }
            return OpticalFrameResult(
                isCalibrating = true,
                isDownPosition = false,
                repDetected = false,
                depthProgress = 0f
            )
        }

        // Adapt baseline slowly only when clearly above dip range so lighting shifts stay calibrated
        val dipThreshold = minOf(baselineLuma * 0.72f, baselineLuma - 14f).coerceAtLeast(8f)
        val recoverThreshold = (baselineLuma * 0.84f).coerceAtLeast(dipThreshold + 6f)

        var repCounted = false
        if (luma <= dipThreshold) {
            val prevRep = lastRepTimestampMs
            if (!isDown && hasSeenUpAfterEnable && (prevRep == null || timestampMs - prevRep >= cooldownMs)) {
                repCounted = true
                lastRepTimestampMs = timestampMs
                isDown = true
            } else {
                isDown = true
            }
        } else if (luma >= recoverThreshold) {
            isDown = false
            hasSeenUpAfterEnable = true
            // Gentle EMA drift tracking at top position
            baselineLuma = baselineLuma * 0.96f + luma * 0.04f
        }

        val depth = if (baselineLuma > dipThreshold) {
            ((baselineLuma - luma) / (baselineLuma - dipThreshold)).coerceIn(0f, 1f)
        } else 0f

        return OpticalFrameResult(
            isCalibrating = false,
            isDownPosition = isDown,
            repDetected = repCounted,
            depthProgress = depth
        )
    }

    companion object {
        /**
         * Computes mean Y-plane luminance across the central 40% crop of a CameraX ImageProxy.
         */
        fun computeCenterRoiLuminance(image: ImageProxy): Float {
            val plane = image.planes.firstOrNull() ?: return 0f
            val buffer = plane.buffer
            val rowStride = plane.rowStride
            val pixelStride = plane.pixelStride
            val width = image.width
            val height = image.height
            if (width <= 0 || height <= 0) return 0f

            val startX = (width * 0.30f).toInt()
            val endX = (width * 0.70f).toInt().coerceAtLeast(startX + 1)
            val startY = (height * 0.30f).toInt()
            val endY = (height * 0.70f).toInt().coerceAtLeast(startY + 1)

            var sum = 0L
            var count = 0
            val step = 4
            var y = startY
            while (y < endY) {
                val rowOffset = y * rowStride
                var x = startX
                while (x < endX) {
                    val index = rowOffset + x * pixelStride
                    if (index in 0 until buffer.limit()) {
                        sum += (buffer.get(index).toInt() and 0xFF)
                        count++
                    }
                    x += step
                }
                y += step
            }
            return if (count > 0) sum.toFloat() / count.toFloat() else 0f
        }
    }
}
