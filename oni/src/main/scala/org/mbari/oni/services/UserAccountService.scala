/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.services

import jakarta.persistence.EntityManagerFactory
import org.mbari.oni.{AccessDenied, AccessDeniedMissingCredentials, OniException, WrappedException}
import org.mbari.oni.domain.{UserAccount, UserAccountRoles, UserAccountUpdate}
import org.mbari.oni.jpa.EntityManagerFactories.*
import org.mbari.oni.jpa.entities.UserAccountEntity
import org.mbari.oni.jpa.repositories.UserAccountRepository
import org.mbari.oni.etc.jdk.Loggers.given

import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

class UserAccountService(entityManagerFactory: EntityManagerFactory):

    private val log = System.getLogger(getClass.getName)

    def findAll(): Either[Throwable, Seq[UserAccount]] =
        val attempt = entityManagerFactory.transaction(entityManager =>
            val repo = UserAccountRepository(entityManager)
            repo.findAll()
        )
        attempt.map(_.asScala
            .toSeq
            .map(UserAccount.from))

    def findByUserName(name: String): Either[Throwable, Option[UserAccount]] =
        val attempt = entityManagerFactory.transaction(entityManager =>
            val repo = UserAccountRepository(entityManager)
            repo.findByUserName(name)
        )
        attempt.map(_.map(UserAccount.from).toScala)

    def findAllByRole(role: String): Either[Throwable, Seq[UserAccount]] =
        val attempt = entityManagerFactory.transaction(entityManager =>
            val repo = UserAccountRepository(entityManager)
            repo.findAllByRole(role)
        )
        attempt.map(_.asScala
            .toSeq
            .map(UserAccount.from))

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
                    UserAccount.from(entity)
                case None         =>
                    throw new IllegalArgumentException(
                        s"UserAccount with username ${username} does not exist"
                    )
        )

    def verifyWriteAccess(userName: Option[String]): Either[OniException, UserAccount] =
        userName match
            case Some(name) =>
                findByUserName(name) match
                    case Left(e)        =>
                        Left(WrappedException(s"An error occurred while finding user account with username ${name}", e))
                    case Right(None)    => Left(AccessDenied(name))
                    case Right(Some(u)) =>
                        if u.role != UserAccountRoles.READONLY.getRoleName then Right(u)
                        else Left(AccessDenied(name))
            case None       => Left(AccessDeniedMissingCredentials)
