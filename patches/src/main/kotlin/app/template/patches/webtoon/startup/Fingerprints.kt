package app.template.patches.webtoon.startup

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

/**
 * Works out which sub tab a launch intent asks for.
 *
 * The intent carries the sub tab as the `sub_tab` string extra, which is looked up by tab name.
 * When the extra is missing, which is the case for an ordinary launcher start, the method falls
 * back to the default sub tab of the tab that is currently selected, and that fallback is the one
 * branch the patch rewrites. Both call sites go through it: the one in `onCreate` runs only when
 * there is no saved instance state, so a rotation or a process restart still restores the tab the
 * user was on.
 *
 * The class and the returned launch target are both obfuscated, so the match is made on the
 * signature and the contents alone. `MainTab.SubTab` keeps its name, and so do its entries,
 * because the tab name lookup and `valueOf` need them, which makes the fallback call the anchor.
 *
 * Java equivalent:
 * ```java
 * public LaunchTarget resolveLaunchTarget(Intent intent) {
 *     LaunchTarget target = new LaunchTarget();
 *     String subTabName = intent.getStringExtra(MainTab.ARG_SELECT_SUB_TAB);
 *     if (TextUtils.isEmpty(subTabName)) {
 *         target.setSubTab(getDefaultSubTabOfSelectedTab());
 *         return target;
 *     }
 *     MainTab.SubTab subTab = this.availableSubTab.invoke(subTabName);
 *     target.setSubTab(subTab);
 *     target.setArguments(buildArguments(subTab, intent.getExtras()));
 *     return target;
 * }
 * ```
 */
internal object ResolveLaunchSubTabFingerprint : Fingerprint(
    // The launch target is an obfuscated type, so only "some object" is matched.
    returnType = "L",
    parameters = listOf("Landroid/content/Intent;"),
    filters = listOf(
        // The name of the extra that carries an explicitly requested sub tab.
        string("sub_tab"),
        // The check for that extra being absent, separated from the string above by the read of
        // the extra itself.
        methodCall(
            definingClass = "Landroid/text/TextUtils;",
            name = "isEmpty",
            location = MatchAfterWithin(3),
        ),
        // The fallback the branch returns, which is the call the patch replaces. Its defining
        // class is obfuscated but its return type is not, and it is separated from the check by
        // the move-result and the if instruction.
        methodCall(
            returnType = "Lcom/naver/linewebtoon/main/MainTab\$SubTab;",
            parameters = emptyList(),
            location = MatchAfterWithin(3),
        ),
        // The register the fallback is read into, which the patch writes instead.
        opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately()),
    ),
)

/**
 * Builds the intent for a navigation target, and hard codes the Home tab for the one that opens the
 * main screen.
 *
 * This is what the splash screen goes through on a cold start: it asks the navigator for the main
 * screen intent and starts it, and that intent is built with an explicit `sub_tab` extra naming the
 * Home tab. So the launch intent never leaves the sub tab unset, which is why the branch
 * [ResolveLaunchSubTabFingerprint] anchors is not the one a cold start takes.
 *
 * The navigator, its target types and the method are all obfuscated, so the match is made on the
 * signature and on the three names R8 kept: the flags every main screen start carries, the Home
 * entry of `MainTab.SubTab`, and the companion of `MainActivity` that turns a sub tab into an
 * intent.
 *
 * Java equivalent:
 * ```java
 * public Intent create(NavigationTarget target) {
 *     ...
 *     int flags = Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP;
 *     if (target instanceof NavigationTarget.Main) {
 *         Intent intent = MainActivity.Companion.intent(this.context, MainTab.SubTab.HOME);
 *         intent.putExtra("fromChurnCarePush", ((NavigationTarget.Main) target).getFromChurnCarePush());
 *         ...
 *     }
 *     ...
 * }
 * ```
 */
internal object BuildMainScreenIntentFingerprint : Fingerprint(
    returnType = "Landroid/content/Intent;",
    // The navigation target is an obfuscated type, so only "some object" is matched.
    parameters = listOf("L"),
    filters = listOf(
        // The single top and clear top flags every main screen start carries, which is the only
        // literal of its kind in the method and is read after the branch is taken.
        literal(0x24000000),
        // The Home tab the main screen intent is built for, which is the instruction the patch
        // rewrites. It is separated from the flags by the type check, the companion and the
        // context the intent is built with.
        fieldAccess(
            definingClass = "Lcom/naver/linewebtoon/main/MainTab\$SubTab;",
            name = "HOME",
            location = MatchAfterWithin(4),
        ),
        // The call the tab is handed to, which pins the match to the main screen branch rather
        // than to any other mention of the Home tab.
        methodCall(
            definingClass = "Lcom/naver/linewebtoon/main/MainActivity\$a;",
            parameters = listOf("Landroid/content/Context;", "Lcom/naver/linewebtoon/main/MainTab\$SubTab;"),
            returnType = "Landroid/content/Intent;",
            location = MatchAfterImmediately(),
        ),
    ),
)
