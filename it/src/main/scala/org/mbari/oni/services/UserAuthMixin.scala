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
import org.mbari.oni.domain.{UserAccount, UserAccountRoles}
import org.mbari.oni.etc.jdk.Strings
import org.mbari.oni.jpa.entities.TestEntityFactory

import scala.util.Try

/**
 * Mixin for running tests with a user account. The new admin user is created for each call to `runWithUserAuth` and
 * passed to the function `f`. The user account is deleted after the function `f` is executed.
 */
trait UserAuthMixin:

    def entityManagerFactory: EntityManagerFactory

    /**
     * User account service. Its needed to create and delete user accounts.
     */
    lazy val userAccountService: UserAccountService = new UserAccountService(entityManagerFactory)

    /**
     * Run a function that returns a value with a user account. The user account is created before the function `f` is
     * executed and deleted after the function `f` is executed.
     * @param f
     *   The function to run with the user account
     * @tparam T
     *   The return type of the function
     * @return
     *   The result of the function `f`
     */
    def runWithUserAuth[T](f: UserAccount => Either[Throwable, T]): Either[Throwable, T] =
        val userAccountEntity = TestEntityFactory.createUserAccount(UserAccountRoles.ADMINISTRATOR.getRoleName)
        val userAccount       = UserAccount.from(userAccountEntity)
        for
            user   <- userAccountService.create(userAccount)
            result <- f.apply(user)
            _      <- userAccountService.deleteByUserName(user.username)
        yield result

    /**
     * Run a function that returns unit with a user account. The user account is created before the function `f` is
     * executed and deleted after the function `f` is executed.
     * @param f The function to run with the user account
     * @param password The unencrypted password to use for the user account. If not provided, a random password is generated.
     */
    def testWithUserAuth(f: UserAccount => Unit, password: String = Strings.random(10)): Either[Throwable, Unit] =
        val userAccountEntity = TestEntityFactory.createUserAccount(UserAccountRoles.ADMINISTRATOR.getRoleName, password)
        val userAccount       = UserAccount.from(userAccountEntity)
        for
            user   <- userAccountService.create(userAccount)
            result <- Try(f.apply(user)).toEither
            _      <- userAccountService.deleteByUserName(user.username)
        yield ()
