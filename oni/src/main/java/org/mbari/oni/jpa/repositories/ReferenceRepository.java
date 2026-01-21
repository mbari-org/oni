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
import org.mbari.oni.jpa.entities.ReferenceEntity;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ReferenceRepository extends Repository {

    public ReferenceRepository(EntityManager entityManager) {
        super(entityManager);
    }

    public List<ReferenceEntity> findAll(int limit, int offset) {
        return findByNamedQuery("Reference.findAll", limit, offset);
    }

    public Optional<ReferenceEntity> findByDoi(URI doi) {
        List<ReferenceEntity> results = findByNamedQuery("Reference.findByDoi", Map.of("doi", doi));
        return results.stream().findFirst();
    }

    public List<ReferenceEntity> findByGlob(String glob, int limit, int offset) {
        var key = '%' + glob + '%';
        return findByNamedQuery("Reference.findByGlob", Map.of("glob", key), limit, offset);
    }

    public List<ReferenceEntity> findByConceptName(String name) {
        return findByNamedQuery("Reference.findByConceptName", Map.of("name", name));
    }

    public Optional<ReferenceEntity> findById(Long id) {
        List<ReferenceEntity> results = findByNamedQuery("Reference.findById", Map.of("id", id));
        return results.stream().findFirst();
    }

}
