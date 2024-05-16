/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.jpa.repositories;

import jakarta.persistence.EntityManager;
import org.mbari.oni.jpa.entities.PreferenceNodeEntity;

import java.util.List;
import java.util.Optional;

public class PrefNodeRepository extends Repository {

    public PrefNodeRepository(EntityManager entityManager) {
        super(entityManager);
    }

    public Optional<PreferenceNodeEntity> findByNodeNameAndPrefKey(String name, String key) {
        return findByNamedQuery("PreferenceNode.findByNodeNameAndPrefKey", Map.of("nodeName", name, "prefKey", key))
                .stream()
                .findFirst();
    }

    public PreferenceNodeEntity create(String name, String key, String value) {
        var prefNode = new PreferenceNodeEntity();
        prefNode.setName(name);
        prefNode.setKey(key);
        prefNode.setValue(value);
        return entityManager.persist(prefNode);
    }

    public Optional<PreferenceNodeEntity> update(String name, String key, String value) {
        var prefNode = findByNodeNameAndPrefKey(name, key);
        prefNode.setValue(value);
        return entityManager.merge(prefNode);
    }

    public void delete(String name, String key) {
        var opt = findByNodeNameAndPrefKey(name, key);
        opt.ifPresent(prefNode -> entityManager.remove(prefNode));
    }

    public List<PreferenceNodeEntity> findByNodeName(String name) {
        return findByNamedQuery("PreferenceNode.findAllByNodeName", Map.of("nodeName", name));
    }

    public List<PreferenceNodeEntity> findByNodeNameLike(String name) {
        return findByNamedQuery("PreferenceNode.findAllLikeNodeName", Map.of("nodeName", name + '%'));
    }
}
