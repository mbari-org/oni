/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.etc.xml


import org.w3c.dom.NodeList
import org.w3c.dom.Node

given Conversion[NodeList, List[Node]] with
  def apply(nodeList: NodeList): List[Node] =
    (0 until nodeList.getLength).map(nodeList.item).toList
