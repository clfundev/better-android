package io.github.clfundev.better.ui.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.clfundev.better.application.BetterApplication
import io.github.clfundev.better.data.network.api.UserApi
import io.github.clfundev.better.data.network.model.RegisterRequest
import io.github.clfundev.better.utils.Message
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RegisterViewModel(private val userApi: UserApi) : ViewModel() {
    private val _registerState = MutableStateFlow(RegisterState.IDLE)
    private val _messages: MutableStateFlow<List<Message>> = MutableStateFlow(emptyList())
    private val _account = MutableStateFlow("")
    private val _password = MutableStateFlow("")
    private val _confirmPassword = MutableStateFlow("")
    private val _nickname = MutableStateFlow("")
    private val _profile = MutableStateFlow("")

    val registerState = _registerState.asStateFlow()
    val messages = _messages.asStateFlow()
    val account = _account.asStateFlow()
    val password = _password.asStateFlow()
    val confirmPassword = _confirmPassword.asStateFlow()
    val nickname = _nickname.asStateFlow()
    val profile = _profile.asStateFlow()

    val isAccountValid get() = _account.value.trim().isNotEmpty()
    val isPasswordValid get() = _password.value.trim().isNotEmpty()
    val isConfirmPasswordValid get() = _confirmPassword.value == _password.value
    val isNicknameValid get() = _nickname.value.trim().isNotEmpty()

    fun register() {
        if (_registerState.value != RegisterState.IDLE) {
            return
        }
        // 检查所有输入合法性
        if (!isAccountValid || !isPasswordValid ||
            !isConfirmPasswordValid || !isNicknameValid
        ) {
            _registerState.value = RegisterState.IDLE
            return
        }
        _registerState.value = RegisterState.REGISTERING
        viewModelScope.launch {
            try {
                val req =
                    RegisterRequest(account.value, password.value, nickname.value, profile.value)
                val resp = userApi.register(req)
                if (resp.isSuccessful()) {
                    _registerState.value = RegisterState.REGISTERED
                } else {
                    showMessage(Message(plainText = "register error: ${resp.message}"))
                    _registerState.value = RegisterState.IDLE
                }
            } catch (e: Exception) {
                showMessage(Message(plainText = "register error: ${e.message}"))
                _registerState.value = RegisterState.IDLE
            }
        }
    }

    fun onAccountChange(account: String) {
        _account.value = account
    }

    fun onPasswordChange(password: String) {
        _password.value = password
    }

    fun onConfirmPasswordChange(confirmPassword: String) {
        _confirmPassword.value = confirmPassword
    }

    fun onNicknameChange(nickname: String) {
        _nickname.value = nickname
    }

    fun onProfileChange(profile: String) {
        _profile.value = profile
    }

    private fun showMessage(message: Message) {
        _messages.update { currentMessages ->
            currentMessages + message
        }
    }

    /**
     * 暂时性展示的信息（比如Toast），展示过后UI应通知ViewModel信息已经展示，否则配置更改后信息会重复展示
     * (参考自https://developer.android.google.cn/topic/architecture/ui-layer/events#consuming-trigger-updates)
     */
    fun messageShown(message: Message) {
        _messages.update { currentMessages ->
            currentMessages.filterNot { it.id == message.id }
        }
    }

    companion object {
        val factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val appContainer = (this[APPLICATION_KEY] as BetterApplication).appContainer
                RegisterViewModel(
                    userApi = appContainer.retrofitClient.userApi
                )
            }
        }
    }
}

enum class RegisterState {
    IDLE,
    REGISTERING,
    REGISTERED
}