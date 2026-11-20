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

import org.mbari.oni.domain.{UserAccount, UserAccountInfo, UserAccountRoles, UserAccountUpdate}
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

    /** Responses must never include password hashes (or the related isEncrypted flag) */
    private def assertNoPasswords(body: Either[String, String]): Unit =
        body match
            case Left(error) => fail(s"Expected a successful response but got: $error")
            case Right(json) =>
                assert(!json.contains("\"password\""), s"Response contains a password field: $json")
                assert(!json.contains("\"isEncrypted\""), s"Response contains an isEncrypted field: $json")

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
                assertNoPasswords(response.body)
                val obtained = checkResponse[Seq[UserAccountInfo]](response.body)
                assertEquals(obtained, userAccounts.map(UserAccountInfo.from))
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
                assertNoPasswords(response.body)
                val obtained = checkResponse[UserAccountInfo](response.body)
                assertEquals(obtained, UserAccountInfo.from(user))
        )
    }

    test("findAllEndpoint") {
        val userAccounts = create(11, UserAccountRoles.ADMINISTRATOR)
        runGet(
            endpoints.findAllEndpointImpl,
            "http://test.com/v1/users",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                assertNoPasswords(response.body)
                val obtained = checkResponse[Seq[UserAccountInfo]](response.body)
                assertEquals(obtained, userAccounts.map(UserAccountInfo.from))
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
                assertNoPasswords(response.body)
                val obtained = checkResponse[UserAccountInfo](response.body)
                assertEquals(
                    obtained.copy(id = None, lastUpdated = None),
                    UserAccountInfo.from(userAccount).copy(id = None, lastUpdated = None)
                )
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
                assertNoPasswords(response.body)
                val obtained = checkResponse[UserAccountInfo](response.body)
                assertEquals(
                    obtained.copy(id = None, lastUpdated = None),
                    UserAccountInfo.from(userAccount).copy(id = None, lastUpdated = None)
                )
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
                assertNoPasswords(response.body)
                val obtained = checkResponse[UserAccountInfo](response.body)
                assertEquals(
                    obtained.copy(id = None, lastUpdated = None),
                    UserAccountInfo.from(userAccount).copy(id = None, lastUpdated = None)
                )
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
                assertNoPasswords(response.body)
                val obtained = checkResponse[UserAccountInfo](response.body)
                assertEquals(
                    obtained.copy(id = None, lastUpdated = None),
                    UserAccountInfo.from(userAccount).copy(id = None, lastUpdated = None)
                )
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
                assertNoPasswords(response.body)
                val obtained = checkResponse[UserAccountInfo](response.body)
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

    test("createEndpoint (maintainer can't create an administrator)") {
        // A maintainer must not be able to create accounts, e.g. an administrator account for themselves
        val userAccount = newUserJson(UserAccountRoles.ADMINISTRATOR)
        withRole(UserAccountRoles.MAINTENANCE)(jwt =>
            runPost(
                endpoints.createEndpointImpl,
                "http://test.com/v1/users",
                userAccount.stringify,
                response => assertEquals(response.code, StatusCode.Forbidden),
                jwt = jwt
            )
        )
        assertEquals(userAccountService.findByUserName(userAccount.username), Right(None))
    }

    test("createEndpoint (read-only user is denied)") {
        val userAccount = newUserJson()
        withRole(UserAccountRoles.READONLY)(jwt =>
            runPost(
                endpoints.createEndpointImpl,
                "http://test.com/v1/users",
                userAccount.stringify,
                response => assertEquals(response.code, StatusCode.Forbidden),
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
                response => assertEquals(response.code, StatusCode.Forbidden),
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
                response => assertEquals(response.code, StatusCode.Forbidden),
                jwt = jwt
            )
        )
        assert(userAccountService.findByUserName(user.username).exists(_.isDefined))
    }

    test("createEndpoint (token for a deleted user is unauthorized)") {
        val userAccount = newUserJson()
        withRole(UserAccountRoles.ADMINISTRATOR)(jwt =>
            // The token is still validly signed, but its user no longer exists
            val username = jwt.flatMap(jwtService.decode).map(_.username).getOrElse(fail("Could not decode token"))
            assert(userAccountService.deleteByUserName(username).isRight)
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

    // --- Maintainers can create ReadOnly accounts; anyone can update their own account; only admins change roles

    private def createAs(role: UserAccountRoles, account: UserAccount, expected: StatusCode): Unit =
        withRole(role)(jwt =>
            runPost(
                endpoints.createEndpointImpl,
                "http://test.com/v1/users",
                account.stringify,
                r => assertEquals(r.code, expected, r.body.toString),
                jwt = jwt
            )
        )

    private def storedRole(username: String): Option[String] =
        userAccountService.findByUserName(username).toOption.flatten.map(_.role)

    private def updateSelf(role: UserAccountRoles, update: UserAccountUpdate, expected: StatusCode): Option[String] =
        var result: Option[String] = None
        withRoleAccount(role)((username, jwt) =>
            runPut(
                endpoints.updateEndpointImpl,
                s"http://test.com/v1/users/$username",
                update.stringify,
                r => assertEquals(r.code, expected, r.body.toString),
                jwt = jwt
            )
            result = storedRole(username)
        )
        result

    test("createEndpoint (maintainer can create a ReadOnly account)") {
        val account = newUserJson(UserAccountRoles.READONLY)
        createAs(UserAccountRoles.MAINTENANCE, account, StatusCode.Ok)
        assertEquals(storedRole(account.username), Some(UserAccountRoles.READONLY.getRoleName))
    }

    test("createEndpoint (a loosely written role is stored by its canonical name)") {
        // Access checks compare role names exactly, so "readonly" must be stored as "ReadOnly"
        val account = newUserJson().copy(role = "readonly")
        createAs(UserAccountRoles.MAINTENANCE, account, StatusCode.Ok)
        assertEquals(storedRole(account.username), Some(UserAccountRoles.READONLY.getRoleName))
    }

    test("createEndpoint (maintainer can't create a maintainer)") {
        val account = newUserJson(UserAccountRoles.MAINTENANCE)
        createAs(UserAccountRoles.MAINTENANCE, account, StatusCode.Forbidden)
        assertEquals(storedRole(account.username), None)
    }

    test("createEndpoint (an invalid role is a bad request)") {
        val account = newUserJson().copy(role = "superuser")
        createAs(UserAccountRoles.ADMINISTRATOR, account, StatusCode.BadRequest)
        assertEquals(storedRole(account.username), None)
    }

    test("updateEndpoint (maintainer can update their own account)") {
        val firstName = Strings.random(8)
        withRoleAccount(UserAccountRoles.MAINTENANCE)((username, jwt) =>
            runPut(
                endpoints.updateEndpointImpl,
                s"http://test.com/v1/users/$username",
                UserAccountUpdate(firstName = Some(firstName)).stringify,
                r =>
                    assertEquals(r.code, StatusCode.Ok)
                    assertEquals(checkResponse[UserAccountInfo](r.body).firstName, Some(firstName))
                ,
                jwt = jwt
            )
        )
    }

    test("updateEndpoint (read-only user can update their own account)") {
        val role = updateSelf(UserAccountRoles.READONLY, UserAccountUpdate(lastName = Some("Smith")), StatusCode.Ok)
        assertEquals(role, Some(UserAccountRoles.READONLY.getRoleName))
    }

    test("updateEndpoint (maintainer can't change their own role)") {
        val update = UserAccountUpdate(role = Some(UserAccountRoles.ADMINISTRATOR.getRoleName))
        assertEquals(
            updateSelf(UserAccountRoles.MAINTENANCE, update, StatusCode.Forbidden),
            Some(UserAccountRoles.MAINTENANCE.getRoleName)
        )
    }

    test("updateEndpoint (read-only user can't change their own role)") {
        val update = UserAccountUpdate(role = Some(UserAccountRoles.MAINTENANCE.getRoleName))
        assertEquals(
            updateSelf(UserAccountRoles.READONLY, update, StatusCode.Forbidden),
            Some(UserAccountRoles.READONLY.getRoleName)
        )
    }

    test("updateEndpoint (resending your current role is allowed)") {
        val update = UserAccountUpdate(role = Some(UserAccountRoles.MAINTENANCE.getRoleName), email = Some("m@mbari.org"))
        assertEquals(
            updateSelf(UserAccountRoles.MAINTENANCE, update, StatusCode.Ok),
            Some(UserAccountRoles.MAINTENANCE.getRoleName)
        )
    }

    test("updateEndpoint (locked user can't update their own account)") {
        updateSelf(UserAccountRoles.LOCKED, UserAccountUpdate(firstName = Some("x")), StatusCode.Forbidden)
    }

    test("updateEndpoint (administrator can change another user's role)") {
        val user = create(1).head
        withRole(UserAccountRoles.ADMINISTRATOR)(jwt =>
            runPut(
                endpoints.updateEndpointImpl,
                s"http://test.com/v1/users/${user.username}",
                UserAccountUpdate(role = Some("maint")).stringify,
                r => assertEquals(r.code, StatusCode.Ok),
                jwt = jwt
            )
        )
        assertEquals(storedRole(user.username), Some(UserAccountRoles.MAINTENANCE.getRoleName))
    }

    test("updateEndpoint (unknown user is not found)") {
        withRole(UserAccountRoles.ADMINISTRATOR)(jwt =>
            runPut(
                endpoints.updateEndpointImpl,
                s"http://test.com/v1/users/${Strings.random(16)}",
                UserAccountUpdate(firstName = Some("x")).stringify,
                r => assertEquals(r.code, StatusCode.NotFound),
                jwt = jwt
            )
        )
    }

    test("deleteByUserNameEndpoint (unknown user is not found)") {
        runDelete(
            endpoints.deleteByUserNameEndpointImpl,
            s"http://test.com/v1/users/${Strings.random(16)}",
            r => assertEquals(r.code, StatusCode.NotFound),
            jwt = jwtService.authorize(jwtService.apiKey)
        )
    }

    // --- Roles

    test("listRolesEndpoint returns every recognized role") {
        runGet(
            endpoints.listRolesEndpointImpl,
            "http://test.com/v1/users/list/roles",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val roles = checkResponse[Seq[String]](response.body)
                assertEquals(roles, Seq("Admin", "Maint", "ReadOnly", "Locked"))
                // Every listed role must be one the access checks recognize as itself
                for role <- roles do assertEquals(UserAccountRoles.fromStoredRole(role).getRoleName, role)
        )
    }

    test("listRolesEndpoint needs no login") {
        runGet(endpoints.listRolesEndpointImpl, "http://test.com/v1/users/list/roles", r => assertEquals(r.code, StatusCode.Ok))
    }

    test("a user named 'list' is still found by username") {
        val entity = TestEntityFactory.createUserAccount(UserAccountRoles.READONLY.getRoleName)
        entity.setUserName("list")
        userAccountService.create(UserAccount.from(entity)) match
            case Left(e)  => fail(e.toString)
            case Right(_) =>
                runGet(
                    endpoints.findByUserNameEndpointImpl,
                    "http://test.com/v1/users/list",
                    response =>
                        assertEquals(response.code, StatusCode.Ok)
                        assertEquals(checkResponse[UserAccountInfo](response.body).username, "list")
                )
    }

    // --- End to end: a read-only user logs in and can only update their own account

    test("a read-only user can log in, update their own account, and nothing else") {
        val password = Strings.random(12)
        val username = "readonly-" + Strings.random(8)
        assert(
            userAccountService
                .create(UserAccount(username, password, UserAccountRoles.READONLY.getRoleName, isEncrypted = Some(false)))
                .isRight
        )

        // Log in through the real login endpoint
        import org.mbari.oni.etc.sdk.Futures.*
        val authEndpoints = AuthorizationEndpoints(entityManagerFactory)
        val credentials   = java.util.Base64.getEncoder.encodeToString(s"$username:$password".getBytes)
        val loginResponse = sttp.client3.basicRequest
            .post(sttp.model.Uri.unsafeParse("http://test.com/v1/auth/login"))
            .header("Authorization", s"BASIC $credentials")
            .send(newBackendStub(authEndpoints.loginEndpointImpl))
            .join
        assertEquals(loginResponse.code, StatusCode.Ok)
        val jwt = loginResponse.body.toOption
            .flatMap(b => io.circe.parser.decode[org.mbari.oni.domain.AuthorizationSC](b).toOption)
            .map(_.access_token)
        assert(jwt.isDefined)

        // Can update their own account
        runPut(
            endpoints.updateEndpointImpl,
            s"http://test.com/v1/users/$username",
            UserAccountUpdate(email = Some("me@mbari.org")).stringify,
            r => assertEquals(r.code, StatusCode.Ok),
            jwt = jwt
        )
        // Can't make themselves a maintainer
        runPut(
            endpoints.updateEndpointImpl,
            s"http://test.com/v1/users/$username",
            UserAccountUpdate(role = Some(UserAccountRoles.MAINTENANCE.getRoleName)).stringify,
            r => assertEquals(r.code, StatusCode.Forbidden),
            jwt = jwt
        )
        // Can't create accounts or update anyone else
        runPost(
            endpoints.createEndpointImpl,
            "http://test.com/v1/users",
            newUserJson().stringify,
            r => assertEquals(r.code, StatusCode.Forbidden),
            jwt = jwt
        )
        val other = create(1).head
        runPut(
            endpoints.updateEndpointImpl,
            s"http://test.com/v1/users/${other.username}",
            UserAccountUpdate(email = Some("x@mbari.org")).stringify,
            r => assertEquals(r.code, StatusCode.Forbidden),
            jwt = jwt
        )
        assertEquals(storedRole(username), Some(UserAccountRoles.READONLY.getRoleName))
    }
