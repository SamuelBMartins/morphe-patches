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

    /**
     * WEBTOON ships as a split bundle (base + per-abi splits), so APKM is the file type users
     * should supply. The APKM variants are split by architecture pair rather than by dpi,
     * because the app ships nodpi resources.
     *
     * No [app.morphe.patcher.patch.SupportedAbi] version code restriction is declared, because
     * only the arm64-v8a + armeabi-v7a variant was inspected and the remaining variants may
     * carry their own version codes. Matching on the version name alone is the permissive
     * choice, and the patches themselves are architecture independent.
     */
    val COMPATIBILITY_WEBTOON = Compatibility(
        name = "WEBTOON",
        packageName = "com.naver.linewebtoon",
        apkFileType = ApkFileType.APKM,
        // WEBTOON brand green, matching the launcher icon.
        appIconColor = 0x00DC64,
        targets = listOf(
            // Latest target, and expected to keep working with future releases because the
            // fingerprints anchor on the app's own unobfuscated class names.
            AppTarget(
                version = null,
                isExperimental = true,
            ),
            // Confirmed working.
            AppTarget(
                version = "3.9.11",
            ),
        ),
    )
}
