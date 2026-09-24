package com.sleepcontroller.util

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.tasks.await
import java.io.InputStream

/**
 * EXIF metadata extractor and face detection for photo task verification.
 *
 * Validates that a photo:
 *  1. Was taken recently (within the configured recency window)
 *  2. Is a genuine camera photo (has EXIF datetime, not a screenshot or old file)
 *  3. Contains a human face (selfie while doing the task)
 *
 * Uses AndroidX ExifInterface for EXIF and ML Kit (thin/Play Services) for face detection.
 */
object ExifVerifier {

    private const val TAG = "ExifVerifier"

    /** Maximum age of a photo in milliseconds to be considered "fresh" */
    private const val MAX_PHOTO_AGE_MS = 10 * 60 * 1000L  // 10 minutes

    data class VerificationResult(
        val isValid: Boolean,
        val reason: String,
        val dateTaken: Long? = null,
        val cameraModel: String? = null,
        val imageWidth: Int? = null,
        val imageHeight: Int? = null
    )

    /**
     * Verify a photo URI's EXIF data and face presence.
     *
     * Verification chain:
     *  1. EXIF timestamp exists? (rejects screenshots/downloads)
     *  2. Photo taken within 10 min? (rejects old photos)
     *  3. Timestamp not in future? (rejects clock manipulation)
     *  4. Face detected? (rejects photos of walls/ceilings)
     *
     * @param context Application context
     * @param photoUri URI of the photo to verify
     * @param maxAgeMs Maximum age in milliseconds (default 10 minutes)
     * @return VerificationResult with pass/fail and metadata
     */
    suspend fun verifyPhoto(
        context: Context,
        photoUri: Uri,
        maxAgeMs: Long = MAX_PHOTO_AGE_MS
    ): VerificationResult {
        // Step 1-3: EXIF verification
        val exifResult = verifyExif(context, photoUri, maxAgeMs)
        if (!exifResult.isValid) return exifResult

        // Step 4: Face detection
        val hasFace = verifyFace(context, photoUri)
        if (!hasFace) {
            return VerificationResult(
                isValid = false,
                reason = "No face detected. Take a selfie while doing your task.",
                dateTaken = exifResult.dateTaken,
                cameraModel = exifResult.cameraModel
            )
        }

        // All checks passed
        return exifResult
    }

    /**
     * Step 1-3: Verify EXIF metadata (timestamp recency).
     */
    private fun verifyExif(
        context: Context,
        photoUri: Uri,
        maxAgeMs: Long
    ): VerificationResult {
        return try {
            val inputStream: InputStream = context.contentResolver.openInputStream(photoUri)
                ?: return VerificationResult(false, "Could not open photo")

            inputStream.use { stream ->
                val exif = ExifInterface(stream)

                // Extract date/time
                val dateTimeOriginal = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                    ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
                    ?: exif.getAttribute(ExifInterface.TAG_DATETIME_DIGITIZED)

                if (dateTimeOriginal == null) {
                    return VerificationResult(
                        isValid = false,
                        reason = "No timestamp found in photo. Please take a fresh photo with your camera."
                    )
                }

                // Parse the EXIF date format: "yyyy:MM:dd HH:mm:ss"
                val dateTakenMs = parseExifDateTime(dateTimeOriginal)
                if (dateTakenMs == null) {
                    return VerificationResult(
                        isValid = false,
                        reason = "Could not parse photo timestamp: $dateTimeOriginal"
                    )
                }

                // Check recency
                val ageMs = System.currentTimeMillis() - dateTakenMs
                if (ageMs > maxAgeMs) {
                    val ageMinutes = ageMs / 60000
                    return VerificationResult(
                        isValid = false,
                        reason = "Photo is ${ageMinutes} minutes old. Please take a fresh photo.",
                        dateTaken = dateTakenMs
                    )
                }

                if (ageMs < -60_000) { // Allow 1 minute clock drift
                    return VerificationResult(
                        isValid = false,
                        reason = "Photo timestamp is in the future. Please take a genuine photo.",
                        dateTaken = dateTakenMs
                    )
                }

                // Extract additional metadata
                val cameraModel = exif.getAttribute(ExifInterface.TAG_MODEL)
                val width = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
                val height = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)

                Log.d(TAG, "EXIF verified: taken=$dateTimeOriginal, model=$cameraModel, ${width}x${height}")

                VerificationResult(
                    isValid = true,
                    reason = "Photo verified ✓",
                    dateTaken = dateTakenMs,
                    cameraModel = cameraModel,
                    imageWidth = if (width > 0) width else null,
                    imageHeight = if (height > 0) height else null
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "EXIF verification failed", e)
            VerificationResult(
                isValid = false,
                reason = "Verification error: ${e.message}"
            )
        }
    }

    /**
     * Step 4: Verify a human face is present in the photo.
     * Uses ML Kit thin model (downloaded via Play Services, ~300KB in APK).
     *
     * This ensures the user took a selfie while doing the task,
     * not a photo of the ceiling/wall.
     */
    private suspend fun verifyFace(context: Context, photoUri: Uri): Boolean {
        return try {
            val image = InputImage.fromFilePath(context, photoUri)
            val options = FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setMinFaceSize(0.15f)  // Face must be at least 15% of image
                .build()
            val detector = FaceDetection.getClient(options)
            val faces = detector.process(image).await()
            detector.close()

            val result = faces.isNotEmpty()
            Log.d(TAG, "Face detection: ${faces.size} face(s) found, valid=$result")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Face detection failed", e)
            // If face detection fails (model not downloaded yet, etc.),
            // fall back to accepting the photo (EXIF already verified)
            true
        }
    }

    /**
     * Parse EXIF datetime string "yyyy:MM:dd HH:mm:ss" to epoch millis.
     */
    private fun parseExifDateTime(dateTime: String): Long? {
        return try {
            val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")
            val localDateTime = java.time.LocalDateTime.parse(dateTime, formatter)
            localDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse EXIF date: $dateTime", e)
            null
        }
    }
}
