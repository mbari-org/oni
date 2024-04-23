package org.mbari.oni.domain;


import java.util.Collection;
import java.util.Comparator;

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
     * Return all links in a collection that match a given {@link ILink}. This compares the linkName,
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
}

