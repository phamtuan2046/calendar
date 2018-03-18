package com.phamtuan.calendar.util

import android.content.res.Resources

/**
 * Created by Dell on 1/30/2018.
 */
object DimensionUtils {
    /**
     * This method converts dp unit to equivalent pixels, depending on device density.
     *
     * @param dp A value in dp (density independent pixels) unit. Which we need to convert into pixels
     * @return A float value to represent px equivalent to dp depending on device density
     */
    fun dpToPx(dp: Float): Float {
        return (dp * Resources.getSystem().displayMetrics.density)
    }

    /**
     * This method converts device specific pixels to density independent pixels.
     *
     * @param px A value in px (pixels) unit. Which we need to convert into db
     * @return A float value to represent dp equivalent to px value
     */
    fun pxToDp(px: Float): Float {
        return (px / Resources.getSystem().displayMetrics.density)
    }
}