package com.phamtuan.calendar.wigets

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView

/**
 * Created by P.Tuan on 12/25/2017.
 */
class CustomTextView: TextView{
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun setColors(textColor: Int, accentColor: Int, backgroundColor: Int) {
        setTextColor(textColor)
        setLinkTextColor(accentColor)
    }
}