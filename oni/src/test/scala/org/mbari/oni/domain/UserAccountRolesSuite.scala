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

package org.mbari.oni.domain

import org.mbari.oni.domain.UserAccountRoles.*

class UserAccountRolesSuite extends munit.FunSuite:

    test("fromStoredRole recognizes each role name exactly, ignoring case and surrounding whitespace") {
        for role <- UserAccountRoles.values do
            assertEquals(fromStoredRole(role.getRoleName), role)
            assertEquals(fromStoredRole(role.getRoleName.toLowerCase), role)
            assertEquals(fromStoredRole(role.getRoleName.toUpperCase), role)
            assertEquals(fromStoredRole(s"  ${role.getRoleName} "), role)
    }

    test("fromStoredRole treats anything unrecognized as ReadOnly") {
        for value <- Seq(null, "", "   ", "banana", "Maintenance", "Administrator", "a", "m", "Read Only") do
            assertEquals(fromStoredRole(value), READONLY, s"stored role '$value'")
    }

    test("an account with an unrecognized role has ReadOnly access") {
        val account = UserAccount("someone", "", "banana")
        assertEquals(account.effectiveRole, READONLY)
        assert(!account.isAdministrator)
        assert(!account.isMaintainer)
        assert(!account.isLocked)
        assert(!account.canWrite)
    }

    test("an account whose role differs only in case keeps that role") {
        val account = UserAccount("someone", "", "admin")
        assert(account.isAdministrator)
        assert(account.canWrite)
    }

    test("the entity uses the same rule as the domain model") {
        val entity = UserAccount("someone", "", "banana").toEntity
        assert(entity.isReadOnly)
        assert(!entity.isAdministrator && !entity.isMaintainer && !entity.isLocked)
        assertEquals(UserAccount("someone", "", "MAINT").toEntity.isMaintainer, true)
    }

    test("UserAccount.from reports the role the account actually has") {
        val entity = UserAccount("someone", "", "banana").toEntity
        assertEquals(UserAccount.from(entity).role, READONLY.getRoleName)
        assertEquals(UserAccount.from(UserAccount("someone", "", "admin").toEntity).role, ADMINISTRATOR.getRoleName)
    }
