package app.template.patches.webtoon.recommend

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.COMPATIBILITY_WEBTOON
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

@Suppress("unused")
val hideRecommendPopupsPatch = bytecodePatch(
    name = "Hide recommend popups",
    description = "Hides the popup suggesting other series that is shown after " +
            "the last episode of a series, in both the comic and the short drama viewer. " +
            "Covers the full screen popup, the bottom sheet and the web popup variants.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_WEBTOON)

    execute {
        // The three popup variants are all decided by this one response mapper, so overriding it
        // covers every one of them, for every viewer that asks for a recommendation.
        //
        // Rather than deleting the calls that present each popup, the mapper is made to report
        // "no popup", which is a state the app already produces whenever the server has no
        // recommendation to give. Every consumer therefore stays on a path it already takes:
        // the comic viewer never emits a UI event, and the short drama viewer maps it to its own
        // "not available" result. No fragment is left half constructed and no observer is left
        // waiting.
        ViewerEndRecommendAsModelFingerprint.let { fingerprint ->
            // instructionMatches[2] is the sget-object of the sealed type's "no popup" singleton.
            // Its names are obfuscated, so the reference is taken from the app itself.
            val noPopup = fingerprint.method.getInstruction<ReferenceInstruction>(
                fingerprint.instructionMatches[2].index,
            ).reference as FieldReference

            // v0 is written before it is next read, so it is free here.
            fingerprint.method.addInstructions(
                0,
                """
                    sget-object v0, ${noPopup.definingClass}->${noPopup.name}:${noPopup.type}
                    return-object v0
                """,
            )
        }
    }
}
