package fr.ac_versailles.crdp.apiscol.meta.searchEngine;

public class AbstractSearchEngineFactory {

	public enum SearchEngineType {
		JERSEY, SOLRJ
	};

	public static ISearchEngineFactory getSearchEngineFactory(
			SearchEngineType type) throws Exception {
		if (type == SearchEngineType.SOLRJ)
			return new SolrJSearchEngineFactory();
		throw new Exception("This search engine type is not implemented");
	}
}
