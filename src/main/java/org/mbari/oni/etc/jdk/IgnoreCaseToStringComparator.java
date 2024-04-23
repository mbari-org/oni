package org.mbari.oni.etc.jdk;

import java.util.Comparator;

public class IgnoreCaseToStringComparator<T> implements Comparator<T> {

    @Override
    public int compare(T s1, T s2) {
        if (s1 == null) {
            return s2 == null ? 0 : -1;
        } else if (s2 == null) {
            return 1;
        } else {
            return s1.toString().compareToIgnoreCase(s2.toString());
        }
    }
}
