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
import org.mbari.oni.etc.circe.CirceCodecs.{*, given}
import org.mbari.oni.etc.jdk.Loggers.given
import org.mbari.oni.etc.jdk.Strings
import org.mbari.oni.etc.jwt.JwtService
import org.mbari.oni.jpa.DataInitializer
import org.mbari.oni.jpa.entities.TestEntityFactory
import org.mbari.oni.services.UserAccountService
import sttp.model.StatusCode

trait UserAccountsEndpointsSuite extends EndpointsSuite with DataInitializer with EndpointsAuthMixin:

    given jwtService: JwtService             = JwtService("mbari", "foo", "bar")
    lazy val endpoints: UserAccountEndpoints = UserAccountEndpoints(entityManagerFactory)
    // userAccountService is provided by EndpointsAuthMixin (via UserAuthMixin)

    override def beforeEach(context: BeforeEach): Unit =
        super.beforeEach(context)
        userAccountService.findAll() match
            case Right(entities) =>
                entities.foreach(entity => userAccountService.deleteByUserName(entity.username))
            case Left(error)     => log.atDebug.withCause(error).log("Failed to delete all user account entities")

    def create(n: Int, role: UserAccountRoles = UserAccountRoles.READONLY): Seq[UserAccount] =
        val userAccountService = new UserAccountService(entityManagerFactory)
        val userAccounts       = Seq.newBuilder[UserAccount]
        for i <- 0 until n do
            val userEntity  = TestEntityFactory.createUserAccount(role.getRoleName)
            val userAccount = UserAccount.from(userEntity)
            userAccountService.create(userAccount) match
                case Right(user) => userAccounts += user
                case Left(error) => fail(error.getMessage)
        userAccounts.result()

    test("findAllByRoleEndpoint") {
        val userAccounts = create(3, UserAccountRoles.ADMINISTRATOR)
        create(3, UserAccountRoles.READONLY)
        create(3, UserAccountRoles.MAINTENANCE)

        runGet(
            endpoints.findAllByRoleEndpointImpl,
            s"http://test.com/v1/users/role/${UserAccountRoles.ADMINISTRATOR.getRoleName}",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[Seq[UserAccount]](response.body)
                assertEquals(obtained, userAccounts)
        )
    }
    test("findByUserNameEndpoint") {
        val userAccounts = create(3, UserAccountRoles.ADMINISTRATOR)
        val user         = userAccounts.head
        runGet(
            endpoints.findByUserNameEndpointImpl,
            s"http://test.com/v1/users/${user.username}",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[UserAccount](response.body)
                assertEquals(obtained, user)
        )
    }

    test("findAllEndpoint") {
        val userAccounts = create(11, UserAccountRoles.ADMINISTRATOR)
        runGet(
            endpoints.findAllEndpointImpl,
            "http://test.com/v1/users",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[Seq[UserAccount]](response.body)
                assertEquals(obtained, userAccounts)
        )
    }

    test("deleteByUserNameEndpoint") {
        val userAccounts = create(3, UserAccountRoles.ADMINISTRATOR)
        val user         = userAccounts.head
        runDelete(
            endpoints.deleteByUserNameEndpointImpl,
            s"http://test.com/v1/users/${user.username}",
            response => assertEquals(response.code, StatusCode.Ok),
            jwt = jwtService.authorize(jwtService.apiKey)
        )

        userAccountService.findByUserName(user.username) match
            case Right(None)         => assert(true)
            case Right(Some(entity)) =>
                fail(s"UserAccount with username '${user.username}' was found after it was deleted")
            case Left(error)         => fail(error.getMessage)
    }

    test("createEndpoint (JSON)") {
        val entity      = TestEntityFactory.createUserAccount(UserAccountRoles.ADMINISTRATOR.getRoleName)
        val userAccount = UserAccount.from(entity).copy(password = Strings.random(10))

        runPost(
            endpoints.createEndpointImpl,
            "http://test.com/v1/users",
            userAccount.stringify,
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[UserAccount](response.body)
                assertEquals(obtained.copy(id = None, password = userAccount.password, lastUpdated = None), userAccount)
            ,
            jwt = jwtService.authorize(jwtService.apiKey)
        )
    }

    test("createEndpoint (ReadOnly from JSON)") {
        val entity      = TestEntityFactory.createUserAccount(UserAccountRoles.READONLY.getRoleName)
        val userAccount = UserAccount.from(entity).copy(password = Strings.random(10))

        runPost(
            endpoints.createEndpointImpl,
            "http://test.com/v1/users",
            userAccount.stringify,
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[UserAccount](response.body)
                assertEquals(obtained.copy(id = None, password = userAccount.password, lastUpdated = None), userAccount)
            ,
            jwt = jwtService.authorize(jwtService.apiKey)
        )

    }

    test("createEndpoint (form camelCase)") {
        val entity      = TestEntityFactory.createUserAccount(UserAccountRoles.ADMINISTRATOR.getRoleName)
        val userAccount = UserAccount.from(entity).copy(password = Strings.random(10))
        val formBody    = userAccount.toFormBody

        runPost(
            endpoints.createEndpointImpl,
            "http://test.com/v1/users",
            formBody,
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[UserAccount](response.body)
                assertEquals(obtained.copy(id = None, password = userAccount.password, lastUpdated = None), userAccount)
            ,
            jwt = jwtService.authorize(jwtService.apiKey),
            contentType = "application/x-www-form-urlencoded"
        )
    }

    test("createEndpoint (form snake_case)") {
        val entity      = TestEntityFactory.createUserAccount(UserAccountRoles.ADMINISTRATOR.getRoleName)
        val userAccount = UserAccount.from(entity).copy(password = Strings.random(10))
        val formBody    = userAccount.toFormBody.replace("firstName", "first_name").replace("lastName", "last_name")

        runPost(
            endpoints.createEndpointImpl,
            "http://test.com/v1/users",
            formBody,
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[UserAccount](response.body)
                assertEquals(obtained.copy(id = None, password = userAccount.password, lastUpdated = None), userAccount)
            ,
            jwt = jwtService.authorize(jwtService.apiKey),
            contentType = "application/x-www-form-urlencoded"
        )
    }

    test("updateEndpoint") {
        val userAccounts = create(3, UserAccountRoles.ADMINISTRATOR)
        val user         = userAccounts.head
        val update       = UserAccountUpdate(role = Some(UserAccountRoles.READONLY.getRoleName))
        runPut(
            endpoints.updateEndpointImpl,
            s"http://test.com/v1/users/${user.username}",
            update.stringify,
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[UserAccount](response.body)
                assertEquals(obtained.role, update.role.getOrElse(""))
            ,
            jwt = jwtService.authorize(jwtService.apiKey)
        )
    }

    // --- Authorization: API key or administrator only

    private def newUserJson(role: UserAccountRoles = UserAccountRoles.READONLY): UserAccount =
        UserAccount.from(TestEntityFactory.createUserAccount(role.getRoleName)).copy(password = Strings.random(10))

    test("createEndpoint (administrator login)") {
        val userAccount = newUserJson()
        withRole(UserAccountRoles.ADMINISTRATOR)(jwt =>
            runPost(
                endpoints.createEndpointImpl,
                "http://test.com/v1/users",
                userAccount.stringify,
                response => assertEquals(response.code, StatusCode.Ok),
                jwt = jwt
            )
        )
        assert(userAccountService.findByUserName(userAccount.username).exists(_.isDefined))
    }

    test("createEndpoint (maintainer is denied)") {
        // A maintainer must not be able to create accounts, e.g. an administrator account for themselves
        val userAccount = newUserJson(UserAccountRoles.ADMINISTRATOR)
        withRole(UserAccountRoles.MAINTENANCE)(jwt =>
            runPost(
                endpoints.createEndpointImpl,
                "http://test.com/v1/users",
                userAccount.stringify,
                response => assertEquals(response.code, StatusCode.Unauthorized),
                jwt = jwt
            )
        )
        assertEquals(userAccountService.findByUserName(userAccount.username), Right(None))
    }

    test("createEndpoint (demoted read-only user is denied)") {
        val userAccount = newUserJson()
        withRole(UserAccountRoles.READONLY)(jwt =>
            runPost(
                endpoints.createEndpointImpl,
                "http://test.com/v1/users",
                userAccount.stringify,
                response => assertEquals(response.code, StatusCode.Unauthorized),
                jwt = jwt
            )
        )
        assertEquals(userAccountService.findByUserName(userAccount.username), Right(None))
    }

    test("createEndpoint (no token)") {
        runPost(
            endpoints.createEndpointImpl,
            "http://test.com/v1/users",
            newUserJson().stringify,
            response => assertEquals(response.code, StatusCode.Unauthorized)
        )
    }

    test("updateEndpoint (maintainer cannot promote a user)") {
        val user = create(1, UserAccountRoles.MAINTENANCE).head
        withRole(UserAccountRoles.MAINTENANCE)(jwt =>
            runPut(
                endpoints.updateEndpointImpl,
                s"http://test.com/v1/users/${user.username}",
                UserAccountUpdate(role = Some(UserAccountRoles.ADMINISTRATOR.getRoleName)).stringify,
                response => assertEquals(response.code, StatusCode.Unauthorized),
                jwt = jwt
            )
        )
        userAccountService.findByUserName(user.username) match
            case Right(Some(found)) => assertEquals(found.role, UserAccountRoles.MAINTENANCE.getRoleName)
            case other              => fail(s"Unexpected lookup result: $other")
    }

    test("deleteByUserNameEndpoint (administrator login)") {
        val user = create(1).head
        withRole(UserAccountRoles.ADMINISTRATOR)(jwt =>
            runDelete(
                endpoints.deleteByUserNameEndpointImpl,
                s"http://test.com/v1/users/${user.username}",
                response => assertEquals(response.code, StatusCode.Ok),
                jwt = jwt
            )
        )
        assertEquals(userAccountService.findByUserName(user.username), Right(None))
    }

    test("deleteByUserNameEndpoint (maintainer is denied)") {
        val user = create(1).head
        withRole(UserAccountRoles.MAINTENANCE)(jwt =>
            runDelete(
                endpoints.deleteByUserNameEndpointImpl,
                s"http://test.com/v1/users/${user.username}",
                response => assertEquals(response.code, StatusCode.Unauthorized),
                jwt = jwt
            )
        )
        assert(userAccountService.findByUserName(user.username).exists(_.isDefined))
    }
