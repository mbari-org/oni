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

import org.mbari.oni.domain.{BadRequest, Forbidden, UserAccount, UserAccountRoles}
import org.mbari.oni.domain.UserAccountRoles.*

class UserAccountAccessSuite extends munit.FunSuite:

    private def user(name: String, role: UserAccountRoles): Caller = Caller.User(UserAccount(name, "", role.getRoleName))

    private val admin    = user("alice", ADMINISTRATOR)
    private val maint    = user("mike", MAINTENANCE)
    private val readOnly = user("rita", READONLY)
    private val locked   = user("lou", LOCKED)

    private def isForbidden[T](e: Either[?, T]): Boolean  = e.left.exists(_.isInstanceOf[Forbidden])
    private def isBadRequest[T](e: Either[?, T]): Boolean = e.left.exists(_.isInstanceOf[BadRequest])

    // ---------------------------------------------------------------- parseRole

    test("parseRole matches loosely and returns the canonical role") {
        assertEquals(UserAccountAccess.parseRole("ReadOnly"), Right(READONLY))
        assertEquals(UserAccountAccess.parseRole("readonly"), Right(READONLY))
        assertEquals(UserAccountAccess.parseRole("maint"), Right(MAINTENANCE))
        assertEquals(UserAccountAccess.parseRole(" Admin "), Right(ADMINISTRATOR))
    }

    test("parseRole rejects blank and unknown roles") {
        // getRole("") would otherwise match the first role, Admin
        assert(isBadRequest(UserAccountAccess.parseRole("")))
        assert(isBadRequest(UserAccountAccess.parseRole("   ")))
        assert(isBadRequest(UserAccountAccess.parseRole("superuser")))
    }

    // ---------------------------------------------------------------- create

    test("the API key and administrators can create accounts with any role") {
        for caller <- Seq(Caller.ApiKey, admin); role <- UserAccountRoles.values do
            assertEquals(UserAccountAccess.checkCreate(caller, Some(role.getRoleName)), Right(role))
    }

    test("maintainers can create ReadOnly accounts only") {
        assertEquals(UserAccountAccess.checkCreate(maint, Some("ReadOnly")), Right(READONLY))
        assertEquals(UserAccountAccess.checkCreate(maint, Some("readonly")), Right(READONLY)) // stored canonically
        assertEquals(UserAccountAccess.checkCreate(maint, None), Right(READONLY))             // the default role
        assert(isForbidden(UserAccountAccess.checkCreate(maint, Some("Maint"))))
        assert(isForbidden(UserAccountAccess.checkCreate(maint, Some("Admin"))))
        assert(isForbidden(UserAccountAccess.checkCreate(maint, Some("Locked"))))
    }

    test("read-only and locked users can't create accounts") {
        assert(isForbidden(UserAccountAccess.checkCreate(readOnly, Some("ReadOnly"))))
        assert(isForbidden(UserAccountAccess.checkCreate(locked, Some("ReadOnly"))))
    }

    test("an invalid role is a bad request, but only once the caller may create accounts at all") {
        assert(isBadRequest(UserAccountAccess.checkCreate(admin, Some("superuser"))))
        assert(isBadRequest(UserAccountAccess.checkCreate(maint, Some("superuser"))))
        assert(isForbidden(UserAccountAccess.checkCreate(readOnly, Some("superuser"))))
    }

    // ---------------------------------------------------------------- update

    test("the API key and administrators can update any account, including its role") {
        for caller <- Seq(Caller.ApiKey, admin) do
            assertEquals(UserAccountAccess.checkUpdate(caller, "someone-else", Some("Admin")), Right(Some(ADMINISTRATOR)))
            assertEquals(UserAccountAccess.checkUpdate(caller, "someone-else", None), Right(None))
    }

    test("maintainers and read-only users can update their own account") {
        assertEquals(UserAccountAccess.checkUpdate(maint, "mike", None), Right(None))
        assertEquals(UserAccountAccess.checkUpdate(readOnly, "rita", None), Right(None))
    }

    test("non-administrators can't change their own role") {
        assert(isForbidden(UserAccountAccess.checkUpdate(maint, "mike", Some("Admin"))))
        assert(isForbidden(UserAccountAccess.checkUpdate(readOnly, "rita", Some("Maint"))))
    }

    test("sending your current role back is not a role change") {
        assertEquals(UserAccountAccess.checkUpdate(maint, "mike", Some("Maint")), Right(Some(MAINTENANCE)))
        assertEquals(UserAccountAccess.checkUpdate(readOnly, "rita", Some("readonly")), Right(Some(READONLY)))
    }

    test("non-administrators can't update other accounts") {
        assert(isForbidden(UserAccountAccess.checkUpdate(maint, "rita", None)))
        assert(isForbidden(UserAccountAccess.checkUpdate(readOnly, "mike", None)))
    }

    test("locked users can't update anything, not even their own account") {
        assert(isForbidden(UserAccountAccess.checkUpdate(locked, "lou", None)))
    }

    test("an invalid role in an update is a bad request") {
        assert(isBadRequest(UserAccountAccess.checkUpdate(admin, "rita", Some("superuser"))))
        assert(isBadRequest(UserAccountAccess.checkUpdate(Caller.ApiKey, "rita", Some(""))))
    }

    // ---------------------------------------------------------------- unrecognized stored roles

    private val unrecognized = Caller.User(UserAccount("una", "", "banana"))

    test("a caller with an unrecognized role is treated as ReadOnly") {
        assert(isForbidden(UserAccountAccess.checkCreate(unrecognized, Some("ReadOnly"))))
        assertEquals(UserAccountAccess.checkUpdate(unrecognized, "una", None), Right(None))
        assert(isForbidden(UserAccountAccess.checkUpdate(unrecognized, "una", Some("Maint"))))
        assert(isForbidden(UserAccountAccess.checkUpdate(unrecognized, "someone-else", None)))
        // Their effective role is ReadOnly, so sending ReadOnly back is not a role change
        assertEquals(UserAccountAccess.checkUpdate(unrecognized, "una", Some("ReadOnly")), Right(Some(READONLY)))
    }

    test("a caller whose stored role differs only in case keeps that role") {
        val lowerAdmin = Caller.User(UserAccount("al", "", "admin"))
        assertEquals(UserAccountAccess.checkCreate(lowerAdmin, Some("Maint")), Right(MAINTENANCE))
    }
