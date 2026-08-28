package app.template.patches.tapas.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.removeInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.COMPATIBILITY_TAPAS

@Suppress("unused")
val hidePromoPopupsPatch = bytecodePatch(
    name = "Hide promo popups",
    description = "Hides the full screen promo popups shown on the home tab, " +
            "including the \"Free Ink offer\" popup, " +
            "and stops Braze marketing overlays from being shown. " +
            "Push notifications are not affected.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_TAPAS)

    execute {
        // Resolve every match before touching the method, because all three fingerprints match
        // the same method and removing an instruction invalidates any later match index.
        val showDialogIndices = listOf(
            ShowFreeInkPromoDialogFingerprint,
            ShowAnnouncementDialogFingerprint,
            ShowImageOnlyAnnouncementDialogFingerprint,
        ).map { fingerprint ->
            // instructionMatches[1] is the DialogFragment.show() call that immediately
            // follows the fragment tag string.
            fingerprint.instructionMatches[1].index
        }

        // All three popups are shown from the same method.
        val showAnnouncementMethod = ShowFreeInkPromoDialogFingerprint.method

        // Drop the show() calls so the dialogs are constructed but never presented.
        // The calls return void and are not branch targets, so removing them leaves the
        // surrounding control flow intact.
        //
        // Removal happens from the highest index down, so that the indices resolved above
        // remain valid for the instructions still to be removed.
        showDialogIndices.sortedDescending().forEach { index ->
            showAnnouncementMethod.removeInstruction(index)
        }

        // Never let Braze bind an activity, so an in app message can never be presented.
        //
        // Returning early is safe because the method already treats "no activity registered" as
        // a valid state: it logs a warning and returns when passed a null activity, and the
        // matching unregister call is a no op for an activity that was never registered.
        RegisterBrazeInAppMessageManagerFingerprint.method.addInstruction(0, "return-void")
    }
}
