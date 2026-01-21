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

package org.mbari.oni.jpa.repositories;

import jakarta.persistence.EntityManager;
import org.mbari.oni.jpa.entities.PreferenceNodeEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PrefNodeRepository extends Repository {

    public PrefNodeRepository(EntityManager entityManager) {
        super(entityManager);
    }

    public Optional<PreferenceNodeEntity> findByNodeNameAndPrefKey(String name, String key) {
        return findByNamedQuery("PreferenceNode.findByNodeNameAndPrefKey", Map.of("nodeName", name, "prefKey", key))
                .stream()
                .map(PreferenceNodeEntity.class::cast)
                .findFirst();
    }

    public PreferenceNodeEntity create(String name, String key, String value) {
        var prefNode = new PreferenceNodeEntity();
        prefNode.setNodeName(name);
        prefNode.setPrefKey(key);
        prefNode.setPrefValue(value);
        entityManager.persist(prefNode);
        return prefNode;
    }

    public Optional<PreferenceNodeEntity> update(String name, String key, String value) {
        return findByNodeNameAndPrefKey(name, key).map(prefNode -> {
            prefNode.setPrefValue(value);
            return entityManager.merge(prefNode);
        });

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

    public List<PreferenceNodeEntity> findAll(int limit, int offset) {
        return findByNamedQuery("PreferenceNode.findAll", limit, offset);
    }
}
