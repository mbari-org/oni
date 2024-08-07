/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.jpa.repositories;


import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Query;
import org.mbari.oni.etc.jdk.Logging;
import org.mbari.oni.jpa.entities.PreferenceNodeEntity;


/**
 * Created by IntelliJ IDEA.
 * User: brian
 * Date: Aug 25, 2009
 * Time: 9:49:54 AM
 * To change this template use File | Settings | File Templates. 
 */
public class VarsUserPreferences extends AbstractPreferences {

    private static final Logging log = new Logging(VarsUserPreferences.class);
    private final EntityManagerFactory entityManagerFactory;

    /**
     * This is the main constructor that takes in a VARSPreferences object that will serve
     * as the parent preference to the new VARSPreference that will be created with the
     * name that is passed in
     *
     * @param  parent This is the VARSPreferences object that will be the parent of the new
     * VARSPreferences object created with this constructor
     * @param  name This is the name that will be used for the new VARSPreferences node
     */
    public VarsUserPreferences(EntityManagerFactory entityManagerFactory, VarsUserPreferences parent, String name) {
        super(parent, name);
        this.entityManagerFactory = entityManagerFactory;
    }


    /**`
     * This method puts the give key/value association into this preference node (and persists it to the database)
     *
     * @param  key This is the key that will be used to insert and extract the associated value
     * from the database
     * @param  value This is the value that is associated with the give key and will be stored
     * in the database
     */
    protected void putSpi(String key, String value) {
        /*
         * Check to see if the node/key combo exists in the database already. If so
         * we update it with the new value. If not we insert a record.
         */
        try {

            PreferenceNodeEntity node = findByKey(key);
            if (node == null) {
                node = new PreferenceNodeEntity();
                node.setNodeName(absolutePath());
                node.setPrefKey(key);
                node.setPrefValue(value);
                insert(node);
            }
            else {
                if (!value.equals(node.getPrefValue())) {
                    node.setPrefValue(value);
                    update(node);
                }
            }
        }
        catch (Exception e) {
            log.atError().withCause(e).log("Failed to on call of putSpi(" + key + ", " + value + ")");
        }
    }

    private void insert(PreferenceNodeEntity node) {
        try (EntityManager entityManager = entityManagerFactory.createEntityManager()) {
            EntityTransaction entityTransaction = entityManager.getTransaction();
            entityTransaction.begin();
            entityManager.persist(node);
            entityTransaction.commit();
        }
        catch (Exception e) {
            log.atError().withCause(e).log("Failed to insert node");
        }
    }

    private void update(PreferenceNodeEntity node) {
        try(EntityManager entityManager = entityManagerFactory.createEntityManager()) {
            EntityTransaction entityTransaction = entityManager.getTransaction();
            entityTransaction.begin();
            entityManager.merge(node);
            entityTransaction.commit();
        }
        catch (Exception e) {
            log.atError().withCause(e).log("Failed to update node");
        }
    }

    private void delete(PreferenceNodeEntity node) {
        try (EntityManager entityManager = entityManagerFactory.createEntityManager()) {
            EntityTransaction entityTransaction = entityManager.getTransaction();
            entityTransaction.begin();
            node = entityManager.merge(node);
            entityManager.remove(node);
            entityTransaction.commit();
        }
        catch (Exception e) {
            log.atError().withCause(e).log("Failed to delete node");
        }
    }




    /**
     * Executes a named query using a map of named parameters
     *
     * @param name
     *            The name of the query to execute
     * @param namedParameters
     *            A Map<String, Object> of the 'named' parameters to assign in
     *            the query
     * @return A list of objects returned by the query.
     */
    public List<PreferenceNodeEntity> findByNamedQuery(String name, Map<String, Object> namedParameters) {
        if (log.logger().isLoggable(System.Logger.Level.DEBUG)) {
            StringBuilder sb = new StringBuilder("Executing FIND using named query '");
            sb.append(name).append("'");
            if (namedParameters.size() > 0) {
                sb.append(" with parameters:\n");
                for (String string : namedParameters.keySet()) {
                    sb.append("\t").append(string).append(" = ").append(namedParameters.get(string));
                }
            }
            log.atDebug().log(sb.toString());
        }
        List<PreferenceNodeEntity> resultList = List.of();
        try(EntityManager entityManager = entityManagerFactory.createEntityManager()) {
            Query query = entityManager.createNamedQuery(name);
            namedParameters.forEach(query::setParameter);
            resultList = query.getResultList();
        }
        catch (Exception e) {
            log.atError().withCause(e).log("Failed to execute named query " + name);
        }

        return resultList;
    }

     /**
     * This method returns the value associated with the specified key at the
     * preference node, or <code>null</code> if there is no association for this key,
     * or the association cannot be determined at this time.  It is guaranteed that
     * <code>key</code> is non-null.  Also, it is guaranteed that this node has not
     * been removed.
     *
     * @param  key The key to be searched for
     * @return  A String that is the value associated with the given key
     */
    protected String getSpi(String key) {
        String prefValue = null;

        try {
            PreferenceNodeEntity node = findByKey(key);
            if (node != null) {
                prefValue = node.getPrefValue();
            }
        }
        catch (Exception e) {
            log.atError().withCause(e).log("Call of getSpi(" + key + ") failed");
        }

        return prefValue;
    }

    /**
     *  Remove the association (if any) for the specified key at this preference node.  It is
     * guaranteed that <code>key</code> is non-null.  Also, it is guaranteed that this node
     * has not been removed.
     *
     * @param  key This is the key (name) of the preference to be removed
     */
    protected void removeSpi(String key) {
        try {
            PreferenceNodeEntity node = findByKey(key);
            delete(node);
        }
        catch (Exception e) {
            log.atError().withCause(e).log("Failed to retrieve keys for " + absolutePath());
        }
    }

