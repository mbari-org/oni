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

import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.integrator.spi.Integrator;

/**
 * Registers the {@link CommitNotifier} with every Hibernate SessionFactory. Hibernate discovers this class through
 * META-INF/services/org.hibernate.integrator.spi.Integrator, so it applies no matter how the EntityManagerFactory
 * is created.
 */
public class ChangeNotifierIntegrator implements Integrator {

    @Override
    public void integrate(Metadata metadata, BootstrapContext bootstrapContext, SessionFactoryImplementor sessionFactory) {
        var registry = sessionFactory.getServiceRegistry().requireService(EventListenerRegistry.class);
        var notifier = new CommitNotifier();
        registry.appendListeners(EventType.POST_COMMIT_INSERT, notifier);
        registry.appendListeners(EventType.POST_COMMIT_UPDATE, notifier);
        registry.appendListeners(EventType.POST_COMMIT_DELETE, notifier);
    }
}
