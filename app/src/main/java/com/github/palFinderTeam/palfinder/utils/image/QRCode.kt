package com.github.palFinderTeam.palfinder.utils.image

import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.ceylonlabs.imageviewpopup.ImagePopup
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


object QRCode {
    /**
     * Saves the image as PNG to the app's private external storage folder.
     * @param image Bitmap to save.
     * @return Uri of the saved file or null
     */
    fun saveImageExternal(image: Bitmap, parent: Activity): Uri? {
        val imagesFolder = File(parent.cacheDir, "images")
        var uri: Uri? = null
        try {
            imagesFolder.mkdirs()
            val file = File(imagesFolder, "shared_image.png")
            val stream = FileOutputStream(file)
            image.compress(Bitmap.CompressFormat.PNG, 90, stream)
            stream.flush()
            stream.close()
            uri = FileProvider.getUriForFile(parent, "com.palfinder.fileprovider", file)
        } catch (e: IOException) {
            Log.d(TAG, "IOException while trying to write file for sharing: " + e.message)
        }
        return uri
    }

    /**
     * Shares the PNG image from Uri.
     * @param uri Uri of image to share.
     */
    private fun shareImageUri(image: Bitmap, parent: Activity): Intent? {
        val intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(Intent.EXTRA_STREAM, saveImageExternal(image, parent))
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.type = "image/png"
        return intent
    }

    /**
     * from a bitmap representing a qrcode, creates an imagePopup that display
     * it and allows to share it
     */
    fun shareQRcode(bitmap: Bitmap, parent: Activity) {
        //Set up the popup image
        val imagePopup = ImagePopup(parent)
        //Convert the bitmap(QR Code) into a drawable
        val d: Drawable = BitmapDrawable(parent.resources, bitmap)

        //Displays the popup image
        imagePopup.initiatePopup(d);
        imagePopup.viewPopup()
        parent.startActivity(shareImageUri(bitmap, parent))
    }
}