    /**
     * This method rmoves this prefernce node, invalidating it and any prefernces that it contains.  The named
     * child will have no descendants at the time this invocation is made (the removeNode() will be called
     * recursively from the bottom up.
     *
     * @throws  BackingStoreException - if something goes wrong with the removal from the back end storage and
     * this operation cannot be peformed successfully
     */
    protected void removeNodeSpi() throws BackingStoreException {
        try {
            List<PreferenceNodeEntity> preferenceNodes = findByNodeNameLike(absolutePath());
            for(PreferenceNodeEntity node : preferenceNodes) {
                delete(node);
            }
        }
        catch (Exception e) {
            log.atError().withCause(e).log("Failed to retrieve keys for " + absolutePath());
            throw new BackingStoreException(e);
        }
    }

    /**
     * Returns allImpl of the keys that have an associated value in this preference node.  The returned array will
     * have size of zero if this node has no preferences.
     *
     * @return  String[] that contains the keys of the preferences that have associated values at this node
     * @throws  BackingStoreException - if something goes wrong and the operation cannot be completed successfully
     */
    protected String[] keysSpi() throws BackingStoreException {
        ArrayList keys = new ArrayList();
        try {
            Collection<PreferenceNodeEntity> preferenceNodes = findByNodeName(absolutePath());
            for (PreferenceNodeEntity node : preferenceNodes) {
                keys.add(node.getPrefKey());
            }
        }
        catch (Exception e) {
            log.atError().withCause(e).log("Failed to retrieve keys for " + absolutePath());
            throw new BackingStoreException(e);
        }

        return (String[]) keys.toArray(new String[keys.size()]);
    }

/**
     * This method returns an array of strings that represent the names of the children that are directly
     * below this node.
     *
     * @return  Description of the Return Value
     * @returns  String [] which is the list of children node names directly below this node
     * @throws  BackingStoreException - if anything goes wrong and the operation cannot complete
     */
    protected String[] childrenNamesSpi() throws BackingStoreException {
        List keys = new ArrayList();
        String parentNode = absolutePath();
        try {

            List<PreferenceNodeEntity> preferenceNodes = findByNodeNameLike(parentNode);
            for (PreferenceNodeEntity node : preferenceNodes) {
                 // Grab the next child name
                String nodeName = node.getNodeName();

                // The first thing to do is strip off the base path
                String childPath = nodeName.substring(parentNode.length(), nodeName.length());

                // Now check to see if the child without base name starts with a / and if so,
                // strip it off
                if (childPath.startsWith("/")) {
                    childPath = childPath.substring(1, childPath.length());
                }

                // Now we must take only the part up to the first / (or the whole thing if there is no /)
                String bareName = null;
                if (childPath.indexOf("/") >= 0) {
                    bareName = childPath.substring(0, childPath.indexOf("/"));
                }
                else {
                    bareName = childPath;
                }

                if ((!keys.contains(bareName)) && (bareName.compareTo("") != 0)) {
                    keys.add(bareName);
                }
            }

        }
        catch (Exception e) {
            log.atError().withCause(e).log("Failed to retrieve keys for " + absolutePath());
            throw new BackingStoreException(e);
        }

        return (String[]) keys.toArray(new String[keys.size()]);
    }


    /**
     * This method is used to get a child node with a specific name from the current node
     *
     * @param  name The name of the child node to return from the current node
     * @return  AbstractPreferences This is the new VARSPreferences that has the name supplied
     * in the parameter.  If no node exists, one will be created.
     */
    protected AbstractPreferences childSpi(String name) {
        // Return a new VARSPreferences with the name of the node
        return new VarsUserPreferences(entityManagerFactory, this, name);
    }

    protected void syncSpi() throws BackingStoreException{
        // Do nothing
    }

    protected void flushSpi() throws BackingStoreException {
        // No implementation

    }

    /**
     * This method is used to copy one preference 'tree' to another.  It provides a 'deep' copy.
     *
     * @param  source This is the Preferences object that contains the preferences object you want to copy
     * @param  destination This is the Preferences object that contains the preferences object you want to
     * copy to
     */
    public static void copyPrefs(Preferences source, Preferences destination) {
        try {
            // Copy the key/value pairs first
            String[] prefKeys = source.keys();
            for (int i = 0; i < prefKeys.length; i++) {
                destination.put(prefKeys[i], source.get(prefKeys[i], ""));
            }

            // Now grab allImpl the names of the children nodes
            String[] childrenNames = source.childrenNames();

            // Recursively copy the preference nodes
            for (int i = 0; i < childrenNames.length; i++) {
                copyPrefs(source.node(childrenNames[i]),
                        destination.node(childrenNames[i]));
            }
        }
        catch (Exception e) {
            //log.error("Failed to copy preferences.", e);
        }
    }

    private PreferenceNodeEntity findByKey(String key) {
        PreferenceNodeEntity node = null;
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("nodeName", absolutePath());
        params.put("prefKey", key);
        List<PreferenceNodeEntity> preferenceNodes = findByNamedQuery("PreferenceNodeEntity.findByNodeNameAndPrefKey", params);
        if (preferenceNodes.size() > 0) {
            node = preferenceNodes.get(0);
        }
        return node;
    }

    private List<PreferenceNodeEntity> findByNodeName(String nodeName) {
        return findByNamedQuery("PreferenceNodeEntity.findAllByNodeName", Map.of("nodeName", nodeName));
    }

    private List<PreferenceNodeEntity> findByNodeNameLike(String nodeName) {
        return findByNamedQuery("PreferenceNodeEntity.findAllLikeNodeName", Map.of("nodeName", nodeName + "%"));
    }

}
