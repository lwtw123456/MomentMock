package com.example.momentmock

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.ImageView
import kotlin.math.roundToInt

fun Context.dp(value: Float): Int =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics)
        .roundToInt()

fun Context.dpf(value: Float): Float =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics)

fun Context.spInt(value: Float): Int =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, resources.displayMetrics)
        .roundToInt()

fun Context.textPx(value: Float, sp: Boolean = false): Float {
    val m = resources.displayMetrics
    val px = value * (if (sp) m.scaledDensity else m.density)
    return (if (px >= 0f) (px + 0.5f) else (px - 0.5f)).toInt().toFloat()
}

class RoundedImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : ImageView(context, attrs, defStyle) {

    var cornerRadius: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    var cornerFactor: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    private val clip = Path()

    override fun onDraw(canvas: Canvas) {
        if (width == 0 || height == 0) return
        clip.reset()
        val r = if (cornerFactor > 0f) width * cornerFactor else cornerRadius
        clip.addRoundRect(
            0f, 0f, width.toFloat(), height.toFloat(),
            r, r, Path.Direction.CW
        )
        val save = canvas.save()
        canvas.clipPath(clip)
        super.onDraw(canvas)
        canvas.restoreToCount(save)
    }
}

class SquareImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : ImageView(context, attrs, defStyle) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec)
        setMeasuredDimension(measuredWidth, measuredWidth)
    }
}
