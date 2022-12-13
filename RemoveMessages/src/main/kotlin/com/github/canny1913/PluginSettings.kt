package com.github.canny1913

import android.annotation.SuppressLint
import android.text.Editable
import android.view.View
import com.aliucord.Utils
import com.aliucord.api.SettingsAPI
import com.aliucord.fragments.SettingsPage
import com.aliucord.views.Button
import com.aliucord.views.TextInput
import com.discord.utilities.view.text.TextWatcher

class PluginSettings(private val settings: SettingsAPI): SettingsPage() {
    @SuppressLint("SetTextI18n")
    override fun onViewBound(view: View?) {
        super.onViewBound(view)

        setActionBarTitle("RemoveMessages")

        val ctx = requireContext()

        val input = TextInput(ctx)
        input.setHint("User ID")

        val editText = input.editText

        val button = Button(ctx)
        button.text = "DONE"
        button.setOnClickListener {
            settings.setLong("id", editText.toString().toLong())
            Utils.showToast("Saved!")
            close()
        }
        editText.maxLines = 1
        editText.setText(settings.getLong("id", 0L).toString())
        editText.addTextChangedListener(object : TextWatcher() {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                if (!(s.toString().all { it.isDigit() } && s.toString().length > 16)) {
                    button.alpha = 0.5f
                    button.isClickable = false
                } else {
                    button.alpha = 1f
                    button.isClickable = true
                }
            }
        })

        addView(input)
        addView(button)
    }
}