package org.mbari.oni.etc.jdk;

/**
 * @author Brian Schlining
 * @since 2019-08-22T17:05:00
 */
public class Preconditions {

    public static void checkArgument(boolean ok) {
        checkArgument(ok, "Argument failed validation check");
    }

    public static final void checkArgument(boolean ok, String msg) {
        if (!ok) {
            throw new IllegalArgumentException(msg);
        }
    }
}
