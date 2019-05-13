package com.marchuck.latlngboundsfeature.ui

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ProgressBar

class OverlayView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {
    init {
        setBackgroundColor(Color.BLACK)
        alpha = 0.8f
        setOnTouchListener { _, _ -> true }

        addView(
            ProgressBar(context),
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER or Gravity.CENTER_HORIZONTAL or Gravity.CENTER_VERTICAL
            }
        )
    }
}