/*
 * Copyright 2024 Monterey Bay Aquarium Research Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

