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

import org.hibernate.Hibernate;
import org.hibernate.event.spi.PostCommitDeleteEventListener;
import org.hibernate.event.spi.PostCommitInsertEventListener;
import org.hibernate.event.spi.PostCommitUpdateEventListener;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Hibernate post-commit listener that publishes a {@link ChangeEvent} on the {@link ChangeBus} for every entity that
 * was inserted, updated or deleted by a transaction that committed successfully. Nothing is published if the
 * transaction rolls back. It is registered by {@link ChangeNotifierIntegrator}.
 */
public class CommitNotifier
        implements PostCommitInsertEventListener, PostCommitUpdateEventListener, PostCommitDeleteEventListener {

    @Override
    public void onPostInsert(PostInsertEvent event) {
        notify(ChangeEvent.Action.CREATED, event.getEntity(), event.getId());
    }

    @Override
    public void onPostUpdate(PostUpdateEvent event) {
        notify(ChangeEvent.Action.UPDATED, event.getEntity(), event.getId());
    }

    @Override
    public void onPostDelete(PostDeleteEvent event) {
        notify(ChangeEvent.Action.DELETED, event.getEntity(), event.getId());
    }

    // Nothing to do when the commit fails: no change was made, so nothing is published
    @Override
    public void onPostInsertCommitFailed(PostInsertEvent event) {}

    @Override
    public void onPostUpdateCommitFailed(PostUpdateEvent event) {}

    @Override
    public void onPostDeleteCommitFailed(PostDeleteEvent event) {}

    @Override
    public boolean requiresPostCommitHandling(EntityPersister persister) {
        return true;
    }

    private void notify(ChangeEvent.Action action, Object entity, Object id) {
        ChangeEvent.DataType.of(Hibernate.getClass(entity))
                .ifPresent(dataType -> ChangeBus.publish(action, dataType, id));
    }
}
