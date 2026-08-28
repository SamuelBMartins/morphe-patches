package app.template.patches.tapas.startup

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.COMPATIBILITY_TAPAS
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

/**
 * The Library section to open. Every navigation action id lives on the same generated R class as
 * the one the fingerprint harvests, so only the field name has to be substituted.
 */
private const val SUBSCRIBED_ACTION_NAME = "action_to_library_subscribed"

@Suppress("unused")
val openLibrarySubscribedOnStartupPatch = bytecodePatch(
    name = "Open Library on startup",
    description = "Opens the Library tab on its Subscribed list at startup, " +
            "instead of the Home tab. " +
            "Opening the app from a notification or a link is not affected.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_TAPAS)

    execute {
        // Rather than spelling out the obfuscated names, which change with every app update, the
        // references are taken from the app's own "switch to the Library tab, then open one of its
        // sections" sequence. Only the section id is swapped.
        val references = NavigateToLibrarySectionFingerprint.let { fingerprint ->
            fingerprint.method.let { method ->
                fingerprint.instructionMatches.map {
                    method.getInstruction<ReferenceInstruction>(it.index).reference
                }
            }
        }

        val libraryTabId = references[1]
        val selectBottomNavigationTab = references[2]
        val getSharedViewModel = references[3]
        val navDirectionsClass = references[4]
        val navDirectionsConstructor = references[6]
        val navigate = references[7]

        val subscribedActionId = (references[5] as FieldReference).let {
            "${it.definingClass}->$SUBSCRIBED_ACTION_NAME:${it.type}"
        }

        MainActivityOnCreateFingerprint.method.apply {
            // Inserting after the bottom navigation setup call puts this inside onCreate's
            // "no saved instance state" branch, so the jump only happens on a cold start and does
            // not fight the restored tab after a rotation or a process restart. It is also before
            // onCreate forwards the launch intent to onNewIntent, so a notification or a link
            // still wins: its own navigation command is handled after this one.
            val index = MainActivityOnCreateFingerprint.instructionMatches.first().index + 1

            // v0 and v1 are both written before they are next read, so they are free here.
            // v2 is the monitor register of the surrounding synchronized block and v3 is still
            // live, so neither may be touched.
            addInstructions(
                index,
                """
                    # Show the Library tab. This is the app's own tab switch helper, so it also
                    # suppresses the analytics event that a real tab tap would send.
                    sget v0, $libraryTabId
                    invoke-virtual { p0, v0 }, $selectBottomNavigationTab

                    # Ask the Library to show its Subscribed list. The request is delivered through
                    # the shared view model, which holds it until the Library fragment starts
                    # observing, so it does not matter that the tab is not attached yet.
                    new-instance v0, $navDirectionsClass
                    sget v1, $subscribedActionId
                    invoke-direct { v0, v1 }, $navDirectionsConstructor
                    invoke-virtual { p0 }, $getSharedViewModel
                    move-result-object v1
                    invoke-virtual { v1, v0 }, $navigate
                """,
            )
        }
    }
}
