package app.ffn.patches.network

import app.ffn.patches.shared.Constants.COMPATIBILITY_FANFICTION
import app.ffn.patches.shared.RequestExecuteFingerprint
import app.ffn.patches.shared.RequestFailureFingerprint
import app.ffn.patches.shared.RequestResultFingerprint
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.patch.patch
import app.morphe.patcher.string
import app.morphe.patcher.util.addInstructionsAtControlFlowLabel
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod

private const val EXTENSION_CLASS = "Lapp/ffn/extension/RequestDedup;"

// Added to the request class: runs the base class callback pipeline. The request class overrides
// that method to send the request instead, so only code inside the class can reach it (invoke-super).
private const val RUN_CALLBACKS_METHOD = "ffnRunCallbacks"

@Suppress("unused")
val mergeDuplicateRequestsPatch = patch(
    name = "Merge duplicate requests",
    description = "Sends identical API requests that are in flight at the same time only once, " +
        "and shares the response. The server answers one request at a time, so every duplicate removed " +
        "makes the rest of the app respond sooner.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_FANFICTION)

    extendWith("extensions/extension.mpe")

    execute {
        val requestClass = RequestFailureFingerprint.classDef
        val resultMatch = RequestResultFingerprint.instructionMatches
        val resultField = resultMatch[0].fieldReference
        val baseRunCallbacks = resultMatch[1].methodReference

        requestClass.methods.add(
            ImmutableMethod(
                requestClass.type,
                RUN_CALLBACKS_METHOD,
                listOf(),
                "V",
                AccessFlags.PUBLIC.value or AccessFlags.FINAL.value,
                null,
                null,
                MutableMethodImplementation(1),
            ).toMutable().apply {
                addInstructions(
                    0,
                    """
                        invoke-super { p0 }, $baseRunCallbacks
                        return-void
                    """,
                )
            },
        )

        // Fill in the extension's placeholders.
        val requestType = requestClass.type
        val executeMethod = RequestExecuteFingerprint.method
        val extension = mutableClassDefBy(EXTENSION_CLASS)
        fun placeholder(name: String, body: String) =
            extension.methods.first { it.name == name }.addInstructions(0, body)

        placeholder(
            "getResult",
            """
                check-cast p0, $requestType
                iget-object p0, p0, $resultField
                return-object p0
            """,
        )
        placeholder(
            "setResult",
            """
                check-cast p0, $requestType
                iput-object p1, p0, $resultField
                return-void
            """,
        )
        placeholder(
            "runCallbacks",
            """
                check-cast p0, $requestType
                invoke-virtual { p0 }, $requestType->$RUN_CALLBACKS_METHOD()V
                return-void
            """,
        )
        placeholder(
            "execute",
            """
                check-cast p0, $requestType
                invoke-virtual { p0 }, $requestType->${executeMethod.name}()V
                return-void
            """,
        )

        // Followers of an in-flight identical request skip sending.
        executeMethod.addInstructions(
            0,
            """
                invoke-static { p0 }, $EXTENSION_CLASS->shouldDefer(Ljava/lang/Object;)Z
                move-result v0
                if-eqz v0, :send
                return-void
                :send
                nop
            """,
        )

        // The leader's result is stored; share it before its own callbacks run.
        // Several branches jump to this call, so the hook must take over its label.
        resultMatch[1].let { superCall ->
            val request = superCall.getInstruction<FiveRegisterInstruction>().registerC
            RequestResultFingerprint.method.addInstructionsAtControlFlowLabel(
                superCall.index,
                "invoke-static { v$request }, $EXTENSION_CLASS->onSuccess(Ljava/lang/Object;)V",
            )
        }

        // Every failure and cancellation of any request passes through the base class failure method.
        Fingerprint(
            definingClass = requestClass.superclass!!,
            accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
            returnType = "V",
            parameters = listOf("Ljava/lang/Throwable;"),
            filters = listOf(string("e")),
        ).method.addInstructions(
            0,
            "invoke-static { p0 }, $EXTENSION_CLASS->onFailure(Ljava/lang/Object;)V",
        )
    }
}
