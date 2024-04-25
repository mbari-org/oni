/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.jpa;

/**
 * Created by IntelliJ IDEA.
 * User: brian
 * Date: Aug 6, 2009
 * Time: 4:05:30 PM
 * To change this template use File | Settings | File Templates.
 */
public interface IPersistentObject {

    Long getId();

    void setId(Long id);

}
