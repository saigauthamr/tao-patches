package app.ffn.extension;

import android.util.Log;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Called from code injected by the "Enable debug logging" patch. Logs to logcat under {@link #TAG}.
 * Read with: adb logcat -s FFNTao
 */
@SuppressWarnings("unused")
public final class DebugLog {
    private static final String TAG = "FFNTao";
    private static final AtomicInteger requestCount = new AtomicInteger();
    private static final Map<Object, Long> startTimes = new WeakHashMap<>();

    private DebugLog() {
    }

    /** Replacement body of the app's own log functions, which are empty in release builds. */
    public static void log(Object owner, String message) {
        Log.d(TAG, simpleName(owner) + ": " + message);
    }

    public static void requestStarted(Object request) {
        synchronized (startTimes) {
            startTimes.put(request, System.currentTimeMillis());
        }
        Log.i(TAG, "request #" + requestCount.incrementAndGet() + " " + url(request));
    }

    public static void requestFailed(Object request, Throwable error) {
        Long start;
        synchronized (startTimes) {
            start = startTimes.remove(request);
        }
        String elapsed = start == null ? "?" : (System.currentTimeMillis() - start) + "ms";
        Log.w(TAG, "request FAILED after " + elapsed + " " + url(request), error);
    }

    /** The request URL lives in an obfuscated String field; find it by its value. */
    private static String url(Object request) {
        for (Class<?> c = request.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                if (field.getType() != String.class) continue;
                try {
                    field.setAccessible(true);
                    Object value = field.get(request);
                    if (value instanceof String) {
                        String s = (String) value;
                        if (s.startsWith("/api") || s.startsWith("http")) return s;
                    }
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                }
            }
        }
        return "<unknown url>";
    }

    private static String simpleName(Object owner) {
        return owner == null ? "null" : owner.getClass().getName();
    }
}
