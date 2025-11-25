package app.revanced.patches.tiktok.misc.settings

import app.revanced.patcher.fingerprint

internal val adPersonalizationActivityOnCreateFingerprint = fingerprint {
    custom { method, classDef ->
        classDef.endsWith("/AdPersonalizationActivity;") && method.name == "onCreate"
    }
}

// New target: Support page where various setting units are added.
internal val supportPageOnViewCreatedFingerprint = fingerprint {
    custom { method, classDef ->
        classDef.endsWith("/SupportPage;") && method.name == "onViewCreated"
    }
}

// Wrapper unit class (LX/0k8G) exposes the error string, stable anchor.
internal val settingsEntryWrapperFingerprint = fingerprint {
    strings("pls pass item or extends the EventUnit")
}

// Item class (LX/0k8E) & its superclass (LX/0k8L) both check these strings; combined increases specificity.
internal val settingsEntryItemFingerprint = fingerprint {
    strings(
        "title",
        "cellVariant",
    )
}

// Legacy fingerprint still used by other TikTok patches (feed filter, downloads, spoof sim)
// Keep for backward compatibility until those patches are migrated.
internal val settingsStatusLoadFingerprint = fingerprint {
    custom { method, classDef ->
        classDef.endsWith("Lapp/revanced/extension/tiktok/settings/SettingsStatus;") && method.name == "load"
    }
}
