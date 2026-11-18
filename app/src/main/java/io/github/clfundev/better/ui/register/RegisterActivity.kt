package io.github.clfundev.better.ui.register

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import io.github.clfundev.better.R
import io.github.clfundev.better.databinding.ActivityRegisterBinding
import io.github.clfundev.better.ui.BaseActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class RegisterActivity : BaseActivity() {
    private val vm: RegisterViewModel by viewModels { RegisterViewModel.factory }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ivRegisterBack.setOnClickListener { finish() }

        binding.btnRegisterSignup.setOnClickListener {
            var isValid = true
            binding.tilRegisterAccount.error = if (vm.isAccountValid) {
                null
            } else {
                isValid = false
                getString(R.string.register_invalid_account)
            }
            binding.tilRegisterPassword.error = if (vm.isPasswordValid) {
                null
            } else {
                isValid = false
                getString(R.string.register_invalid_password)
            }
            binding.tilRegisterConfirmpassword.error = if (vm.isConfirmPasswordValid) {
                null
            } else {
                isValid = false
                getString(R.string.register_mismatch_password)
            }
            binding.tilRegisterNickname.error = if (vm.isNicknameValid) {
                null
            } else {
                isValid = false
                getString(R.string.register_invalid_nickname)
            }
            if (isValid) {
                vm.register()
            }
        }

        binding.tilRegisterAccount.editText?.apply {
            onFocusChangeListener = { _, hasFocus ->
                if (!hasFocus) {
                    binding.tilRegisterAccount.error = if (vm.isAccountValid) {
                        null
                    } else {
                        getString(R.string.register_invalid_account)
                    }
                }
            }

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

        binding.tilRegisterPassword.editText?.apply {
            onFocusChangeListener = { _, hasFocus ->
                if (!hasFocus) {
                    binding.tilRegisterPassword.error = if (vm.isPasswordValid) {
                        null
                    } else {
                        getString(R.string.register_invalid_password)
                    }
                    // "确认密码"的合法性与"密码"相关，因此也要校验
                    binding.tilRegisterConfirmpassword.error = if (vm.isConfirmPasswordValid) {
                        null
                    } else {
                        getString(R.string.register_mismatch_password)
                    }
                }
            }

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
        }

        binding.tilRegisterConfirmpassword.editText?.apply {
            onFocusChangeListener = { _, hasFocus ->
                if (!hasFocus) {
                    binding.tilRegisterConfirmpassword.error = if (vm.isConfirmPasswordValid) {
                        null
                    } else {
                        getString(R.string.register_mismatch_password)
                    }
                }
            }

            doAfterTextChanged {
                vm.onConfirmPasswordChange(text.toString())
            }

            lifecycleScope.launch {
                vm.confirmPassword.collectLatest {
                    if (it != text.toString()) {
                        setText(it)
                    }
                }
            }
        }

        binding.tilRegisterNickname.editText?.apply {
            onFocusChangeListener = { _, hasFocus ->
                if (!hasFocus) {
                    binding.tilRegisterNickname.error = if (vm.isNicknameValid) {
                        null
                    } else {
                        getString(R.string.register_invalid_nickname)
                    }
                }
            }

            doAfterTextChanged {
                vm.onNicknameChange(text.toString())
            }

            lifecycleScope.launch {
                vm.nickname.collectLatest {
                    if (it != text.toString()) {
                        setText(it)
                    }
                }
            }
        }

        binding.tilRegisterProfile.editText?.apply {
            doAfterTextChanged {
                vm.onProfileChange(text.toString())
            }

            lifecycleScope.launch {
                vm.profile.collectLatest {
                    if (it != text.toString()) {
                        setText(it)
                    }
                }
            }
        }

        lifecycleScope.launch {
            vm.registerState.collectLatest { state ->
                binding.lpiRegister.visibility =
                    (if (state == RegisterState.REGISTERING) View.VISIBLE else View.GONE)
                if (state == RegisterState.REGISTERED) {
                    Toast.makeText(
                        this@RegisterActivity,
                        R.string.register_succeeded,
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }
        }

        lifecycleScope.launch {
            vm.messages.collectLatest { messages ->
                messages.firstOrNull()?.let {
                    it.showAsShortToast(this@RegisterActivity)
                    vm.messageShown(it)
                }
            }
        }
    }
}