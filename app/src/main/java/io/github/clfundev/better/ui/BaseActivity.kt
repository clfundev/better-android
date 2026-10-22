package io.github.clfundev.better.ui

import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

// 重写setContentView以启用EdgeToEdge
// TODO: 目前只支持setContentView(View)一种重载
open class BaseActivity : AppCompatActivity() {
    override fun setContentView(view: View?) {
        view?.let {
            enableEdgeToEdge()
            window.setNavigationBarContrastEnforced(false)
            ViewCompat.setOnApplyWindowInsetsListener(it) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }
        super.setContentView(view)
    }
}
