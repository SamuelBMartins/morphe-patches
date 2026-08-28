package app.template.patches.tapas.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.COMPATIBILITY_TAPAS

@Suppress("unused")
val hideAdsPatch = bytecodePatch(
    name = "Hide ads",
    description = "Hides banner ads in the episode viewer, comments and series pages, " +
            "and hides Tapas' own in-house promo ads.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_TAPAS)

    execute {
        // Return before a banner ad view is created, so no banner is ever requested or attached.
        //
        // The banner view holder starts out GONE and is only made visible from the ad load
        // callback, so suppressing the load here also leaves the placeholder collapsed
        // instead of showing an empty gap.
        LoadBannerAdFingerprint.method.addInstruction(0, "return-void")

        // Overwrite the custom ad list parameter with null. The setter already collapses the
        // layout and returns early for a null or empty list, so reusing that path keeps the
        // patch to a single instruction and leaves the app's own state consistent.
        SetCustomAdsFingerprint.method.addInstruction(0, "const/4 p1, 0x0")
    }
}
