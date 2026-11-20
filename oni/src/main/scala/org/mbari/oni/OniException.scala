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

package org.mbari.oni

/**
 * Defines custom exceptions used by Oni
 */
sealed trait OniException extends Throwable

trait NotFoundException        extends OniException
trait AccessException          extends OniException
trait ConceptNotFoundException extends NotFoundException
trait BadRequestException      extends OniException

case class AccessDenied(user: String)
    extends Exception(s"I'm sorry `$user`, I can not let you do that.")
    with AccessException
case class AdministratorRequired(user: String, action: String)
    extends Exception(s"User `$user` must be an administrator to $action")
    with AccessException
case class AphiaIdNotFound(aphiaId: Long) extends Exception(s"AphiaId `$aphiaId` was not found") with NotFoundException

case class ChildConceptNotFound(parentName: String, childName: String)
    extends Exception(s"Child concept `$childName` not found under `$parentName`")
    with ConceptNotFoundException
case class ConceptNameAlreadyExists(name: String)
    extends Exception(s"Concept name `$name` already exists")
    with BadRequestException
case class ConceptNameNotFound(name: String)
    extends Exception(s"Concept name `$name` was not found")
    with ConceptNotFoundException
case class ConceptNameIdNotFound(id: Long)
    extends Exception(s"Concept name with id `$id` was not found")
    with ConceptNotFoundException
case class InvalidConceptNameChange(msg: String)           extends Exception(msg) with BadRequestException
case class InvalidConceptChange(msg: String)               extends Exception(msg) with BadRequestException
case class InvalidRank(msg: String)                        extends Exception(msg) with BadRequestException
case class ConceptIdNotFound(id: Long)
    extends Exception(s"Concept with id `$id` was not found")
    with ConceptNotFoundException
case class HistoryHasBeenPreviouslyProcessed(id: Long)
    extends Exception(s"History with id `$id` has already been processed")
    with OniException
case class HistoryIsInvalid(msg: String)                   extends Exception(msg) with OniException
case class InvalidSortField(field: String, cause: Throwable)
    extends Exception(s"`$field` is not a valid sort field", cause)
    with BadRequestException
case class ItemNotFound(msg: String)                       extends Exception(msg) with NotFoundException
case class LinkRealizationIdNotFound(id: Long)
    extends Exception(s"LinkRealization with `$id` was not found")
    with NotFoundException
case class LinkTemplateIdNotFound(id: Long)
    extends Exception(s"LinkTemplate with `$id` was not found")
    with NotFoundException
case class ParentConceptNotFound(name: String)
    extends Exception(s"Parent concept for `$name` was not found")
    with ConceptNotFoundException
case class ReferenceIdNotFound(id: Long)                   extends Exception(s"Reference with `$id` was not found") with NotFoundException
case class WrappedException(msg: String, cause: Throwable) extends Exception(msg, cause) with OniException

case object AccessDeniedMissingCredentials
    extends Exception("Missing credentials: no user name was provided, so the caller could not be identified")
    with AccessException
case object MissingRootConcept extends Exception("Root concept is missing") with NotFoundException
case object RootAlreadyExists  extends Exception("Root concept already exists") with OniException
