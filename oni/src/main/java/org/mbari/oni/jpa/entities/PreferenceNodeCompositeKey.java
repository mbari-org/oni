/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.jpa.entities;

import java.io.Serializable;
import jakarta.persistence.Embeddable;

/**
 * A {@link PreferenceNodeEntity} has a unique nodename, prefKey combination. We'll use
 * that as a composite key. This class is a construct needed by JPA to work
 * with composite keys.
 * 
 * @author brian
 */
@Embeddable
public class PreferenceNodeCompositeKey implements Serializable {

    String prefKey;
    String nodeName;

    /**
     * Constructs ...
     */
    public PreferenceNodeCompositeKey() {}

    /**
     * Constructs ...
     *
     * @param nodeName
     * @param prefKey
     */
    public PreferenceNodeCompositeKey(String nodeName, String prefKey) {
        this.nodeName = nodeName;
        this.prefKey = prefKey;
    }

    /**
     * @return
     */
    public String getPrefKey() {
        return prefKey;
    }

    /**
     * @return
     */
    public String getNodeName() {
        return nodeName;
    }

    /**
     *
     * @param key
     */
    public void setPrefKey(String key) {
        this.prefKey = key;
    }

    /**
     *
     * @param node
     */
    public void setNodeName(String node) {
        this.nodeName = node;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PreferenceNodeCompositeKey other = (PreferenceNodeCompositeKey) obj;
        if ((this.prefKey == null) ? (other.prefKey != null) : !this.prefKey.equals(other.prefKey)) {
            return false;
        }
        if ((this.nodeName == null) ? (other.nodeName != null) : !this.nodeName.equals(other.nodeName)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + (this.prefKey != null ? this.prefKey.hashCode() : 0);
        hash = 97 * hash + (this.nodeName != null ? this.nodeName.hashCode() : 0);
        return hash;
    }


}
