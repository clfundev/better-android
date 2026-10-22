package io.github.clfundev.better.utils

import android.content.Context
import android.widget.Toast
import java.util.UUID

/**
 * ViewModel产生的消息，消息内容优先采用字符串资源，其次采用普通字符串
 */
data class Message(
    val id: Long = UUID.randomUUID().mostSignificantBits,
    private val resId: Int? = null,
    private val plainText: String = ""
) {
    fun getText(context: Context) = resId?.let { context.getString(it) } ?: plainText

    fun showAsShortToast(context: Context) =
        Toast.makeText(context, getText(context), Toast.LENGTH_SHORT).show()
}