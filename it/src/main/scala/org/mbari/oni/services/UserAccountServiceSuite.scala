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

import org.mbari.oni.domain.{UserAccount, UserAccountRoles, UserAccountUpdate}
import org.mbari.oni.jpa.DataInitializer

trait UserAccountServiceSuite extends DataInitializer:

    lazy val userAccountService = new UserAccountService(entityManagerFactory)

    test("create") {
        val expected = UserAccount("test", "password", "admin")
        userAccountService.create(expected) match
            case Left(e)         => fail(e.getMessage)
            case Right(obtained) =>
                assertEquals(obtained.username, expected.username)
                assertNotEquals(obtained.password, expected.password)
                // Roles are reported by their canonical name: "admin" is recognized as Admin
                assertEquals(obtained.role, UserAccountRoles.ADMINISTRATOR.getRoleName)
                assert(obtained.id.isDefined)
                assert(obtained.lastUpdated.isDefined)
    }

    test("lastUpdated is populated and advances on update") {
        val either = for
            created <- userAccountService.create(UserAccount("test11", "password", "admin"))
            updated <- userAccountService.update("test11", UserAccountUpdate(affiliation = Some("MBARI")))
            found   <- userAccountService.findByUserName("test11")
        yield
            val createdAt = created.lastUpdated.getOrElse(fail("create did not return a lastUpdated"))
            val updatedAt = updated.lastUpdated.getOrElse(fail("update did not return a lastUpdated"))
            assert(!updatedAt.isBefore(createdAt), s"$updatedAt should not be before $createdAt")
            // The timestamp should survive a round trip through the database
            assertEquals(found.flatMap(_.lastUpdated), Some(updatedAt))
        assert(either.isRight)
    }

    test("update") {
        val original = UserAccount("test2", "password", "admin")
        val either   = for
            created <- userAccountService.create(original)
            update  <- Right(UserAccountUpdate(Some("newpassword"), Some("plebeian"), Some("MBARI")))
            updated <- userAccountService.update("test2", update)
        yield
            assertEquals(updated.username, "test2")
            // An unrecognized role is treated, and reported, as ReadOnly
            assertEquals(updated.role, UserAccountRoles.READONLY.getRoleName)
            assertEquals(updated.affiliation, Some("MBARI"))
            assertNotEquals(updated.password, original.password)
            assertNotEquals(updated.password, created.password)
        assert(either.isRight)
    }

    test("deleteByUserName") {
        val expected = UserAccount("test3", "secretpassword", UserAccountRoles.MAINTENANCE.getRoleName)
        userAccountService.create(expected) match
            case Left(e)  => fail(e.getMessage)
            case Right(_) =>
                userAccountService.deleteByUserName("test3") match
                    case Left(e)  => fail(e.getMessage)
                    case Right(_) =>
                        userAccountService.findByUserName("test3") match
                            case Left(e)                  => fail(e.getMessage)
                            case Right(None)              => // expected
                            case Right(Some(userAccount)) => fail(s"Expected None but got $userAccount")
    }

    test("findByUserName") {
        val expected = UserAccount("test4", "notmypassword", UserAccountRoles.READONLY.getRoleName)
        userAccountService.create(expected) match
            case Left(e)  => fail(e.getMessage)
            case Right(_) =>
                userAccountService.findByUserName("test4") match
                    case Left(e)                  => fail(e.getMessage)
                    case Right(None)              => fail("Expected Some but got None")
                    case Right(Some(userAccount)) =>
                        assertEquals(userAccount.username, "test4")
                        assertEquals(userAccount.role, UserAccountRoles.READONLY.getRoleName)
    }

    test("findAll") {
        val expected = Seq(
            UserAccount("test5", "password", UserAccountRoles.READONLY.getRoleName),
            UserAccount("test6", "password", UserAccountRoles.READONLY.getRoleName),
            UserAccount("test7", "password", UserAccountRoles.READONLY.getRoleName)
        )
        expected.foreach(userAccountService.create)
        userAccountService.findAll() match
            case Left(e)         => fail(e.getMessage)
            case Right(obtained) =>
                val obtainedUsernames = obtained.map(_.username).sorted
                val expectedUsernames = expected.map(_.username).sorted
                for e <- expectedUsernames
                do assert(obtainedUsernames.contains(e))
    }

    test("findAllByRole") {
        val expected = Seq(
            UserAccount("test8", "password", UserAccountRoles.READONLY.getRoleName),
            UserAccount("test9", "password", UserAccountRoles.READONLY.getRoleName),
            UserAccount("test10", "password", UserAccountRoles.READONLY.getRoleName)
        )
        expected.foreach(userAccountService.create)

        userAccountService.findAllByRole(UserAccountRoles.READONLY.getRoleName) match
            case Left(e)         => fail(e.getMessage)
            case Right(obtained) =>
                obtained.foreach(u => assertEquals(u.role, UserAccountRoles.READONLY.getRoleName))

                val expectedUsernames = expected.map(_.username).sorted
                val obtainedUsernames = obtained.map(_.username).sorted
                for e <- expectedUsernames
                do assert(obtainedUsernames.contains(e))
    }

    // --- verifyWriteAccess is an allow-list: only Admin and Maint may write

    private def writeAccessFor(role: String): Boolean =
        val entity = org.mbari.oni.jpa.entities.TestEntityFactory.createUserAccount(role)
        val user   = userAccountService.create(UserAccount.from(entity)).toOption.get
        try userAccountService.verifyWriteAccess(Some(user.username)).isRight
        finally userAccountService.deleteByUserName(user.username)

    test("verifyWriteAccess allows administrators and maintainers") {
        assert(writeAccessFor(UserAccountRoles.ADMINISTRATOR.getRoleName))
        assert(writeAccessFor(UserAccountRoles.MAINTENANCE.getRoleName))
    }

    test("verifyWriteAccess denies read-only and locked users") {
        assert(!writeAccessFor(UserAccountRoles.READONLY.getRoleName))
        assert(!writeAccessFor(UserAccountRoles.LOCKED.getRoleName), "Locked users must not be able to write")
    }

    test("verifyWriteAccess recognizes role names regardless of case") {
        assert(writeAccessFor("admin"))
        assert(writeAccessFor("MAINT"))
        assert(!writeAccessFor("readonly"))
    }

    test("verifyWriteAccess treats unrecognized roles as ReadOnly") {
        assert(!writeAccessFor("banana"))
        assert(!writeAccessFor("Maintenance"))
        assert(!writeAccessFor(""))
    }

    test("an account stored with an unrecognized role is reported as ReadOnly") {
        val entity = org.mbari.oni.jpa.entities.TestEntityFactory.createUserAccount("banana")
        val user   = userAccountService.create(UserAccount.from(entity)).toOption.get
        try
            assertEquals(
                userAccountService.findByUserName(user.username).toOption.flatten.map(_.role),
                Some(UserAccountRoles.READONLY.getRoleName)
            )
        finally userAccountService.deleteByUserName(user.username)
    }

    test("verifyWriteAccess denies unknown and missing users") {
        assert(userAccountService.verifyWriteAccess(Some("no-such-user-" + System.nanoTime())).isLeft)
        assert(userAccountService.verifyWriteAccess(None).isLeft)
    }

    // --- findAllByRole uses each account's effective role

    private def usernamesWithRole(role: String): Set[String] =
        userAccountService.findAllByRole(role).fold(e => fail(e.getMessage), _.map(_.username).toSet)

    private def withStoredRoles(roles: String*)(fn: Seq[String] => Unit): Unit =
        val created = roles.map(role =>
            val entity = org.mbari.oni.jpa.entities.TestEntityFactory.createUserAccount(role)
            userAccountService.create(UserAccount.from(entity)).toOption.get.username
        )
        try fn(created)
        finally created.foreach(userAccountService.deleteByUserName)

    test("findAllByRole lists accounts with an unrecognized stored role under ReadOnly") {
        withStoredRoles("ReadOnly", "banana", "", "Maintenance") { case Seq(readOnly, banana, blank, maintenance) =>
            val obtained = usernamesWithRole(UserAccountRoles.READONLY.getRoleName)
            for name <- Seq(readOnly, banana, blank, maintenance) do assert(obtained.contains(name), name)
            assert(!usernamesWithRole(UserAccountRoles.MAINTENANCE.getRoleName).contains(maintenance))
        }
    }

    test("findAllByRole recognizes stored roles regardless of case") {
        withStoredRoles("admin", "Admin") { case Seq(lower, canonical) =>
            val admins = usernamesWithRole(UserAccountRoles.ADMINISTRATOR.getRoleName)
            assert(admins.contains(lower) && admins.contains(canonical))
            assert(!usernamesWithRole(UserAccountRoles.READONLY.getRoleName).contains(lower))
        }
    }

    test("findAllByRole matches the requested role exactly, ignoring case") {
        withStoredRoles("Maint") { case Seq(maint) =>
            assert(usernamesWithRole("maint").contains(maint))
            assert(usernamesWithRole(" MAINT ").contains(maint))
            assert(!usernamesWithRole("Ma").contains(maint), "Prefixes are not role names")
        }
    }

    test("findAllByRole returns nothing for an unrecognized requested role") {
        withStoredRoles("banana") { _ =>
            // Must not fall back to ReadOnly, which would list every read-only account
            assertEquals(usernamesWithRole("banana"), Set.empty[String])
            assertEquals(usernamesWithRole("%"), Set.empty[String])
        }
    }
