/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.util

import scala.reflect.ClassTag

object Reflect:

    /**
     * Create an instance of a class from a Map of parameters. The keys of the map must match the names of the
     * constructor parameters. This works for both case classes and regular classes.
     *
     * @param m
     *   The map of parameters
     * @tparam T
     *   The type of the class to create
     * @return
     *   A new instance of the class
     * @throws IllegalArgumentException
     *   if a required parameter is missing \@
     */
    def fromMap[T: ClassTag](m: Map[String, ?]): T =

        val classTag        = implicitly[ClassTag[T]]
        val constructor     = classTag.runtimeClass.getDeclaredConstructors.head
        val constructorArgs = constructor
            .getParameters()
            .map { param =>
                val paramName = param.getName
                if param.getType == classOf[Option[?]] then m.get(paramName)
                else
                    m.get(paramName)
                        .getOrElse(throw new IllegalArgumentException(s"Missing required parameter: $paramName"))
            }
        constructor.newInstance(constructorArgs*).asInstanceOf[T]

    def toMap[T: ClassTag](t: T): Map[String, ?] =
        val classTag = implicitly[ClassTag[T]]
        val fields   = classTag.runtimeClass.getDeclaredFields
        fields.map { field =>
            field.setAccessible(true)
            field.getName -> field.get(t)
        }.toMap
