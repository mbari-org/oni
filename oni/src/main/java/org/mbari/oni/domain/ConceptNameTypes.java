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
 * Time: 2:23:33 PM
 * To change this template use File | Settings | File Templates.
 */
public enum ConceptNameTypes {

    PRIMARY("primary"), ALTERNATE("alternate"), COMMON("common"), FORMER("former"), SYNONYM("synonym");

    private final String type;

    ConceptNameTypes(String type) {
        this.type = type;
    }


    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return getType();
    }


}

