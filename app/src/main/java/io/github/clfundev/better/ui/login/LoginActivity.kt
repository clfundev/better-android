package io.github.clfundev.better.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import io.github.clfundev.better.R
import io.github.clfundev.better.databinding.ActivityLoginBinding
import io.github.clfundev.better.ui.BaseActivity
import io.github.clfundev.better.ui.register.RegisterActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LoginActivity : BaseActivity() {
    private val vm: LoginViewModel by viewModels { LoginViewModel.factory }
    private lateinit var binding: ActivityLoginBinding

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tilLoginAccount.editText?.apply {
            doAfterTextChanged {
                vm.onAccountChange(text.toString())
            }

            lifecycleScope.launch {
                vm.account.collectLatest {
                    if (it != text.toString()) {
                        setText(it)
                    }
                }
            }
        }

        binding.tilLoginPassword.editText?.apply {
            doAfterTextChanged {
                vm.onPasswordChange(text.toString())
            }

            lifecycleScope.launch {
                vm.password.collectLatest {
                    if (it != text.toString()) {
                        setText(it)
                    }
                }
            }

            // 监听输入法完成键
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    vm.login()
                }
                false
            }
        }

        binding.btnLoginLogin.setOnClickListener { vm.login() }

        binding.btnLoginSignup.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.tvLoginSkip.setOnClickListener { finish() }

        lifecycleScope.launch {
            vm.loginState.collectLatest { state ->
                binding.btnLoginSignup.isEnabled = state == LoginState.IDLE
                binding.lpiLogin.visibility =
                    if (state == LoginState.LOGGING_IN) View.VISIBLE else View.GONE
                if (state == LoginState.LOGGED_IN) {
                    Toast.makeText(
                        this@LoginActivity,
                        R.string.login_succeeded,
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }
        }

        lifecycleScope.launch {
            vm.messages.collectLatest { messages ->
                messages.firstOrNull()?.let {
                    it.showAsShortToast(this@LoginActivity)
                    vm.messageShown(it)
                }
            }
        }

        lifecycleScope.launch {
            vm.loginEnabled.collectLatest { loginEnabled ->
                binding.btnLoginLogin.isEnabled = loginEnabled
            }
        }
    }
}