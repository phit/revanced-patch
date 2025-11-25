package app.revanced.patches.tiktok.misc.share

import app.revanced.patcher.fingerprint

internal val getShareUrlFingerprint = fingerprint {
    returns("Ljava/lang/String;")
    custom { method, classDef ->
        classDef.type.endsWith("/feed/model/Aweme;") && method.name == "getShareUrl" && method.parameterTypes.isEmpty()
    }
}
