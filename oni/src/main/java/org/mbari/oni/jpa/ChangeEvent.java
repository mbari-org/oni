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

package org.mbari.oni.jpa;

import org.mbari.oni.jpa.entities.*;

import java.util.Optional;

/**
 * Describes a committed change to persistent data. Instances are published on the {@link ChangeBus}, either by the
 * {@link CommitNotifier} (for changes made through Hibernate) or explicitly by code that bypasses Hibernate's entity
 * lifecycle (e.g. bulk native updates).
 *
 * @param action   what happened
 * @param dataType the kind of record that changed
 * @param id       the primary key of the affected record, or null if many/unknown records were affected
 */
public record ChangeEvent(Action action, DataType dataType, Object id) {

    public enum Action {
        CREATED,
        UPDATED,
        DELETED
    }

    public enum DataType {
        CONCEPT,
        CONCEPT_METADATA,
        CONCEPT_NAME,
        HISTORY,
        LINK_REALIZATION,
        LINK_TEMPLATE,
        MEDIA,
        PREFERENCE_NODE,
        REFERENCE,
        USER_ACCOUNT;

        public static Optional<DataType> of(Class<?> entityClass) {
            if (ConceptEntity.class.isAssignableFrom(entityClass)) return Optional.of(CONCEPT);
            if (ConceptMetadataEntity.class.isAssignableFrom(entityClass)) return Optional.of(CONCEPT_METADATA);
            if (ConceptNameEntity.class.isAssignableFrom(entityClass)) return Optional.of(CONCEPT_NAME);
            if (HistoryEntity.class.isAssignableFrom(entityClass)) return Optional.of(HISTORY);
            if (LinkRealizationEntity.class.isAssignableFrom(entityClass)) return Optional.of(LINK_REALIZATION);
            if (LinkTemplateEntity.class.isAssignableFrom(entityClass)) return Optional.of(LINK_TEMPLATE);
            if (MediaEntity.class.isAssignableFrom(entityClass)) return Optional.of(MEDIA);
            if (PreferenceNodeEntity.class.isAssignableFrom(entityClass)) return Optional.of(PREFERENCE_NODE);
            if (ReferenceEntity.class.isAssignableFrom(entityClass)) return Optional.of(REFERENCE);
            if (UserAccountEntity.class.isAssignableFrom(entityClass)) return Optional.of(USER_ACCOUNT);
            return Optional.empty();
        }
    }
}
