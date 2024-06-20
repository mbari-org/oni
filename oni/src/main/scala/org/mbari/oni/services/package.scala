/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni

/**
 * This package contains services that interact with the database. In general, these services handle all transactions
 * for you, except when the method name starts with `inTxn`. Many services will contain a [[UserAccountService]] to
 * handle authentication and authorization. Otherwise, do not nest services except for the [[HistoryActionService]].
 */
package object services {}
