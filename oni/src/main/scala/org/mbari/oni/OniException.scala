/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni

sealed trait OniException extends Throwable

case class AccessDenied(user: String)
    extends Exception(s"I'm sorry `$user`, I can not let you do that.")
    with OniException
case class AphiaIdNotFound(aphiaId: Long)    extends Exception(s"AphiaId `$aphiaId` was not found") with OniException
case class ConceptNameAlreadyExists(name: String)
    extends Exception(s"Concept name `$name` already exists")
    with OniException
case class ConceptNameNotFound(name: String) extends Exception(s"Concept name `$name` was not found") with OniException
case object AccessDeniedMissingCredentials   extends Exception("I'm sorry, I can not let you do that.") with OniException
case object MissingRootConcept               extends Exception("Root concept is missing") with OniException
case object RootAlreadyExists                extends Exception("Root concept already exists") with OniException
