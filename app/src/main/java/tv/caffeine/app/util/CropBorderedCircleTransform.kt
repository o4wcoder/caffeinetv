package tv.caffeine.app.util

import android.graphics.*
import androidx.annotation.ColorInt
import com.squareup.picasso.Transformation

class CropBorderedCircleTransformation(@ColorInt color: Int, strokeWidth: Float) : Transformation {
    private val borderPaint = Paint().apply {
        this.color = color
        style = Paint.Style.STROKE
        isAntiAlias = true
        this.strokeWidth = strokeWidth
    }

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

        // draw border
        canvas.drawCircle(r, r, r, borderPaint)

        source.recycle()

        return bitmap
    }

    override fun key(): String {
        return "CropCircleTransformation()"
    }
}