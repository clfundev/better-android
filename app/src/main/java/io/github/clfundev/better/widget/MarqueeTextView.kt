package io.github.clfundev.better.widget

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class MarqueeTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.textViewStyle
) : AppCompatTextView(context, attrs, defStyleAttr) {
    init {
        // 启用走马灯效果
        ellipsize = TextUtils.TruncateAt.MARQUEE
        setSingleLine(true)
        marqueeRepeatLimit = -1 // 走马灯动画无限循环
    }

    override fun isFocused(): Boolean {
        return true // 该方法返回true时，走马灯效果才能开启
    }
}
