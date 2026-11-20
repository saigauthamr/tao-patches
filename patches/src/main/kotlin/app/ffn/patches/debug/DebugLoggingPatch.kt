package app.ffn.patches.debug

import app.ffn.patches.shared.Constants.COMPATIBILITY_FANFICTION
import app.ffn.patches.shared.FrameParserFingerprint
import app.ffn.patches.shared.RequestExecuteFingerprint
import app.ffn.patches.shared.RequestFailureFingerprint
import app.ffn.patches.shared.RequestResponseFingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.patch.patch

private const val EXTENSION_CLASS = "Lapp/ffn/extension/DebugLog;"

@Suppress("unused")
val debugLoggingPatch = patch(
    name = "Enable debug logging",
    description = "Logs network requests, request failures and the app's internal debug messages " +
        "to logcat under the tag FFNTao. For troubleshooting only.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_FANFICTION)

    extendWith("extensions/extension.mpe")

    execute {
        // The app's logger: two static (Object, String)V functions emptied by R8 in release builds.
        val loggerType = RequestFailureFingerprint.instructionMatches[1].methodReference.definingClass
        mutableClassDefBy(loggerType).methods
            .filter { it.parameterTypes == listOf("Ljava/lang/Object;", "Ljava/lang/String;") && it.returnType == "V" }
            .forEach { logger ->
                logger.addInstructions(
                    0,
                    """
                        invoke-static { p0, p1 }, $EXTENSION_CLASS->log(Ljava/lang/Object;Ljava/lang/String;)V
                        return-void
                    """,
                )
            }

        RequestExecuteFingerprint.method.addInstructions(
            0,
            "invoke-static { p0 }, $EXTENSION_CLASS->requestStarted(Ljava/lang/Object;)V",
        )

        RequestFailureFingerprint.method.addInstructions(
            0,
            "invoke-static { p0, p2 }, $EXTENSION_CLASS->requestFailed(Ljava/lang/Object;Ljava/lang/Throwable;)V",
        )

        RequestResponseFingerprint.method.addInstructions(
            0,
            "invoke-static { p0, p2 }, $EXTENSION_CLASS->requestResponded(Ljava/lang/Object;Ljava/lang/Object;)V",
        )

        // Uses many registers, so the parameters must be passed with a range call.
        FrameParserFingerprint.method.addInstructions(
            0,
            "invoke-static/range { p0 .. p2 }, $EXTENSION_CLASS->frameReceived(Ljava/lang/Object;Ljava/lang/Object;[B)V",
        )
    }
}
