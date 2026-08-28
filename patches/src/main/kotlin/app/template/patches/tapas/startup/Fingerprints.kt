package app.template.patches.tapas.startup

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

private const val MAIN_ACTIVITY = "Lcom/tapastic/ui/main/MainActivity;"

/**
 * Handles an in-app navigation command, such as one produced by tapping a push notification.
 *
 * Its last branch is the "downloaded episodes" command, which is the only place in the app that
 * switches to the Library tab *and* then navigates to one of the Library's sections. That branch
 * is therefore reused as the template for opening the Library on the Subscribed section, which
 * needs exactly the same two steps.
 *
 * Every type and member it touches is obfuscated, so this fingerprint exists to harvest their
 * references rather than to be patched. The R field names are kept by the resource shrinker, so
 * they are the anchors.
 *
 * Java equivalent:
 * ```java
 * public final void handleNavCommand(int destinationId) {
 *     ...
 *     } else if (destinationId == R.id.action_to_library_downloaded_episode) {
 *         selectBottomNavigationTab(R.id.library);
 *         getSharedViewModel().navigate(
 *             new ActionOnlyNavDirections(R.id.action_to_library_downloaded_series));
 *     }
 *     ...
 * }
 * ```
 */
internal object NavigateToLibrarySectionFingerprint : Fingerprint(
    definingClass = MAIN_ACTIVITY,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("I"),
    filters = listOf(
        // The guard of the "downloaded episodes" branch. Matching it first means every filter
        // below is pinned to that branch, even though the preceding branches have the same shape.
        fieldAccess(name = "action_to_library_downloaded_episode"),
        // R.id.library, the id of the Library item in the bottom navigation bar.
        // Separated from the guard by the if instruction.
        fieldAccess(name = "library", location = MatchAfterWithin(2)),
        // selectBottomNavigationTab(int)
        methodCall(
            definingClass = MAIN_ACTIVITY,
            parameters = listOf("I"),
            returnType = "V",
            location = MatchAfterImmediately(),
        ),
        // getSharedViewModel(), the only other no-argument call on the activity that follows.
        // Its return type is obfuscated, so only the parameter list is matched.
        methodCall(
            definingClass = MAIN_ACTIVITY,
            parameters = emptyList(),
            location = MatchAfterImmediately(),
        ),
        // new ActionOnlyNavDirections(...), separated by the move-result-object.
        opcode(Opcode.NEW_INSTANCE, location = MatchAfterWithin(2)),
        // The section to navigate to, and the R class shared by every navigation action id.
        fieldAccess(
            name = "action_to_library_downloaded_series",
            location = MatchAfterImmediately(),
        ),
        // The ActionOnlyNavDirections constructor.
        opcode(Opcode.INVOKE_DIRECT, location = MatchAfterImmediately()),
        // navigate(NavDirections)
        opcode(Opcode.INVOKE_VIRTUAL, location = MatchAfterImmediately()),
    ),
)

/**
 * Creates the main screen and its bottom navigation bar.
 *
 * The patch injects after the bottom navigation setup call, which is the only no-argument void
 * call the activity makes on itself here, and which onCreate runs only when there is no saved
 * instance state.
 *
 * Java equivalent:
 * ```java
 * protected void onCreate(Bundle savedInstanceState) {
 *     ...
 *     if (savedInstanceState == null) {
 *         setUpBottomNavigation();
 *     }
 *     ...
 *     onNewIntent(getIntent());
 * }
 * ```
 */
internal object MainActivityOnCreateFingerprint : Fingerprint(
    definingClass = MAIN_ACTIVITY,
    name = "onCreate",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
    filters = listOf(
        methodCall(
            definingClass = MAIN_ACTIVITY,
            parameters = emptyList(),
            returnType = "V",
        ),
    ),
)
