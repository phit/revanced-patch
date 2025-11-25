package app.revanced.patches.tiktok.misc.settings

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.layout.branding.addBrandLicensePatch
import app.revanced.patches.tiktok.misc.extension.sharedExtensionPatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction22c
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/tiktok/settings/TikTokActivityHook;"

val settingsPatch = bytecodePatch(
    name = "Settings",
    description = "Adds ReVanced settings to TikTok.",
) {
    dependsOn(sharedExtensionPatch, addBrandLicensePatch)

    compatibleWith(
        "com.ss.android.ugc.trill"("42.6.4"),
        "com.zhiliaoapp.musically"("42.6.4"),
    )

    execute {
        val adPersonalizationActivityClassName =
            "Lcom/bytedance/ies/ugc/aweme/commercialize/compliance/personalization/AdPersonalizationActivity;"
        val initializeSettingsMethodDescriptor =
            "$EXTENSION_CLASS_DESCRIPTOR->initialize($adPersonalizationActivityClassName)Z"

        val createSettingsEntryMethodDescriptor =
            "$EXTENSION_CLASS_DESCRIPTOR->createSettingsEntry(" +
                "Ljava/lang/String;" +
                "Ljava/lang/String;" +
                ")Ljava/lang/Object;"

        fun String.toClassName(): String = substring(1, this.length - 1).replace("/", ".")

        // New entry class & wrapper based on updated fingerprints.
        val settingsItemClass = settingsEntryItemFingerprint.originalClassDef.type.toClassName()
        val settingsWrapperClass = settingsEntryWrapperFingerprint.originalClassDef.type.toClassName()

        // Inject into SupportPage.onViewCreated: locate first invoke to LIZ on LX/0k7r and insert before it.
        supportPageOnViewCreatedFingerprint.method.apply {
            val impl = implementation!!
            val addUnitIndex = impl.instructions.indexOfFirst { ins ->
                if (ins.opcode != Opcode.INVOKE_VIRTUAL) return@indexOfFirst false
                val invoke = ins as Instruction35c
                val methodRef = invoke.reference
                val methodString = methodRef.toString()
                methodString.contains("->LIZ(") && methodString.contains("LX/0k7r;")
            }
            if (addUnitIndex == -1) error("Could not find invoke to LIZ on LX/0k7r in SupportPage.onViewCreated")

            // Retrieve manager register from original invoke (first register of Instruction35c is registerC)
            val originalInvoke = getInstruction<Instruction35c>(addUnitIndex)
            val managerRegister = originalInvoke.registerC

            addInstructions(
                addUnitIndex,
                """
                    # ReVanced settings injection
                    const-string v0, "$settingsItemClass"
                    const-string v1, "$settingsWrapperClass"
                    invoke-static {v0, v1}, $createSettingsEntryMethodDescriptor
                    move-result-object v0
                    new-instance v1, ${settingsEntryWrapperFingerprint.originalClassDef.type}
                    invoke-direct {v1, v0}, ${settingsEntryWrapperFingerprint.originalClassDef.type}-><init>(${settingsEntryItemFingerprint.originalClassDef.type})V
                    invoke-virtual {v$managerRegister, v1}, LX/0k7r;->LIZ(LX/0k7p;)V
                """,
            )
        }

        // Initialize the settings menu once the replaced setting entry is clicked.
        adPersonalizationActivityOnCreateFingerprint.method.apply {
            val initializeSettingsIndex = implementation!!.instructions.indexOfFirst { it.opcode == Opcode.INVOKE_SUPER } + 1
            val thisRegister = getInstruction<Instruction35c>(initializeSettingsIndex - 1).registerC
            val usableRegister = implementation!!.registerCount - parameters.size - 2
            addInstructionsWithLabels(
                initializeSettingsIndex,
                """
                    invoke-static {v$thisRegister}, $initializeSettingsMethodDescriptor
                    move-result v$usableRegister
                    if-eqz v$usableRegister, :skip_opening_revanced_settings
                    return-void
                """,
                ExternalLabel("skip_opening_revanced_settings", getInstruction(initializeSettingsIndex)),
            )
        }
    }
}
