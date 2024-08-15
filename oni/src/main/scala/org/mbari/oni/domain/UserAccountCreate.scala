/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
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
) {
    def toUserAccount: UserAccount = UserAccount(
        username,
        password,
        role.getOrElse(UserAccountRoles.READONLY.getRoleName),
        affiliation,
        firstName.orElse(first_name),
        lastName.orElse(last_name),
        email
    )
}

object UserAccountCreate {
    def fromUserAccount(userAccount: UserAccount): UserAccountCreate = UserAccountCreate(
        userAccount.username,
        userAccount.password,
        Some(userAccount.role),
        userAccount.affiliation,
        userAccount.firstName,
        userAccount.lastName,
        userAccount.email
    )
}
