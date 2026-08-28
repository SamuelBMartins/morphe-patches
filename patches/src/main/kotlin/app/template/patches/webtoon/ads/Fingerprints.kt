package app.template.patches.webtoon.ads

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags

/**
 * WEBTOON keeps its own class names but renames every method to a single letter, and the same is
 * true of the bundled Naver GFP ad SDK. The fingerprints below therefore never rely on a method
 * name: each one is pinned by its defining class, its signature, and either a string literal or
 * a call into a type whose name did survive (`com.naver.gfpsdk.AdParam`,
 * `com.naver.linewebtoon.navigator.Navigator`, `kotlin.Unit`, the `androidx` and `com.braze`
 * libraries, and the Kotlin `$…$1` continuation classes).
 */

private const val AD_PARAM = "Lcom/naver/gfpsdk/AdParam;"

/**
 * Reports whether a WebView implementation is available, which is what gates the splash ad.
 *
 * When it reports `false` the splash sequence skips the ad entirely, logs `webViewNotAvailable`
 * and immediately signals that ad loading is done, so startup continues without waiting. That is
 * the path the patch reuses; it is also the only caller of this method, so nothing else changes.
 *
 * Java equivalent:
 * ```java
 * public final boolean isWebViewAvailable() {
 *     boolean available = WebViewCompat.getCurrentWebViewPackage(this) != null;
 *     if (!available) Timber.w("[SplashActivity] WebView is not available: " + available);
 *     return available;
 * }
 * ```
 */
internal object SplashAdWebViewAvailableFingerprint : Fingerprint(
    definingClass = "Lcom/naver/linewebtoon/splash/SplashActivity;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = emptyList(),
    filters = listOf(
        methodCall(
            definingClass = "Landroidx/webkit/WebViewCompat;",
            name = "getCurrentWebViewPackage",
        ),
        string("[SplashActivity] WebView is not available: "),
    ),
)

/**
 * Sets up both Home tab ad slots once the personalized ads info arrives: the mid list ad, which
 * is handed to the Home adapter, and the "premium" ad, which gets its own loader.
 *
 * The method opens with the app's own ad-free check, and for an ad-free user it does nothing but
 * write a log line. Returning at the top is therefore the same thing the app already does for a
 * user who paid to remove ads: the adapter's loader stays null, so the mid ad view holder returns
 * immediately and its placeholder stays collapsed rather than leaving an empty band, and the
 * premium loader is never constructed.
 *
 * Java equivalent:
 * ```java
 * public static Unit setUpAds(HomeFragment fragment, HomeAdapter adapter, View view,
 *                            PersonalizedAdsInfo adsInfo) {
 *     if (!fragment.isAdFreeUser().invoke()) {
 *         adapter.setPersonalizedAdsInfo(adsInfo);
 *         if (fragment.homePremiumAdLoader == null) {
 *             fragment.homePremiumAdLoader = new HomePremiumAdLoader(...);
 *             fragment.homePremiumAdLoader.load();
 *         }
 *     } else {
 *         AdLogTracker.log("HomePremiumAdLoader", "isAdFreeUser");
 *     }
 *     return Unit.INSTANCE;
 * }
 * ```
 */
internal object SetUpHomeAdsFingerprint : Fingerprint(
    definingClass = "Lcom/naver/linewebtoon/main/HomeFragment;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "Lkotlin/Unit;",
    // The adapter and the ads info are obfuscated types, so they are declared as plain objects.
    parameters = listOf(
        "Lcom/naver/linewebtoon/main/HomeFragment;",
        "L",
        "Landroid/view/View;",
        "L",
    ),
    filters = listOf(
        // The log line written for an ad-free user, which pins the two filters below to the tail
        // of the method.
        string("HomePremiumAdLoader"),
        string("isAdFreeUser", MatchAfterImmediately()),
        // The Unit singleton this method returns. Its field name is obfuscated, so the reference
        // is harvested from here. It is separated from the string above by the log call.
        fieldAccess(type = "Lkotlin/Unit;", location = MatchAfterWithin(3)),
    ),
)

