package app.template.patches.webtoon.startup

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.COMPATIBILITY_WEBTOON
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

/**
 * The Subscribed list of the MY tab. The app calls it `MY_FAVORITES` internally, and its tab name
 * is `favorites`, which is the value the `sub_tab` extra of a deep link carries.
 *
 * Both the enum and its entries keep their names, because the app looks a sub tab up by tab name
 * over `MainTab.SubTab.getEntries()` and also exposes `valueOf`, so neither can be renamed without
 * breaking that lookup.
 */
private const val SUBSCRIBED_SUB_TAB = "Lcom/naver/linewebtoon/main/MainTab\$SubTab;->" +
        "MY_FAVORITES:Lcom/naver/linewebtoon/main/MainTab\$SubTab;"

@Suppress("unused")
val openSubscribedOnStartupPatch = bytecodePatch(
    name = "Open Subscribed on startup",
    description = "Opens the MY tab on its Subscribed list instead of the Home tab " +
            "whenever the app opens its main screen, " +
            "which includes the cold start from the launcher. " +
            "A notification or a link that points at a specific tab, title or episode " +
            "is not affected, and neither is the Home tab of the bottom bar.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_WEBTOON)

    execute {
        // Rather than driving the tab bar directly, the sub tab the app asks for itself is changed
        // in the two places that pick one, so the rest of the startup path is the one the app
        // already runs for a link that opens it on the Subscribed list. The tab is selected, its
        // fragment is created with the arguments the sub tab asks for, and the parent tab is
        // derived from the sub tab, so the bottom bar and the back stack stay consistent. The
        // Subscribed list takes no arguments, so naming it is all that is needed.

        // The splash screen is the launcher activity, and it hands over to the main screen through
        // the navigator, which builds that intent with the Home tab spelled out. That is what a
        // cold start actually goes through, so this is the injection that decides the tab the app
        // opens on.
        //
        // Only the target that opens the main screen is rewritten. Every other target the
        // navigator builds an intent for, which is what a link or a notification pointing at a
        // title or an episode resolves to, is left alone.
        BuildMainScreenIntentFingerprint.let { fingerprint ->
            val index = fingerprint.instructionMatches[1].index

            // Writing the register the Home tab was read into keeps the call that follows, and
            // the flags that are set on the intent afterwards, unchanged.
            fingerprint.method.apply {
                val register = getInstruction<OneRegisterInstruction>(index).registerA

                replaceInstruction(index, "sget-object v$register, $SUBSCRIBED_SUB_TAB")
            }
        }

        // The main screen also resolves a sub tab for itself when the intent that started it names
        // none. Nothing on the cold start path leaves it unset, but a start from elsewhere in the
        // app can, so the fallback is pointed at the Subscribed list as well for consistency.
        //
        // Only the branch taken when the intent names no sub tab is rewritten, so a notification
        // or a link that targets a specific tab still wins. That branch is not reached when there
        // is a saved instance state either, which leaves the restored tab alone after a rotation
        // or a process restart.
        ResolveLaunchSubTabFingerprint.let { fingerprint ->
            // The call that reads the default sub tab, followed by the register it is read into.
            val fallbackIndex = fingerprint.instructionMatches[2].index
            val register = fingerprint.method.getInstruction<OneRegisterInstruction>(
                fingerprint.instructionMatches[3].index,
            ).registerA

            fingerprint.method.apply {
                // Neither instruction is a branch target, and writing the same register keeps the
                // rest of the branch, which hands the sub tab to the launch target, unchanged.
                removeInstructions(fallbackIndex, 2)
                addInstruction(fallbackIndex, "sget-object v$register, $SUBSCRIBED_SUB_TAB")
            }
        }
    }
}
