/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.jpa.entities;


/**
 *
 * @author brian
 */
public class EntityUtilities {

    public static String buildTextTree(ConceptEntity concept) {
        return buildTextTree(concept, 0);
    }

    private static String buildTextTree(ConceptEntity concept, int depth) {
        final StringBuilder sb = new StringBuilder();
        String a = "";
        for (int i = 0; i < depth; i++) {
            a += "    ";
        }

        sb.append(a).append(">-- ").append(concept).append("\n");
        for (ConceptNameEntity conceptName : concept.getConceptNames()) {
            sb.append(a).append("    |-- ").append(conceptName).append("\n");
        }

        final ConceptMetadataEntity conceptMetadata = concept.getConceptMetadata();
        sb.append(a).append("    `-- ").append(conceptMetadata).append("\n");

        for (MediaEntity media : conceptMetadata.getMedias()) {
            sb.append(a).append("        |-- ").append(media).append("\n");
        }

        for (HistoryEntity obj : conceptMetadata.getHistories()) {
            sb.append(a).append("        |-- ").append(obj).append("\n");
        }

        for (LinkRealizationEntity obj : conceptMetadata.getLinkRealizations()) {
            sb.append(a).append("        |-- ").append(obj).append("\n");
        }

        for (LinkTemplateEntity obj : conceptMetadata.getLinkTemplates()) {
            sb.append(a).append("        |-- ").append(obj).append("\n");
        }

        depth++;
        for (ConceptEntity child : concept.getChildConcepts()) {
            sb.append(buildTextTree(child, depth));
        }

        return sb.toString();
    }
}

