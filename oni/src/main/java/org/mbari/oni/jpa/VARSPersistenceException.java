/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.jpa;

import org.mbari.oni.VARSException;

/**
 * Thrown when something bad happens in the DataPersistenceService
 * @author brian
 */
public class VARSPersistenceException extends VARSException {

    public VARSPersistenceException(Throwable throwable) {
        super(throwable);
    }

    public VARSPersistenceException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public VARSPersistenceException(String s) {
        super(s);
    }

    public VARSPersistenceException() {
    }

}
