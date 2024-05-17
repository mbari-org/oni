/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.services

import jakarta.persistence.EntityManagerFactory
import org.mbari.oni.domain.{UserAccount, UserAccountUpdate}
import org.mbari.oni.jpa.EntityManagerFactories.*
import org.mbari.oni.jpa.repositories.UserAccountRepository

import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

class UserAccountService(entityManagerFactory: EntityManagerFactory):

    def findAll(): Either[Throwable, Seq[UserAccount]] =
        entityManagerFactory.transaction(entityManager =>
            val repo = UserAccountRepository(entityManager)
            repo.findAll()
                .asScala
                .toSeq
                .map(UserAccount.from)
        )

    def findByUserName(name: String): Either[Throwable, Option[UserAccount]] =
        entityManagerFactory.transaction(entityManager =>
            val repo = UserAccountRepository(entityManager)
            repo.findByUserName(name)
                .map(UserAccount.from)
                .toScala
        )

    def findAllByRole(role: String): Either[Throwable, Seq[UserAccount]] =
        entityManagerFactory.transaction(entityManager =>
            val repo = UserAccountRepository(entityManager)
            repo.findAllByRole(role)
                .asScala
                .toSeq
                .map(UserAccount.from)
        )

    def deleteByUserName(name: String): Either[Throwable, Unit] =
        entityManagerFactory.transaction(entityManager =>
            val repo = UserAccountRepository(entityManager)
            repo.findByUserName(name).toScala match
                case Some(entity) => repo.delete(entity)
                case None         => throw new IllegalArgumentException(s"UserAccount with username ${name} does not exist")
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

    def update(userAccount: UserAccountUpdate): Either[Throwable, UserAccount] =
        entityManagerFactory.transaction(entityManager =>
            val repo = UserAccountRepository(entityManager)
            repo.findByUserName(userAccount.username).toScala match
                case Some(entity) =>
                    userAccount.password.foreach(entity.setPassword)
                    userAccount.role.foreach(entity.setRole)
                    userAccount.affiliation.foreach(entity.setAffiliation)
                    userAccount.firstName.foreach(entity.setFirstName)
                    userAccount.lastName.foreach(entity.setLastName)
                    userAccount.email.foreach(entity.setEmail)
                    UserAccount.from(entity)
                case None         =>
                    throw new IllegalArgumentException(
                        s"UserAccount with username ${userAccount.username} does not exist"
                    )
        )

