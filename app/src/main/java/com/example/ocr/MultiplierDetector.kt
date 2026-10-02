package com.example.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class MultiplierCandidate(
    val value: Double,
    val rawText: String,
    val leftX: Int
)

class MultiplierDetector {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    // Regex to match multipliers like 1.25x, 15.00x, 2x, 1,45X, 100.50×, etc.
    private val multiplierRegex = Regex("""(\d{1,4}(?:[.,]\d{1,2})?)\s*[xX×]?""")

    suspend fun detectLeftmostMultiplier(bitmap: Bitmap): MultiplierCandidate? =
        suspendCancellableCoroutine { continuation ->
            try {
                val inputImage = InputImage.fromBitmap(bitmap, 0)
                recognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        val candidate = extractLeftmostMultiplier(visionText)
                        if (continuation.isActive) {
                            continuation.resume(candidate)
                        }
                    }
                    .addOnFailureListener { error ->
                        if (continuation.isActive) {
                            continuation.resume(null)
                        }
                    }
            } catch (e: Exception) {
                if (continuation.isActive) {
                    continuation.resume(null)
                }
            }
        }

    private fun extractLeftmostMultiplier(visionText: Text): MultiplierCandidate? {
        val candidates = mutableListOf<MultiplierCandidate>()

        for (block in visionText.textBlocks) {
            for (line in block.lines) {
                val lineBox = line.boundingBox
                val lineLeft = lineBox?.left ?: 0

                for (element in line.elements) {
                    val elemBox = element.boundingBox
                    val elemLeft = elemBox?.left ?: lineLeft
                    val text = element.text.trim()

                    parseMultiplierFromToken(text, elemLeft)?.let {
                        candidates.add(it)
                    }
                }

                // Also check full line string if elements were split awkwardly
                if (candidates.isEmpty()) {
                    parseMultiplierFromToken(line.text, lineLeft)?.let {
                        candidates.add(it)
                    }
                }
            }
        }

        // If no structured candidates were found, fallback to parsing raw text
        if (candidates.isEmpty() && visionText.text.isNotBlank()) {
            val matches = multiplierRegex.findAll(visionText.text)
            for (match in matches) {
                val numStr = match.groupValues[1].replace(',', '.')
                val numVal = numStr.toDoubleOrNull()
                if (numVal != null && numVal in 0.5..10000.0) {
                    return MultiplierCandidate(
                        value = numVal,
                        rawText = match.value,
                        leftX = match.range.first
                    )
                }
            }
        }

        // Sort by left X coordinate (leftmost first)
        return candidates.minByOrNull { it.leftX }
    }

    private fun parseMultiplierFromToken(token: String, leftX: Int): MultiplierCandidate? {
        val clean = token.replace(" ", "").replace("x", "x").replace("X", "x")
        val match = multiplierRegex.find(clean) ?: return null

        val numStr = match.groupValues[1].replace(',', '.')
        val numVal = numStr.toDoubleOrNull() ?: return null

        // Filter out absurd numbers (multipliers are generally between 1.00 and 50000.00)
        if (numVal in 0.9..99999.0) {
            return MultiplierCandidate(
                value = numVal,
                rawText = match.value,
                leftX = leftX
            )
        }
        return null
    }

    fun close() {
        try {
            recognizer.close()
        } catch (_: Exception) {}
    }
}
