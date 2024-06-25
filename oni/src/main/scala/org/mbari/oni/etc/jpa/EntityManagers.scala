/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.etc.jpa

import jakarta.persistence.EntityManager
import org.checkerframework.checker.units.qual.N
import org.mbari.oni.MissingRootConcept

import scala.util.control.NonFatal
import org.mbari.oni.etc.jdk.Loggers.given

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
                case NonFatal(e) =>
                    log.atError.withCause(e).log("Error in transaction: " + e.getCause)
                    Left(e)
            finally
                if transaction.isActive then
//                    log.atWarn
//                        .log(
//                            "A JPA transaction was still active after commit. This is likely due to an exception during the transaction. Rolling back"
//                        )
                    transaction.rollback()
