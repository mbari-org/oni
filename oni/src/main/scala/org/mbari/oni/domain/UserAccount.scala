/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
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
