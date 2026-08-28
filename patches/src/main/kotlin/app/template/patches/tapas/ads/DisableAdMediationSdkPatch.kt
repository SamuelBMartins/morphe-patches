package app.template.patches.tapas.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.COMPATIBILITY_TAPAS

@Suppress("unused")
val disableAdMediationSdkPatch = bytecodePatch(
    name = "Disable ad mediation SDK",
    description = "Prevents the Unity LevelPlay, AppLovin, ironSource, Vungle and Tapjoy SDKs " +
            "from starting up, which also stops their tracking. " +
            "Rewarded ads can no longer be watched to earn Ink or unlock episodes.",
    // Opt in, because this also disables the voluntary "watch an ad to earn Ink" reward flow.
    default = false,
) {
    compatibleWith(COMPATIBILITY_TAPAS)

    execute {
        // Return before LevelPlay.init(), so no mediation SDK is ever started.
        //
        // This is an androidx.startup initializer, so returning early is safe: the only thing
        // the original body does is resolve the ad manager from Hilt and initialise the SDKs.
        AdsInitializerFingerprint.method.addInstruction(0, "return-void")
    }
}
