package org.elasticsearch.test.integration.search.child

import org.elasticsearch.index.query._, FilterBuilders._, QueryBuilders._
import org.elasticsearch.action.search._
import scalastic.elasticsearch._

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner]) class SimpleChildQuerySearchTests extends IndexerBasedTest {

  test("multiLevelChild") {
    indexer.putMapping(indexName, "child", """{"type": {"_parent": {"type": "parent"}}}""")
    indexer.putMapping(indexName, "grandchild", """{"type": {"_parent": {"type": "child"}}}""")
    indexer.index(indexName, "parent", "p1", """{"p_field": "p_value1"}""")
    indexer.index(indexName, "child", "c1", """{"c_field": "c_value1"}""", parent = "p1")
    indexer.index(indexName, "grandchild", "gc1", """{"gc_field": "gc_value1"}""", parent = "c1", routing = Some("gc1"))
    indexer.refresh()
    val response = indexer.search(Seq(indexName), query = filteredQuery(matchAllQuery, hasChildFilter("child", filteredQuery(termQuery("c_field", "c_value1"), hasChildFilter("grandchild", termQuery("gc_field", "gc_value1"))))))
    response.shardFailures.length should be (0)
    response.hits.totalHits should be === (1)
  }

  test("simpleChildQuery") {
    indexer.putMapping(indexName, "child", """{"type": {"_parent": {"type": "parent"}}}""")
    indexer.index(indexName, "parent", "p1", """{"p_field": "p_value1"}""")
    indexer.index(indexName, "child", "c1", """{"c_field": "red"}""", parent = "p1")
    indexer.index(indexName, "child", "c2", """{"c_field": "yellow"}""", parent = "p1")
    indexer.index(indexName, "parent", "p2", """{"p_field": "p_value2"}""")
    indexer.index(indexName, "child", "c3", """{"c_field": "blue"}""", parent = "p2")
    indexer.index(indexName, "child", "c4", """{"c_field": "red"}""", parent = "p2")
    indexer.refresh()

    var response = indexer.search(Seq(indexName), query = idsQuery("child").ids("c1"), fields = Seq("_parent"))
    response.shardFailures.length should be === (0)
    response.failedShards() should be === (0)
    response.hits.totalHits should be === (1)
    response.hits.getAt(0).id should be === ("c1")
    response.hits.getAt(0).field("_parent").value().toString should be === ("p1")

    response = indexer.search(Seq(indexName), query = termQuery("child._parent", "p1"), fields = Seq("_parent"))
    response.shardFailures.length should be === (0)
    response.failedShards() should be === (0)
    response.hits.totalHits should be === (2)
    //    response.hits.getAt(0).id(), anyOf(equalTo("c1") should be === ("c2"))
    response.hits.getAt(0).field("_parent").value().toString should be === ("p1")
    //    response.hits.getAt(1).id(), anyOf(equalTo("c1") should be === ("c2"))
    response.hits.getAt(1).field("_parent").value().toString should be === ("p1")

    response = indexer.search(Seq(indexName), query = termQuery("_parent", "p1"), fields = Seq("_parent"))
    response.shardFailures.length should be === (0)
    response.failedShards() should be === (0)
    response.hits.totalHits should be === (2)
    //    response.hits.getAt(0).id(), anyOf(equalTo("c1") should be === ("c2"))
    response.hits.getAt(0).field("_parent").value().toString should be === ("p1")
    //    response.hits.getAt(1).id(), anyOf(equalTo("c1") should be === ("c2"))
    response.hits.getAt(1).field("_parent").value().toString should be === ("p1")

    response = indexer.search(Seq(indexName), query = queryString("_parent:p1"), fields = Seq("_parent"))
    response.shardFailures.length should be === (0)
    response.failedShards() should be === (0)
    response.hits.totalHits should be === (2)
    //    response.hits.getAt(0).id(), anyOf(equalTo("c1") should be === ("c2"))
    response.hits.getAt(0).field("_parent").value().toString should be === ("p1")
    //    response.hits.getAt(1).id(), anyOf(equalTo("c1") should be === ("c2"))
    response.hits.getAt(1).field("_parent").value().toString should be === ("p1")

    response = indexer.search(Seq(indexName), query = topChildrenQuery("child", termQuery("c_field", "yellow")))
    response.shardFailures.length should be === (0)
    response.failedShards() should be === (0)
    response.hits.totalHits should be === (1)
    response.hits.getAt(0).id should be === ("p1")

    response = indexer.search(Seq(indexName), query = topChildrenQuery("child", termQuery("c_field", "blue")))
    if (response.failedShards() > 0) {
      //logger.warn("Failed shards:")
      for (shardSearchFailure <- response.shardFailures) {
        //logger.warn("-> {}", shardSearchFailure)
      }
    }
    response.failedShards() should be === (0)
    response.hits.totalHits should be === (1)
    response.hits.getAt(0).id should be === ("p2")
    response = indexer.search(Seq(indexName), query = topChildrenQuery("child", termQuery("c_field", "red")))
    response.shardFailures.length should be === (0)
    response.failedShards() should be === (0)
    response.hits.totalHits should be === (2)
    //    response.hits.getAt(0).id(), anyOf(equalTo("p2") should be === ("p1"))
    //    response.hits.getAt(1).id(), anyOf(equalTo("p2") should be === ("p1"))
    response = indexer.search(Seq(indexName), query = hasChildQuery("child", termQuery("c_field", "yellow")))
    if (response.failedShards() > 0) {
      //logger.warn("Failed shards:")
      for (shardSearchFailure <- response.shardFailures) {
        //logger.warn("-> {}", shardSearchFailure)
      }
    }
    response.failedShards() should be === (0)
    response.hits.totalHits should be === (1)
    response.hits.getAt(0).id should be === ("p1")
    response = indexer.search(Seq(indexName), query = hasChildQuery("child", termQuery("c_field", "blue")))
    response.shardFailures.length should be === (0)
    response.failedShards() should be === (0)
    response.hits.totalHits should be === (1)
    response.hits.getAt(0).id should be === ("p2")
    response = indexer.search(Seq(indexName), query = hasChildQuery("child", termQuery("c_field", "red")))
    response.shardFailures.length should be === (0)
    response.failedShards() should be === (0)
    response.hits.totalHits should be === (2)
    //    response.hits.getAt(0).id(), anyOf(equalTo("p2") should be === ("p1"))
    //    response.hits.getAt(1).id(), anyOf(equalTo("p2") should be === ("p1"))
    response = indexer.search(Seq(indexName), query = constantScoreQuery(hasChildFilter("child", termQuery("c_field", "yellow"))))
    response.shardFailures.length should be === (0)
    response.failedShards() should be === (0)
    response.hits.totalHits should be === (1)
    response.hits.getAt(0).id should be === ("p1")
    response = indexer.search(Seq(indexName), query = constantScoreQuery(hasChildFilter("child", termQuery("c_field", "blue"))))
    if (response.failedShards() > 0) {
      //logger.warn("Failed shards:")
      for (shardSearchFailure <- response.shardFailures) {
        //logger.warn("-> {}", shardSearchFailure)
      }
    }
    response.failedShards() should be === (0)
    response.hits.totalHits should be === (1)
    response.hits.getAt(0).id should be === ("p2")
    response = indexer.search(Seq(indexName), query = constantScoreQuery(hasChildFilter("child", termQuery("c_field", "red"))))
    response.shardFailures.length should be === (0)
    response.failedShards() should be === (0)
    response.hits.totalHits should be === (2)
    //    response.hits.getAt(0).id(), anyOf(equalTo("p2") should be === ("p1"))
    //    response.hits.getAt(1).id(), anyOf(equalTo("p2") should be === ("p1"))
  }

  test("simpleChildQueryWithFlush") {
    indexer.putMapping(indexName, "child", """{"type": {"_parent": {"type": "parent"}}}""")
    indexer.index(indexName, "parent", "p1", """{"p_field": "p_value1"}""")
    indexer.index(indexName, "child", "c1", """{"c_field": "red"}""", parent = "p1")
    indexer.index(indexName, "child", "c2", """{"c_field": "yellow"}""", parent = "p1")
    indexer.index(indexName, "parent", "p2", """{"p_field": "p_value2"}""")
    indexer.index(indexName, "child", "c3", """{"c_field": "blue"}""", parent = "p2")
    indexer.index(indexName, "child", "c4", """{"c_field": "red"}""", parent = "p2")
    indexer.refresh()
    var response = indexer.search(Seq(indexName), query = topChildrenQuery("child", termQuery("c_field", "yellow")))
    response.shardFailures.length should be === (0)
    response.failedShards() should be === (0)
    response.hits.totalHits should be === (1)
    response.hits.getAt(0).id should be === ("p1")
    response = indexer.search(Seq(indexName), query = topChildrenQuery("child", termQuery("c_field", "blue")))
    if (response.failedShards() > 0) {
      //logger.warn("Failed shards:")
      for (shardSearchFailure <- response.shardFailures) {
        //logger.warn("-> {}", shardSearchFailure)
      }
    }
    response.failedShards() should be === (0)
    response.hits.totalHits should be === (1)
    response.hits.getAt(0).id should be === ("p2")
    response = indexer.search(Seq(indexName), query = topChildrenQuery("child", termQuery("c_field", "red")))
    response.shardFailures.length should be === (0)
    response.failedShards() should be === (0)
    response.hits.totalHits should be === (2)
    //    response.hits.getAt(0).id(), anyOf(equalTo("p2") should be === ("p1"))
    //    response.hits.getAt(1).id(), anyOf(equalTo("p2") should be === ("p1"))
    response = indexer.search(Seq(indexName), query = hasChildQuery("child", termQuery("c_field", "yellow")))
    response.shardFailures.length should be === (0)
    response.failedShards() should be === (0)
    response.hits.totalHits should be === (1)
    response.hits.getAt(0).id should be === ("p1")
    response = indexer.search(Seq(indexName), query = hasChildQuery("child", termQuery("c_field", "blue")))
    response.shardFailures.length should be === (0)
    response.failedShards() should be === (0)
    response.hits.totalHits should be === (1)
    response.hits.getAt(0).id should be === ("p2")
    response = indexer.search(Seq(indexName), query = hasChildQuery("child", termQuery("c_field", "red")))
    response.shardFailures.length should be === (0)
    response.failedShards() should be === (0)
    response.hits.totalHits should be === (2)
    //    response.hits.getAt(0).id(), anyOf(equalTo("p2") should be === ("p1"))
    //    response.hits.getAt(1).id(), anyOf(equalTo("p2") should be === ("p1"))
    response = indexer.search(Seq(indexName), query = constantScoreQuery(hasChildFilter("child", termQuery("c_field", "yellow"))))
    response.shardFailures.length should be === (0)
    response.failedShards() should be === (0)
    response.hits.totalHits should be === (1)
    response.hits.getAt(0).id should be === ("p1")
    response = indexer.search(Seq(indexName), query = constantScoreQuery(hasChildFilter("child", termQuery("c_field", "blue"))))
    response.shardFailures.length should be === (0)
    response.failedShards() should be === (0)
    response.hits.totalHits should be === (1)
    response.hits.getAt(0).id should be === ("p2")
    response = indexer.search(Seq(indexName), query = constantScoreQuery(hasChildFilter("child", termQuery("c_field", "red"))))
    response.shardFailures.length should be === (0)
    response.failedShards() should be === (0)
    response.hits.totalHits should be === (2)
    //    response.hits.getAt(0).id(), anyOf(equalTo("p2") should be === ("p1"))
    //    response.hits.getAt(1).id(), anyOf(equalTo("p2") should be === ("p1"))
  }

  test("simpleChildQueryWithFlushAnd3Shards") {
    indexer.putMapping(indexName, "child", """{"type": {"_parent": {"type": "parent"}}}""")
    indexer.index(indexName, "parent", "p1", """{"p_field": "p_value1"}""")
    indexer.index(indexName, "child", "c1", """{"c_field": "red"}""", parent = "p1")
    indexer.index(indexName, "child", "c2", """{"c_field": "yellow"}""", parent = "p1")
    indexer.index(indexName, "parent", "p2", """{"p_field": "p_value2"}""")
    indexer.index(indexName, "child", "c3", """{"c_field": "blue"}""", parent = "p2")
    indexer.index(indexName, "child", "c4", """{"c_field": "red"}""", parent = "p2")
    indexer.refresh()
    var response = indexer.search(Seq(indexName), query = topChildrenQuery("child", termQuery("c_field", "yellow")))
    response.shardFailures.length should be === (0)
    response.failedShards() should be === (0)
    response.hits.totalHits should be === (1)
    response.hits.getAt(0).id should be === ("p1")
    response = indexer.search(Seq(indexName), query = topChildrenQuery("child", termQuery("c_field", "blue")))
    if (response.failedShards() > 0) {
      //logger.warn("Failed shards:")
      for (shardSearchFailure <- response.shardFailures) {
        //logger.warn("-> {}", shardSearchFailure)
      }
    }
    response.failedShards() should be === (0)
    response.hits.totalHits should be === (1)
    response.hits.getAt(0).id should be === ("p2")
    response = indexer.search(Seq(indexName), query = topChildrenQuery("child", termQuery("c_field", "red")))
    response.shardFailures.length should be === (0)
    response.failedShards() should be === (0)
    response.hits.totalHits should be === (2)
    //    response.hits.getAt(0).id(), anyOf(equalTo("p2") should be === ("p1"))
    //    response.hits.getAt(1).id(), anyOf(equalTo("p2") should be === ("p1"))
    response = indexer.search(Seq(indexName), query = hasChildQuery("child", termQuery("c_field", "yellow")))
    response.shardFailures.length should be === (0)
    response.failedShards() should be === (0)
    response.hits.totalHits should be === (1)
    response.hits.getAt(0).id should be === ("p1")
    response = indexer.search(Seq(indexName), query = hasChildQuery("child", termQuery("c_field", "blue")))
    response.shardFailures.length should be === (0)
    response.failedShards() should be === (0)
    response.hits.totalHits should be === (1)
    response.hits.getAt(0).id should be === ("p2")
    response = indexer.search(Seq(indexName), query = hasChildQuery("child", termQuery("c_field", "red")))
    response.shardFailures.length should be === (0)
    response.failedShards() should be === (0)
    response.hits.totalHits should be === (2)
    //    response.hits.getAt(0).id(), anyOf(equalTo("p2") should be === ("p1"))
    //    response.hits.getAt(1).id(), anyOf(equalTo("p2") should be === ("p1"))
    response = indexer.search(Seq(indexName), query = constantScoreQuery(hasChildFilter("child", termQuery("c_field", "yellow"))))
    response.shardFailures.length should be === (0)
    response.failedShards() should be === (0)
    response.hits.totalHits should be === (1)
    response.hits.getAt(0).id should be === ("p1")
    response = indexer.search(Seq(indexName), query = constantScoreQuery(hasChildFilter("child", termQuery("c_field", "blue"))))
    response.shardFailures.length should be === (0)
    response.failedShards() should be === (0)
    response.hits.totalHits should be === (1)
    response.hits.getAt(0).id should be === ("p2")
    response = indexer.search(Seq(indexName), query = constantScoreQuery(hasChildFilter("child", termQuery("c_field", "red"))))
    response.shardFailures.length should be === (0)
    response.failedShards() should be === (0)
    response.hits.totalHits should be === (2)
    //    response.hits.getAt(0).id(), anyOf(equalTo("p2") should be === ("p1"))
    //    response.hits.getAt(1).id(), anyOf(equalTo("p2") should be === ("p1"))
  }

  test("testScopedFacet") {
    indexer.putMapping(indexName, "child", """{"type": {"_parent": {"type": "parent"}}}""")
    indexer.index(indexName, "parent", "p1", """{"p_field": "p_value1"}""")
    indexer.index(indexName, "child", "c1", """{"c_field": "red"}""", parent = "p1")
    indexer.index(indexName, "child", "c2", """{"c_field": "yellow"}""", parent = "p1")
    indexer.index(indexName, "parent", "p2", """{"p_field": "p_value2"}""")
    indexer.index(indexName, "child", "c3", """{"c_field": "blue"}""", parent = "p2")
    indexer.index(indexName, "child", "c4", """{"c_field": "red"}""", parent = "p2")
    indexer.refresh()
    val response: SearchResponse = indexer.search_prepare(Seq(indexName)).setQuery(topChildrenQuery("child", boolQuery().should(termQuery("c_field",
      "red")).should(termQuery("c_field", "yellow")))
      .scope("child1"))
      .addFacet(org.elasticsearch.search.facet.FacetBuilders.termsFacet("facet1").field("c_field").scope("child1")).execute.actionGet
    response.shardFailures.length should be === (0)
    response.failedShards() should be === (0)
    response.hits.totalHits should be === (2)
    //    response.hits.getAt(0).id(), anyOf(equalTo("p2") should be === ("p1"))
    //    response.hits.getAt(1).id(), anyOf(equalTo("p2") should be === ("p1"))
    response.facets().facets().size should be === (1)
    val termsFacet: org.elasticsearch.search.facet.terms.TermsFacet = response.facets().facet("facet1")
    termsFacet.entries().size should be === (2)
    termsFacet.entries().get(0).term() should be === ("red")
    termsFacet.entries().get(0).count() should be === (2)
    termsFacet.entries().get(1).term() should be === ("yellow")
    termsFacet.entries().get(1).count() should be === (1)
  }

  test("testDeletedParent") {
    indexer.putMapping(indexName, "child", """{"type": {"_parent": {"type": "parent"}}}""")
    indexer.index(indexName, "parent", "p1", """{"p_field": "p_value1"}""")
    indexer.index(indexName, "child", "c1", """{"c_field": "red"}""", parent = "p1")
    indexer.index(indexName, "child", "c2", """{"c_field": "yellow"}""", parent = "p1")
    indexer.index(indexName, "parent", "p2", """{"p_field": "p_value2"}""")
    indexer.index(indexName, "child", "c3", """{"c_field": "blue"}""", parent = "p2")
    indexer.index(indexName, "child", "c4", """{"c_field": "red"}""", parent = "p2")
    indexer.refresh()
    var response = indexer.search(Seq(indexName), query = topChildrenQuery("child", termQuery("c_field", "yellow")))
    if (response.failedShards() > 0) {
      //logger.warn("Failed shards:")
      for (shardSearchFailure <- response.shardFailures) {
        //logger.warn("-> {}", shardSearchFailure)
      }
    }
    response.failedShards() should be === (0)
    response.hits.totalHits should be === (1)
    response.hits.getAt(0).id should be === ("p1")
    response.hits.getAt(0).sourceAsString() should include("\"p_value1\"")
    response = indexer.search(Seq(indexName), query = constantScoreQuery(hasChildFilter("child", termQuery("c_field", "yellow"))))
    response.shardFailures.length should be === (0)
    response.failedShards() should be === (0)
    response.hits.totalHits should be === (1)
    response.hits.getAt(0).id should be === ("p1")
    response.hits.getAt(0).sourceAsString() should include("\"p_value1\"")
    indexer.index(indexName, "parent", "p1", """{"p_field": "p_value1_updated"}""")
    indexer.refresh()
    response = indexer.search(Seq(indexName), query = topChildrenQuery("child", termQuery("c_field", "yellow")))
    response.shardFailures.length should be === (0)
    response.failedShards() should be === (0)
    response.hits.totalHits should be === (1)
    response.hits.getAt(0).id should be === ("p1")
    response.hits.getAt(0).sourceAsString() should include("\"p_value1_updated\"")
    response = indexer.search(Seq(indexName), query = constantScoreQuery(hasChildFilter("child", termQuery("c_field", "yellow"))))
    response.shardFailures.length should be === (0)
    response.failedShards() should be === (0)
    response.hits.totalHits should be === (1)
    response.hits.getAt(0).id should be === ("p1")
    response.hits.getAt(0).sourceAsString() should include("\"p_value1_updated\"")
  }

  test("testDfsSearchType") {
    indexer.putMapping(indexName, "child", """{"type": {"_parent": {"type": "parent"}}}""")
    indexer.index(indexName, "parent", "p1", """{"p_field": "p_value1"}""")
    indexer.index(indexName, "child", "c1", """{"c_field": "red"}""", parent = "p1")
    indexer.index(indexName, "child", "c2", """{"c_field": "yellow"}""", parent = "p1")
    indexer.index(indexName, "parent", "p2", """{"p_field": "p_value2"}""")
    indexer.index(indexName, "child", "c3", """{"c_field": "blue"}""", parent = "p2")
    indexer.index(indexName, "child", "c4", """{"c_field": "red"}""", parent = "p2")
    indexer.refresh()
    val response = indexer.search(Seq(indexName), searchType = Some(SearchType.DFS_QUERY_THEN_FETCH), query = boolQuery().mustNot(hasChildQuery("child", boolQuery().should(queryString("c_field:*")))))
    response.shardFailures.length should be === (0)
  }
}
