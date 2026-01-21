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

case class UserAccountCreate(
    username: String,
    password: String,
    role: Option[String] = Some(UserAccountRoles.READONLY.getRoleName),
    affiliation: Option[String] = None,
    firstName: Option[String] = None,
    lastName: Option[String] = None,
    email: Option[String] = None,
    first_name: Option[String] = None,
    last_name: Option[String] = None
):
    def toUserAccount: UserAccount = UserAccount(
        username,
        password,
        role.getOrElse(UserAccountRoles.READONLY.getRoleName),
        affiliation,
        firstName.orElse(first_name),
        lastName.orElse(last_name),
        email
    )

object UserAccountCreate:
    def fromUserAccount(userAccount: UserAccount): UserAccountCreate = UserAccountCreate(
        userAccount.username,
        userAccount.password,
        Some(userAccount.role),
        userAccount.affiliation,
        userAccount.firstName,
        userAccount.lastName,
        userAccount.email
    )
