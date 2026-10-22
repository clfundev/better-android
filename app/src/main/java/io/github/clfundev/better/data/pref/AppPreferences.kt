package io.github.clfundev.better.data.pref

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceManager

/**
 * 以SharedPreferences实现的设置访问类
 */
class AppPreferences(context: Context) {
    private val sharedPreferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)

    var focusDuration: Long
        get() = sharedPreferences.getString(KEY_FOCUS_DURATION, DEFAULT_FOCUS_DURATION)!!.toLong()
        set(value) {
            sharedPreferences.edit {
                putString(KEY_FOCUS_DURATION, value.toString())
            }
        }

    var sound: Boolean
        get() = sharedPreferences.getBoolean(KEY_SOUND, DEFAULT_SOUND)
        set(value) {
            sharedPreferences.edit {
                putBoolean(KEY_SOUND, value)
            }
        }

    var vibration: Boolean
        get() = sharedPreferences.getBoolean(KEY_VIBRATION, DEFAULT_VIBRATION)
        set(value) {
            sharedPreferences.edit {
                putBoolean(KEY_VIBRATION, value)
            }
        }

    var flashlight: Boolean
        get() = sharedPreferences.getBoolean(KEY_FLASHLIGHT, DEFAULT_FLASHLIGHT)
        set(value) {
            sharedPreferences.edit {
                putBoolean(KEY_FLASHLIGHT, value)
            }
        }

    var strictTime: Boolean
        get() = sharedPreferences.getBoolean(KEY_STRICT_TIME, DEFAULT_STRICT_TIME)
        set(value) {
            sharedPreferences.edit {
                putBoolean(KEY_STRICT_TIME, value)
            }
        }

    var keepScreenOn: Boolean
        get() = sharedPreferences.getBoolean(KEY_KEEP_SCREEN_ON, DEFAULT_KEEP_SCREEN_ON)
        set(value) {
            sharedPreferences.edit {
                putBoolean(KEY_KEEP_SCREEN_ON, value)
            }
        }

    var token: String?
        get() = sharedPreferences.getString(KEY_TOKEN, null)
        set(value) {
            sharedPreferences.edit {
                putString(KEY_TOKEN, value)
            }
        }

    companion object {
        // 各设置对应的键
        const val KEY_FOCUS_DURATION: String = "focus_duration"
        const val KEY_SOUND: String = "sound"
        const val KEY_VIBRATION: String = "vibration"
        const val KEY_FLASHLIGHT: String = "flashlight"
        const val KEY_STRICT_TIME: String = "strict_time"
        const val KEY_KEEP_SCREEN_ON: String = "keep_screen_on"
        const val KEY_CLEAR_DATA: String = "clear_data"
        const val KEY_RESET_ALL_SETTINGS: String = "reset_all_settings"
        const val KEY_IGNORE_BATTERY_OPTIMIZATION: String = "ignore_battery_optimization"
        const val KEY_MANAGE_STARTUP_APPS: String = "manage_startup_apps"
        const val KEY_LOGIN: String = "login"
        const val KEY_BACKUP: String = "backup"
        const val KEY_RESTORE: String = "restore"

        // 设置之外使用SharedPreferences存储的数据的键
        const val KEY_TOKEN: String = "token"

        // 设置的默认值，注意要与root_preferences.xml一致
        const val DEFAULT_FOCUS_DURATION: String = "3000000"
        const val DEFAULT_SOUND: Boolean = true
        const val DEFAULT_VIBRATION: Boolean = false
        const val DEFAULT_FLASHLIGHT: Boolean = false
        const val DEFAULT_STRICT_TIME: Boolean = true
        const val DEFAULT_KEEP_SCREEN_ON: Boolean = true
    }
}
