package io.github.clfundev.better.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.clfundev.better.R
import io.github.clfundev.better.application.BetterApplication
import io.github.clfundev.better.data.network.api.UserApi
import io.github.clfundev.better.data.network.model.LoginRequest
import io.github.clfundev.better.data.pref.AppPreferences
import io.github.clfundev.better.utils.Message
import io.github.clfundev.better.utils.NetworkMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    private val appPreferences: AppPreferences,
    private val userApi: UserApi,
    networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _loginState = MutableStateFlow(LoginState.IDLE)
    private val _messages: MutableStateFlow<List<Message>> = MutableStateFlow(emptyList())
    private val _account = MutableStateFlow("")
    private val _password = MutableStateFlow("")

    val loginState = _loginState.asStateFlow()
    val messages = _messages.asStateFlow()
    val account = _account.asStateFlow()
    val password = _password.asStateFlow()
    val loginEnabled = combine(loginState, account, password) { loginState, account, password ->
        loginState == LoginState.IDLE
                && account.trim().isNotEmpty()
                && password.trim().isNotEmpty()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    private val isOffline = networkMonitor.isOnline
        .map { !it }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    fun login() {
        if (!loginEnabled.value) {
            return
        }
        if (isOffline.value) {
            showMessage(Message(resId = R.string.all_no_network))
            return
        }
        _loginState.value = LoginState.LOGGING_IN
        viewModelScope.launch {
            try {
                val resp = userApi.login(LoginRequest(account.value, password.value))
                if (resp.isSuccessful()) {
                    appPreferences.accessToken = resp.data?.accessToken
                    appPreferences.refreshToken = resp.data?.refreshToken
                    _loginState.value = LoginState.LOGGED_IN
                } else {
                    showMessage(Message(plainText = "login error: ${resp.message}"))
                    _loginState.value = LoginState.IDLE
                }
            } catch (e: Exception) {
                showMessage(Message(plainText = "login error: ${e.message}"))
                _loginState.value = LoginState.IDLE
            }
        }
    }

    fun onAccountChange(account: String) {
        _account.value = account
    }

    fun onPasswordChange(password: String) {
        _password.value = password
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
                val application = this[APPLICATION_KEY] as BetterApplication
                val appContainer = application.appContainer
                LoginViewModel(
                    appPreferences = appContainer.appPreferences,
                    userApi = appContainer.retrofitClient.userApi,
                    networkMonitor = NetworkMonitor(application)
                )
            }
        }
    }
}

enum class LoginState {
    IDLE,
    LOGGING_IN,
    LOGGED_IN
}