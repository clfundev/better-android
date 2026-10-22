package io.github.clfundev.better.utils

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.util.TypedValue
import androidx.core.content.ContextCompat
import io.github.clfundev.better.application.AppContainer
import io.github.clfundev.better.application.BetterApplication
import com.google.android.material.color.MaterialColors

fun Context.getAppContainer(): AppContainer = (applicationContext as BetterApplication).appContainer

fun dp2px(context: Context, dpValue: Float): Int =
    TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dpValue + 0.5f,
        context.resources.displayMetrics
    ).toInt()

fun sp2px(context: Context, spValue: Float): Int =
    TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        spValue + 0.5f,
        context.resources.displayMetrics
    ).toInt()

fun String.isValidIp(): Boolean {
    val num = "([01]?\\d\\d?|2[0-4]\\d|25[0-5])"
    val ip = "^$num\\.$num\\.$num\\.$num$"
    return matches(ip.toRegex())
}

fun Paint.FontMetrics.getTextHeight(): Float {
    return descent - ascent
}

fun Paint.FontMetrics.getLineHeight(): Float {
    return bottom - top
}

/**
 * 计算文字垂直居中于 [y] 时的基线y坐标
 *
 * @param y 垂直居中位置
 * @return 文字垂直居中于 [y] 时的基线y坐标
 */
fun Paint.FontMetrics.getBaselineForCenteredOn(y: Float): Float {
    return y - (ascent + descent) / 2f
}

/**
 * 获取 `R.attr` 中的颜色
 */
fun Context.getThemeColor(resId: Int): Int {
    return MaterialColors.getColor(this, resId, Color.BLACK)
}

/**
 * 获取 `R.color` 中的颜色
 */
fun Context.getSimpleColor(resId: Int): Int {
    return ContextCompat.getColor(this, resId)
}