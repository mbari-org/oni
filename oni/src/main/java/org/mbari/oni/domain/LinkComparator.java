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
 * For comparing links using linkName, toConcept and linkValue fields.
 * @author brian
 */
public class LinkComparator implements Comparator<ILink> {
    
    private final Comparator<String> comparator = new IgnoreCaseToStringComparator<>();

    public int compare(ILink o1, ILink o2) {
    	
        int c = comparator.compare(o1.getLinkName(), o2.getLinkName());
        
        if (c == 0) {
            c = comparator.compare(o1.getToConcept(), o2.getToConcept());
        }
        
        if (c == 0) {
            c = comparator.compare(o1.getLinkValue(), o2.getLinkValue());
        }
        
        return c;
    }
}
