package com.github.canny1913.settings

import android.view.View
import com.aliucord.Utils
import com.aliucord.api.SettingsAPI
import com.aliucord.fragments.SettingsPage
import com.aliucord.settings.SettingsDelegate
import com.aliucord.settings.delegate
import com.discord.views.CheckedSetting

class AversionSettings: SettingsPage() {

    override fun onViewBound(view: View?) {
        super.onViewBound(view)
        setActionBarTitle("Aversion Settings")
        createSetting(
            "Hide Blocked Messages",
            "Hide blocked messages in chat and enable chat indicator",
            hideBlockedMessagesDelegate
        ).apply(::addView)
        createSetting(
            "Hide Ignored Messages",
            "Hide ignored messages in chat and enable chat indicator",
            hideIgnoredMessagesDelegate
        ).apply(::addView)
        createSetting(
            "Enable Chat Indicator",
            "Adds an indicator at top of chat screen informing presence of blocked/ignored users in chat.",
            enableIndicatorDelegate
        ).apply(::addView)
    }
    private fun createSetting(
        title: String?,
        description: String?,
        delegate: SettingsDelegate<Boolean>,
    ): CheckedSetting {
        return Utils.createCheckedSetting(
            requireContext(), CheckedSetting.ViewType.SWITCH, title, description
        ).apply {
            var setting by delegate
            isChecked = setting
            setOnCheckedListener {
                setting = !setting
            }
        }
    }
    companion object {
        private val settingsAPI = SettingsAPI("Aversion")
        private val hideBlockedMessagesDelegate = settingsAPI.delegate("hideBlockedMessages", false)
        val hideBlockedMessages by hideBlockedMessagesDelegate
        private val hideIgnoredMessagesDelegate = settingsAPI.delegate("hideIgnoredMessages", false)
        val hideIgnoredMessages by hideIgnoredMessagesDelegate
        private val enableIndicatorDelegate = settingsAPI.delegate("enableIndicator", false)
        val enableIndicator by enableIndicatorDelegate
    }
}

