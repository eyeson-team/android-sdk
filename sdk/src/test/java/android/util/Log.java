package android.util;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Takes the place of the stub in android.jar, whose methods throw "not mocked" in local unit
 * tests. OkHttp's Android build calls {@link #isLoggable} when it starts and prints a stack trace
 * if that throws. Only what OkHttp and the SDK's Logger call is here; add a method when a test
 * needs another one.
 */
public final class Log {
    public static final int VERBOSE = 2;
    public static final int DEBUG = 3;
    public static final int INFO = 4;
    public static final int WARN = 5;
    public static final int ERROR = 6;
    public static final int ASSERT = 7;

    private Log() {
    }

    /** False, so OkHttp only forwards its warnings. */
    public static boolean isLoggable(String tag, int level) {
        return false;
    }

    public static int println(int priority, String tag, String msg) {
        System.out.println(tag + ": " + msg);
        return msg.length();
    }

    public static String getStackTraceString(Throwable tr) {
        if (tr == null) {
            return "";
        }
        StringWriter writer = new StringWriter();
        tr.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }
}
