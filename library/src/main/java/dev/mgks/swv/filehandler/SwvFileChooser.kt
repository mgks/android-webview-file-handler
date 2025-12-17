package dev.mgks.swv.filehandler

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Smart WebView File Handler
 * Handles file uploads, camera captures, and gallery selections for Android WebViews.
 */
class SwvFileChooser @JvmOverloads constructor(
    private val activity: Activity,
    private val config: Config = Config()
) {

    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private var cameraPhotoPath: String? = null
    private var cameraVideoPath: String? = null

    /**
     * Configuration class to toggle features.
     * Java users can build this easily.
     */
    data class Config @JvmOverloads constructor(
        @JvmField var allowCamera: Boolean = true,
        @JvmField var allowGallery: Boolean = true,
        @JvmField var allowMultiple: Boolean = true,
        @JvmField var useFileProvider: Boolean = true,
        @JvmField var authority: String? = null // Auto-detected if null, or set manually
    )

    /**
     * Call this method from your WebChromeClient's onShowFileChooser.
     *
     * @return true if the request is handled, false otherwise.
     */
    fun onShowFileChooser(
        filePathCallback: ValueCallback<Array<Uri>>,
        fileChooserParams: WebChromeClient.FileChooserParams
    ): Boolean {
        // Cancel any pending callbacks to avoid hanging the previous request
        this.filePathCallback?.onReceiveValue(null)
        this.filePathCallback = filePathCallback

        val acceptTypes = fileChooserParams.acceptTypes
        var allowImage = false
        var allowVideo = false

        // Basic MIME type checking
        if (acceptTypes.any { it.contains("image") } || acceptTypes.isEmpty() || acceptTypes[0].isEmpty()) {
            allowImage = true
        }
        if (acceptTypes.any { it.contains("video") } || acceptTypes.isEmpty() || acceptTypes[0].isEmpty()) {
            allowVideo = true
        }

        val intentList = ArrayList<Intent>()

        // 1. Add Camera Intents (if enabled)
        if (config.allowCamera) {
            if (allowImage) {
                val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                if (takePictureIntent.resolveActivity(activity.packageManager) != null) {
                    try {
                        val photoFile = createImageFile()
                        takePictureIntent.putExtra("PhotoPath", cameraPhotoPath)
                        val photoURI = getUriForFile(photoFile)
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                        intentList.add(takePictureIntent)
                    } catch (ex: IOException) {
                        Log.e("SwvFileChooser", "Image file creation failed", ex)
                    }
                }
            }
            if (allowVideo) {
                val takeVideoIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
                if (takeVideoIntent.resolveActivity(activity.packageManager) != null) {
                    try {
                        val videoFile = createVideoFile()
                        takeVideoIntent.putExtra("VideoPath", cameraVideoPath)
                        val videoURI = getUriForFile(videoFile)
                        takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, videoURI)
                        intentList.add(takeVideoIntent)
                    } catch (ex: IOException) {
                        Log.e("SwvFileChooser", "Video file creation failed", ex)
                    }
                }
            }
        }

        // 2. Add Gallery Intent (if enabled)
        val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)
        contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
        contentSelectionIntent.type = "*/*"
        if (acceptTypes.isNotEmpty() && acceptTypes[0].isNotEmpty()) {
            contentSelectionIntent.putExtra(Intent.EXTRA_MIME_TYPES, acceptTypes)
        }
        if (config.allowMultiple) {
            contentSelectionIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }

        // 3. Combine Intents
        val chooserIntent = Intent(Intent.ACTION_CHOOSER)
        chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
        chooserIntent.putExtra(Intent.EXTRA_TITLE, "Select File")
        if (intentList.isNotEmpty()) {
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentList.toTypedArray())
        }

        try {
            activity.startActivityForResult(chooserIntent, REQUEST_CODE)
        } catch (e: Exception) {
            Log.e("SwvFileChooser", "Cannot open file chooser", e)
            return false
        }

        return true
    }

    /**
     * Call this method from your Activity's onActivityResult.
     */
    fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?): Boolean {
        if (requestCode != REQUEST_CODE) return false

        if (filePathCallback == null) return true

        var results: Array<Uri>? = null

        if (resultCode == Activity.RESULT_OK) {
            if (intent?.data != null) {
                // Single file from gallery
                results = arrayOf(intent.data!!)
            } else if (intent?.clipData != null) {
                // Multiple files from gallery
                val clip = intent.clipData!!
                results = Array(clip.itemCount) { i -> clip.getItemAt(i).uri }
            } else if (cameraPhotoPath != null) {
                // Photo from camera
                val file = File(cameraPhotoPath!!.replace("file:", ""))
                if (file.exists()) {
                    results = arrayOf(Uri.fromFile(file))
                }
            } else if (cameraVideoPath != null) {
                // Video from camera
                val file = File(cameraVideoPath!!.replace("file:", ""))
                if (file.exists()) {
                    results = arrayOf(Uri.fromFile(file))
                }
            }
        }

        filePathCallback?.onReceiveValue(results)
        filePathCallback = null
        cameraPhotoPath = null
        cameraVideoPath = null
        return true
    }

    private fun getUriForFile(file: File): Uri {
        val authority = config.authority ?: "${activity.packageName}.fileprovider"
        return FileProvider.getUriForFile(activity, authority, file)
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
        cameraPhotoPath = "file:" + image.absolutePath
        return image
    }

    @Throws(IOException::class)
    private fun createVideoFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir = activity.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        val video = File.createTempFile("VID_${timeStamp}_", ".mp4", storageDir)
        cameraVideoPath = "file:" + video.absolutePath
        return video
    }

    companion object {
        const val REQUEST_CODE = 1001
    }
}