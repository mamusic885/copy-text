package com.example.ocr

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.example.model.OcrLanguage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class OcrEngine {

    // Lazy initialization of offline recognizers
    private val latinRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    private val devanagariRecognizer by lazy {
        TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
    }

    suspend fun recognizeText(
        context: Context,
        imageUri: Uri,
        language: OcrLanguage
    ): String = withContext(Dispatchers.IO) {
        val inputImage = try {
            InputImage.fromFilePath(context, imageUri)
        } catch (e: Exception) {
            throw IllegalArgumentException("Unable to load image file. Please select a valid photo.", e)
        }

        recognizeTextFromInputImage(inputImage, language)
    }

    suspend fun recognizeTextFromBitmap(
        bitmap: Bitmap,
        language: OcrLanguage
    ): String = withContext(Dispatchers.IO) {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        recognizeTextFromInputImage(inputImage, language)
    }

    private suspend fun recognizeTextFromInputImage(
        inputImage: InputImage,
        language: OcrLanguage
    ): String {
        return when (language) {
            OcrLanguage.ENGLISH -> {
                val result = processWithRecognizer(latinRecognizer, inputImage)
                result.text
            }
            OcrLanguage.HINDI -> {
                val result = processWithRecognizer(devanagariRecognizer, inputImage)
                result.text
            }
            OcrLanguage.PUNJABI, OcrLanguage.AUTO -> {
                // Run both recognizers to capture mixed Latin, Devanagari, and Gurmukhi script characters
                val latinResult = try {
                    processWithRecognizer(latinRecognizer, inputImage)
                } catch (e: Exception) {
                    null
                }

                val devanagariResult = try {
                    processWithRecognizer(devanagariRecognizer, inputImage)
                } catch (e: Exception) {
                    null
                }

                mergeOcrResults(latinResult, devanagariResult)
            }
        }
    }

    private suspend fun processWithRecognizer(
        recognizer: com.google.mlkit.vision.text.TextRecognizer,
        inputImage: InputImage
    ): Text = suspendCancellableCoroutine { continuation ->
        recognizer.process(inputImage)
            .addOnSuccessListener { text ->
                if (continuation.isActive) {
                    continuation.resume(text)
                }
            }
            .addOnFailureListener { exception ->
                if (continuation.isActive) {
                    continuation.resumeWithException(exception)
                }
            }
    }

    private fun mergeOcrResults(result1: Text?, result2: Text?): String {
        val text1 = result1?.text?.trim().orEmpty()
        val text2 = result2?.text?.trim().orEmpty()

        if (text1.isEmpty() && text2.isEmpty()) return ""
        if (text1.isEmpty()) return text2
        if (text2.isEmpty()) return text1
        if (text1 == text2) return text1

        // Extract blocks and sort spatially by top bounding position
        val allBlocks = mutableListOf<BlockInfo>()

        result1?.textBlocks?.forEach { block ->
            if (block.text.isNotBlank()) {
                allBlocks.add(BlockInfo(block.text.trim(), block.boundingBox?.top ?: 0, block.boundingBox?.left ?: 0))
            }
        }

        result2?.textBlocks?.forEach { block ->
            if (block.text.isNotBlank()) {
                val textTrimmed = block.text.trim()
                // Avoid duplicating blocks that match closely
                val isDuplicate = allBlocks.any { existing ->
                    existing.text == textTrimmed ||
                    (existing.text.contains(textTrimmed) || textTrimmed.contains(existing.text)) &&
                    Math.abs(existing.top - (block.boundingBox?.top ?: 0)) < 40
                }
                if (!isDuplicate) {
                    allBlocks.add(BlockInfo(textTrimmed, block.boundingBox?.top ?: 0, block.boundingBox?.left ?: 0))
                }
            }
        }

        // Sort blocks primarily by Y position (top), secondarily by X position (left)
        allBlocks.sortWith(compareBy({ it.top }, { it.left }))

        return allBlocks.joinToString("\n\n") { it.text }
    }

    private data class BlockInfo(
        val text: String,
        val top: Int,
        val left: Int
    )

    fun close() {
        try {
            latinRecognizer.close()
            devanagariRecognizer.close()
        } catch (_: Exception) {}
    }
}
