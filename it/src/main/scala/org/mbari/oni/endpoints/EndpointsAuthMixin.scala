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

package org.mbari.oni.endpoints

import org.mbari.oni.domain.{UserAccount, UserAccountRoles, UserAccountUpdate}
import org.mbari.oni.etc.jdk.Strings
import org.mbari.oni.etc.jwt.JwtService
import org.mbari.oni.jpa.entities.TestEntityFactory
import org.mbari.oni.services.UserAuthMixin

/**
 * Helpers for endpoint tests that need a JWT for a user with a specific role.
 */
trait EndpointsAuthMixin extends UserAuthMixin:
    this: munit.FunSuite =>

    def jwtService: JwtService

    /**
     * Create a temporary user with the given role, run `f` with a JWT for that user, then delete the user.
     *
     * `JwtService.login` only issues tokens to administrators and maintainers. To get a token for a READONLY user the
     * user is logged in as a maintainer and then demoted. This mirrors a real scenario (a user demoted while holding a
     * still-valid token) and makes sure the request reaches the service-layer access checks instead of being rejected
     * for a missing token.
     */
    def withRole(role: UserAccountRoles)(f: Option[String] => Unit): Unit =
        val password  = Strings.random(12)
        val loginRole = if role == UserAccountRoles.READONLY then UserAccountRoles.MAINTENANCE else role
        val entity    = TestEntityFactory.createUserAccount(loginRole.getRoleName, password)

        userAccountService.create(UserAccount.from(entity)) match
            case Left(error) => fail(s"Failed to create test user: $error")
            case Right(user) =>
                try
                    val jwt = jwtService.login(user.username, password, user.toEntity)
                    assert(jwt.isDefined, s"Failed to log in test user with role ${loginRole.getRoleName}")
                    if role != loginRole then
                        userAccountService.update(user.username, UserAccountUpdate(role = Some(role.getRoleName))) match
                            case Left(error) => fail(s"Failed to change test user role to ${role.getRoleName}: $error")
                            case Right(_)    => ()
                    f(jwt)
                finally userAccountService.deleteByUserName(user.username)
