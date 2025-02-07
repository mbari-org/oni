/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni

/**
 * Defines custom exceptions used by Oni
 */
sealed trait OniException extends Throwable

trait NotFoundException        extends OniException
trait AccessException          extends OniException
trait ConceptNotFoundException extends NotFoundException

case class AccessDenied(user: String)
    extends Exception(s"I'm sorry `$user`, I can not let you do that.")
    with AccessException
case class AphiaIdNotFound(aphiaId: Long) extends Exception(s"AphiaId `$aphiaId` was not found") with NotFoundException

case class ChildConceptNotFound(parentName: String, childName: String)
    extends Exception(s"Child concept `$childName` not found under `$parentName`")
    with ConceptNotFoundException
case class ConceptNameAlreadyExists(name: String)
    extends Exception(s"Concept name `$name` already exists")
    with OniException
case class ConceptNameNotFound(name: String)
    extends Exception(s"Concept name `$name` was not found")
    with ConceptNotFoundException
case class HistoryHasBeenPreviouslyProcessed(id: Long)
    extends Exception(s"History with id `$id` has already been processed")
    with OniException
case class HistoryIsInvalid(msg: String)                   extends Exception(msg) with OniException
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
    extends Exception("I'm sorry, I can not let you do that.")
    with AccessException
case object MissingRootConcept extends Exception("Root concept is missing") with NotFoundException
case object RootAlreadyExists  extends Exception("Root concept already exists") with OniException
