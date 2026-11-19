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

import java.time.Instant

/**
 * The public view of a user account returned by the API. It deliberately has no password (or password-encryption)
 * fields, so password hashes are never sent to clients. Use [[UserAccount]] internally.
 */
case class UserAccountInfo(
    username: String,
    role: String,
    affiliation: Option[String] = None,
    firstName: Option[String] = None,
    lastName: Option[String] = None,
    email: Option[String] = None,
    id: Option[Long] = None,
    lastUpdated: Option[Instant] = None
)

object UserAccountInfo:
    def from(userAccount: UserAccount): UserAccountInfo =
        UserAccountInfo(
            username = userAccount.username,
            role = userAccount.role,
            affiliation = userAccount.affiliation,
            firstName = userAccount.firstName,
            lastName = userAccount.lastName,
            email = userAccount.email,
            id = userAccount.id,
            lastUpdated = userAccount.lastUpdated
        )
