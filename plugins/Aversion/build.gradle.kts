version = "0.0.4"
description = "Reimagines blocked message support and adds ignored user support."

aliucord {
    changelog = """
        # 0.0.4
        * Expanded message group colors should be more consistent now.
        * Channel list indicator color is changed.
        * Duplicate 'Ignore' button issue in user actions should be resolved now.
        * Fixed error spam in logs caused by invalid timestamp comparison.
        * Fixed an edgecase where blocked message button was getting misplaced after expanding.
        # 0.0.3
        * Fixed incorrect setting description
        # 0.0.2
        * Fixed incorrect blocked group position when blocked message is at bottom.
        * Fixed compatibility with older Aliucord cores.
        # 0.0.1
        * Initial plugin release.
    """.trimIndent()
}