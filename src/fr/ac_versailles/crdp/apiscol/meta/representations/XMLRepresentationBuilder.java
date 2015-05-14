package fr.ac_versailles.crdp.apiscol.meta.representations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.sourceforge.cardme.engine.VCardEngine;
import net.sourceforge.cardme.vcard.VCard;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.CDATASection;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import fr.ac_versailles.crdp.apiscol.UsedNamespaces;
import fr.ac_versailles.crdp.apiscol.database.DBAccessException;
import fr.ac_versailles.crdp.apiscol.meta.MetadataKeySyntax;
import fr.ac_versailles.crdp.apiscol.meta.codeSnippets.SnippetGenerator;
import fr.ac_versailles.crdp.apiscol.meta.codeSnippets.SnippetGenerator.OPTIONS;
import fr.ac_versailles.crdp.apiscol.meta.dataBaseAccess.AbstractResourcesDataHandler.MetadataProperties;
import fr.ac_versailles.crdp.apiscol.meta.dataBaseAccess.IResourceDataHandler;
import fr.ac_versailles.crdp.apiscol.meta.fileSystemAccess.FileSystemAccessException;
import fr.ac_versailles.crdp.apiscol.meta.fileSystemAccess.InvalidProvidedMetadataFileException;
import fr.ac_versailles.crdp.apiscol.meta.fileSystemAccess.MetadataNotFoundException;
import fr.ac_versailles.crdp.apiscol.meta.fileSystemAccess.ResourceDirectoryInterface;
import fr.ac_versailles.crdp.apiscol.meta.searchEngine.ISearchEngineResultHandler;
import fr.ac_versailles.crdp.apiscol.meta.searchEngine.SolrRecordsSyntaxAnalyser;
import fr.ac_versailles.crdp.apiscol.utils.TimeUtils;
import fr.ac_versailles.crdp.apiscol.utils.XMLUtils;

