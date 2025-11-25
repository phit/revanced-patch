package app.revanced.patches.tiktok.interaction.speed

import app.revanced.patcher.fingerprint

internal val setSpeedFingerprint = fingerprint {
    custom { method, classDef ->
        classDef.endsWith("/BaseListFragmentPanel;") && method.name == "onFeedSpeedSelectedEvent"
    }
    returns("V")
}
