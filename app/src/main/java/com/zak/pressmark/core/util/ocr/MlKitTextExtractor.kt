package com.zak.pressmark.core.util.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class MlKitTextExtractor(
    private val appContext: Context,
) : TextExtractor {
    override suspend fun extract(imageUri: Uri, hint: OcrHint?): Result<OcrResult> {
        return runCatching {
            val image = InputImage.fromFilePath(appContext, imageUri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val text = suspendCancellableCoroutine { cont ->
                recognizer.process(image)
                    .addOnSuccessListener { result ->
                        cont.resume(result)
                    }
                    .addOnFailureListener { error ->
                        cont.resumeWith(Result.failure(error))
                    }
            }
            val lines = text.textBlocks
                .flatMap { it.lines }
                .map { it.text.trim() }
                .filter { it.isNotBlank() }

            OcrResult(
                rawText = text.text,
                lines = lines,
            )
        }
    }
}
