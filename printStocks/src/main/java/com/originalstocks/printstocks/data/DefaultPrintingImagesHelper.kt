package com.originalstocks.printstocks.data

import android.graphics.Bitmap
import com.originalstocks.printstocks.utilities.ImageUtils

/**
 * Base class for Default Image Helper which converts [bitmap] into [ByteArray]
 * */

class DefaultPrintingImagesHelper : PrintingImagesHelper {
    override fun getBitmapAsByteArray(bitmap: Bitmap): ByteArray {
        return ImageUtils.decodeBitmap(bitmap)!!
    }

}