public class XMLRepresentationBuilder extends
		AbstractRepresentationBuilder<Document> {

	private static SnippetGenerator snippetGenerator = new SnippetGenerator();
	private static VCardEngine vcardengine = new VCardEngine();

	@Override
	public Document getMetadataRepresentation(UriInfo uriInfo,
			String apiscolInstanceName, String resourceId,
			boolean includeDescription, Map<String, String> params,
			IResourceDataHandler resourceDataHandler, String editUri)
			throws MetadataNotFoundException, DBAccessException {
		Document XMLRepresentation = createXMLDocument();
		addXMLSubTreeForMetadata(XMLRepresentation, XMLRepresentation, uriInfo,
				apiscolInstanceName, resourceId, includeDescription, -1,
				resourceDataHandler, editUri);
		addNameSpaces(XMLRepresentation);
		return XMLRepresentation;
	}

	@Override
	public Document getMetadataSnippetRepresentation(UriInfo uriInfo,
			String apiscolInstanceName, String metadataId, String version) {
		Document XMLRepresentation = createXMLDocument();
		Element rootElement = XMLRepresentation
				.createElement("apiscol:snippet");
		Element frameworkElement = XMLRepresentation
				.createElement("apiscol:framework");
		Element scriptElement = XMLRepresentation
				.createElement("apiscol:script");
		Element tagElement = XMLRepresentation
				.createElement("apiscol:tag-pattern");
		Element optionsElement = XMLRepresentation
				.createElement("apiscol:options");
		Element iframeElement = XMLRepresentation
				.createElement("apiscol:iframe");
		CDATASection framework = XMLRepresentation
				.createCDATASection(snippetGenerator.getScript(
						SnippetGenerator.SCRIPTS.JQUERY, version));
		frameworkElement.appendChild(framework);
		CDATASection script = XMLRepresentation
				.createCDATASection(snippetGenerator.getScript(
						SnippetGenerator.SCRIPTS.APISCOL, version));
		scriptElement.appendChild(script);
		tagElement.appendChild(XMLRepresentation
				.createCDATASection(snippetGenerator
						.getTagPattern(getMetadataUri(uriInfo, metadataId))));
		ArrayList<OPTIONS> options = snippetGenerator.getTagOptions();
		for (Iterator<OPTIONS> iterator = options.iterator(); iterator
				.hasNext();) {
			optionsElement.appendChild(getOptionTag(iterator.next(),
					XMLRepresentation));
		}
		iframeElement.appendChild(XMLRepresentation
				.createCDATASection(snippetGenerator.getIframe(getMetadataUri(
						uriInfo, metadataId))));
		XMLRepresentation.appendChild(rootElement);
		rootElement.appendChild(frameworkElement);
		rootElement.appendChild(scriptElement);
		rootElement.appendChild(tagElement);
		rootElement.appendChild(optionsElement);
		rootElement.appendChild(iframeElement);

		addNameSpaces(XMLRepresentation);
		return XMLRepresentation;
	}

	private Node getOptionTag(OPTIONS option, Document doc) {
		Element optionElement = doc.createElement("apiscol:options");
		optionElement.setAttribute("token",
				snippetGenerator.getOptionToken(option));
		ArrayList<String> values = snippetGenerator.getOptionsValues(option);
		for (Iterator<String> iterator = values.iterator(); iterator.hasNext();) {
			Element valueElement = doc.createElement("apiscol:value");
			valueElement.setTextContent(iterator.next());
			optionElement.appendChild(valueElement);
		}
		return optionElement;
	}

	@Override
	public Document getCompleteMetadataListRepresentation(UriInfo uriInfo,
			final String apiscolInstanceName,
			final String apiscolInstanceLabel, int start, int rows,
			boolean includeDescription,
			IResourceDataHandler resourceDataHandler, String editUri,
			String version) throws DBAccessException {
		ArrayList<String> metadatasList = getMetadataList();
		Document response = createXMLDocument();
		Element feedElement = response.createElementNS(
				UsedNamespaces.ATOM.getUri(), "feed");
		feedElement.setAttribute("length", "" + metadatasList.size());
		addFeedInfos(response, feedElement, apiscolInstanceName,
				apiscolInstanceLabel, uriInfo, version);
		Iterator<String> it = metadatasList.iterator();
		int counter = -1;
		Long maxUpdated = 0L;
		while (it.hasNext()) {
			String metadataId = it.next();
			counter++;
			if (counter < start)
				continue;
			if (counter >= (start + rows))
				break;
			try {
				maxUpdated = Math.max(
						addXMLSubTreeForMetadata(response, feedElement,
								uriInfo, apiscolInstanceName, metadataId,
								includeDescription, -1, resourceDataHandler,
								editUri), maxUpdated);
			} catch (MetadataNotFoundException e) {
				logger.error(String
						.format("The metadata %s was not found while trying to build xml representation",
								metadataId));
			}

		}
		response.appendChild(feedElement);
		addNameSpaces(response);
		return response;
	}

	@Override
	public MediaType getMediaType() {
		return MediaType.APPLICATION_XML_TYPE;
	}

	private Long addXMLSubTreeForMetadata(Document XMLDocument,
			Node insertionElement, UriInfo uriInfo, String apiscolInstanceName,
			String metadataId, boolean includeDescription, float score,
			IResourceDataHandler resourceDataHandler, String editUri)
			throws MetadataNotFoundException, DBAccessException {
		Element rootElement = XMLDocument.createElement("entry");
		Element updatedElement = XMLDocument.createElement("updated");
		long utcTime = Long.parseLong(getEtagForMetadata(metadataId));

		updatedElement.setTextContent(TimeUtils.toRFC3339(utcTime));
		rootElement.appendChild(updatedElement);
		if (score != -1) {
			Element scoreElement = XMLDocument.createElement("apiscol:score");
			scoreElement.setTextContent(Float.toString(score));
			rootElement.appendChild(scoreElement);
		}
		Element idElement = XMLDocument.createElement("id");
		idElement
				.setTextContent(getMetadataUrn(metadataId, apiscolInstanceName));
		rootElement.appendChild(idElement);
		if (includeDescription) {
			HashMap<String, String> mdProperties;
			try {
				if (resourceDataHandler != null)
					mdProperties = resourceDataHandler
							.getMetadataProperties(metadataId);
				else
					mdProperties = ResourceDirectoryInterface
							.getMetadataProperties(metadataId);
				if (!StringUtils.isBlank(mdProperties
						.get(MetadataProperties.title.toString()))) {
					Element titleElement = XMLDocument.createElement("title");
					titleElement.setTextContent(mdProperties
							.get(MetadataProperties.title.toString()));
					rootElement.appendChild(titleElement);
				}

				if (!StringUtils.isBlank(mdProperties
						.get(MetadataProperties.description.toString()))) {
					Element descElement = XMLDocument.createElement("summary");
					descElement.setTextContent(mdProperties
							.get(MetadataProperties.description.toString()));
					rootElement.appendChild(descElement);
				}
				if (!StringUtils.isBlank(mdProperties
						.get(MetadataProperties.aggregationLevel.toString()))) {
					Element categoryElement = XMLDocument
							.createElement("category");
					categoryElement.setAttribute("term",
							mdProperties
									.get(MetadataProperties.aggregationLevel
											.toString()));
					rootElement.appendChild(categoryElement);
				}
				if (!StringUtils.isBlank(mdProperties
						.get(MetadataProperties.contentRestUrl.toString()))) {
					Element contentRestAtomLinkElement = XMLDocument
							.createElement("link");
					contentRestAtomLinkElement.setAttribute("rel", "describes");
					contentRestAtomLinkElement.setAttribute("type",
							"application/atom+xml");
					contentRestAtomLinkElement.setAttribute("href",
							mdProperties.get(MetadataProperties.contentRestUrl
									.toString()));
					rootElement.appendChild(contentRestAtomLinkElement);
					Element contentRestHtmlLinkElement = XMLDocument
							.createElement("link");
					contentRestHtmlLinkElement.setAttribute("rel", "describes");
					contentRestHtmlLinkElement
							.setAttribute("type", "text/html");
					contentRestHtmlLinkElement.setAttribute(
							"href",
							mdProperties.get(
									MetadataProperties.contentRestUrl
											.toString()).replaceAll("\\?.*$",
									""));
					rootElement.appendChild(contentRestHtmlLinkElement);
				}

				String iconUrl = mdProperties.get(MetadataProperties.icon
						.toString());
				if (!StringUtils.isBlank(iconUrl)) {
					Element iconElement = XMLDocument.createElement("link");
					iconElement.setAttribute("rel", "icon");
					String type = "image/*";
					if (iconUrl.endsWith("jpeg") || iconUrl.endsWith("jpg"))
						type = "image/jpeg";
					else if (iconUrl.endsWith("png"))
						type = "image/png";
					else if (iconUrl.endsWith("tiff"))
						type = "image/tiff";
					else if (iconUrl.endsWith("ico"))
						type = "image/ico";
					iconElement.setAttribute("href", iconUrl.toLowerCase());
					iconElement.setAttribute("type", type);
					rootElement.appendChild(iconElement);
				}
				if (!StringUtils.isBlank(mdProperties
						.get(MetadataProperties.contentUrl.toString()))
						&& !StringUtils
								.isBlank(mdProperties
										.get(MetadataProperties.contentMime
												.toString()))) {
					Element contentElement = XMLDocument
							.createElement("content");
					contentElement.setAttribute("src", mdProperties
							.get(MetadataProperties.contentUrl.toString()));
					contentElement.setAttribute("type", mdProperties
							.get(MetadataProperties.contentMime.toString()));
					rootElement.appendChild(contentElement);
				}

				int authorNumber = 0;
				Element authorElement = XMLDocument.createElement("author");
				rootElement.appendChild(authorElement);
				Element nameElement = XMLDocument.createElement("name");
				authorElement.appendChild(nameElement);
				while (!StringUtils.isEmpty(mdProperties
						.get(MetadataProperties.author.toString()
								+ authorNumber))) {

					String inlineVcard = mdProperties
							.get(MetadataProperties.author.toString()
									+ authorNumber);
					String multilineVcard = inlineVcard.replaceAll("([A-Z]+:)",
							"\n$1")
							.replaceAll("VCARDVERSION", "VCARD\nVERSION");
					try {
						VCard vcardParsed = vcardengine.parse(multilineVcard);

						if (vcardParsed != null
								&& vcardParsed.getName() != null)
							nameElement.setTextContent(vcardParsed.getName()
									.getFamilyName());

					} catch (DOMException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					authorNumber++;
				}

			} catch (InvalidProvidedMetadataFileException e) {
				logger.error(String
						.format("Impossible to read the xml file for metadata %s while trying to build xml representation, syntax problem",
								metadataId));
			} catch (FileSystemAccessException e) {
				logger.error(String
						.format("Impossible to reach the xml file for metadata %s while trying to build xml representation, file system access problem",
								metadataId));
			}

		}
		Element selfHTMLLinkElement = XMLDocument.createElement("link");
		selfHTMLLinkElement.setAttribute("rel", "self");
		selfHTMLLinkElement.setAttribute("type", "text/html");
		selfHTMLLinkElement.setAttribute("href",
				getMetadataHTMLUri(uriInfo, metadataId));
		rootElement.appendChild(selfHTMLLinkElement);
		Element selfAtomXMLLinkElement = XMLDocument.createElement("link");
		selfAtomXMLLinkElement.setAttribute("rel", "self");
		selfAtomXMLLinkElement.setAttribute("type", "application/atom+xml");
		selfAtomXMLLinkElement.setAttribute("href",
				getMetadataAtomXMLUri(uriInfo, metadataId));
		rootElement.appendChild(selfAtomXMLLinkElement);
		if (StringUtils.isNotEmpty(editUri)) {
			Element editionLinkElement = XMLDocument.createElement("link");
			editionLinkElement.setAttribute("rel", "edit");
			editionLinkElement.setAttribute("type", "application/atom+xml");
			editionLinkElement.setAttribute("href",
					getMetadataEditUri(editUri, metadataId));
			rootElement.appendChild(editionLinkElement);
		}

		Element downloadLinkElement = XMLDocument.createElement("link");
		downloadLinkElement.setAttribute("rel", "describedby");
		downloadLinkElement.setAttribute("type", "application/lom+xml");
		downloadLinkElement.setAttribute("href",
				getMetadataDownloadUri(uriInfo, metadataId));
		rootElement.appendChild(downloadLinkElement);
		Element jsonDownloadLinkElement = XMLDocument.createElement("link");
		jsonDownloadLinkElement.setAttribute("type", "application/javascript");
		jsonDownloadLinkElement.setAttribute("rel", "describedby");
		jsonDownloadLinkElement.setAttribute("href",
				getMetadataJsonpDownloadUri(uriInfo, metadataId));
		rootElement.appendChild(jsonDownloadLinkElement);

		Element snippetElement = XMLDocument
				.createElement("apiscol:code-snippet");
		snippetElement.setAttribute("href",
				getMetadataSnippetUri(uriInfo, metadataId));
		rootElement.appendChild(snippetElement);
		insertionElement.appendChild(rootElement);

		return utcTime;
	}

	private void addXmlSubTreeForStaticFacets(Document response,
			Element facetsElement,
			HashMap<String, HashMap<String, String>> facetsGroups,
			HashMap<String, String> rangefacetGaps) {
		Iterator<String> it = facetsGroups.keySet().iterator();
		while (it.hasNext()) {
			String facetGroupName = (String) it.next();
			Element facetGroupElement = response
					.createElement("apiscol:static-facets");
			facetGroupElement.setAttribute("name", facetGroupName);
			if (rangefacetGaps.keySet().contains(facetGroupName))
				facetGroupElement.setAttribute("gap",
						rangefacetGaps.get(facetGroupName));
			facetsElement.appendChild(facetGroupElement);
			HashMap<String, String> facetGroup = facetsGroups
					.get(facetGroupName);
			Iterator<String> it2 = facetGroup.keySet().iterator();
			while (it2.hasNext()) {
				String facet = (String) it2.next();

				String value = facetGroup.get(facet);
				Element facetElement = response.createElement("apiscol:facet");
				if (facetGroupName.equals("relation")) {
					ArrayList<String> segments = SolrRecordsSyntaxAnalyser
							.dynamicFacetEntrySegments(facet);
					// TODO si ça ne marche pas
					facetElement.setAttribute("type", segments.get(0));
					facetElement.setAttribute("taxon", segments.get(1));
					facetElement.setTextContent(segments.get(2));
				} else {

					facetElement.setTextContent(facet);

				}
				facetElement.setAttribute("count", value);
				facetGroupElement.appendChild(facetElement);
			}

		}

	}

	private void addXmlSubTreeForDynamicFacets(
			Document response,
			Element facetsElement,
			HashMap<String, HashMap<String, HashMap<String, ArrayList<String>>>> dynamicFacetsGroups) {
		Iterator<String> it = dynamicFacetsGroups.keySet().iterator();
		while (it.hasNext()) {
			String facetGroupName = (String) it.next();
			Element facetGroupElement = response
					.createElement("apiscol:dynamic-facets");
			facetGroupElement.setAttribute("name", facetGroupName);
			facetsElement.appendChild(facetGroupElement);
			HashMap<String, HashMap<String, ArrayList<String>>> facetGroup = dynamicFacetsGroups
					.get(facetGroupName);
			Iterator<String> it2 = facetGroup.keySet().iterator();
			while (it2.hasNext()) {
				String taxonIdentifier = (String) it2.next();
				HashMap<String, ArrayList<String>> entry = facetGroup
						.get(taxonIdentifier);
				Iterator<String> it3 = entry.keySet().iterator();
				Element taxonElement = response.createElement("apiscol:taxon");
				taxonElement.setAttribute("identifier", taxonIdentifier);
				ArrayList<Element> entryList = new ArrayList<Element>();
				while (it3.hasNext()) {

					String entryIdentifier = it3.next();
					Element entryElement = response
							.createElement("apiscol:entry");
					entryElement.setAttribute("identifier", entryIdentifier);
					entryElement.setAttribute("count",
							entry.get(entryIdentifier).get(1));
					entryElement.setAttribute("label",
							entry.get(entryIdentifier).get(0));
					entryList.add(entryElement);
				}
				Collections.sort(entryList, new EntryComparator());
				while (entryList.size() > 0) {
					Element element = entryList.remove(0);
					insertIntoTree(taxonElement, element);
				}
				facetGroupElement.appendChild(taxonElement);

			}
			// clears identifiers concatenation
			// would be graceful but query filters very difficult to implement
			// in solr config
			// simplifyIdentifiers(facetGroupElement);
			facetsElement.appendChild(facetGroupElement);
		}

	}

	// private void simplifyIdentifiers(Element facetGroupElement) {
	// NodeList entries = facetGroupElement
	// .getElementsByTagName("apiscol:entry");
	// System.out.println(facetGroupElement.getNodeName());
	// for (int i = 0; i < entries.getLength(); i++) {
	// Element entry = (Element) entries.item(i);
	// String identifier = entry.getAttribute("identifier");
	// if (!identifier.contains(SEARCH_ENGINE_CONCATENED_FIELDS_SEPARATOR))
	// continue;
	// String newId = identifier.substring(identifier
	// .lastIndexOf(SEARCH_ENGINE_CONCATENED_FIELDS_SEPARATOR
	// )+ SEARCH_ENGINE_CONCATENED_FIELDS_SEPARATOR
	// .length());
	// entry.setAttribute("identifier", newId);
	// }
	//
	// }

	private void insertIntoTree(Element tree, Element newElement) {
		String elementId = newElement.getAttribute("identifier");
		NodeList childs = tree.getChildNodes();
		boolean insertionInChild = false;
		if (!StringUtils.isEmpty(elementId)) {
			for (int i = 0; i < childs.getLength(); i++) {
				Node item = childs.item(i);
				if (item.getNodeType() == Node.ELEMENT_NODE) {
					String childId = ((Element) item)
							.getAttribute("identifier");
					if (elementId.startsWith(new StringBuilder(childId).append(
							SEARCH_ENGINE_CONCATENED_FIELDS_SEPARATOR)
							.toString())) {
						insertIntoTree((Element) item, newElement);
						insertionInChild = true;
					}

				}
			}
		}
		if (!insertionInChild)
			tree.appendChild(newElement);
	}

	class EntryComparator implements Comparator<Element> {
		public int compare(Element e1, Element e2) {
			return e1.getAttribute("identifier").compareTo(
					e2.getAttribute("identifier"));
		}
	}

	private static Document createXMLDocument() {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory
				.newInstance();
		docFactory.setNamespaceAware(true);
		DocumentBuilder docBuilder = null;
		try {
			docBuilder = docFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Document doc = docBuilder.newDocument();
		return doc;
	}

	private void addNameSpaces(Document xmlTree) {
		xmlTree.getDocumentElement().setAttributeNS(
				"http://www.w3.org/2000/xmlns/", "xmlns",
				UsedNamespaces.ATOM.getUri());
		xmlTree.getDocumentElement().setAttributeNS(
				"http://www.w3.org/2000/xmlns/", "xmlns:apiscol",
				UsedNamespaces.APISCOL.getUri());
	}

	@Override
	public Document selectMetadataFollowingCriterium(UriInfo uriInfo,
			final String apiscolInstanceName,
			final String apiscolInstanceLabel,
			ISearchEngineResultHandler handler, int start, int rows,
			boolean includeDescription,
			IResourceDataHandler resourceDataHandler, String editUri,
			String version) throws NumberFormatException, DBAccessException {

		Document response = createXMLDocument();
		Element feedElement = response.createElementNS(
				UsedNamespaces.ATOM.getUri(), "feed");

		addFeedInfos(response, feedElement, apiscolInstanceName,
				apiscolInstanceLabel, uriInfo, version);
		Element updatedElement = response.createElement("updated");
		feedElement.appendChild(updatedElement);

		Element facetsElement = response.createElement("apiscol:facets");
		Element hitsElement = response.createElement("apiscol:hits");
		Element spellcheckElement = response
				.createElement("apiscol:spellcheck");

		Set<String> resultsIds = handler.getResultsIds();
		System.out.println(resultsIds.size());
		Iterator<String> it = resultsIds.iterator();
		Element lengthElement = response.createElement("apiscol:length");
		lengthElement.setTextContent(String.valueOf(handler
				.getTotalResultsFound()));
		feedElement.appendChild(lengthElement);
		String resultId, score;
		List<String> snippets;
		int counter = -1;
		long maxUpdated = 0;

		while (it.hasNext()) {
			String url = it.next();
			resultId = MetadataKeySyntax.extractMetadataIdFromUrl(url);
			counter++;
			if (counter >= rows)
				break;

			score = handler.getResultScoresById().get(url);
			snippets = handler.getResultSnippetsById().get(url);

			try {
				maxUpdated = Math.max(
						addXMLSubTreeForMetadata(response, feedElement,
								uriInfo, apiscolInstanceName, resultId,
								includeDescription, Float.parseFloat(score),
								resourceDataHandler, editUri), maxUpdated);
				addXMLSubTreeForResult(response, hitsElement, resultId, score,
						snippets, apiscolInstanceName);
			} catch (MetadataNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		updatedElement.setTextContent(TimeUtils.toRFC3339(maxUpdated));
		HashMap<String, HashMap<String, String>> staticFacetsGroups = handler
				.getStaticFacetGroups();
		HashMap<String, String> rangefacetGaps = handler.getRangefacetsGaps();
		HashMap<String, HashMap<String, HashMap<String, ArrayList<String>>>> dynamicFacetsGroups = handler
				.getDynamicFacetGroups();
		addXmlSubTreeForStaticFacets(response, facetsElement,
				staticFacetsGroups, rangefacetGaps);
		addXmlSubTreeForDynamicFacets(response, facetsElement,
				dynamicFacetsGroups);
		List<String> suggestionsforQuery = handler.getQuerySuggestions();
		Map<String, List<String>> suggestionsforTerms = handler
				.getWordSuggestionsByQueryTerms();
		addXMLSubTreeForSpellcheck(response, spellcheckElement,
				suggestionsforTerms, suggestionsforQuery);
		response.appendChild(feedElement);
		feedElement.appendChild(facetsElement);
		feedElement.appendChild(hitsElement);
		feedElement.appendChild(spellcheckElement);
		addNameSpaces(response);
		return response;
	}

	private void addFeedInfos(Document response, Element feedElement,
			String apiscolInstanceName, String apiscolInstanceLabel,
			UriInfo uriInfo, String version) {
		Element linkElement = response.createElementNS(
				UsedNamespaces.ATOM.getUri(), "link");
		linkElement.setAttribute("rel", "self");
		linkElement.setAttribute("href", uriInfo.getRequestUri().toString());
		feedElement.appendChild(linkElement);
		Element logoElement = response.createElementNS(
				UsedNamespaces.ATOM.getUri(), "logo");
		logoElement.setTextContent("http://apiscol.crdp-versailles.fr/cdn/"
				+ version + "/img/logo-api.png");
		feedElement.appendChild(logoElement);
		Element iconElement = response.createElementNS(
				UsedNamespaces.ATOM.getUri(), "icon");
		iconElement.setTextContent("http://apiscol.crdp-versailles.fr/cdn/"
				+ version + "/img/logo-api.png");

		feedElement.appendChild(iconElement);
		Element idElement = response.createElementNS(
				UsedNamespaces.ATOM.getUri(), "id");
		idElement.setTextContent(uriInfo.getBaseUri().toString());
		feedElement.appendChild(idElement);
		Element titleElement = response.createElementNS(
				UsedNamespaces.ATOM.getUri(), "title");
		titleElement.setTextContent(apiscolInstanceLabel);
		feedElement.appendChild(titleElement);
		Element generatorElement = response.createElementNS(
				UsedNamespaces.ATOM.getUri(), "generator");
		generatorElement
				.setTextContent("ApiScol, Dépôt de ressources pédagogiques - CRDP de l'Académie de Versailles");
		feedElement.appendChild(generatorElement);
	}

	private void addXMLSubTreeForResult(Document XMLDocument,
			Node insertionElement, String metadataId, String score,
			List<String> snippets, String apiscolInstanceName) {
		Element rootElement = XMLDocument.createElement("apiscol:hit");
		insertionElement.appendChild(rootElement);
		rootElement.setAttribute("metadataId",
				getMetadataUrn(metadataId, apiscolInstanceName));
		if (snippets == null) {
			// TODO mieux gérér ça
			System.out
					.println("attention snippets est null pour " + metadataId);
			return;
		}
		Iterator<String> it = snippets.iterator();
		Element matchesElement = XMLDocument.createElement("apiscol:matches");
		rootElement.appendChild(matchesElement);
		while (it.hasNext()) {
			Element matchElement = XMLDocument.createElement("apiscol:match");
			matchesElement.appendChild(matchElement);
			matchElement.setTextContent(it.next());
		}

	}

	private void addXMLSubTreeForSpellcheck(Document XMLDocument,
			Element insertionElement,
			Map<String, List<String>> suggestionsforTerms,
			List<String> suggestionsforQuery) {
		Iterator<String> it = suggestionsforTerms.keySet().iterator();
		String term;
		while (it.hasNext()) {
			term = it.next();
			Element queryTermElement = XMLDocument
					.createElement("apiscol:query_term");
			insertionElement.appendChild(queryTermElement);
			queryTermElement.setAttribute("requested", term);
			Iterator<String> it2 = suggestionsforTerms.get(term).iterator();
			while (it2.hasNext()) {
				Element wordElement = XMLDocument.createElement("apiscol:word");
				queryTermElement.appendChild(wordElement);
				wordElement.setTextContent(it2.next());

			}
		}
		Element queriesElement = XMLDocument.createElement("apiscol:queries");
		insertionElement.appendChild(queriesElement);
		Iterator<String> it3 = suggestionsforQuery.iterator();
		while (it3.hasNext()) {
			Element queryElement = XMLDocument.createElement("apiscol:query");
			queriesElement.appendChild(queryElement);
			queryElement.setTextContent(it3.next());

		}

	}

	@Override
	public Document getMetadataSuccessfulDestructionReport(UriInfo uriInfo,
			String apiscolInstanceName, String metadataId, String warnings) {
		Document report = createXMLDocument();
		Element rootElement = report.createElement("status");
		Element stateElement = report.createElement("state");
		Element idElement = report.createElement("id");
		idElement
				.setTextContent(getMetadataUrn(metadataId, apiscolInstanceName));
		stateElement.setTextContent("done");
		Element linkElement = report.createElementNS(
				UsedNamespaces.ATOM.getUri(), "link");
		linkElement.setAttribute("href",
				getMetadataHTMLUri(uriInfo, metadataId));
		linkElement.setAttribute("type", "text/html");
		linkElement.setAttribute("rel", "self");
		Element messageElement = report.createElement("message");
		messageElement.setTextContent("Resource deleted " + warnings);
		rootElement.appendChild(stateElement);
		rootElement.appendChild(linkElement);
		rootElement.appendChild(idElement);
		rootElement.appendChild(messageElement);
		report.appendChild(rootElement);
		XMLUtils.addNameSpaces(report, UsedNamespaces.APISCOL);
		return report;
	}

	@Override
	public Document getSuccessfullOptimizationReport(String requestedFormat,
			UriInfo uriInfo) {
		Document report = createXMLDocument();
		Element rootElement = report.createElement("status");
		Element stateElement = report.createElement("state");
		stateElement.setTextContent("done");
		Element linkElement = report.createElementNS(
				UsedNamespaces.ATOM.getUri(), "link");
		linkElement.setAttribute("href",
				uriInfo.getBaseUri() + uriInfo.getPath());
		Element messageElement = report.createElement("message");
		messageElement.setTextContent("Search engine index has been optimized");
		rootElement.appendChild(stateElement);
		rootElement.appendChild(linkElement);
		rootElement.appendChild(messageElement);
		report.appendChild(rootElement);
		XMLUtils.addNameSpaces(report, UsedNamespaces.APISCOL);
		return report;
	}

	@Override
	public Document getSuccessfulGlobalDeletionReport() {
		Document report = createXMLDocument();
		Element rootElement = report.createElement("status");
		Element stateElement = report.createElement("state");
		stateElement.setTextContent("done");
		Element messageElement = report.createElement("message");
		messageElement
				.setTextContent("All resource have been deleted in metadata repository.");
		rootElement.appendChild(stateElement);
		rootElement.appendChild(messageElement);
		report.appendChild(rootElement);
		XMLUtils.addNameSpaces(report, UsedNamespaces.APISCOL);
		return report;
	}

	@Override
	public Document getSuccessfulRecoveryReport() {
		Document report = createXMLDocument();
		Element rootElement = report.createElement("status");
		Element stateElement = report.createElement("state");
		stateElement.setTextContent("done");
		Element messageElement = report.createElement("message");
		messageElement.setTextContent("Search engine index has been restored.");
		rootElement.appendChild(stateElement);
		rootElement.appendChild(messageElement);
		report.appendChild(rootElement);
		XMLUtils.addNameSpaces(report, UsedNamespaces.APISCOL);
		return report;
	}

}
