package app.template.patches.webtoon.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.COMPATIBILITY_WEBTOON

@Suppress("unused")
val hidePromoPopupsPatch = bytecodePatch(
    name = "Hide promo popups",
    description = "Hides the full screen promotion popups, " +
            "both the ones shown when the Home tab opens and the ones that appear " +
            "after liking, subscribing, reading or purchasing, " +
            "and stops Braze marketing overlays from being shown. " +
            "Push notifications are not affected.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_WEBTOON)

    execute {
        // Never build an intent for the promotion web popup, so it can neither be queued behind
        // the Home tab nor launched over whatever is on screen.
        //
        // Null is what this factory already returns for a promotion without a URL, and both
        // callers check for it: the one that launches the popup directly returns early, and the
        // one that builds the Home popup queue leaves the entry out of the list. The queue
        // therefore stays consistent and keeps advancing through its remaining entries.
        //
        // v0 is written before it is next read, so it is free here.
        CreateInAppPromotionIntentFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return-object v0
            """,
        )

        // Never let Braze bind an activity, so an in app message can never be presented.
        //
        // Returning early is safe because the method already treats "no activity registered" as a
        // valid state: it logs a warning and returns when passed a null activity, and the matching
        // unregister call is a no op for an activity that was never registered.
        RegisterBrazeInAppMessageManagerFingerprint.method.addInstruction(0, "return-void")
    }
}
