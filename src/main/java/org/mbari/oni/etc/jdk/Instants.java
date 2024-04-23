package org.mbari.oni.etc.jdk;

import java.sql.Timestamp;
import java.time.Instant;

public class Instants {

    public static Instant from(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return timestamp.toInstant();
    }
}
