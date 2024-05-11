package org.mbari.oni.jdbc

import org.mbari.oni.domain.ConceptNameTypes

class MutableConceptSuite extends munit.FunSuite {


    /* 
        1 - root, object
        `- 2 - child2
              |- 4 - child4
              |  |- 8 - child8
              |  `- 9 - child9
              |     `- 10 - child10
              `- 5 - child5
        `- 3 - child3
                |- 6 - child6
                `- 7 - child7
    */
    val rows = Seq(
        ConceptRow(1, None, "root"),
        ConceptRow(1, None, "object", nameType = ConceptNameTypes.ALTERNATE.getType),
        ConceptRow(2, Some(1), "child2"),
        ConceptRow(3, Some(1), "child3"),
        ConceptRow(4, Some(2), "child4"),
        ConceptRow(5, Some(2), "child5"),
        ConceptRow(6, Some(3), "child6"),
        ConceptRow(7, Some(3), "child7"),
        ConceptRow(8, Some(4), "child8"),
        ConceptRow(9, Some(4), "child9"),
        ConceptRow(10, Some(9), "child10")
    )

    test("toTree") {
        val (rootOpt, nodes) = MutableConcept.toTree(rows)
        assert(rootOpt.isDefined)
        val root = rootOpt.get
        assertEquals(root.id.get, 1L)
        assertEquals(root.children.size, 2)
        assertEquals(nodes.size, 10)
        assertEquals(root.primaryName, Some("root"))
        assertEquals(root.names.map(_.name).sorted, Seq("object", "root"))
    }

    test("root") {
        val (rootOpt, nodes) = MutableConcept.toTree(rows)
        val root = rootOpt.get
        val child2 = root.children.head
        val child4 = child2.children.head
        val child8 = child4.children.head
        val child9 = child4.children(1)
        val child5 = root.children(1)
        val child3 = root.children(1)
        val child6 = child3.children.head
        val child7 = child3.children(1)
        assertEquals(child8.root().id.get, 1L)
        assertEquals(child9.root().id.get, 1L)
        assertEquals(child5.root().id.get, 1L)
        assertEquals(child6.root().id.get, 1L)
        assertEquals(child7.root().id.get, 1L)
    }

    test("copyUp") {
        val (rootOpt, nodes) = MutableConcept.toTree(rows)
        val child9 = nodes.find(_.id.get == 9).get
        val child9Copy = child9.copyUp()
        assertEquals(child9Copy.id, child9.id)
        assertEquals(child9Copy.rank, child9.rank)
        assertEquals(child9Copy.names, child9.names)
        assertEquals(child9Copy.children, Nil)
        assertEquals(child9Copy.parent.get.id, child9.parent.get.id)
        assertEquals(child9Copy.root().id.get, 1L)
    }

    test("toImmutable") {
        val (rootOpt, nodes) = MutableConcept.toTree(rows)
        val root = rootOpt.get
        val concept = root.toImmutable
        assertEquals(concept.name, "root")
        assertEquals(concept.rank, None)
        assertEquals(concept.alternativeNames, Seq("object"))
        assertEquals(concept.children.size, 2)
        assertEquals(concept.children.head.name, "child2")
        assertEquals(concept.children(1).name, "child3")
    }
  
}
