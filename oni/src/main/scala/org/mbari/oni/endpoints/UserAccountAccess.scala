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

import org.mbari.oni.domain.{BadRequest, ErrorMsg, Forbidden, UserAccountRoles}

/**
 * Who may create and update user accounts. Pure functions, so the rules can be tested without a database.
 *
 * | Caller        | Create                     | Update                                              |
 * |---------------|----------------------------|-----------------------------------------------------|
 * | API key       | any role                   | any account, including its role                     |
 * | Administrator | any role                   | any account, including its role                     |
 * | Maintainer    | ReadOnly accounts only     | their own account, but not its role                 |
 * | ReadOnly *    | no                         | their own account, but not its role                 |
 * | Locked        | no                         | no                                                  |
 *
 * \* Including accounts whose stored role isn't recognized (see [[UserAccountRoles.fromStoredRole]]).
 *
 * Requested roles are matched loosely (see [[UserAccountRoles.getRole]]) and the result is always stored by its
 * canonical name. A caller's own role is the role their account actually has: an unrecognized stored role counts as
 * ReadOnly (see [[UserAccountRoles.fromStoredRole]]).
 */
object UserAccountAccess:

    /** Parses a requested role. Blank or unrecognized roles are a bad request. */
    def parseRole(role: String): Either[ErrorMsg, UserAccountRoles] =
        // getRole matches by prefix, so a blank string would match the first role (Admin); reject it explicitly
        Option(role).map(_.trim).filter(_.nonEmpty).flatMap(r => Option(UserAccountRoles.getRole(r))) match
            case Some(parsed) => Right(parsed)
            case None         =>
                Left(BadRequest(s"Invalid role `$role`. Must be one of ${UserAccountRoles.values.mkString(", ")}"))

    /** @return the role to store for the new account, or why the caller may not create it */
    def checkCreate(caller: Caller, requestedRole: Option[String]): Either[ErrorMsg, UserAccountRoles] =
        val role = requestedRole.getOrElse(UserAccountRoles.READONLY.getRoleName)
        caller match
            case Caller.ApiKey                         => parseRole(role)
            case Caller.User(user) if user.isAdministrator => parseRole(role)
            case Caller.User(user) if user.isMaintainer    =>
                parseRole(role).flatMap(parsed =>
                    if parsed == UserAccountRoles.READONLY then Right(parsed)
                    else
                        Left(Forbidden(s"Maintainers can only create ${UserAccountRoles.READONLY} user accounts"))
                )
            case Caller.User(user)                     =>
                Left(Forbidden(s"User `${user.username}` is not permitted to create user accounts"))

    /**
     * @param target
     *   The username of the account being updated
     * @param requestedRole
     *   The role in the update, if any
     * @return
     *   the role to store (None to leave it unchanged), or why the caller may not make this update
     */
    def checkUpdate(
        caller: Caller,
        target: String,
        requestedRole: Option[String]
    ): Either[ErrorMsg, Option[UserAccountRoles]] =
        val parsedRole: Either[ErrorMsg, Option[UserAccountRoles]] = requestedRole match
            case None    => Right(None)
            case Some(r) => parseRole(r).map(Some(_))

        caller match
            case Caller.ApiKey                      => parsedRole
            case Caller.User(user) if user.isAdministrator => parsedRole
            case Caller.User(user) if user.isLocked        =>
                Left(Forbidden(s"User `${user.username}` is locked"))
            case Caller.User(user) if user.username == target =>
                parsedRole.flatMap {
                    // Sending your current role back is fine; it's not a change
                    case Some(role) if role != user.effectiveRole =>
                        Left(Forbidden("Only administrators can change a user's role"))
                    case other                                       => Right(other)
                }
            case Caller.User(user)                  =>
                Left(Forbidden(s"User `${user.username}` can only update their own account"))

