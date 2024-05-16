/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.domain

import org.jasypt.util.password.BasicPasswordEncryptor
import org.mbari.oni.jpa.entities.UserAccountEntity
import org.mbari.oni.etc.jdk.Numbers.given

case class UserAccount(
                   username: String,
                   password: String,
                   role: String = "ReadOnly",
                   affiliation: Option[String] = None,
                   firstName: Option[String] = None,
                   lastName: Option[String] = None,
                   email: Option[String] = None,
                   id: Option[Long] = None,
                   isEncrypted: Option[Boolean] = None
               ) {

    def toEntity: UserAccountEntity = {
        val entity = new UserAccountEntity()
        entity.setUserName(username)
        // If the password is not encrypted, then encrypt it
        if (isEncrypted.getOrElse(false))
            entity.setEncryptedPassword(password)
        else
            entity.setPassword(password)
        entity.setRole(role)
        entity.setAffiliation(affiliation.orNull)
        entity.setFirstName(firstName.orNull)
        entity.setLastName(lastName.orNull)
        entity.setEmail(email.orNull)
        entity.setId(id.map(_.asInstanceOf[java.lang.Long]).orNull)
        entity
    }

}

object UserAccount {

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

}
