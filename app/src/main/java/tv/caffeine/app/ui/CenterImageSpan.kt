package tv.caffeine.app.ui

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.text.style.DynamicDrawableSpan

class CenterImageSpan(drawable: Drawable) : DynamicDrawableSpan(ALIGN_BASELINE) {
    private val _drawable = drawable

    override fun getDrawable() = _drawable

    override fun getSize(paint: Paint, text: CharSequence?, start: Int, end: Int, fm: Paint.FontMetricsInt?): Int {
        val rect = _drawable.bounds
        fm?.apply {
            ascent = -rect.bottom
            descent = 0
            top = ascent
            bottom = 0
        }
        return rect.right
    }

    override fun draw(canvas: Canvas, text: CharSequence?, start: Int, end: Int, x: Float, top: Int, y: Int, bottom: Int, paint: Paint) {
        canvas.save()
        val fm = paint.fontMetricsInt
        val transY = bottom - (_drawable.bounds.bottom / 2) - (fm.descent - (fm.ascent / 2))
        canvas.translate(x, transY.toFloat())
        _drawable.draw(canvas)
        canvas.restore()
    }
}
