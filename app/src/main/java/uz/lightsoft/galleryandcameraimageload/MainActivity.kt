package uz.lightsoft.galleryandcameraimageload

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File


class MainActivity : AppCompatActivity() {

    private val operationCaptureImage = 1
    private val operationChooseImage = 2
    private val cameraPermissionCode = 4
    private val galleryPermissionCode = 5

    private val capturedImage: File by lazy {
        File(
            getOutputDirectory(this),
            System.currentTimeMillis().toString() + ".jpg"
        )
    }
    private var mUri: Uri? = null

    private val permissionStorage = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnCamera).setOnClickListener {
            val checkSelfPermission =
                ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            if (checkSelfPermission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    cameraPermissionCode
                )
            } else {
                capturePhoto()
            }
        }

        findViewById<Button>(R.id.btnGallery).setOnClickListener {
            val checkSelfPermission =
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            if (checkSelfPermission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    permissionStorage,
                    galleryPermissionCode
                )
            } else {
                openGallery()
            }
        }
    }

    private fun capturePhoto() {
        if (capturedImage.exists()) {
            capturedImage.delete()
        }
        capturedImage.createNewFile()
        mUri = if (Build.VERSION.SDK_INT >= 24) {
            FileProvider.getUriForFile(
                this, "uz.lightsoft.galleryandcameraimageload.fileprovider",
                capturedImage
            )
        } else {
            Uri.fromFile(capturedImage)
        }
        val intent = Intent("android.media.action.IMAGE_CAPTURE")
        intent.putExtra(MediaStore.EXTRA_OUTPUT, mUri)
        startActivityForResult(intent, operationCaptureImage)
    }

    private fun openGallery() {
        /*val intent = Intent("android.intent.action.GET_CONTENT")
        intent.type = "image/*"
        startActivityForResult(intent, operationChooseImage)*/
         */
        val pickPhoto = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(pickPhoto, operationChooseImage)
    }

    @SuppressLint("Range")
    private fun getImagePath(uri: Uri?, selection: String?): String {
        var path: String? = null
        val cursor = contentResolver.query(uri!!, null, selection, null, null)
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA))
            }
            cursor.close()
        }
        return path!!
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            operationCaptureImage ->
                if (resultCode == Activity.RESULT_OK) {
                    val resizePath = resizeImage(capturedImage.absolutePath)
                    Log.d("TTT", "Camera $resizePath")
                    // todo upload capture image to server
                }
            operationChooseImage ->
                if (resultCode == Activity.RESULT_OK) {
                    imagePathHandler(data)
                }
        }
    }

    private fun imagePathHandler(data: Intent?) {
        var imagePath: String? = null
        val uri = data!!.data
        if (DocumentsContract.isDocumentUri(this, uri)) {
            val docId = DocumentsContract.getDocumentId(uri)
            if ("com.android.providers.media.documents" == uri?.authority) {
                val id = docId.split(":")[1]
                val selsetion = MediaStore.Images.Media._ID + "=" + id
                imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selsetion)
            } else if ("com.android.providers.downloads.documents" == uri?.authority) {
                val contentUri = ContentUris.withAppendedId(
                    Uri.parse(
                        "content://downloads/public_downloads"
                    ), java.lang.Long.valueOf(docId)
                )
                imagePath = getImagePath(contentUri, null)
            }
        } else if ("content".equals(uri?.scheme, ignoreCase = true)) {
            imagePath = getImagePath(uri, null)
        } else if ("file".equals(uri?.scheme, ignoreCase = true)) {
            imagePath = uri?.path
        }
        renderImage(imagePath)
    }

    private fun renderImage(imagePath: String?) {
        if (imagePath != null) {
            val bitmap = BitmapFactory.decodeFile(imagePath)

            findViewById<ImageView>(R.id.imageView).setImageBitmap(bitmap)
            Log.d("TTT", "Gallery $imagePath")
            // todo upload gallery image to server
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantedResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantedResults)
        when (requestCode) {
            galleryPermissionCode -> {
                if (grantedResults.isNotEmpty() && grantedResults[0] ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    openGallery()
                } else {
                    Toast.makeText(
                        this,
                        "Unfortunately You are Denied Permission to Perform this Operataion.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            cameraPermissionCode -> {
                if (grantedResults.isNotEmpty() && grantedResults[0] == PackageManager.PERMISSION_GRANTED) {
                    capturePhoto()
                } else {
                    Toast.makeText(
                        this,
                        "Unfortunately You are Denied Permission to Perform this Operataion.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun resizeImage(selectedImage: String): String? {
        val pictureFile: File?
        val resizeImageFile = File(
            getOutputDirectory(this),
            "1sonia" + System.currentTimeMillis() / 100 + ".jpg"
        )
        val resizeImagePath: String = resizeImageFile.toString()
        pictureFile = File(selectedImage)
        if (pictureFile.exists()) {
            val imageBitmap = BitmapFactory.decodeFile(pictureFile.absolutePath)
//            getResizedImage(imageBitmap, resizeImagePath)
            findViewById<ImageView>(R.id.imageView).setImageBitmap(imageBitmap)
            return resizeImagePath
        }
        return null
    }
}