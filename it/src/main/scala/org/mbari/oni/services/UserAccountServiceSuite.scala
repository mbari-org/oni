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

trait UserAccountServiceSuite extends DataInitializer {

    lazy val userAccountService = new UserAccountService(entityManagerFactory)

    test("create") {
        val expected = UserAccount("test", "password", "admin")
        userAccountService.create(expected) match {
            case Left(e) => fail(e.getMessage)
            case Right(obtained) =>
                assertEquals(obtained.username, expected.username)
                assertNotEquals(obtained.password, expected.password)
                assertEquals(obtained.role, expected.role)
                assert(obtained.id.isDefined)
        }
    }

    test("update") {
        val original = UserAccount("test2", "password", "admin")
        val either = for
            created <- userAccountService.create(original)
            update <- Right(UserAccountUpdate("test2", Some("newpassword"), Some("plebeian"), Some("MBARI" )))
            updated <- userAccountService.update(update)
        yield
            assertEquals(updated.username, "test2")
            assertEquals(updated.role, "plebeian")
            assertEquals(updated.affiliation, Some("MBARI"))
            assertNotEquals(updated.password, original.password)
            assertNotEquals(updated.password, created.password)
        assert(either.isRight)
    }

    test("deleteByUserName") {
        val expected = UserAccount("test3", "secretpassword", UserAccountRoles.MAINTENANCE.getRoleName)
        userAccountService.create(expected) match {
            case Left(e) => fail(e.getMessage)
            case Right(_) =>
                userAccountService.deleteByUserName("test3") match {
                    case Left(e) => fail(e.getMessage)
                    case Right(_) =>
                        userAccountService.findByUserName("test3") match {
                            case Left(e) => fail(e.getMessage)
                            case Right(None) => // expected
                            case Right(Some(userAccount)) => fail(s"Expected None but got $userAccount")
                        }
                }
        }
    }

    test("findByUserName") {
        val expected = UserAccount("test4", "notmypassword", UserAccountRoles.READONLY.getRoleName)
        userAccountService.create(expected) match {
            case Left(e) => fail(e.getMessage)
            case Right(_) =>
                userAccountService.findByUserName("test4") match {
                    case Left(e) => fail(e.getMessage)
                    case Right(None) => fail("Expected Some but got None")
                    case Right(Some(userAccount)) =>
                        assertEquals(userAccount.username, "test4")
                        assertEquals(userAccount.role, UserAccountRoles.READONLY.getRoleName)
                }
        }
    }

    test("findAll") {
        val expected = Seq(
            UserAccount("test5", "password", UserAccountRoles.READONLY.getRoleName),
            UserAccount("test6", "password", UserAccountRoles.READONLY.getRoleName),
            UserAccount("test7", "password", UserAccountRoles.READONLY.getRoleName)
        )
        expected.foreach(userAccountService.create)
        userAccountService.findAll() match {
            case Left(e) => fail(e.getMessage)
            case Right(obtained) =>
                val obtainedUsernames = obtained.map(_.username).sorted
                val expectedUsernames = expected.map(_.username).sorted
                for
                    e <- expectedUsernames
                do
                    assert(obtainedUsernames.contains(e))
        }
    }

    test("findAllByRole") {
        val expected = Seq(
            UserAccount("test8", "password", UserAccountRoles.READONLY.getRoleName),
            UserAccount("test9", "password", UserAccountRoles.READONLY.getRoleName),
            UserAccount("test10", "password", UserAccountRoles.READONLY.getRoleName)
        )
        expected.foreach(userAccountService.create)


        userAccountService.findAllByRole(UserAccountRoles.READONLY.getRoleName) match {
            case Left(e) => fail(e.getMessage)
            case Right(obtained) =>
                obtained.foreach(u => assertEquals(u.role, UserAccountRoles.READONLY.getRoleName))

                val expectedUsernames = expected.map(_.username).sorted
                val obtainedUsernames = obtained.map(_.username).sorted
                for
                    e <- expectedUsernames
                do
                    assert(obtainedUsernames.contains(e))
        }
    }

}
