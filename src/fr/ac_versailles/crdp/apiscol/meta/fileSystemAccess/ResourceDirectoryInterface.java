package fr.ac_versailles.crdp.apiscol.meta.fileSystemAccess;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.UriInfo;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import net.sf.json.JSON;
import net.sf.json.xml.XMLSerializer;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.xml.sax.SAXException;

import fr.ac_versailles.crdp.apiscol.UsedNamespaces;
import fr.ac_versailles.crdp.apiscol.meta.references.RelationKinds;
import fr.ac_versailles.crdp.apiscol.meta.references.Source;
import fr.ac_versailles.crdp.apiscol.utils.FileUtils;
import fr.ac_versailles.crdp.apiscol.utils.LogUtility;

public class ResourceDirectoryInterface {

	private static Namespace lomNs = Namespace.getNamespace("",
			UsedNamespaces.LOM.getUri());

	private static Logger logger = null;

	private static String fileRepoPath;

	private static String defaultLanguage;

	private static Validator validator;

	private static String temporaryFilesPrefix;

	private static String tabulationsPattern = "\\t+";

	public static void initialize(String fileRepoPath, String defaultLanguage,
			String xsdPath, String temporaryFilesPrefix) {
		ResourceDirectoryInterface.fileRepoPath = fileRepoPath;
		ResourceDirectoryInterface.defaultLanguage = defaultLanguage;
		ResourceDirectoryInterface.temporaryFilesPrefix = temporaryFilesPrefix;
		initializeLogger();
		createValidator(xsdPath);
	}

	private static void createValidator(String xsdPath) {
		SchemaFactory factory = SchemaFactory
				.newInstance("http://www.w3.org/2001/XMLSchema");
		File schemaLocation = new File(xsdPath);
		Schema schema = null;
		try {
			schema = factory.newSchema(schemaLocation);
		} catch (SAXException e1) {
			logger.error("The scolomfr xsd files seems to be corrupted");
			e1.printStackTrace();
		}

		validator = schema.newValidator();
	}

	public static boolean isInitialized() {
		return fileRepoPath != null && !fileRepoPath.isEmpty();
	}

	private static void initializeLogger() {
		if (logger == null)
			logger = LogUtility.createLogger(ResourceDirectoryInterface.class
					.getCanonicalName());

	}

	public static void checkMetadataExistence(String metadataId)
			throws MetadataNotFoundException {
		getMetadataFile(metadataId);
	}

	private static File getMetadataFile(String metadataId, boolean temporary)
			throws MetadataNotFoundException {
		File file = new File(getFilePath(metadataId, temporary));
		if (!file.exists() || !file.isFile()) {
			logger.warn(String.format(
					"File not found for metadataId %s with path %s",
					metadataId, file.getAbsolutePath()));
			throw new MetadataNotFoundException(metadataId);
		}
		return file;
	}

	public static File getMetadataFile(String metadataId)
			throws MetadataNotFoundException {
		return getMetadataFile(metadataId, false);
	}

	public static String getTimeStamp(String metadataId)
			throws MetadataNotFoundException {
		return String.valueOf(getMetadataFile(metadataId).lastModified());
	}

	public static ArrayList<String> getMetadataList() {
		ArrayList<String> list = new ArrayList<String>();
		File dir = new File(fileRepoPath);
		for (File child1 : dir.listFiles()) {
			// TODO tester si xml et schema de nommage
			// TODO ignorer les @ignore
			if (".".equals(child1.getName()) || "..".equals(child1.getName())
					|| child1.isFile()) {
				continue;
			}
			for (File child2 : child1.listFiles()) {
				// TODO tester si xml et schema de nommage
				// TODO ignorer les @ignore
				if (".".equals(child2.getName())
						|| "..".equals(child2.getName()) || child2.isFile()) {
					continue;
				}
				// TODO extraire l'id avec une expressionr égulière
				for (File child3 : child2.listFiles()) {
					// TODO tester si xml et schema de nommage
					// TODO ignorer les @ignore
					if (".".equals(child3.getName())
							|| "..".equals(child3.getName()) || child3.isFile()) {
						continue;
					}
					// TODO extraire l'id avec une expressionr égulière
					for (File child4 : child3.listFiles()) {
						// TODO tester si xml et schema de nommage
						// TODO ignorer les @ignore
						if (".".equals(child4.getName())
								|| "..".equals(child4.getName())
								|| !child4.isFile()
								|| child4.getName().startsWith(
										temporaryFilesPrefix)
								|| child4.getName().endsWith(".js")) {
							continue;
						}
						// TODO extraire l'id avec une expressionr égulière
						list.add(new StringBuilder().append(child1.getName())
								.append(child2.getName())
								.append(child3.getName())
								.append(child4.getName().replace(".xml", ""))
								.toString());
					}
				}
			}
		}

		return list;
	}

