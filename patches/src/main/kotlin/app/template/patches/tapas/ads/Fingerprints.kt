package app.template.patches.tapas.ads

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

/**
 * Tapas obfuscates its `ads` Gradle module (currently the `ln` package), but the Kotlin
 * `checkNotNullParameter` name strings and the calls into the non obfuscated
 * `com.tapastic.*` classes survive obfuscation, so those are used as anchors.
 */

/**
 * Creates, loads and attaches a mediated banner ad view for a given banner slot.
 *
 * This is the single choke point used by the episode viewer, the comment list and the
 * series page, so overriding this one method suppresses every banner in the app.
 *
 * Java equivalent:
 * ```java
 * public final void b(Activity activity, boolean recreate, String bannerKey,
 *                    BannerDisplayType bannerDisplayType, BannerPlacement bannerPlacement) {
 *     ...
 *     AdsExtensionsKt.createBanner(activity, size, "<ad unit id>", placement);
 *     ...
 * }
 * ```
 */
internal object LoadBannerAdFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    // The last two parameters are obfuscated enum types, so they are declared as plain objects.
    parameters = listOf("Landroid/app/Activity;", "Z", "Ljava/lang/String;", "L", "L"),
    filters = listOf(
        // Kotlin parameter null checks, emitted in declaration order.
        string("bannerKey"),
        string("bannerDisplayType"),
        string("bannerPlacement"),
        // The placement names ("Episode" / "Comment") are deliberately not matched, because the
        // compiler is free to emit the two switch branches in either order.
        methodCall(
            definingClass = "Lcom/tapastic/extensions/AdsExtensionsKt;",
            name = "createBanner",
        ),
    ),
)

/**
 * `androidx.startup` initializer body that boots the ad mediation SDKs
 * (Unity LevelPlay, which in turn drives AppLovin, ironSource, Vungle and Tapjoy).
 *
 * The method name is a single obfuscated character because it overrides
 * `BaseInitializer`, so it is matched by defining class and signature only.
 *
 * Java equivalent:
 * ```java
 * public final void a(Application app) {
 *     ...
 *     LevelPlay.init(app, request, listener);
 *     AppLovinSdk.getInstance(app).getSettings().setVerboseLogging(false);
 * }
 * ```
 */
internal object AdsInitializerFingerprint : Fingerprint(
    definingClass = "Lcom/tapastic/init/AdsInitializer;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Landroid/app/Application;"),
)

/**
 * Binds Tapas' own in-house promo ads ("custom ads") into the episode viewer layout.
 *
 * The method already hides itself when the list is null or empty, which the patch reuses.
 *
 * Java equivalent:
 * ```java
 * public final void setCustomAds(List<CustomAd> list) {
 *     this.customAds = list;
 *     if (list == null || list.isEmpty()) {
 *         setVisibility(GONE);
 *         return;
 *     }
 *     ...
 * }
 * ```
 */
internal object SetCustomAdsFingerprint : Fingerprint(
    definingClass = "Lcom/tapastic/ui/widget/CustomAdLayout;",
    name = "setCustomAds",
    returnType = "V",
    parameters = listOf("Ljava/util/List;"),
)

/**
 * The three promo popups below are all shown from one obfuscated lambda that observes the
 * "latest announcement" event in `MainActivity`. Each is shown with the same shape:
 *
 * ```asm
 * const-string vN, "<fragment tag>"
 * invoke-virtual {v0, p1, vN}, Landroidx/fragment/app/x;->show(Landroidx/fragment/app/m1;Ljava/lang/String;)V
 * ```
 *
 * The fragment tags are plain string literals that survive obfuscation and each occurs
 * exactly once in the app, so anchoring on the tag plus the call that immediately follows it
 * is both unique and minimal. The `DialogFragment.show` defining class is obfuscated, so the
 * call is matched by opcode only.
 *
 * All three fingerprints resolve to the same method. They must therefore all be matched
 * before the method is modified, and then edited from the highest index to the lowest,
 * otherwise the earlier match indices go stale.
 */
private fun showDialogFilters(fragmentTag: String) = listOf(
    string(fragmentTag),
    opcode(Opcode.INVOKE_VIRTUAL, MatchAfterImmediately()),
)

/** Shows the full screen "Free Ink offer" promo (an announcement carrying a sub ad campaign). */
internal object ShowFreeInkPromoDialogFingerprint : Fingerprint(
    filters = showDialogFilters("dialog_subAdCampaign"),
)

/** Shows a full screen promo announcement with a call to action button. */
internal object ShowAnnouncementDialogFingerprint : Fingerprint(
    filters = showDialogFilters("dialog_announcement"),
)

/** Shows a full screen image only promo announcement. */
internal object ShowImageOnlyAnnouncementDialogFingerprint : Fingerprint(
    filters = showDialogFilters("dialog_image_only_announcement"),
)

/**
 * Binds the Braze in app message manager to an activity.
 *
 * Braze in app messages are server triggered marketing overlays (modal, full screen, slide up
 * and HTML), so they are the same kind of surface as the announcement dialogs above, just
 * delivered by the marketing SDK instead of the Tapas API.
 *
 * This is the only registration entry point, and patching it covers all four callers:
 * `com.tapastic.ui.base.BaseActivity.onResume`, `BaseComposeActivity.onResume`,
 * `BrazeActivityLifecycleCallbackListener` and `BrazeBaseFragmentActivity`. Patching only the
 * two `com.tapastic` call sites would not be enough, because the lifecycle listener is
 * registered application wide and would keep binding every activity on its own.
 *
 * Braze class names are not obfuscated in this build, unlike the app's own ad module.
 */
internal object RegisterBrazeInAppMessageManagerFingerprint : Fingerprint(
    definingClass = "Lcom/braze/ui/inappmessage/BrazeInAppMessageManager;",
    name = "registerInAppMessageManager",
    returnType = "V",
    parameters = listOf("Landroid/app/Activity;"),
)
