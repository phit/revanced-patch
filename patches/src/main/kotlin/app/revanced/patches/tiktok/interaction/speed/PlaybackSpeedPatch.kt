package app.revanced.patches.tiktok.interaction.speed

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.tiktok.shared.getEnterFromFingerprint
import app.revanced.patches.tiktok.shared.onRenderFirstFrameFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction11x
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val playbackSpeedPatch = bytecodePatch(
    name = "Playback speed",
    description = "Enables the playback speed option for all videos and " +
        "retains the speed configurations in between videos.",
) {
    compatibleWith(
        "com.ss.android.ugc.trill"("42.6.4"),
        "com.zhiliaoapp.musically"("42.6.4"),
    )

    execute {
        setSpeedFingerprint.let { speedEventFingerprint ->
            // Remember speed after it is applied in the event method.
            speedEventFingerprint.method.apply {
                val applyIndex = indexOfFirstInstructionOrThrow {
                    val ref = getReference<MethodReference>()
                    ref?.definingClass == "LX/0NgW;" && ref.parameterTypes.size == 1 && ref.parameterTypes[0] == "F" && ref.returnType == "V"
                }
                // Determine speed register dynamically by parsing the invoke instruction text.
                val invokeInstr = getInstruction(applyIndex)
                val speedRegToken = Regex("\\{([^}]*)}").find(invokeInstr.toString())?.groupValues?.get(1)
                    ?.split(',')?.map { it.trim() }?.lastOrNull() ?: "p1" // fallback to method param
                // Normalize register form for injection: keep as-is (may be pX or vX)
                addInstruction(
                    applyIndex + 1,
                    "invoke-static { $speedRegToken }, Lapp/revanced/extension/tiktok/speed/PlaybackSpeedPatch;->rememberPlaybackSpeed(F)V",
                )
            }

            // Reapply remembered speed at start of first frame render.
            onRenderFirstFrameFingerprint.method.addInstructions(
                0,
                """
                    iget-object v0, p0, Lcom/ss/android/ugc/aweme/feed/panel/BaseListFragmentPanel;->LLLJ:Lcom/ss/android/ugc/aweme/feed/controller/BaseController;
                    if-eqz v0, :skip_set_speed
                    invoke-static { }, Lapp/revanced/extension/tiktok/speed/PlaybackSpeedPatch;->getPlaybackSpeed()F
                    move-result v1
                    # Apply speed using controller interface (method name may vary, so find dynamically is not possible in raw smali injection).
                    invoke-interface { v0, v1 }, LX/0NgW;->LJJJIL(F)V
                    :skip_set_speed
                """,
            )

            // Force enable playback speed option (if there is a boolean method we can override).
            speedEventFingerprint.classDef.methods.find { method -> method.returnType == "Z" }?.addInstructions(
                0,
                """
                    const/4 v0, 0x1
                    return v0
                """,
            )
        }
    }
}
