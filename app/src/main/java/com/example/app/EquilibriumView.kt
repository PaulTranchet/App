package com.example.app

import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.util.AttributeSet
import android.view.View

class EquilibriumView(context: Context, attributes: AttributeSet) : View(context, attributes) {
    var xTilt = 0F
    var yTilt = 0F


    private var paint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.BLACK
    }
    private var backgroundPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.LTGRAY
    }

    val size = 200
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val finalMeasureSpec = MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY)
        super.onMeasure(finalMeasureSpec, finalMeasureSpec)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        var center = size / 2
        var halfWidth = 12F
        var baseOffset = 25F
        var topOffset = 82F

        canvas!!.drawCircle(center.toFloat(), center.toFloat(), size.toFloat() / 2, backgroundPaint) // background circle

        canvas!!.drawRoundRect(center - halfWidth, center + topOffset, center + halfWidth, center + baseOffset, 4F, 4F, paint) // bottom
        canvas!!.drawRoundRect(center - halfWidth, center - topOffset, center + halfWidth, center - baseOffset, 4F, 4F, paint) // top
        canvas!!.drawRoundRect(center - topOffset, center + halfWidth, center - baseOffset, center - halfWidth, 4F, 4F, paint) // left
        canvas!!.drawRoundRect(center + topOffset, center + halfWidth, center + baseOffset, center - halfWidth, 4F, 4F, paint) // right
    }
}