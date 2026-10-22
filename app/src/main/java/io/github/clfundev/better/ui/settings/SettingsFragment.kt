package io.github.clfundev.better.ui.settings

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import io.github.clfundev.better.R
import io.github.clfundev.better.data.pref.AppPreferences
import io.github.clfundev.better.databinding.FragmentSettingsBinding
import io.github.clfundev.better.ui.ConfirmDialogFragment
import io.github.clfundev.better.ui.backup.BackupActivity
import io.github.clfundev.better.ui.login.LoginActivity
import io.github.clfundev.better.ui.restore.RestoreActivity
import io.github.clfundev.better.utils.getAppContainer

class SettingsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            childFragmentManager
                .beginTransaction()
                .replace(R.id.fragmentcontainer_settings, PreferenceFragment())
                .commit()
        }
    }

    class PreferenceFragment : PreferenceFragmentCompat() {
        @SuppressLint("BatteryLife")
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
            val context = requireContext()
            // 获取各个Preference
            val focusDurationPref = findPreference<ListPreference>(
                AppPreferences.KEY_FOCUS_DURATION
            ) ?: return
            val soundPref = findPreference<SwitchPreferenceCompat>(
                AppPreferences.KEY_SOUND
            ) ?: return
            val vibrationPref = findPreference<SwitchPreferenceCompat>(
                AppPreferences.KEY_VIBRATION
            ) ?: return
            val flashlightPref = findPreference<SwitchPreferenceCompat>(
                AppPreferences.KEY_FLASHLIGHT
            ) ?: return
            val strictTimePref = findPreference<SwitchPreferenceCompat>(
                AppPreferences.KEY_STRICT_TIME
            ) ?: return
            val keepScreenOnPref = findPreference<SwitchPreferenceCompat>(
                AppPreferences.KEY_KEEP_SCREEN_ON
            ) ?: return
            val clearDataPref = findPreference<Preference>(
                AppPreferences.KEY_CLEAR_DATA
            ) ?: return
            val resetAllSettingsPref = findPreference<Preference>(
                AppPreferences.KEY_RESET_ALL_SETTINGS
            ) ?: return
            val ignoreBatteryOptimizationPref = findPreference<Preference>(
                AppPreferences.KEY_IGNORE_BATTERY_OPTIMIZATION
            ) ?: return
            val manageStartupAppsPref = findPreference<Preference>(
                AppPreferences.KEY_MANAGE_STARTUP_APPS
            ) ?: return
            val loginPref = findPreference<Preference>(
                AppPreferences.KEY_LOGIN
            ) ?: return
            val backupPref = findPreference<Preference>(
                AppPreferences.KEY_BACKUP
            ) ?: return
            val restorePref = findPreference<Preference>(
                AppPreferences.KEY_RESTORE
            ) ?: return
            // 设置几个自定义Preference的点击监听
            val fragmentManager = getChildFragmentManager()
            clearDataPref.setOnPreferenceClickListener {
                if (fragmentManager.findFragmentByTag(ClearDataDialogFragment.TAG) == null) {
                    ClearDataDialogFragment().show(fragmentManager, ClearDataDialogFragment.TAG)
                }
                true
            }
            resetAllSettingsPref.setOnPreferenceClickListener {
                if (fragmentManager.findFragmentByTag(ConfirmDialogFragment.TAG) == null) {
                    ConfirmDialogFragment.newInstance(
                        CONFIRM_RESET_ALL_SETTINGS,
                        getString(R.string.all_warning),
                        getString(R.string.settings_reset_warning),
                        getString(R.string.all_yes),
                        getString(R.string.all_no)
                    ).show(fragmentManager, ConfirmDialogFragment.TAG)
                }
                true
            }
            ignoreBatteryOptimizationPref.setOnPreferenceClickListener {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                if (!powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    intent.setData("package:${context.packageName}".toUri())
                    try {
                        startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        Log.e(TAG, "startActivity error", e)
                        Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                    }
                }
                true
            }
            manageStartupAppsPref.setOnPreferenceClickListener {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.setData("package:${context.packageName}".toUri())
                try {
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    Log.e(TAG, "startActivity error", e)
                    Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                }
                true
            }
            loginPref.setOnPreferenceClickListener {
                val intent = Intent(context, LoginActivity::class.java)
                startActivity(intent)
                true
            }
            backupPref.setOnPreferenceClickListener {
                val appPreferences = context.getAppContainer().appPreferences
                val token = appPreferences.token
                if (token.isNullOrEmpty()) {
                    Toast.makeText(context, R.string.all_not_logged_in, Toast.LENGTH_SHORT).show()
                } else {
                    startActivity(Intent(context, BackupActivity::class.java))
                }
                true
            }
            restorePref.setOnPreferenceClickListener {
                val appPreferences = context.getAppContainer().appPreferences
                val token = appPreferences.token
                if (token.isNullOrEmpty()) {
                    Toast.makeText(context, R.string.all_not_logged_in, Toast.LENGTH_SHORT).show()
                } else {
                    startActivity(Intent(context, RestoreActivity::class.java))
                }
                true
            }
            // 监听各对话框的返回结果
            fragmentManager.setFragmentResultListener(
                CONFIRM_RESET_ALL_SETTINGS,
                this
            ) { _, result ->
                if (result.getInt(ConfirmDialogFragment.KEY_WHICH) ==
                    ConfirmDialogFragment.RESULT_POSITIVE
                ) {
                    // 恢复默认设置
                    focusDurationPref.setValue(AppPreferences.DEFAULT_FOCUS_DURATION)
                    soundPref.setChecked(AppPreferences.DEFAULT_SOUND)
                    vibrationPref.setChecked(AppPreferences.DEFAULT_VIBRATION)
                    flashlightPref.setChecked(AppPreferences.DEFAULT_FLASHLIGHT)
                    strictTimePref.setChecked(AppPreferences.DEFAULT_STRICT_TIME)
                    keepScreenOnPref.setChecked(AppPreferences.DEFAULT_KEEP_SCREEN_ON)
                }
            }
        }

        companion object {
            // 用于监听对话框结果
            private const val CONFIRM_RESET_ALL_SETTINGS = "CONFIRM_RESET_ALL_SETTINGS"
        }
    }

    companion object {
        private const val TAG = "SettingsFragment"
    }
}