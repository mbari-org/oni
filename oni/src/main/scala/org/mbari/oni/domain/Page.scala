package org.mbari.oni.domain

case class Page[T](content: T, limit: Int, offset: Int)
