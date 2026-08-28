package app.template.patches.webtoon.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.COMPATIBILITY_WEBTOON
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

@Suppress("unused")
val hideAdsPatch = bytecodePatch(
    name = "Hide ads",
    description = "Hides the ad on the splash screen and the display ads on the Home tab, " +
            "the More tab, the episode list of a series, " +
            "and the top and the end of the episode viewer. " +
            "Rewarded ads can still be watched to earn coins or unlock episodes.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_WEBTOON)

    execute {
        // Every ad slot is suppressed at the point where the app decides to request it, rather
        // than by hiding a view after the fact. That keeps each placeholder collapsed, because
        // none of them is made visible until an ad actually loads, and it also means no ad is
        // ever fetched or reported as an impression.
        //
        // The mediation SDKs are deliberately left running: they also serve the rewarded ads that
        // are watched to earn coins and unlock episodes, which stay working.

        // Splash screen ad. Reporting no WebView takes the sequence down its own "skip the ad"
        // branch, which signals that loading is finished straight away, so startup is not held
        // for the two seconds the ad request is otherwise given.
        SplashAdWebViewAvailableFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return v0
            """,
        )

        // Home tab mid list ad and "premium" ad. Returning here is what the app itself does for a
        // user who paid to remove ads, so both slots are dropped by the app's own code path.
        SetUpHomeAdsFingerprint.let { fingerprint ->
            // The Unit singleton field name is obfuscated, so the reference is taken from the
            // return this method already ends with.
            val unit = fingerprint.method.getInstruction<ReferenceInstruction>(
                fingerprint.instructionMatches.last().index,
            ).reference as FieldReference

            // v0 is written before it is next read, so it is free here.
            fingerprint.method.addInstructions(
                0,
                """
                    sget-object v0, ${unit.definingClass}->${unit.name}:${unit.type}
                    return-object v0
                """,
            )
        }

        // More tab ad. This is the same loader the Home mid list ad uses, so suppressing the
        // request here also stops the Home slot from reaching the network at all.
        StartCommonAdLoadFingerprint.method.addInstruction(0, "return-void")

        // Banner ad on the episode list of a series.
        LoadEpisodeListBannerAdFingerprint.method.addInstruction(0, "return-void")

        // Viewer end ad and the paid product placement ad, in the Originals, Canvas and short
        // drama viewers. False is the value this method already returns when a load is in flight,
        // and the caller discards it.
        StartViewerEndAdLoadFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return v0
            """,
        )

        // Viewer end ad in the branded episode viewer, which drives the underlying loader itself
        // instead of going through the one patched above.
        StartBrandedViewerEndAdLoadFingerprint.method.addInstruction(0, "return-void")

        // Ad above the panels at the top of the viewer.
        AttachViewerTopAdFingerprint.let { fingerprint ->
            // As above, the obfuscated Unit singleton reference is harvested from the method's
            // own existing "no ad" return.
            val unit = fingerprint.method.getInstruction<ReferenceInstruction>(
                fingerprint.instructionMatches.first().index,
            ).reference as FieldReference

            fingerprint.method.addInstructions(
                0,
                """
                    sget-object v0, ${unit.definingClass}->${unit.name}:${unit.type}
                    return-object v0
                """,
            )
        }
    }
}