/**
 * Starts the ad request of the loader shared by the Home mid list ad and the More tab ad.
 *
 * The method is already a no-op on its second call, so the loader tolerates never having
 * requested anything: the "ad load completed" flow simply never emits, which leaves the waiting
 * coroutine suspended before it would have made the ad container visible.
 *
 * Java equivalent:
 * ```java
 * public final void startLoad() {
 *     if (this.startedLoad) return;
 *     this.startedLoad = true;
 *     this.tracker.onRequest();
 *     if (this.adLoader != null) this.adLoader.loadAd(this.adParam);
 * }
 * ```
 */
internal object StartCommonAdLoadFingerprint : Fingerprint(
    definingClass = "Lcom/naver/linewebtoon/ad/GfpCommonAdLoader;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = emptyList(),
    filters = listOf(
        // The GFP SDK request call. The loader class is obfuscated but its parameter type is not,
        // and this is the only method on the class that hands an AdParam to the SDK.
        methodCall(parameters = listOf(AD_PARAM), returnType = "V"),
    ),
)

/**
 * Builds and starts the banner ad request for the episode list of a series.
 *
 * The loader lives in the fragment rather than in a dedicated class. Returning at the top leaves
 * the fragment's "ad loaded" flow without a value, so the collector that would attach the banner
 * stays suspended and both ad containers keep the hidden state they are given on every bind. The
 * only caller is a `LiveData` observer that discards the result inside a `try`/`catch`.
 *
 * Java equivalent:
 * ```java
 * public final void loadAd(EpisodeListBannerAdUnit adUnit) {
 *     Context context = getContext();
 *     if (context == null) return;
 *     AdParam adParam = adUnit.adParam();
 *     AdLogTracker tracker = new AdLogTracker("EpisodeListAdLoader", adParam.getAdUnitId());
 *     this.adLoader = new GfpAdLoader.Builder(context, adParam)…build();
 *     this.adLoader.loadAd();
 * }
 * ```
 */
internal object LoadEpisodeListBannerAdFingerprint : Fingerprint(
    definingClass = "Lcom/naver/linewebtoon/episode/list/OriginalTitleHomeEpisodesFragment;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    // The ad unit is an obfuscated type, so it is declared as a plain object.
    parameters = listOf("L"),
    filters = listOf(
        string("EpisodeListAdLoader"),
    ),
)

/**
 * Kicks off the ad shown under the last panel of an episode in the vertical viewer, choosing
 * between the ordinary viewer end ad and the paid product placement ad.
 *
 * It only requests a load by emitting into a shared flow, and it already returns `false` without
 * emitting when a load is in flight. Returning `false` at the top therefore means no loader is
 * ever constructed for either variant. The single caller discards the result inside a
 * `try`/`catch`.
 *
 * This covers the Originals, Canvas and short drama viewers. The branded episode viewer drives
 * [StartBrandedViewerEndAdLoadFingerprint] directly instead.
 *
 * Java equivalent:
 * ```java
 * public final boolean startAdLoad() {
 *     if (this.startedAdLoad) return false;
 *     this.startedAdLoad = true;
 *     switch (this.titleType) {
 *         case WEBTOON: this.loadEvent.tryEmit(ViewerEndAdType.Normal.INSTANCE); break;
 *         case CHALLENGE: this.loadEvent.tryEmit(ViewerEndAdType.Ppl.INSTANCE); break;
 *         default: throw new NoWhenBranchMatchedException();
 *     }
 *     return true;
 * }
 * ```
 */
internal object StartViewerEndAdLoadFingerprint : Fingerprint(
    definingClass = "Lcom/naver/linewebtoon/ad/VerticalViewerEndGfpAdLoader;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = emptyList(),
    filters = listOf(
        // The exhaustiveness throw of the Kotlin `when` over the title type. The exception class
        // is part of the Kotlin standard library, so its name survives.
        methodCall(
            definingClass = "Lkotlin/NoWhenBranchMatchedException;",
            name = "<init>",
        ),
    ),
)

