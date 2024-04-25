/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.domain;

import java.util.Comparator;

/**
 * For comparing links using linkName and linkValue fields.
 * @author brian
 */
public class LinkNameAndValueComparator implements Comparator<ILink> {

    public int compare(ILink o1, ILink o2) {
        final String s1 = o1.getLinkName() + ILink.DELIMITER + o1.getLinkValue();
        final String s2 = o2.getLinkName() + ILink.DELIMITER + o2.getLinkValue();

        return s1.compareTo(s2);
    }

}
