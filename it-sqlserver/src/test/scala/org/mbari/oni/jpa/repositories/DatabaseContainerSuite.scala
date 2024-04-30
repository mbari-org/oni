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

package org.mbari.oni.jpa.repositories

import org.mbari.oni.jpa.AzureEntityManagerFactoryProvider

import scala.jdk.CollectionConverters.*

class DatabaseContainerSuite extends munit.FunSuite  {
  test("SqlServer container should be started"):
    assert(AzureEntityManagerFactoryProvider.container.isRunning)
    val entityManager = AzureEntityManagerFactoryProvider.entityManagerFactory.createEntityManager()
    val repo = ConceptRepository(entityManager)
    val all = repo.findByName("foo")
    assert(all.isEmpty)
    entityManager.close()

  test("SqlServer init script should have been run"):
    val em = AzureEntityManagerFactoryProvider.entityManagerFactory.createEntityManager()
    val q = em.createNativeQuery("SELECT COUNT(*) FROM Media")
    val r = q.getResultList().asScala.toList.head.asInstanceOf[Number].longValue()
    assert(r >= 0)
    
}



