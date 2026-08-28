package app.template.patches.webtoon.recommend

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.Opcode

/**
 * Converts the `viewerEndRecommendPersonal.json` response into the sealed "what to show" model
 * that drives the recommend popup shown after the last episode of a series.
 *
 * The response class survives obfuscation because its field names are the Gson wire format, and
 * the method keeps its name because it is part of the app's data layer API. The sealed model
 * type it returns *is* obfuscated, which is why the return type is matched as a bare object.
 *
 * The first branch is the one the patch reuses: when the response carries no `area`, the method
 * returns the sealed type's "no popup" singleton. Every consumer already handles that case, so
 * the patch only has to make the method take that branch unconditionally.
 *
 * Java equivalent:
 * ```java
 * public ViewerEndRecommend asModel() {
 *     if (this.area == null) {
 *         return ViewerEndRecommend.NoPopup.INSTANCE;
 *     }
 *     if (this.inAppPreviewUrl != null && this.recommendTitles.firstOrNull() != null) {
 *         return new ViewerEndRecommend.BottomSheetPopup(...);
 *     }
 *     if (this.inAppPopupUrl != null) {
 *         return new ViewerEndRecommend.ModalWebPopup(...);
 *     }
 *     return new ViewerEndRecommend.NativePopup(...);
 * }
 * ```
 */
internal object ViewerEndRecommendAsModelFingerprint : Fingerprint(
    definingClass =
        "Lcom/naver/linewebtoon/data/network/internal/webtoon/model/ViewerEndRecommendResponse;",
    name = "asModel",
    // The sealed model type is obfuscated, so only "some object" is matched.
    returnType = "L",
    parameters = emptyList(),
    filters = listOf(
        // The `area` guard. `area` is read again further down, so this deliberately relies on
        // the first match being the guard at the very top of the method.
        fieldAccess(name = "area"),
        opcode(Opcode.IF_NEZ, MatchAfterImmediately()),
        // The "no popup" singleton the guard returns. Its class and field names are obfuscated,
        // so the reference is harvested from here rather than spelled out.
        opcode(Opcode.SGET_OBJECT, MatchAfterImmediately()),
    ),
)
