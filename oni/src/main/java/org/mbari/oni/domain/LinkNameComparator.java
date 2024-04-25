/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.domain;

import org.mbari.oni.etc.jdk.IgnoreCaseToStringComparator;

import java.util.Comparator;

/**
 * @author brian
 * @version $Id: $
 * @since Dec 14, 2006 3:31:47 PM PST
 */
public class LinkNameComparator<T extends ILink> implements Comparator<T> {

    private static final Comparator<String> stringComparator = new IgnoreCaseToStringComparator<String>();

    /**
     * TODO: Add JavaDoc
     *
     * @param o1
     * @param o2
     * @return
     */
    public int compare(T o1, T o2) {
        return stringComparator.compare(o1.getLinkName(), o2.getLinkName());
    }
}
