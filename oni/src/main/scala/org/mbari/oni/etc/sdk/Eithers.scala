/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.etc.sdk

import java.util.Optional

object Eithers {

    private val emptyOptionalError = new NoSuchElementException("Optional is empty")

    extension [B](opt: Optional[B])

        def toEither: Either[Throwable, B] =
            if opt.isPresent then Right(opt.get)
            else Left(emptyOptionalError)
            
    
}
