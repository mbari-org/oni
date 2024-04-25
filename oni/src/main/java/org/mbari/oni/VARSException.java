/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni;

public class VARSException extends RuntimeException {

    public VARSException() {
    }

    public VARSException(String s) {
        super(s);
    }

    public VARSException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public VARSException(Throwable throwable) {
        super(throwable);
    }
}