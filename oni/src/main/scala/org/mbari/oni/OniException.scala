/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni

sealed trait OniException extends Throwable

case class ConceptNameNotFound(name: String) extends Exception(s"Concept name $name was not found") with OniException
case object MissingRootConcept               extends Exception("Root concept is missing") with OniException