	private static void writeStringToFile(String string, File file) {
		BufferedWriter out;
		try {
			out = new BufferedWriter(new FileWriter(file));
			out.write(string);
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void registerMetadataFile(String metadataId,
			InputStream uploadedInputStream, String url,
			String apiscolInstanceName) throws FileSystemAccessException,
			InvalidProvidedMetadataFileException {
		File newXMLFile = null;
		newXMLFile = new File(getFilePath(metadataId, true));
		try {
			FileUtils.writeStreamToFile(uploadedInputStream, newXMLFile);
		} catch (IOException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}
		validateFile(newXMLFile);
		updateMetadata(newXMLFile, url, apiscolInstanceName);
		serializeIntoJSon(newXMLFile, metadataId);
	}

	public static void renewJsonpFile(String metadataId) {
		File XMLFile = new File(getFilePath(metadataId, false));
		File actualJsonpFile = new File(getFilePath(metadataId, false, "js"));
		if (actualJsonpFile.exists())
			actualJsonpFile.delete();
		serializeIntoJSon(XMLFile, metadataId);
		commitTemporaryJsonMetadataFile(metadataId);
	}

	private static void convertToJSon(File xmlFile, String metadataId) {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(xmlFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String xml = null;
		try {
			xml = IOUtils.toString(fis);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		XMLSerializer xmlSerializer = new XMLSerializer();
		JSON json = xmlSerializer.read(xml);
		File file = new File(getFilePath(metadataId, true, "js"));
		writeStringToFile(
				new StringBuilder().append("notice(").append(json.toString())
						.append(");").toString(), file);

	}

	private static void serializeIntoJSon(File xmlFile, String metadataId) {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(xmlFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String xml = null;
		try {
			xml = IOUtils.toString(fis);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		XMLSerializer xmlSerializer = new XMLSerializer();
		JSON json = xmlSerializer.read(xml);
		File file = new File(getFilePath(metadataId, true, "js"));
		writeStringToFile(
				new StringBuilder()
						.append("notice(\"")
						.append(xml.replaceAll("[\n\r]", " ").replace("\"",
								"\\\"")).append("\");").toString(), file);

	}

	public static String getFilePath(String metadataId, boolean temporary,
			String extension) {
		StringBuilder builder = new StringBuilder();
		if (temporary) {

			builder = builder.append(fileRepoPath).append("/")
					.append(temporaryFilesPrefix).append(metadataId)
					.append(".").append(extension);
		} else {
			builder = builder
					.append(FileUtils.getFilePathHierarchy(fileRepoPath,
							metadataId)).append(".").append(extension);
		}
		return builder.toString();
	}

	public static String getFilePath(String metadataId, boolean temporary) {
		return getFilePath(metadataId, temporary, "xml");
	}

	public static String getFilePath(String metadataId) {
		return getFilePath(metadataId, false);
	}

	public static boolean commitTemporaryMetadataFile(String metadataId) {

		String tempFilePath = getFilePath(metadataId, true);
		String definitiveFilePath = getFilePath(metadataId, false);
		logger.error(String.format("Trying to copy file from %s to %s",
				tempFilePath, definitiveFilePath));
		File temporary = new File(tempFilePath);
		File definitive = new File(definitiveFilePath);
		definitive.getParentFile().mkdirs();
		if (!temporary.exists()) {
			logger.error(String
					.format("Trying to commit temporary file %s for metadata %s but the file does not exist",
							temporary.getAbsolutePath(), metadataId));
			return false;
		}
		try {
			//FIXIT lors de la migration sous windows
			//impossible de faire tourner rename
			FileUtils.copyFile(temporary, definitive);
			return true;
		} catch (Exception e) {
			logger.error(e.getMessage());
			return false;
		}

	}

	public static boolean commitTemporaryJsonMetadataFile(String metadataId) {
		File temporary = new File(getFilePath(metadataId, true, "js"));
		File definitive = new File(getFilePath(metadataId, false, "js"));
		definitive.getParentFile().mkdirs();
		if (!temporary.exists()) {
			logger.error(String
					.format("Trying to commit temporary json file %s for metadata %s but the file does not exist",
							temporary.getAbsolutePath(), metadataId));
			return false;
		}
		return temporary.renameTo(definitive);
	}

	private static void validateFile(File scolomFrXml)
			throws InvalidProvidedMetadataFileException,
			FileSystemAccessException {
		StreamSource source = new StreamSource(scolomFrXml);
		try {
			validator.validate(source);
			logger.info(scolomFrXml + " is valid.");
		} catch (SAXException ex) {
			throw new InvalidProvidedMetadataFileException(String.format(
					"The file %s is not valid because %s",
					scolomFrXml.getName(), ex.getMessage()));
		} catch (IOException e) {
			throw new FileSystemAccessException(
					String.format(
							"Impossible to reach the xml file %s when trying to validate",
							scolomFrXml.getName()));
		}

	}

	private static void updateMetadata(File xmlFile, String url,
			String apiscolInstanceName) throws FileSystemAccessException,
			InvalidProvidedMetadataFileException {

		try {

			SAXBuilder builder = new SAXBuilder();

			Document doc = (Document) builder.build(xmlFile);
			Element rootNode = doc.getRootElement();
			Element metaMeta = getOrCreateChild(rootNode, "metaMetadata", lomNs);
			Element identifier = getOrCreateChild(metaMeta, "identifier", lomNs);
			Element catalog = getOrCreateChild(identifier, "catalog", lomNs);
			Element entry = getOrCreateChild(identifier, "entry", lomNs);
			catalog.setText("Apiscol :" + apiscolInstanceName);
			entry.setText(url);
			Element generalElement = getOrCreateChild(rootNode, "general",
					lomNs);
			Element titleContainerElement = getOrCreateChild(generalElement,
					"title", lomNs);
			Element titleElement = getOrCreateChild(titleContainerElement,
					"string", lomNs);
			setLanguageToDefaultIfNotSpecified(titleElement);
			cleanString(titleElement);
			Element descriptionContainerElement = getOrCreateChild(
					generalElement, "description", lomNs);
			Element descriptionElement = getOrCreateChild(
					descriptionContainerElement, "string", lomNs);
			Element coverageContainerElement = getOrCreateChild(generalElement,
					"coverage", lomNs);
			Element coverageElement = getOrCreateChild(
					coverageContainerElement, "string", lomNs);
			Element technicalElement = getOrCreateChild(rootNode, "technical",
					lomNs);
			Element locationElement = getOrCreateChild(technicalElement,
					"location", lomNs);
			Element sizeElem = getOrCreateChild(technicalElement, "size", lomNs);
			locationElement.setText("");
			sizeElem.setText("0");
			setLanguageToDefaultIfNotSpecified(descriptionElement);
			cleanString(descriptionElement);
			cleanString(coverageElement);
			// TODO clean other free text elements
			XMLOutputter xmlOutput = new XMLOutputter();
			xmlOutput.setFormat(Format.getPrettyFormat());
			xmlOutput.output(doc, new FileWriter(xmlFile));
		} catch (IOException io) {
			throw new FileSystemAccessException(
					String.format(
							"Impossible to reach the xml file when trying to write url : %s, transmission problem",
							url));
		} catch (JDOMException e) {
			throw new InvalidProvidedMetadataFileException(
					String.format(
							"Impossible to read the xml file when trying to write url : %s, xml syntax problem",
							url));
		}

	}

	private static void cleanString(Element element) {
		element.setText(element.getText().replaceAll(tabulationsPattern, " "));

	}

	public static HashMap<String, String> getMetadataProperties(
			String metadataId) throws MetadataNotFoundException,
			InvalidProvidedMetadataFileException, FileSystemAccessException {

		HashMap<String, String> mdProperties = new HashMap<String, String>();
		SAXBuilder builder = new SAXBuilder();

		File xmlFile = getMetadataFile(metadataId);
		Document doc = null;
		try {
			doc = (Document) builder.build(xmlFile);
		} catch (JDOMException e) {
			throw new InvalidProvidedMetadataFileException(
					String.format(
							"Impossible to read the xml file when trying to read properties for metadata id : %s, xml syntax problem",
							metadataId));
		} catch (IOException e) {
			throw new FileSystemAccessException(
					String.format(
							"Impossible to reach the xml file when trying to read properties for metadata id : %s, transmission problem",
							metadataId));
		}
		Element rootNode = doc.getRootElement();
		String title = "";
		String description = "";
		try {
			title = getStringInDefaultLanguageIfPossible(rootNode.getChild(
					"general", lomNs).getChild("title", lomNs));
		} catch (NullPointerException e) {
			logger.warn("There shoud be a title element id in the metadata file :"
					+ xmlFile.getAbsolutePath());
		}
		try {
			description = getStringInDefaultLanguageIfPossible(rootNode
					.getChild("general", lomNs).getChild("description"));
		} catch (NullPointerException e) {
			logger.warn("There shoud be a description element id in the metadata file :"
					+ xmlFile.getAbsolutePath());
		}

		mdProperties.put("title", title);
		mdProperties.put("description", description);
		return mdProperties;
	}

	private static String getStringInDefaultLanguageIfPossible(Element parent) {
		Element bestStringElement = null;
		List<Element> stringElements = parent.getChildren("string", lomNs);
		if (stringElements.size() == 0) {
			throw new NullPointerException();
		} else {
			bestStringElement = stringElements.get(0);
			Iterator<Element> it = stringElements.iterator();
			while (it.hasNext()) {
				Element element = it.next();
				Attribute attribute = element.getAttribute("language");
				if (attribute != null && attribute.getName() == defaultLanguage)
					bestStringElement = element;
			}

		}

		return bestStringElement.getText();
	}

	private static void setLanguageToDefaultIfNotSpecified(Element element) {
		Attribute languageAttr = element.getAttribute("language");
		if (languageAttr == null) {
			languageAttr = new Attribute("language", defaultLanguage);
			element.setAttribute(languageAttr);
		}
	}

	private static Element getOrCreateChild(Element parent, String name,
			Namespace ns) {
		List<Element> l = parent.getChildren(name, ns);
		Element e;
		if (l.size() == 0) {
			logger.info(String
					.format("The %s element was not found in this metadata file, it had to be added to its parent %s",
							name, parent.getName()));
			e = new Element(name, ns);
			parent.addContent(e);
		} else
			e = l.get(0);
		return e;
	}

	public static boolean deleteMetadataFile(String metadataId,
			boolean temporary) throws MetadataNotFoundException {
		File metadataFile = new File(getFilePath(metadataId, temporary));
		File jsonpMetadataFile = new File(getFilePath(metadataId, temporary,
				"js"));
		File parent = metadataFile.getParentFile();
		File grandParent = parent.getParentFile();
		File grandGrandParent = grandParent.getParentFile();
		boolean success = true;
		if (metadataFile != null && metadataFile.exists()) {
			success &= metadataFile.delete();
			if (jsonpMetadataFile != null && jsonpMetadataFile.exists())
				success &= jsonpMetadataFile.delete();
			if (success && parent.list().length == 0) {
				success &= FileUtils.deleteDir(parent);
				if (success && grandParent.list().length == 0) {
					success &= FileUtils.deleteDir(grandParent);
					if (success && grandGrandParent.list().length == 0) {
						success &= FileUtils.deleteDir(grandGrandParent);
					}
				}
			}
			return success;
		} else
			logger.warn(String
					.format("The file %s to be deleted is null or does not exist for metadata %s",
							metadataFile.getAbsoluteFile(), metadataId));
		return false;

	}

	public static void deleteAllFiles() {
		File resourceDir = new File(fileRepoPath);
		for (File dir : resourceDir.listFiles()) {
			if (!dir.getName().equals("..") && !dir.getName().equals("."))
				FileUtils.deleteDir(dir);
		}
	}

	public static void updateTechnicalInformation(String metadataId,
			String size, String language, String technicalLocation,
			String apiscolInstance, String location, String format,
			String thumb, String preview) throws FileSystemAccessException,
			MetadataNotFoundException {
		try {
			SAXBuilder builder = new SAXBuilder();
			File xmlFile = getMetadataFile(metadataId);
			Document doc = (Document) builder.build(xmlFile);
			Element rootNode = doc.getRootElement();

			Element technical = getOrCreateChild(rootNode, "technical", lomNs);
			Element general = getOrCreateChild(rootNode, "general", lomNs);
			Element languageElem = getOrCreateChild(general, "language", lomNs);
			Element sizeElem = getOrCreateChild(technical, "size", lomNs);
			Element technicalLocationElem = getOrCreateChild(technical,
					"location", lomNs);
			Element formatElem = getOrCreateChild(technical, "format", lomNs);
			if (StringUtils.isNotEmpty(size))
				sizeElem.setText(size);
			if (StringUtils.isNotEmpty(technicalLocation))
				technicalLocationElem.setText(technicalLocation);
			if (StringUtils.isNotEmpty(format))
				formatElem.setText(format);
			if (StringUtils.isNotEmpty(language))
				languageElem.setText(language);
			setLomIdentifier(rootNode, apiscolInstance, location);

			if (StringUtils.isNotEmpty(thumb)) {
				Iterator<Element> relationsIt = rootNode.getChildren(
						"relation", lomNs).iterator();
				Element thumbRelation = null;
				while (relationsIt.hasNext()) {
					Element relation = relationsIt.next();
					Element kind = relation.getChild("kind", lomNs);

					if (kind == null)
						continue;
					Element value = kind.getChild("value", lomNs);
					if (value == null)
						continue;
					if (value.getText().contains(
							RelationKinds.VIGNETTE.toString())) {
						thumbRelation = relation;
						break;
					}

				}
				if (thumbRelation == null) {
					thumbRelation = createNewRelation(rootNode);
				}
				URI thumbUri;
				try {
					thumbUri = new URI(thumb);
				} catch (URISyntaxException e) {
					logger.error("This is not a valid syntax for thumbs : "
							+ thumb);
					e.printStackTrace();
					return;
				}
				modifyRelation(thumbRelation, Source.SCOLOMFRv10,
						RelationKinds.VIGNETTE, thumbUri);
			}
			if (StringUtils.isNotEmpty(preview)) {
				Iterator<Element> relationsIt = rootNode.getChildren(
						"relation", lomNs).iterator();

				Element previewRelation = null;
				while (relationsIt.hasNext()) {
					Element relation = relationsIt.next();
					Element kind = relation.getChild("kind", lomNs);
					if (kind == null)
						continue;
					Element value = kind.getChild("value", lomNs);
					if (value == null)
						continue;
					if (value.getText().contains(
							RelationKinds.APERCU.toString()))
						previewRelation = relation;

				}
				if (previewRelation == null) {
					previewRelation = createNewRelation(rootNode);
				}
				URI previewUri;
				try {
					previewUri = new URI(preview);
				} catch (URISyntaxException e) {
					logger.error("This is not a valid syntax for previews : "
							+ preview);
					e.printStackTrace();
					return;
				}
				modifyRelation(previewRelation, Source.SCOLOMFRv10,
						RelationKinds.APERCU, previewUri);

			}

			XMLOutputter xmlOutput = new XMLOutputter();
			xmlOutput.setFormat(Format.getPrettyFormat());
			xmlOutput.output(doc, new FileWriter(xmlFile));
		} catch (IOException io) {
			throw new FileSystemAccessException(
					String.format(
							"Impossible to reach the xml file when trying to write technical informations for metadata %s, transmission problem",
							metadataId));
		} catch (JDOMException e) {
			logger.error(String
					.format("Impossible to read the xml file when trying for metadata %s,xml syntax problem",
							metadataId));
		}

	}

	private static void setLomIdentifier(Element rootNode, String catalog,
			String identifier) {
		Element general = getOrCreateChild(rootNode, "general", lomNs);
		Element identifierElement = getOrCreateChild(general, "identifier",
				lomNs);
		Element catalogElement = getOrCreateChild(identifierElement, "catalog",
				lomNs);
		Element entry = getOrCreateChild(identifierElement, "entry", lomNs);
		catalogElement.setText(catalog);
		if (StringUtils.isNotEmpty(identifier))
			entry.setText(identifier);

	}

	public static org.w3c.dom.Document getMetadataAsDocument(String metadataId)
			throws MetadataNotFoundException {
		File metadata = getMetadataFile(metadataId);

		return FileUtils.getXMLFromFile(metadata);
	}

	public static List<String> addPartsToPackMetadata(String packMetadataId,
			String packId, List<String> partsMetadataIdList, UriInfo uriInfo)
			throws MetadataNotFoundException {
		URI packMetadataUri = convertToUri(uriInfo, packMetadataId);
		SAXBuilder builder = new SAXBuilder();
		File partFile = null;
		File packFile = getMetadataFile(packMetadataId);

		List<String> otherAffectedMetadataIds = removePackRelations(
				packMetadataId, uriInfo);
		Document packXMLDoc = null;
		try {
			packXMLDoc = (Document) builder.build(packFile);

			for (Iterator<String> iterator = partsMetadataIdList.iterator(); iterator
					.hasNext();) {
				String partMetadataId = iterator.next();
				URI partMetadataUri = convertToUri(uriInfo, partMetadataId);
				partFile = getMetadataFile(partMetadataId);
				Document partXMLDoc = (Document) builder.build(partFile);
				addRelation(RelationKinds.CONTIENT, Source.LOMV10, packXMLDoc,
						partMetadataUri);
				addRelation(RelationKinds.FAIT_PARTIE_DE, Source.LOMV10,
						partXMLDoc, packMetadataUri);
				XMLOutputter xmlOutput = new XMLOutputter();
				xmlOutput.setFormat(Format.getPrettyFormat());
				xmlOutput.output(partXMLDoc, new FileWriter(partFile));
			}
			setLomIdentifier(packXMLDoc.getRootElement(), "APISCOL", packId);
			XMLOutputter xmlOutput = new XMLOutputter();
			xmlOutput.setFormat(Format.getPrettyFormat());
			xmlOutput.output(packXMLDoc, new FileWriter(packFile));
		} catch (MetadataNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JDOMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return otherAffectedMetadataIds;
	}

	private static URI convertToUri(UriInfo uriInfo, String metadataId) {
		String uriString = new StringBuilder()
				.append(uriInfo.getBaseUri().toString()).append(metadataId)
				.toString();
		try {

			return new URI(uriString);
		} catch (URISyntaxException e) {
			logger.error(uriString + " is not a valid syntax for an URI");
			e.printStackTrace();
		}
		return null;
	}

	private static String removeBaseUri(String metadataId, UriInfo uriInfo) {
		return metadataId.replaceAll(uriInfo.getBaseUri().toString(), "");
	}

	private static void deleteRelation(RelationKinds kind, Document doc,
			URI packMetadataUri) {
		List<Element> relationsToBeDeleted = getRelations(kind, doc,
				packMetadataUri);
		for (Iterator<Element> iterator = relationsToBeDeleted.iterator(); iterator
				.hasNext();) {
			iterator.next().detach();
		}
	}

	private static List<Element> getRelations(RelationKinds kind, Document doc,
			URI metadataUri) {
		List<Element> relations = new ArrayList<Element>();
		Element rootNode = doc.getRootElement();
		Iterator<Element> relationsIt = rootNode.getChildren("relation", lomNs)
				.iterator();
		while (relationsIt.hasNext()) {
			Element relation = relationsIt.next();
			Element kindElem = relation.getChild("kind", lomNs);
			if (kindElem == null)
				continue;
			Element value = kindElem.getChild("value", lomNs);
			if (value == null)
				continue;
			if (value.getText().equals(kind.toString())
					&& (metadataUri == null || StringUtils.equals(
							metadataUri.toString(), value.getTextNormalize())))
				relations.add(relation);

		}

		return relations;
	}

	private static void addRelation(RelationKinds kind, Source source,
			Document doc, URI uri) {
		Element rootNode = doc.getRootElement();
		Element relation = createNewRelation(rootNode);
		modifyRelation(relation, source, kind, uri);

	}

	private static void modifyRelation(Element relation, Source source,
			RelationKinds kind, URI uri) {
		Element kindElement = getOrCreateChild(relation, "kind", lomNs);
		Element sourceElement = getOrCreateChild(kindElement, "source", lomNs);
		sourceElement.setText(source.toString());
		Element valueElement = getOrCreateChild(kindElement, "value", lomNs);
		valueElement.setText(kind.toString());
		Element resourceElement = getOrCreateChild(relation, "resource", lomNs);
		Element relIdentifierElement = getOrCreateChild(resourceElement,
				"identifier", lomNs);
		Element relCatalogElement = getOrCreateChild(relIdentifierElement,
				"catalog", lomNs);
		relCatalogElement.setText("URI");
		Element relEntryElement = getOrCreateChild(relIdentifierElement,
				"entry", lomNs);
		relEntryElement.setText(uri.toString());

	}

	private static Element createNewRelation(Element rootNode) {
		Element previewRelation = new Element("relation");
		previewRelation.setNamespace(lomNs);
		rootNode.addContent(previewRelation);
		return previewRelation;
	}

	public static void setAggregationLevel(String metadataId, int level)
			throws MetadataNotFoundException {
		File xmlFile = getMetadataFile(metadataId);
		SAXBuilder builder = new SAXBuilder();
		Document doc = null;
		try {
			doc = (Document) builder.build(xmlFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Element rootNode = doc.getRootElement();
		Element generalElement = getOrCreateChild(rootNode, "general", lomNs);
		Element aggregationLevel = getOrCreateChild(generalElement,
				"aggregationLevel", lomNs);
		Element sourceElement = getOrCreateChild(aggregationLevel, "source",
				lomNs);
		sourceElement.setText(Source.LOMV10.toString());
		Element valueElement = getOrCreateChild(aggregationLevel, "value",
				lomNs);
		valueElement.setText(String.valueOf(level));
		XMLOutputter xmlOutput = new XMLOutputter();
		xmlOutput.setFormat(Format.getPrettyFormat());
		try {
			xmlOutput.output(doc, new FileWriter(xmlFile));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static Map<String, String> extractPropertiesToSave(String metadataId) {
		logger.error("Properties restoration not implemented in meta ResourcedirectoryInterface");
		return Collections.<String, String> emptyMap();
	}

	public static void restoreProperties(String metadataId,
			Map<String, String> propertiesToSave) {
		// TODO Auto-generated method stub
		logger.error("Properties restoration not implemented in meta ResourcedirectoryInterface");
	}

	public static List<String> getPacksContainingMetadata(String metadataId) {
		logger.error("Properties restoration not implemented in meta ResourcedirectoryInterface");
		return Collections.<String> emptyList();
	}

	public static List<String> getPacksContainedInMetadata(String metadataId) {
		logger.error("Properties restoration not implemented in meta ResourcedirectoryInterface");
		return Collections.<String> emptyList();
	}

	public static List<String> removePackRelations(String packMetadataId,
			UriInfo uriInfo) throws MetadataNotFoundException {
		List<String> otherAffectedMetadataIds = new ArrayList<String>();
		URI packMetadataUri = convertToUri(uriInfo, packMetadataId);
		List<Element> actualRelations = null;
		File packFile = getMetadataFile(packMetadataId);
		File partFile = null;
		SAXBuilder builder = new SAXBuilder();
		Document packXMLDoc = null;
		try {
			packXMLDoc = (Document) builder.build(packFile);

			actualRelations = getRelations(RelationKinds.CONTIENT, packXMLDoc,
					null);
			for (Iterator<Element> iterator = actualRelations.iterator(); iterator
					.hasNext();) {
				String partMetadataUri = iterator.next().getTextNormalize();
				String partMetadataId = removeBaseUri(partMetadataUri, uriInfo);
				otherAffectedMetadataIds.add(partMetadataId);
				partFile = getMetadataFile(partMetadataId);
				Document partXMLDoc = (Document) builder.build(partFile);
				deleteRelation(RelationKinds.FAIT_PARTIE_DE, partXMLDoc,
						packMetadataUri);
				XMLOutputter xmlOutput = new XMLOutputter();
				xmlOutput.setFormat(Format.getPrettyFormat());
				xmlOutput.output(partXMLDoc, new FileWriter(partFile));

			}
			deleteRelation(RelationKinds.CONTIENT, packXMLDoc, null);
		} catch (Exception e) {
			logger.error("It seems impossible to parse file : " + packFile);
			e.printStackTrace();
		}

		return otherAffectedMetadataIds;
	}

}
