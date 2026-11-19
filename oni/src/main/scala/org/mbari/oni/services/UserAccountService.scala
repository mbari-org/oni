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

package org.mbari.oni.services

import jakarta.persistence.EntityManagerFactory
import org.mbari.oni.domain.{UserAccount, UserAccountRoles, UserAccountUpdate}
import org.mbari.oni.jpa.EntityManagerFactories.*
import org.mbari.oni.jpa.entities.UserAccountEntity
import org.mbari.oni.jpa.repositories.UserAccountRepository
import org.mbari.oni.{AccessDenied, AccessDeniedMissingCredentials, ItemNotFound, OniException, WrappedException}

import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

class UserAccountService(entityManagerFactory: EntityManagerFactory):

    private val log = System.getLogger(getClass.getName)

    def findAll(): Either[Throwable, Seq[UserAccount]] =
        val attempt = entityManagerFactory.readOnlyTransaction(entityManager =>
            val repo = UserAccountRepository(entityManager)
            repo.findAll()
        )
        attempt.map(
            _.asScala
                .toSeq
                .map(UserAccount.from)
        )

    def findByUserName(name: String): Either[Throwable, Option[UserAccount]] =
        val attempt = entityManagerFactory.readOnlyTransaction(entityManager =>
            val repo = UserAccountRepository(entityManager)
            repo.findByUserName(name)
        )
        attempt.map(_.map(UserAccount.from).toScala)

    /**
     * Find the accounts that have a role, using the same rule as every access check: each account's effective role (an
     * unrecognized stored role counts as ReadOnly). That can't be expressed as a database query, so all accounts are
     * loaded and filtered; there are few enough user accounts for that to be cheap.
     *
     * @param role
     *   A role name, matched exactly but ignoring case. An unrecognized name matches no accounts.
     */
    def findAllByRole(role: String): Either[Throwable, Seq[UserAccount]] =
        UserAccountRoles.matchExactly(role).toScala match
            case None            => Right(Nil)
            case Some(requested) => findAll().map(_.filter(_.effectiveRole == requested))

    def deleteByUserName(name: String): Either[Throwable, Unit] =
        entityManagerFactory.transaction(entityManager =>
            val repo = UserAccountRepository(entityManager)
            repo.findByUserName(name).toScala match
                case Some(entity) => repo.delete(entity)
                case None         => throw ItemNotFound(s"UserAccount with username ${name} does not exist")
        )

    def create(userAccount: UserAccount): Either[Throwable, UserAccount] =
        entityManagerFactory.transaction(entityManager =>
            val repo = UserAccountRepository(entityManager)
            repo.findByUserName(userAccount.username).toScala match
                case Some(_) =>
                    throw new IllegalArgumentException(
                        s"UserAccount with username ${userAccount.username} already exists"
                    )
                case None    =>
                    val entity = userAccount.toEntity
                    repo.create(entity)
                    UserAccount.from(entity)
        )

    def update(username: String, userAccount: UserAccountUpdate): Either[Throwable, UserAccount] =
        entityManagerFactory.transaction(entityManager =>
            val repo = UserAccountRepository(entityManager)
            repo.findByUserName(username).toScala match
                case Some(entity) =>
                    userAccount.password.foreach(entity.setPassword)
                    userAccount.role.foreach(entity.setRole)
                    userAccount.affiliation.foreach(entity.setAffiliation)
                    userAccount.firstName.foreach(entity.setFirstName)
                    userAccount.lastName.foreach(entity.setLastName)
                    userAccount.email.foreach(entity.setEmail)
                    // Flush so that the optimistic lock version (i.e. lastUpdated) is assigned
                    // before we snapshot the entity into a DTO
                    entityManager.flush()
                    UserAccount.from(entity)
                case None         =>
                    throw ItemNotFound(s"UserAccount with username ${username} does not exist")
        )

    def verifyWriteAccess(userName: Option[String]): Either[OniException, UserAccount] =
        userName match
            case Some(name) =>
                findByUserName(name) match
                    case Left(e)        =>
                        Left(WrappedException(s"An error occurred while finding user account with username ${name}", e))
                    case Right(None)    => Left(AccessDenied(name))
                    case Right(Some(u)) =>
                        // Allow-list: only Admin and Maint may write. Anything else (ReadOnly, Locked, or an
                        // unrecognized role string) is denied.
                        if u.canWrite then Right(u)
                        else Left(AccessDenied(name))
            case None       => Left(AccessDeniedMissingCredentials)
