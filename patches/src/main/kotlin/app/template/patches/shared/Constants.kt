package app.template.patches.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object Constants {
    /**
     * Tapas ships as a split bundle (base + per-abi + per-dpi splits), so APKM is the
     * file type users should supply.
     *
     * All four architecture releases of a given Tapas version share the same version code,
     * so no [app.morphe.patcher.patch.SupportedAbi] version code restriction is declared.
     */
    val COMPATIBILITY_TAPAS = Compatibility(
        name = "Tapas",
        packageName = "com.tapastic",
        apkFileType = ApkFileType.APKM,
        // Tapas brand yellow, matching the launcher icon background.
        appIconColor = 0xF4B404,
        targets = listOf(
            // Latest target, and expected to keep working with future releases because the
            // fingerprints avoid obfuscated names wherever possible.
            AppTarget(
                version = null,
                isExperimental = true,
            ),
            // Confirmed working.
            AppTarget(
                version = "7.13.0",
            ),
        ),
    )
}
