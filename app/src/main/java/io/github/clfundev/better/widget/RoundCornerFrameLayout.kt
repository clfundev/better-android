package io.github.clfundev.better.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.widget.FrameLayout
import io.github.clfundev.better.R

class RoundCornerFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val roundCorner: Int
    private val clipRectF = RectF()
    private val pathToClip = Path()

    init {
        context.obtainStyledAttributes(
            attrs, R.styleable.RoundCornerFrameLayout, defStyleAttr, defStyleRes
        ).use { a ->
            roundCorner = a.getDimensionPixelSize(
                R.styleable.RoundCornerFrameLayout_roundCorner, 0
            )
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        canvas.clipPath(pathToClip)
        super.dispatchDraw(canvas)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        clipRectF.set(
            paddingLeft.toFloat(),
            paddingTop.toFloat(),
            (w - paddingRight).toFloat(),
            (h - paddingBottom).toFloat()
        )
        pathToClip.reset()
        pathToClip.addRoundRect(
            clipRectF,
            roundCorner.toFloat(),
            roundCorner.toFloat(),
            Path.Direction.CW
        )
    }
}
