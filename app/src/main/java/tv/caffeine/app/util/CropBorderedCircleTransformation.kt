package tv.caffeine.app.util

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import androidx.annotation.ColorInt
import com.squareup.picasso.Transformation

class CropBorderedCircleTransformation(@ColorInt color: Int, strokeWidth: Float, @ColorInt gapColor: Int? = null) : Transformation {
    private val borderPaint = buildBorderPaint(color, strokeWidth)
    private val gapBorderPaint = gapColor?.let { buildBorderPaint(it, strokeWidth) }
    private val key = "CropBorderedCircleTransformation($color,$strokeWidth)"

    override fun transform(source: Bitmap): Bitmap {
        val size = Math.min(source.width, source.height)

        val width = (source.width - size) / 2
        val height = (source.height - size) / 2

        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(bitmap)
        val paint = Paint()
        val shader = BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        if (width != 0 || height != 0) {
            // source isn't square, move viewport to center
            val matrix = Matrix()
            matrix.setTranslate((-width).toFloat(), (-height).toFloat())
            shader.setLocalMatrix(matrix)
        }
        paint.shader = shader
        paint.isAntiAlias = true

        val r = size / 2f
        canvas.drawCircle(r, r, r, paint)

        // draw gap
        gapBorderPaint?.let {
            canvas.drawCircle(r, r, (r - it.strokeWidth * 1.5).toFloat(), it)
        }

        // draw border
        canvas.drawCircle(r, r, r - borderPaint.strokeWidth / 2, borderPaint)

        source.recycle()

        return bitmap
    }

    override fun key(): String = key

    private fun buildBorderPaint(@ColorInt color: Int, strokeWidth: Float) = Paint().apply {
        this.color = color
        style = Paint.Style.STROKE
        isAntiAlias = true
        this.strokeWidth = strokeWidth
    }
}
