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

package org.mbari.oni.etc.jpa

import jakarta.persistence.{EntityManager, FlushModeType}
import org.mbari.oni.MissingRootConcept
import org.mbari.oni.etc.jdk.Loggers.given

import scala.util.control.NonFatal

object EntityManagers:

    private val log = System.getLogger(getClass.getName)

    extension (entityManager: EntityManager)
        def runTransaction[R](fn: EntityManager => R): Either[Throwable, R] =

            val transaction = entityManager.getTransaction
            transaction.begin()
            try
                val n = fn.apply(entityManager)
                transaction.commit()
                Right(n)
            catch
                case MissingRootConcept =>
                    log.atInfo.log("Missing root concept")
                    Left(MissingRootConcept)
                case NonFatal(e)        =>
                    log.atError.withCause(e).log("Error in transaction: " + e.getCause)
                    Left(e)
            finally
                if transaction.isActive then
//                    log.atWarn
//                        .log(
//                            "A JPA transaction was still active after commit. This is likely due to an exception during the transaction. Rolling back"
//                        )
                    transaction.rollback()

        /**
         * Synchronous version of runReadOnlyTransaction. Sets flush mode to COMMIT (no auto-flush) and rolls back the
         * transaction instead of committing to ensure no changes are persisted. Also sets read-only hints at the
         * Hibernate session level.
         *
         * Note: We only use Hibernate's session-level read-only mode, not JDBC connection.setReadOnly(),
         * because PostgreSQL does not allow changing the read-only property in the middle of a transaction,
         * and the connection pool may have already started implicit transaction state.
         */
        def runReadOnlyTransaction[R](fn: EntityManager => R): Either[Throwable, R] =
            val originalFlushMode = entityManager.getFlushMode
            val transaction       = entityManager.getTransaction

            // Get underlying Hibernate session and set read-only hints at session level only
            val session = entityManager.unwrap(classOf[org.hibernate.Session])
            session.setDefaultReadOnly(true)

            transaction.begin()
            try
                entityManager.setFlushMode(FlushModeType.COMMIT)
                val n = fn.apply(entityManager)
                // Rollback instead of commit to ensure no changes are persisted
                transaction.rollback()
                Right(n)
            catch
                case NonFatal(e) =>
                    log.atError.withCause(e).log("Error running read-only transaction")
                    Left(e)
            finally
                entityManager.setFlushMode(originalFlushMode)
                session.setDefaultReadOnly(false)
                if transaction.isActive then transaction.rollback()
