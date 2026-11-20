package app.ffn.patches.debug

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags

/**
 * The network request wrapper's failure handler (m4.k.G in 66.5). It also calls the app's
 * stripped logger (E5.w.a in 66.5), which is how [DebugLoggingPatch] locates that class.
 */
internal object RequestFailureFingerprint : Fingerprint(
    returnType = "Lkotlin/Unit;",
    parameters = listOf("L", "Ljava/lang/Throwable;"),
    filters = listOf(
        string("onFailure: request canceled"),
        methodCall(
            parameters = listOf("Ljava/lang/Object;", "Ljava/lang/String;"),
            returnType = "V",
        ),
    ),
)

/** The request wrapper's execute method (m4.k.E in 66.5): launches the request coroutine. */
internal object RequestExecuteFingerprint : Fingerprint(
    classFingerprint = RequestFailureFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL, AccessFlags.DECLARED_SYNCHRONIZED),
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        methodCall(
            parameters = listOf("L", "J", "L", "Lkotlin/jvm/functions/Function2;", "I"),
        ),
    ),
)
