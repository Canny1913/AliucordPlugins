version = "0.0.1" // Plugin version. Increment this to trigger the updater
description = "Patch to remove user messages without blocking them" // Plugin description that will be shown to user

aliucord {
    // Changelog of your plugin
    changelog.set("""
        Initial build.
    """.trimIndent())

    excludeFromUpdaterJson.set(true)
}
