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

import org.mbari.oni.jpa.entities.UserAccountEntity

/**
 * DTO for user account data
 * @param username
 *   The unique user name
 * @param password
 *   A password, can be encrypted or plain. See isEncrypted
 * @param role
 *   A users role (Admin, Maint, ReadOnly)
 * @param affiliation
 *   The users employere/organization
 * @param firstName
 *   The first name
 * @param lastName
 *   The last name
 * @param email
 *   A users email
 * @param id
 *   The primary key, database id
 * @param isEncrypted
 *   If the password is encrypted. If None, the value is assumed to be false and so the password is assumed to be plain
 *   text
 */
case class UserAccount(
    username: String,
    password: String,
    role: String = UserAccountRoles.READONLY.getRoleName,
    affiliation: Option[String] = None,
    firstName: Option[String] = None,
    lastName: Option[String] = None,
    email: Option[String] = None,
    id: Option[Long] = None,
    isEncrypted: Option[Boolean] = None
):

    def toEntity: UserAccountEntity =
        val entity = new UserAccountEntity()
        entity.setUserName(username)
        // If the password is not encrypted, then encrypt it
        if isEncrypted.getOrElse(false) then entity.setEncryptedPassword(password)
        else entity.setPassword(password)
        entity.setRole(role)
        entity.setAffiliation(affiliation.orNull)
        entity.setFirstName(firstName.orNull)
        entity.setLastName(lastName.orNull)
        entity.setEmail(email.orNull)
        entity.setId(id.map(_.asInstanceOf[java.lang.Long]).orNull)
        entity

    lazy val isAdministrator: Boolean = role == UserAccountRoles.ADMINISTRATOR.getRoleName

    def toFormBody: String =
        // Example: username=lchrobak&password=changeme&role=User&first_name=Laura&last_name=Chrobak&affiliation=MBARI&email=lchrobak%40mbari.org
        val fields = Seq(
            Some(s"username=$username"),
            Some(s"password=$password"),
            Some(s"role=$role"),
            firstName.map(v => s"firstName=$v"),
            lastName.map(v => s"lastName=$v"),
            affiliation.map(v => s"affiliation=$v"),
            email.map(v => s"email=$v")
        ).flatten
        fields.mkString("&")

object UserAccount:

    def from(userAccount: UserAccountEntity): UserAccount = UserAccount(
        userAccount.getUserName,
        userAccount.getEncryptedPassword,
        userAccount.getRole,
        Option(userAccount.getAffiliation),
        Option(userAccount.getFirstName),
        Option(userAccount.getLastName),
        Option(userAccount.getEmail),
        Option(userAccount.getPrimaryKey).map(_.asInstanceOf[Long]),
        Some(true)
    )
