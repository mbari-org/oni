/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.etc.jdbc

object Databases:

    enum DatabaseType(val name: String):
        case PostgreSQL extends DatabaseType("postgresql")
        case Oracle     extends DatabaseType("oracle")
        case SQLServer  extends DatabaseType("sqlserver")

    /**
     * Infer the database type from a JDBC URL.
     * @param url
     *   The JDBC URL to infer the type from.
     * @return
     *   The inferred DatabaseType.
     */
    def typeFromUrl(url: String): DatabaseType =
        if url.contains("postgresql") then DatabaseType.PostgreSQL
        else if url.contains("oracle") then DatabaseType.Oracle
        else if url.contains("sqlserver") then DatabaseType.SQLServer
        else throw new IllegalArgumentException(s"Unknown database type for URL: $url")