/**
 * Starts the ad request of the viewer end loader, which the branded episode viewer calls directly
 * as the reader scrolls to the end.
 *
 * As with [StartCommonAdLoadFingerprint] the method is already a no-op on its second call, and
 * the layout it feeds declares its title bar, ad container and divider hidden, so nothing is
 * revealed if the ad never arrives.
 *
 * Java equivalent:
 * ```java
 * public final void startLoad() {
 *     if (this.startedLoad) return;
 *     this.startedLoad = true;
 *     this.tracker.onRequest();
 *     if (this.adLoader != null) this.adLoader.loadAd(this.adParam);
 * }
 * ```
 */
internal object StartBrandedViewerEndAdLoadFingerprint : Fingerprint(
    definingClass = "Lcom/naver/linewebtoon/ad/GfpViewerEndAdLoader;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = emptyList(),
    filters = listOf(
        // As above, the only method on this class that hands an AdParam to the SDK.
        methodCall(parameters = listOf(AD_PARAM), returnType = "V"),
    ),
)

/**
 * Loads and attaches the ad shown above the panels at the top of the viewer.
 *
 * The method already returns without attaching anything when the load does not come back with an
 * ad, and its container is an empty `wrap_content` frame whose close button is hidden until an ad
 * renders, so the row collapses to nothing. The only caller ignores the result and catches
 * everything it throws.
 *
 * Java equivalent:
 * ```java
 * public final Object attachAdViewAfterLoadCompleted(ViewGroup parent, Continuation<Unit> cont) {
 *     ...
 *     ViewerTopAdStatus status = getAndLoad(startedAt, cont);
 *     if (!(status instanceof ViewerTopAdStatus.Loaded)) return Unit.INSTANCE;
 *     attachAdView(status, parent);
 *     ...
 *     return Unit.INSTANCE;
 * }
 * ```
 */
internal object AttachViewerTopAdFingerprint : Fingerprint(
    definingClass = "Lcom/naver/linewebtoon/ad/GfpViewerTopAdManager;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Landroid/view/ViewGroup;", "Lkotlin/coroutines/e;"),
    filters = listOf(
        // The Unit singleton this suspend function returns. Its field name is obfuscated, so the
        // reference is harvested from the method's own existing "no ad" return.
        fieldAccess(type = "Lkotlin/Unit;"),
    ),
)

/**
 * Builds the intent for the full screen in-app promotion web popup.
 *
 * The popup is shown both from the Home tab popup queue and, unprompted, after actions such as
 * liking, subscribing, reading or purchasing. Both call sites go through this one factory and
 * both already handle a null intent, which the method returns for a blank promotion URL.
 *
 * The `Navigator` interface keeps its name, and this is the only method on the class with this
 * signature, so the two together pin the match.
 *
 * Java equivalent:
 * ```java
 * public final Intent createPromotionIntent(int promotionNo, String url) {
 *     if (url == null || url.isBlank()) return null;
 *     return this.navigator.createIntent(new Args.InAppPromotion(url, promotionNo));
 * }
 * ```
 */
internal object CreateInAppPromotionIntentFingerprint : Fingerprint(
    definingClass = "Lcom/naver/linewebtoon/feature/promotion/impl/PromotionManagerImpl;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Landroid/content/Intent;",
    parameters = listOf("I", "Ljava/lang/String;"),
    filters = listOf(
        methodCall(
            definingClass = "Lcom/naver/linewebtoon/navigator/Navigator;",
            returnType = "Landroid/content/Intent;",
        ),
    ),
)

/**
 * Binds the Braze in app message manager to an activity.
 *
 * Braze in app messages are server triggered marketing overlays (modal, full screen, slide up
 * and HTML), delivered by the marketing SDK rather than by the WEBTOON API. This is the only
 * registration entry point, so patching it covers every caller, including the application wide
 * lifecycle listener that would otherwise keep binding activities on its own.
 *
 * Braze class names are not obfuscated, unlike the app's own code.
 */
internal object RegisterBrazeInAppMessageManagerFingerprint : Fingerprint(
    definingClass = "Lcom/braze/ui/inappmessage/BrazeInAppMessageManager;",
    name = "registerInAppMessageManager",
    returnType = "V",
    parameters = listOf("Landroid/app/Activity;"),
)
