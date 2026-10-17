version = "1.0.1"
description = "Adds onboarding functionality."

aliucord {
    changelog.set(
        """
        # 1.0.1
        * Fixed a false-positive error after completing.
        * Added onboarding check for complete button in channel list.
        # 1.0.0
        * Initial plugin release.
        """.trimIndent()
    )
    deploy.set(false)
}
