/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.domain;

/**
 * Created by IntelliJ IDEA.
 * User: brian
 * Date: Aug 5, 2009
 * Time: 2:02:16 PM
 * To change this template use File | Settings | File Templates.
 */
public enum ConceptTypes {

//    /**
//     * Designation for Concept that represents has a lithology structure.
//     */
//    String LITHOLOGY = "lithology";
//
//    /**
//     * Description of the Field
//     */
//    String ORIGINATOR_UNKNOWN = "unknown";
//
//    /**
//     * Designation for Concept that represents has a taxonomy structure.
//     */
//    String TAXONOMY = "taxonomy";

    LITHOLOGY("lithology"), TAXONOMY("taxonomy"), UNSPECIFIED("unspecified");

    private final String type;

    ConceptTypes(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }


}
