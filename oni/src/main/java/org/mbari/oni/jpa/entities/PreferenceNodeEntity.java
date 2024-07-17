/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.jpa.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.mbari.oni.jpa.TransactionLogger;

import java.io.Serializable;

/**
 *
 * @author brian
 */
@Entity(name = "PreferenceNode")
@Table(name = "Prefs", uniqueConstraints = {@UniqueConstraint(columnNames = {"NodeName", "PrefKey"})})
@IdClass(PreferenceNodeCompositeKey.class)
@EntityListeners({ TransactionLogger.class })
@NamedQueries({ @NamedQuery(name = "PreferenceNode.findAll",
                           query = "SELECT p FROM PreferenceNode p"),
                @NamedQuery(name = "PreferenceNode.findAllLikeNodeName",
                            query = "SELECT p FROM PreferenceNode p WHERE p.nodeName LIKE :nodeName"),
                @NamedQuery(name = "PreferenceNode.findByNodeNameAndPrefKey",
                            query = "SELECT p FROM PreferenceNode p WHERE p.nodeName = :nodeName AND p.prefKey = :prefKey") ,
                @NamedQuery(name = "PreferenceNode.findAllByNodeName",
                            query = "SELECT p FROM PreferenceNode p WHERE p.nodeName = :nodeName") })
//@Cacheable
//@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class PreferenceNodeEntity implements Serializable {

    // This composite key will generate a unique constraint on these 2 columns.
    // except in SQL Server, where the unique constrain
    @Id
    @AttributeOverrides({ @AttributeOverride(name = "nodeName", column = @Column(name = "NodeName")) ,
                          @AttributeOverride(name = "prefKey", column = @Column(name = "PrefKey")) })
    
    @Column(
        name = "NodeName",
        nullable = false,
        length = 1024
    )
    String nodeName;

    @Transient
    private String[] nodes;

    @Id 
    @Column(
        name = "PrefKey",
        nullable = false,
        length = 256
    )
    String prefKey;
    
    @Column(
        name = "PrefValue",
        nullable = false,
        length = 256
    )
    String prefValue;

    /**
     *
     * @param obj
     * @return
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        final PreferenceNodeEntity other = (PreferenceNodeEntity) obj;

        if ((this.nodeName == null) ? (other.nodeName != null) : !this.nodeName.equals(other.nodeName)) {
            return false;
        }

        if ((this.prefKey == null) ? (other.prefKey != null) : !this.prefKey.equals(other.prefKey)) {
            return false;
        }

        return true;
    }

    /**
     * @return
     */
    public String getNodeName() {
        return nodeName;
    }

    /**
     * @return
     */
    public String[] getNodes() {
        if ((nodes == null) && (nodeName != null)) {
            nodes = nodeName.split("/");

            if (nodes[0].equals("")) {
                String[] newNodes = new String[nodes.length - 1];

                for (int i = 1; i < nodes.length; i++) {
                    newNodes[i - 1] = nodes[i];
                }

                nodes = newNodes;
            }
        }

        return nodes;
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
    public String getPrefValue() {
        return prefValue;
    }

    /**
     * @return
     */
    @Override
    public int hashCode() {
        int hash = 5;

        hash = 61 * hash + ((this.nodeName != null) ? this.nodeName.hashCode() : 0);
        hash = 61 * hash + ((this.prefKey != null) ? this.prefKey.hashCode() : 0);

        return hash;
    }

    /**
     *
     * @param nodeName
     */
    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
        nodes = null;
    }

    /**
     *
     * @param prefKey
     */
    public void setPrefKey(String prefKey) {
        this.prefKey = prefKey;
    }

    /**
     *
     * @param prefValue
     */
    public void setPrefValue(String prefValue) {
        this.prefValue = prefValue;
    }

    /**
     * @return
     */
    @Override
    public String toString() {
        return getClass().getName() + "(nodeName = " + getNodeName() + ", prefKey = " + prefKey + ", prefValue = " +
               prefValue + ")";
    }

}
