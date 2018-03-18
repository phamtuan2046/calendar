package com.phamtuan.calendar.wigets

import android.content.Context
import android.content.res.ColorStateList
import android.support.design.widget.FloatingActionButton
import android.util.AttributeSet
import com.simplemobiletools.commons.extensions.applyColorFilter
import com.simplemobiletools.commons.extensions.getContrastColor

/**
 * Created by P.Tuan on 12/25/2017.
 */
class CustomFloatingActionButton: FloatingActionButton {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun setColors(textColor: Int, accentColor: Int, backgroundColor: Int) {
        backgroundTintList = ColorStateList.valueOf(accentColor)
        applyColorFilter(accentColor.getContrastColor())
    }
}