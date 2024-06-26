package org.mbari.oni.jpa.repositories

import org.mbari.oni.jpa.PostgresEntityManagerFactoryProvider

import scala.jdk.CollectionConverters.*

class DatabaseContainerSuite extends munit.FunSuite  {
    test("PostgreSQL container should be started"):
        assert(PostgresEntityManagerFactoryProvider.container.isRunning)
        val entityManager = PostgresEntityManagerFactoryProvider.entityManagerFactory.createEntityManager()
        val repo = ConceptRepository(entityManager)
        val all = repo.findByName("foo")
        assert(all.isEmpty)
        entityManager.close()

    test("PostgreSQL init script should have been run"):
        val em = PostgresEntityManagerFactoryProvider.entityManagerFactory.createEntityManager()
        val q = em.createNativeQuery("SELECT COUNT(*) FROM Media")
        val r = q.getResultList().asScala.toList.head.asInstanceOf[Number].longValue()
        assert(r >= 0)

}
