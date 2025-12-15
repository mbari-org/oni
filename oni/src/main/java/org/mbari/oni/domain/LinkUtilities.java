/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.domain;


import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;


/**
 *
 * @author brian
 */
public class LinkUtilities {

    private static final Comparator<ILink> COMPARATOR = new LinkComparator();

    public static String formatAsLongString(ILink link) {
        StringBuilder sb = new StringBuilder();
        String fromConcept = link.getFromConcept();
        fromConcept = (fromConcept == null) ? ILink.VALUE_NIL : fromConcept;
        sb.append(fromConcept).append(ILink.DELIMITER);
        sb.append(link.getLinkName()).append(ILink.DELIMITER);
        sb.append(link.getToConcept()).append(ILink.DELIMITER);
        sb.append(link.getLinkValue());

        return sb.toString();
    }

    public static String formatAsString(ILink link) {
        StringBuilder sb = new StringBuilder();
        sb.append(link.getLinkName()).append(ILink.DELIMITER);
        sb.append(link.getToConcept()).append(ILink.DELIMITER);
        sb.append(link.getLinkValue());

        return sb.toString();
    }
    /**
     * Return allImpl links in a collection that match a given {@link ILink}. This compares the linkName,
     * toConcept, and linkValue fields
     *
     * @param links
     * @param templateLink
     * @return
     */
    public static Collection<ILink> findMatchingLinksIn(Collection<ILink> links, final ILink templateLink) {
        return links.stream()
                .filter(input -> COMPARATOR.compare(input, templateLink) == 0)
                .toList();
    }

/**
     * Parse a String representation of a link into a LinkNode
     * @param stringValue A String in the format produced by ILink.stringValue()
     * @return A LinkNode
     */
    public static LinkNode parseLinkNode(String stringValue) {
        List<String> tokens = Arrays.stream(stringValue.split(ILink.DELIMITER_REGEXP)).map(String::trim).toList();
        if (tokens.size() < 3 || tokens.size() > 4) {
            throw new IllegalArgumentException("Invalid link string value: " + stringValue);
        }
        var n = tokens.size() == 3 ? 0 : 1;
        String linkName = tokens.get(n);
        String toConcept = tokens.get(n + 1);
        String linkValue = tokens.get(n + 2);
        return new LinkNode(linkName, toConcept, linkValue);
    }
}

