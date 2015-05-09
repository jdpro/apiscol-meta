package fr.ac_versailles.crdp.apiscol.meta;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

import fr.ac_versailles.crdp.apiscol.ApiscolApi;
import fr.ac_versailles.crdp.apiscol.ParametersKeys;
import fr.ac_versailles.crdp.apiscol.database.DBAccessException;
import fr.ac_versailles.crdp.apiscol.meta.dataBaseAccess.DBAccessFactory;
import fr.ac_versailles.crdp.apiscol.meta.dataBaseAccess.DBAccessFactory.DBTypes;
import fr.ac_versailles.crdp.apiscol.meta.dataBaseAccess.IResourceDataHandler;
import fr.ac_versailles.crdp.apiscol.meta.fileSystemAccess.FileSystemAccessException;
import fr.ac_versailles.crdp.apiscol.meta.fileSystemAccess.InvalidProvidedMetadataFileException;
import fr.ac_versailles.crdp.apiscol.meta.fileSystemAccess.MetadataNotFoundException;
import fr.ac_versailles.crdp.apiscol.meta.fileSystemAccess.ResourceDirectoryInterface;
import fr.ac_versailles.crdp.apiscol.meta.representations.EntitiesRepresentationBuilderFactory;
import fr.ac_versailles.crdp.apiscol.meta.representations.IEntitiesRepresentationBuilder;
import fr.ac_versailles.crdp.apiscol.meta.representations.XHTMLRepresentationBuilder;
import fr.ac_versailles.crdp.apiscol.meta.searchEngine.AbstractSearchEngineFactory;
import fr.ac_versailles.crdp.apiscol.meta.searchEngine.ISearchEngineFactory;
import fr.ac_versailles.crdp.apiscol.meta.searchEngine.ISearchEngineQueryHandler;
import fr.ac_versailles.crdp.apiscol.meta.searchEngine.ISearchEngineResultHandler;
import fr.ac_versailles.crdp.apiscol.meta.searchEngine.SearchEngineCommunicationException;
import fr.ac_versailles.crdp.apiscol.meta.searchEngine.SearchEngineErrorException;
import fr.ac_versailles.crdp.apiscol.transactions.KeyLock;
import fr.ac_versailles.crdp.apiscol.transactions.KeyLockManager;
import fr.ac_versailles.crdp.apiscol.utils.TimeUtils;

@Path("/")
public class MetadataApi extends ApiscolApi {

	@Context
	UriInfo uriInfo;
	@Context
	ServletContext context;

	private static boolean staticInitialization = false;
	private static String apiscolInstanceName;
	private static String apiscolInstanceLabel;
	private static ISearchEngineFactory searchEngineFactory;
	private static ISearchEngineQueryHandler searchEngineQueryHandler;
	private static String editUri;

	public MetadataApi(@Context ServletContext context) {
		super(context);
		if (!staticInitialization) {
			initializeResourceDirectoryInterface(context);
			initializeStaticParameters();
			createSearchEngineQueryHandler(context);
			staticInitialization = true;
		}
	}

	public static void initializeResourceDirectoryInterface(
			ServletContext context) {
		if (!ResourceDirectoryInterface.isInitialized())
			ResourceDirectoryInterface.initialize(
					getProperty(ParametersKeys.fileRepoPath, context),
					getProperty(ParametersKeys.systemDefaultLanguage, context),
					context.getRealPath("scolomfr-xsd-1-1bis/scolomfr.xsd"),
					getProperty(ParametersKeys.temporaryFilesPrefix, context));
	}

	/**
	 * This method allows new to create a new Entry in ApiScol Meta,
	 * representing the description of a learning object or educational
	 * sequence. The main parameter ('file') contains the scoLOMfr XML document.
	 * It should be UTF-8 encoded.
	 * 
	 * @param uploadedInputStream
	 *            Payload of the POST request as <code>'file'</code> POST
	 *            parameter.
	 * @param fileDetail
	 *            File informations of the POST request as <code>'file'</code>
	 *            POST parameter.
	 * @param editUri
	 *            Edition uri of the ressource as <code>'edit_uri'</code> POST
	 *            parameter. Only ApiScol Edit may deliver this information. It
	 *            will be returned in Metadata representations as
	 *            <code>link rel="edit"</code> ATOM tag.
	 * @param aggregationLevel
	 *            Following the LOM specification, <a href=
	 *            "http://www.lom-fr.fr/scolomfr/la-norme/manuel-technique.html?tx_scolomfr_pi1[detailElt]=48"
	 *            >general.aggregationLevel</a> is an int value between 1 and 4.
	 *            So far, the value 1 is used in ApiScol for sigle resources
	 *            from ApiScol Content and the value 2 for packages from Apiscol
	 *            pack.
	 * @return The newly generated metadata ATOM representation. <br/>
	 *         The <code>id</code> tag provides an URN that may be useful in
	 *         case of mirroring or URL change. <br/>
	 *         The <code>content</code> tag points to the resource associated to
	 *         this metadata. It may be the URI of a single file hosted in
	 *         ApiScol Content, an archive containing many files or an external
	 *         web URL for contents hosted out of ApiScol repository. <br/>
	 *         The meaning of <code>link</code> tags is as follows :
	 *         <table>
	 *         <tr>
	 *         <th>rel attr</th>
	 *         <th>type attr</th>
	 *         <th>meaning</th>
	 *         </tr>
	 *         <tr>
	 *         <td>self</td>
	 *         <td>text/html</td>
	 *         <td>Requested by a browser, with implicit http
	 *         <code>accept</code> parameter set to text/html, produces a rich
	 *         graphical representation similar to ApiScol resource portlet.</td>
	 *         </tr>
	 *         <tr>
	 *         <td>self</td>
	 *         <td>application/atom+xml</td>
	 *         <td>Produces XML Representation of the metadata object. Requested
	 *         by a browser, the 'format' query parameters allow to override
	 *         implicit http accept parameter. For other types of clients, it is
	 *         unuseful.</td>
	 *         </tr>
	 *         <tr>
	 *         <td>edit</td>
	 *         <td>application/atom+xml</td>
	 *         <td>The URL to use to edit this resource (for PUT, POST or DELETE
	 *         requests). ApiScol Meta does not accept direct PUT, POST or
	 *         DELETE requests that have not benn relayed by ApiScol Edit.</td>
	 *         </tr>
	 *         <tr>
	 *         <td>describedby</td>
	 *         <td>application/lom+xml</td>
	 *         <td>URL to use in order to download the whole scoLOMfr file as
	 *         static XML. Notice that it is not a REST API.</td>
	 * 
	 *         </tr>
	 *         <tr>
	 *         <td>describedby</td>
	 *         <td>application/javascript</td>
	 *         <td>The same as previous, but the xml is serialized as raw
	 *         string, escaped and wrapped as JsonP in a javascript function
	 *         call.</td>
	 *         </tr>
	 *         </table>
	 *         <code>
	 * &lt;entry xmlns="http://www.w3.org/2005/Atom" xmlns:apiscol="http://www.crdp.ac-versailles.fr/2012/apiscol"&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;updated&gt;2013-03-04T17:35:05.000+01:00&lt;/updated&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;id&gt;urn:apiscol:demonstrateur:meta:metadata:a23d4b96-dc83-4ad3-88ad-2dbf43068d44&lt;/id&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;title&gt;TP Pose et dépose d’un jeu de manomètres&lt;/title&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;summary&gt;Cette séance doit permettre à l'élève d'apprendre à poser et déposer un jeu de manomètre dans les règles de l’art&lt;/summary&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;link href="http://178.32.219.182/thumbs/files/0/5/0/ccde8ef9980a1eeef83214a6c1e7e.jpg" rel="icon" type="image/jpeg"/&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;content src="http://178.32.219.182/content/resources/e/9/2/c3f5b-9d97-4844-aee8-a72d7716be0c/0209_Sauter_des_lardons.flv" type="video/x-flv"/&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;author&gt;&lt;name/&gt;&lt;/author&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;link href="http://178.32.219.182/meta/a23d4b96-dc83-4ad3-88ad-2dbf43068d44" rel="self" type="text/html"/&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;link href="http://178.32.219.182/meta/a23d4b96-dc83-4ad3-88ad-2dbf43068d44?format=xml" rel="self" type="application/atom+xml"/&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;link href="https://178.32.219.182:8443/edit/meta/a23d4b96-dc83-4ad3-88ad-2dbf43068d44" rel="edit" type="application/atom+xml"/&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;link href="http://178.32.219.182/meta/lom/a/2/3/d4b96-dc83-4ad3-88ad-2dbf43068d44.xml" rel="describedby" type="application/lom+xml"/&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;link href="http://178.32.219.182/meta/lom/a/2/3/d4b96-dc83-4ad3-88ad-2dbf43068d44.js" rel="describedby" type="application/javascript"/&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;apiscol:code-snippet href="http://178.32.219.182/meta/a23d4b96-dc83-4ad3-88ad-2dbf43068d44/snippet"/&gt;
	 * &lt;/entry&gt;
	 * </code>
	 * @throws FileSystemAccessException
	 * @throws InvalidProvidedMetadataFileException
	 * @throws SearchEngineCommunicationException
	 * @throws SearchEngineErrorException
	 * @throws DBAccessException
	 * @throws MetadataNotFoundException
	 */
	@POST
	@Path("/")
	@Produces({ MediaType.APPLICATION_ATOM_XML, MediaType.APPLICATION_XML })
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response createMetadata(
			@Context HttpServletRequest request,
			@FormDataParam("file") InputStream uploadedInputStream,
			@FormDataParam("file") FormDataContentDisposition fileDetail,
			@DefaultValue("") @FormDataParam("edit_uri") String editUri,
			@DefaultValue("1") @FormDataParam("aggregation_level") int aggregationLevel)
			throws FileSystemAccessException,
			InvalidProvidedMetadataFileException,
			SearchEngineCommunicationException, SearchEngineErrorException,
			DBAccessException, MetadataNotFoundException {
		takeAndReleaseGlobalLock();
		String metadataId = UUID.randomUUID().toString();
		String requestedFormat = request.getHeader(HttpHeaders.ACCEPT);
		IEntitiesRepresentationBuilder<?> rb = EntitiesRepresentationBuilderFactory
				.getRepresentationBuilder(requestedFormat, context);
		String url = rb.getMetadataUri(uriInfo, metadataId);
		if (!StringUtils.isEmpty(editUri))
			MetadataApi.editUri = editUri;
		try {
			ResourceDirectoryInterface.registerMetadataFile(metadataId,
					uploadedInputStream, url, apiscolInstanceName);
		} catch (FileSystemAccessException e) {
			logger.error(String
					.format("Registration of file was aborted as a transfer problem occured for metadata %s",
							metadataId));
			throw e;
		} catch (InvalidProvidedMetadataFileException e) {
			logger.error(String
					.format("Registration of file was aborted as a transfer problem occured for metadata %s",
							metadataId));
			throw e;
		}
		try {
			uploadedInputStream.close();
		} catch (IOException e) {
			logger.warn(String
					.format("A probleme was encountered while closing the input stream for file %s : %s",
							fileDetail.getFileName(), e.getMessage()));
		}
		if (!ResourceDirectoryInterface.commitTemporaryMetadataFile(metadataId)) {
			StringBuilder errors = new StringBuilder();
			String error1 = String
					.format("The file for metadata %s was correctly received and parsed but it was impossible to finalize registration",
							metadataId);
			logger.error(error1);
			errors.append(error1);

			throw new FileSystemAccessException(errors.toString());
		}
		if (!ResourceDirectoryInterface
				.commitTemporaryJsonMetadataFile(metadataId)) {
			logger.error(String
					.format("The file for metadata %s was correctly received and parsed but it was impossible to finalize creation of jsonp file",
							metadataId));
		}

		String filePath = "";
		filePath = ResourceDirectoryInterface.getFilePath(metadataId);

		try {
			searchEngineQueryHandler.processAddQuery(filePath);
			searchEngineQueryHandler.processCommitQuery();
		} catch (SearchEngineCommunicationException e1) {
			e1.printStackTrace();
			try {
				ResourceDirectoryInterface.deleteMetadataFile(metadataId);
			} catch (MetadataNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			throw e1;

		} catch (SearchEngineErrorException e1) {
			e1.printStackTrace();
			try {
				ResourceDirectoryInterface.deleteMetadataFile(metadataId);
			} catch (MetadataNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			throw e1;
		}
		ResourceDirectoryInterface.setAggregationLevel(metadataId,
				aggregationLevel);
		createMetadataEntryInDatabase(metadataId);

		try {
			IResourceDataHandler resourceDataHandler = DBAccessFactory
					.getResourceDataHandler(DBTypes.mongoDB);
			return Response
					.ok()
					.entity(rb.getMetadataRepresentation(
							context.getRealPath(""), uriInfo,
							apiscolInstanceName, metadataId, true,
							Collections.<String, String> emptyMap(),
							resourceDataHandler, editUri))
					.type(rb.getMediaType()).build();
		} catch (MetadataNotFoundException e) {
			String message = String
					.format("The metadata %s has just been registred, but it was impossible to find the file",
							metadataId);
			logger.error(message);
			throw new FileSystemAccessException(message);
		}
	}

	@POST
	@Path("/{mdid}/refresh")
	@Produces({ MediaType.APPLICATION_ATOM_XML, MediaType.APPLICATION_XML })
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response updateMetadata(@Context HttpServletRequest request,
			@PathParam(value = "mdid") final String metadataId,
			@FormParam("index") Boolean updateIndex)
			throws FileSystemAccessException,
			InvalidProvidedMetadataFileException, MetadataNotFoundException,
			InvalidEtagException, DBAccessException {
		String requestedFormat = request.getHeader(HttpHeaders.ACCEPT);
		IEntitiesRepresentationBuilder<?> rb = EntitiesRepresentationBuilderFactory
				.getRepresentationBuilder(requestedFormat, context);
		takeAndReleaseGlobalLock();
		ResponseBuilder response = null;
		KeyLock keyLock = null;
		try {
			keyLock = keyLockManager.getLock(metadataId);
			keyLock.lock();
			try {
				logger.info(String
						.format("Entering critical section with mutual exclusion for metadata %s",
								metadataId));
				checkFreshness(request.getHeader(HttpHeaders.IF_MATCH),
						metadataId);
				String url = rb.getMetadataUri(uriInfo, metadataId);

				String filePath = "";
				try {
					searchEngineQueryHandler.processDeleteQuery(url);

				} catch (SearchEngineCommunicationException e1) {
					logger.error(String
							.format("Connexion problem with the search engine while trying to erase from index doc with id %s, with message %s",
									url, e1.getMessage()));
				} catch (SearchEngineErrorException e1) {
					logger.error(String
							.format("Exception thrown by the search engine while trying to erase from index doc with id %s, with message %s",
									url, e1.getMessage()));
				}

				filePath = ResourceDirectoryInterface.getFilePath(metadataId);

				if (!StringUtils.isEmpty(filePath)) {
					try {
						searchEngineQueryHandler.processAddQuery(filePath);

					} catch (SearchEngineCommunicationException e1) {
						logger.error(String
								.format("Connexion problem with the search engine while trying to register metadata file %s, with message %s",
										filePath, e1.getMessage()));
					} catch (SearchEngineErrorException e1) {
						logger.error(String
								.format("Exception thrown by the search engine while trying to register metadata file %s, with message %s",
										filePath, e1.getMessage()));
					}
				}

				try {
					searchEngineQueryHandler.processCommitQuery();
				} catch (SearchEngineCommunicationException e1) {
					logger.error(String
							.format("Connexion problem with the search engine while trying to commit after registering metadata file %s, with message %s",
									filePath, e1.getMessage()));
				} catch (SearchEngineErrorException e1) {
					logger.error(String
							.format("Exception thrown by the search engine while trying to commit after registering metadata file %s, with message %s",
									filePath, e1.getMessage()));
				}

				try {
					IResourceDataHandler resourceDataHandler = DBAccessFactory
							.getResourceDataHandler(DBTypes.mongoDB);
					response = Response
							.ok()
							.entity(rb.getMetadataRepresentation(
									context.getRealPath(""), uriInfo,
									apiscolInstanceName, metadataId, true,
									Collections.<String, String> emptyMap(),
									resourceDataHandler, editUri))
							.type(rb.getMediaType());
				} catch (MetadataNotFoundException e) {
					String message = String
							.format("The metadata %s has just been registred, but it was impossible to find the file",
									metadataId);
					logger.error(message);
					throw new FileSystemAccessException(message);
				}

			} finally {
				keyLock.unlock();
			}
		} finally {
			if (keyLock != null) {
				keyLock.release();
			}
			logger.info(String
					.format("Leaving critical section with mutual exclusion for metadata %s",
							metadataId));
		}
		return response.build();
	}

	@PUT
	@Path("/{mdid}")
	@Produces({ MediaType.APPLICATION_ATOM_XML, MediaType.APPLICATION_XML })
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response changeMetadata(
			@Context HttpServletRequest request,
			@PathParam(value = "mdid") final String metadataId,
			@FormDataParam("file") InputStream uploadedInputStream,
			@FormDataParam("file") FormDataContentDisposition fileDetail,
			@DefaultValue("0") @FormDataParam("aggregation_level") int aggregationLevel)
			throws FileSystemAccessException,
			InvalidProvidedMetadataFileException, MetadataNotFoundException,
			InvalidEtagException, DBAccessException {
		String requestedFormat = request.getHeader(HttpHeaders.ACCEPT);
		IEntitiesRepresentationBuilder<?> rb = EntitiesRepresentationBuilderFactory
				.getRepresentationBuilder(requestedFormat, context);
		StringBuilder warnings = new StringBuilder();
		takeAndReleaseGlobalLock();
		ResponseBuilder response = null;
		boolean continueProcessing = true;
		KeyLock keyLock = null;
		try {
			keyLock = keyLockManager.getLock(metadataId);
			keyLock.lock();
			try {
				logger.info(String
						.format("Entering critical section with mutual exclusion for metadata %s",
								metadataId));
				checkFreshness(request.getHeader(HttpHeaders.IF_MATCH),
						metadataId);
				String url = rb.getMetadataUri(uriInfo, metadataId);
				boolean solrIsWaitingForCommit = false;
				try {
					ResourceDirectoryInterface.registerMetadataFile(metadataId,
							uploadedInputStream, url, apiscolInstanceName);
				} catch (FileSystemAccessException e) {
					logger.error(String
							.format("Registration of file was aborted as a transfer problem occured for metadata %s",
									metadataId));

					throw e;
				} catch (InvalidProvidedMetadataFileException e) {

					logger.error(String
							.format("Registration of file was aborted as a transfer problem occured for metadata %s",
									metadataId));

					throw e;
				}
				try {
					uploadedInputStream.close();
				} catch (IOException e) {
					logger.warn(String
							.format("A probleme was encountered while closing the input streamm for file %s : %s",
									fileDetail.getFileName(), e.getMessage()));
				}
				Map<String, String> propertiesToSave = ResourceDirectoryInterface
						.extractPropertiesToSave(metadataId);
				boolean successFullFileDeletion = ResourceDirectoryInterface
						.deleteMetadataFile(metadataId);

				if (!successFullFileDeletion) {
					// we don't stop, perhaps previous metadata file did not
					// exist
					String errorReport = String
							.format("Failed to delete file for metadata %s",
									metadataId);
					warnings.append(errorReport);
					logger.error(errorReport);
					// delete the temporary file
				}
				boolean fileCommitSuccessfull = false;
				if (continueProcessing) {
					fileCommitSuccessfull = ResourceDirectoryInterface
							.commitTemporaryMetadataFile(metadataId)
							&& ResourceDirectoryInterface
									.commitTemporaryJsonMetadataFile(metadataId);
					continueProcessing &= fileCommitSuccessfull;
				}
				if (!fileCommitSuccessfull) {
					continueProcessing = false;
					String errorReport;
					if (successFullFileDeletion)
						errorReport = String
								.format("The file has been deleted but we failed to  replace it with the new one. Metadata  %s are damaged please send the file again.",
										metadataId);
					else
						errorReport = String
								.format("The file has not been deleted and we failed to  replace it with the new one. Metadata  %s have not changed.",
										metadataId);
					warnings.append(errorReport);
					logger.error(errorReport);
					// delete the temporary file
					ResourceDirectoryInterface.deleteMetadataFile(metadataId);
					throw new FileSystemAccessException(errorReport);
				}
				ResourceDirectoryInterface.restoreProperties(metadataId,
						propertiesToSave);
				// if an aggregation level is specified in form parameters,
				// overwrite the old one
				// else, set level 1 if no level was found in the old file
				if (aggregationLevel > 0)
					ResourceDirectoryInterface.setAggregationLevel(metadataId,
							aggregationLevel);
				else if (!propertiesToSave.containsKey("aggregation_level"))
					ResourceDirectoryInterface.setAggregationLevel(metadataId,
							1);
				String filePath = "";
				if (continueProcessing) {
					try {
						searchEngineQueryHandler.processDeleteQuery(url);
						solrIsWaitingForCommit = true;
					} catch (SearchEngineCommunicationException e1) {
						logger.error(String
								.format("Connexion problem with the search engine while trying to erase from index doc with id %s, with message %s",
										url, e1.getMessage()));
					} catch (SearchEngineErrorException e1) {
						logger.error(String
								.format("Exception thrown by the search engine while trying to erase from index doc with id %s, with message %s",
										url, e1.getMessage()));
					}

				}
				if (continueProcessing) {
					filePath = ResourceDirectoryInterface
							.getFilePath(metadataId);
				}
				if (continueProcessing && !StringUtils.isEmpty(filePath)) {
					try {
						searchEngineQueryHandler.processAddQuery(filePath);
						solrIsWaitingForCommit = true;

					} catch (SearchEngineCommunicationException e1) {
						logger.error(String
								.format("Connexion problem with the search engine while trying to register metadata file %s, with message %s",
										filePath, e1.getMessage()));
					} catch (SearchEngineErrorException e1) {
						logger.error(String
								.format("Exception thrown by the search engine while trying to register metadata file %s, with message %s",
										filePath, e1.getMessage()));
					}
				}

				if (continueProcessing && solrIsWaitingForCommit) {
					try {
						searchEngineQueryHandler.processCommitQuery();
					} catch (SearchEngineCommunicationException e1) {
						logger.error(String
								.format("Connexion problem with the search engine while trying to commit after registering metadata file %s, with message %s",
										filePath, e1.getMessage()));
					} catch (SearchEngineErrorException e1) {
						logger.error(String
								.format("Exception thrown by the search engine while trying to commit after registering metadata file %s, with message %s",
										filePath, e1.getMessage()));
					}

					updateMetadataEntryInDataBase(metadataId);
				}
				try {
					IResourceDataHandler resourceDataHandler = DBAccessFactory
							.getResourceDataHandler(DBTypes.mongoDB);
					response = Response
							.ok()
							.entity(rb.getMetadataRepresentation(
									context.getRealPath(""), uriInfo,
									apiscolInstanceName, metadataId, true,
									Collections.<String, String> emptyMap(),
									resourceDataHandler, editUri))
							.type(rb.getMediaType());
				} catch (MetadataNotFoundException e) {
					String message = String
							.format("The metadata %s has just been registred, but it was impossible to find the file",
									metadataId);
					logger.error(message);
					throw new FileSystemAccessException(message);
				}

			} finally {
				keyLock.unlock();
			}
		} finally {
			if (keyLock != null) {
				keyLock.release();
			}
			logger.info(String
					.format("Leaving critical section with mutual exclusion for metadata %s",
							metadataId));
		}
		return response.build();
	}

	private void updateMetadataEntryInDataBase(String metadataId)
			throws DBAccessException, MetadataNotFoundException {
		IResourceDataHandler resourceDataHandler = DBAccessFactory
				.getResourceDataHandler(DBTypes.mongoDB);
		Document metadata = ResourceDirectoryInterface
				.getMetadataAsDocument(metadataId);
		resourceDataHandler.updateMetadataEntry(metadataId, metadata);

	}

	private void createMetadataEntryInDatabase(String metadataId)
			throws DBAccessException, MetadataNotFoundException {
		IResourceDataHandler resourceDataHandler = DBAccessFactory
				.getResourceDataHandler(DBTypes.mongoDB);
		Document metadata = ResourceDirectoryInterface
				.getMetadataAsDocument(metadataId);
		resourceDataHandler.createMetadataEntry(metadataId, metadata);

	}

	private void deleteMetadataEntryInDatabase(String metadataId)
			throws DBAccessException, MetadataNotFoundException {
		IResourceDataHandler resourceDataHandler = DBAccessFactory
				.getResourceDataHandler(DBTypes.mongoDB);
		resourceDataHandler.deleteMetadataEntry(metadataId);

	}

	@PUT
	@Path("/{mdid}/technical_infos")
	@Produces({ MediaType.APPLICATION_ATOM_XML, MediaType.APPLICATION_XML })
	public Response updateTechnicalInformations(
			@Context HttpServletRequest request,
			@PathParam(value = "mdid") final String metadataId,
			@DefaultValue("") @FormParam("technical-location") String technicalLocation,
			@DefaultValue("") @FormParam("apiscol_instance") String apiscolInstance,
			@DefaultValue("") @FormParam("location") String location,
			@DefaultValue("") @FormParam("format") String format,
			@DefaultValue("") @FormParam("thumb") String thumb,
			@DefaultValue("") @FormParam("preview") String preview,
			@DefaultValue("") @FormParam("size") String size,
			@DefaultValue("") @FormParam("language") String language,
			@DefaultValue("") @FormParam("edit_uri") String editUri)
			throws FileSystemAccessException,
			InvalidProvidedMetadataFileException, MetadataNotFoundException,
			InvalidEtagException, DBAccessException {
		takeAndReleaseGlobalLock();
		KeyLock keyLock = null;
		try {
			keyLock = keyLockManager.getLock(metadataId);
			keyLock.lock();
			try {
				logger.info(String
						.format("Entering critical section with mutual exclusion for metadata %s",
								metadataId));
				if (!StringUtils.isEmpty(editUri))
					MetadataApi.editUri = editUri;
				checkFreshness(request.getHeader(HttpHeaders.IF_MATCH),
						metadataId);
				ResourceDirectoryInterface.updateTechnicalInformation(
						metadataId, size, language, technicalLocation,
						apiscolInstance, location, format, thumb, preview);
				ResourceDirectoryInterface.renewJsonpFile(metadataId);
				updateMetadataEntryInDataBase(metadataId);
				String filePath = ResourceDirectoryInterface
						.getFilePath(metadataId);
				try {
					searchEngineQueryHandler.processDeleteQuery(filePath);
					searchEngineQueryHandler.processAddQuery(filePath);
					searchEngineQueryHandler.processCommitQuery();
				} catch (SearchEngineCommunicationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SearchEngineErrorException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} finally {
				keyLock.unlock();
			}
		} finally {
			if (keyLock != null) {
				keyLock.release();
			}
			logger.info(String
					.format("Leaving critical section with mutual exclusion for metadata %s",
							metadataId));
		}
		return Response.ok().build();
	}

	/**
	 * Allows to list the entries of the metadata repository or to filter them
	 * by many criteriums.
	 * 
	 * @param request
	 * @param format
	 *            Allows to override the HTTP <code>'accept'</code> parameter.
	 * @param query
	 *            The Solr Edismax style free text seach query
	 * @param supplements
	 *            A comma separated list of fully qualified URIs. They are
	 *            representing supplementary metadata to force in the search
	 *            result.<br/>
	 *            Example : http://server-url:server-port/meta/de89d9f6-22d
	 *            4-4e58-822c-43a3df1fa2ac
	 *            ,http://server-url:server-port/meta/c6cba63b
	 *            -8b85-4611-9fbc-605d89277e30 <br/>
	 *            It means that search result in ApiScol-content have brought
	 *            those additionnal metadata
	 * 
	 * 
	 * @param fuzzy
	 *            This enables fuzzy search in Solr. Attention! Fuzzy search
	 *            disables all other treatments on strings and can cause
	 *            confusing results.
	 * @param staticFilters
	 *            A Json style list of strings.<br/>
	 *            Each one is structured following the pattern : element::value<br/>
	 *            example :
	 *            ["educational.place::en atelier","educational.tool::TBI"]
	 * @param dynamicFilters
	 *            A Json style list of strings.<br/>
	 *            Each one is structured following the pattern :
	 *            classification.taxonPath.purpose::source::id::entry <br/>
	 *            example : ["discipline::Diplômes::40022106::BAC PRO Cuisine",
	 *            "discipline::Nomenclature disciplines professionnelle::HRT::Hotellerie restauration tourisme"
	 *            ]
	 * @param start
	 *            Pagination start
	 * @param rows
	 *            Pagination end
	 * @param includeDescription
	 *            If set to true, more informative (textual) content will be
	 *            delivered : title, summary. For clients who are considering
	 *            requesting the whole scolomfr document, it is useless.
	 * @return The ATOM representation of the metadata list with additional
	 *         information concerning the query matches, spellcheck suggestions,
	 *         etc. The content of <code>'entry'</code> tag is the metadata ATOM
	 *         representation. Static and dynamic facets matches the structure
	 *         of scoLOMfr metadata. Dynamic Facets are intended to apply
	 *         filters coming from the <code>'classification'</code> element.
	 *         <code>* &lt;feed xmlns="http://www.w3.org/2005/Atom" xmlns:apiscol="http://www.crdp.ac-versailles.fr/2012/apiscol"&gt;
	 * &nbsp;&nbsp;&nbsp;&lt;link
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;href="http://server.url:server.port/meta/?query=statistiques&format=xml&desc=true"
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;rel="self" /&gt;
	 * &nbsp;&nbsp;&nbsp;&lt;logo&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;http://apiscol.crdp-versailles.fr/cdn/0.0.1/img/logo-api.png
	 * &nbsp;&nbsp;&nbsp;&lt;/logo&gt;
	 * &nbsp;&nbsp;&nbsp;&lt;icon&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;http://apiscol.crdp-versailles.fr/cdn/0.0.1/img/logo-api.png
	 * &nbsp;&nbsp;&nbsp;&lt;/icon&gt;
	 * &nbsp;&nbsp;&nbsp;&lt;id&gt;http://server.url:server.port/meta/&lt;/id&gt;
	 * &nbsp;&nbsp;&nbsp;&lt;title&gt;Example de dépôt de ressources - eclipse&lt;/title&gt;
	 * &nbsp;&nbsp;&nbsp;&lt;generator&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;ApiScol, Dépôt de ressources pédagogiques - CRDP de l'Académie de Versailles
	 * &nbsp;&nbsp;&nbsp;&lt;/generator&gt;
	 * &nbsp;&nbsp;&nbsp;&lt;updated&gt;2013-03-13T15:05:02.000+01:00&lt;/updated&gt;
	 * &nbsp;&nbsp;&nbsp;&lt;apiscol:length&gt;2&lt;/apiscol:length&gt;
	 * &nbsp;&nbsp;&nbsp;&lt;entry&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;updated&gt;2013-03-13T15:05:02.000+01:00&lt;/updated&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;apiscol:score&gt;0.7545044&lt;/apiscol:score&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;id&gt;
	 * &nbsp;&nbsp;&nbsp;urn:apiscol:example-dev:meta:metadata:cf963387-1652-4f7d-a8e2-64b516b876e3
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;/id&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;title&gt;Méthodologie de l'enquête statistique&lt;/title&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;summary&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Introduction générale à l'enquête statistique. La démarche expérimentale en
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;sciences sociales : "Traiter les faits sociaux comme des choses". .
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;/summary&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;link
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;href="http://server.url:server.port/thumbs/files/a/b/f/d5907857c7fff06b3ad252aa146d1.jpg"
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;rel="icon" type="image/jpeg" /&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;content src="http://www.sciences.sociales.fr/url-of-the-content" type="text/html" /&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;author&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;name&gt;Dupont, Victor&lt;/name&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;/author&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;link
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;href="http://server.url:server.port/meta/cf963387-1652-4f7d-a8e2-64b516b876e3"
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;rel="self" type="text/html" /&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;link
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;href="http://server.url:server.port/meta/cf963387-1652-4f7d-a8e2-64b516b876e3?format=xml"
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;rel="self" type="application/atom+xml" /&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;link href="nullmeta/cf963387-1652-4f7d-a8e2-64b516b876e3" rel="edit"
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;type="application/atom+xml" /&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;link
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;href="http://server.url:server.port/meta/lom/c/f/9/63387-1652-4f7d-a8e2-64b516b876e3.xml"
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;rel="describedby" type="application/atom+xml" /&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;link
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;href="http://server.url:server.port/meta/lom/c/f/9/63387-1652-4f7d-a8e2-64b516b876e3.js"
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;rel="describedby" type="application/javascript" /&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;apiscol:code-snippet
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;href="http://server.url:server.port/meta/cf963387-1652-4f7d-a8e2-64b516b876e3/snippet" /&gt;
	 * &nbsp;&nbsp;&nbsp;&lt;/entry&gt;
	 * &nbsp;&nbsp;&nbsp;&lt;apiscol:facets&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;apiscol:static-facets name="lifecycle.status"&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;apiscol:facet count="1"&gt;final&lt;/apiscol:facet&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;/apiscol:static-facets&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;apiscol:static-facets name="rights.copyrightandotherrestrictions"&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;apiscol:facet count="1"&gt;true&lt;/apiscol:facet&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;/apiscol:static-facets&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;apiscol:static-facets name="rights.costs"&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;apiscol:facet count="1"&gt;false&lt;/apiscol:facet&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;/apiscol:static-facets&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;apiscol:static-facets name="educational.place"&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;apiscol:facet count="1"&gt;en salle de classe&lt;/apiscol:facet&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;/apiscol:static-facets&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;apiscol:static-facets name="educational.intendedenduserrole"&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;apiscol:facet count="1"&gt;learner&lt;/apiscol:facet&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;apiscol:facet count="1"&gt;teacher&lt;/apiscol:facet&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;/apiscol:static-facets&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;apiscol:static-facets name="lifecycle.contributor.author"&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;apiscol:facet count="1"&gt;Dornbusch, Joachim&lt;/apiscol:facet&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;/apiscol:static-facets&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;apiscol:static-facets name="educational.learningresourcetype"&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;apiscol:facet count="1"&gt;lecture&lt;/apiscol:facet&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;/apiscol:static-facets&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;apiscol:static-facets name="educational.language"&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;apiscol:facet count="1"&gt;fre&lt;/apiscol:facet&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;/apiscol:static-facets&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;apiscol:static-facets name="educational.educationalmethod"&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;apiscol:facet count="1"&gt;en classe entière&lt;/apiscol:facet&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;/apiscol:static-facets&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;apiscol:static-facets name="educational.activity"&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;apiscol:facet count="1"&gt;apprendre&lt;/apiscol:facet&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;/apiscol:static-facets&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;apiscol:static-facets name="general.generalresourcetype"&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;apiscol:facet count="1"&gt;diaporama&lt;/apiscol:facet&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;/apiscol:static-facets&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;apiscol:static-facets name="technical.format"&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;apiscol:facet count="1"&gt;text/html&lt;/apiscol:facet&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;/apiscol:static-facets&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;apiscol:static-facets name="educational.context"&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;apiscol:facet count="1"&gt;school&lt;/apiscol:facet&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;apiscol:facet count="1"&gt;enseignement secondaire&lt;/apiscol:facet&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;/apiscol:static-facets&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;apiscol:static-facets name="educational.tool"&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;apiscol:facet count="1"&gt;TBI&lt;/apiscol:facet&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;/apiscol:static-facets&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;apiscol:dynamic-facets name="educational_level"&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;apiscol:taxon identifier="scolomfr-voc-022"&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;apiscol:entry count="1" identifier="scolomfr-voc-022-num-027"
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;label="2de générale" /&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;apiscol:entry count="1" identifier="scolomfr-voc-022-num-087"
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;label="lycée général" /&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;/apiscol:taxon&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;/apiscol:dynamic-facets&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;apiscol:dynamic-facets name="public_cible_détaillé"&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;apiscol:taxon identifier="scolomfr-voc-021"&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;apiscol:entry count="1" identifier="scolomfr-voc-021-num-00092"
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;label="professeur de lycée" /&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;/apiscol:taxon&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;/apiscol:dynamic-facets&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;apiscol:dynamic-facets name="enseignement" /&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;apiscol:dynamic-facets name="competency" /&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;apiscol:dynamic-facets name="discipline"&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;apiscol:taxon identifier="Nomenclature disciplines générales"&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;apiscol:entry count="1" identifier="SES"
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;label="Sciences économiques et sociales" /&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;/apiscol:taxon&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;/apiscol:dynamic-facets&gt;
	 * &nbsp;&nbsp;&nbsp;&lt;/apiscol:facets&gt;
	 * &nbsp;&nbsp;&nbsp;&lt;apiscol:hits&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;apiscol:hit
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;metadataId="urn:apiscol:example-dev:meta:metadata:cf963387-1652-4f7d-a8e2-64b516b876e3"&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;apiscol:matches&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;apiscol:match&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;M&#233;thodologie de l'enqu&#234;te
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;b&gt;statistique&lt;/b&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;/apiscol:match&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;/apiscol:matches&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;/apiscol:hit&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;apiscol:hit
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;metadataId="urn:apiscol:example-dev:meta:metadata:6858fcd2-88e2-48d8-8d21-5d854f60ac5a"&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;apiscol:matches&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;apiscol:match&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;M&#233;thodologie de l'enqu&#234;te
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;b&gt;statistique&lt;/b&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;/apiscol:match&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;/apiscol:matches&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;/apiscol:hit&gt;
	 * &nbsp;&nbsp;&nbsp;&lt;/apiscol:hits&gt;
	 * &nbsp;&nbsp;&nbsp;&lt;apiscol:spellcheck&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;apiscol:query_term requested="statistiques"&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;apiscol:word&gt;statistique&lt;/apiscol:word&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;/apiscol:query_term&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;apiscol:queries&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;apiscol:query&gt;statistique&lt;/apiscol:query&gt;
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;/apiscol:queries&gt;
	 * &nbsp;&nbsp;&nbsp;&lt;/apiscol:spellcheck&gt;
	 * &lt;/feed&gt;
	 * </code>
	 * @throws SearchEngineErrorException
	 * @throws NumberFormatException
	 * @throws DBAccessException
	 * @throws InvalidFilterListException
	 */
	@GET
	@Path("/")
	@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_ATOM_XML,
			MediaType.TEXT_HTML, MediaType.APPLICATION_XHTML_XML })
	public Response getMetadataList(
			@Context HttpServletRequest request,
			@QueryParam(value = "format") final String format,
			@QueryParam(value = "query") String query,
			@QueryParam(value = "mdid") String forcedMetadataId,
			@DefaultValue("") @QueryParam(value = "supplements") final String supplements,
			@DefaultValue("0") @QueryParam(value = "fuzzy") final float fuzzy,
			@DefaultValue("[]") @QueryParam(value = "static-filters") final String staticFilters,
			@DefaultValue("[]") @QueryParam(value = "dynamic-filters") final String dynamicFilters,
			@DefaultValue("0") @QueryParam(value = "start") final int start,
			@DefaultValue("10") @QueryParam(value = "rows") final int rows,
			@DefaultValue("false") @QueryParam(value = "desc") boolean includeDescription)
			throws SearchEngineErrorException, NumberFormatException,
			DBAccessException, InvalidFilterListException {
		String requestedFormat = guessRequestedFormat(request, format);
		String[] supplementsIds = StringUtils.split(supplements, ",");

		IEntitiesRepresentationBuilder<?> rb = EntitiesRepresentationBuilderFactory
				.getRepresentationBuilder(requestedFormat, context);
		if (rb instanceof XHTMLRepresentationBuilder)
			includeDescription = true;
		if (StringUtils.isNotEmpty(forcedMetadataId))
			return forcedMetadataSearch(rb, request, forcedMetadataId,
					includeDescription);
		for (int i = 0; i < supplementsIds.length; i++) {
			// TODO it's not very useful
			String metadataId = MetadataKeySyntax
					.extractMetadataIdFromUrl(supplementsIds[i]);
			supplementsIds[i] = rb.getMetadataUri(uriInfo, metadataId);
		}
		boolean disableHighlighting = false;
		if (StringUtils.isBlank(query)) {
			query = "*";
			disableHighlighting = true;
		}
		java.lang.reflect.Type collectionType = new TypeToken<List<String>>() {
		}.getType();
		List<String> staticFiltersList = null;
		if (StringUtils.isNotEmpty(staticFilters))
			try {
				staticFiltersList = new Gson().fromJson(staticFilters,
						collectionType);
			} catch (Exception e) {
				String message = String
						.format("The list of static filters %s is impossible to parse as JSON",
								staticFilters);
				logger.warn(message);
				throw new InvalidFilterListException(message);
			}
		else
			staticFiltersList = Collections.emptyList();
		List<String> dynamicFiltersList = null;
		if (StringUtils.isNotEmpty(dynamicFilters))
			try {
				dynamicFiltersList = new Gson().fromJson(dynamicFilters,
						collectionType);
			} catch (Exception e) {
				String message = String
						.format("The list of dynamic filters %s is impossible to parse as JSON",
								dynamicFilters);
				logger.warn(message);
				throw new InvalidFilterListException(message);
			}
		else
			dynamicFiltersList = Collections.emptyList();
		Object result = searchEngineQueryHandler.processSearchQuery(
				query.trim(), supplementsIds, fuzzy, staticFiltersList,
				dynamicFiltersList, disableHighlighting, start, rows);
		ISearchEngineResultHandler handler = searchEngineFactory
				.getResultHandler();
		handler.parse(result);
		IResourceDataHandler resourceDataHandler = null;
		if (includeDescription) {
			try {
				resourceDataHandler = DBAccessFactory
						.getResourceDataHandler(DBTypes.mongoDB);
			} catch (DBAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return Response
				.ok(rb.selectMetadataFollowingCriterium(
						context.getRealPath(""), uriInfo, apiscolInstanceName,
						apiscolInstanceLabel, handler, start, rows,
						includeDescription, resourceDataHandler, editUri,
						version), rb.getMediaType())
				.header("Access-Control-Allow-Origin", "*").build();

	}

	private Response forcedMetadataSearch(IEntitiesRepresentationBuilder<?> rb,
			HttpServletRequest request, String forcedMetadataId,
			boolean includeDescription) throws NumberFormatException,
			DBAccessException, SearchEngineErrorException {
		Object result = searchEngineQueryHandler
				.processSearchQuery(forcedMetadataId);
		ISearchEngineResultHandler handler = searchEngineFactory
				.getResultHandler();
		handler.parse(result);
		IResourceDataHandler resourceDataHandler = null;
		if (includeDescription) {
			try {
				resourceDataHandler = DBAccessFactory
						.getResourceDataHandler(DBTypes.mongoDB);
			} catch (DBAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return Response.ok(
				rb.selectMetadataFollowingCriterium(context.getRealPath(""),
						uriInfo, apiscolInstanceName, apiscolInstanceLabel,
						handler, 0, 1, includeDescription, resourceDataHandler,
						editUri, version), rb.getMediaType()).build();
	}

	public enum GetModalities {
		base("base"), full("full"), snippet("snippet");
		private String value;

		private GetModalities(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return value;
		}

		public static GetModalities fromString(String modality) {
			if (modality.equals(base.toString()))
				return base;
			if (modality.equals(full.toString()))
				return full;
			if (modality.equals(snippet.toString()))
				return snippet;
			return full;
		}
	}

	private Response getMetadataRepresentation(HttpServletRequest request,
			HttpServletResponse httpServletResponse, String metadataId,
			String format, String style, String device, ServletContext context,
			UriInfo uriInfo, GetModalities modality, boolean includeDescription)
			throws MetadataNotFoundException,
			IncorrectMetadataKeySyntaxException, DBAccessException {
		checkMdidSyntax(metadataId);
		ResourceDirectoryInterface.checkMetadataExistence(metadataId);
		String requestedFormat = guessRequestedFormat(request, format);
		IEntitiesRepresentationBuilder<?> rb = EntitiesRepresentationBuilderFactory
				.getRepresentationBuilder(requestedFormat, context);
		Map<String, String> params = new HashMap<String, String>();
		if (StringUtils.isNotEmpty(version))
			params.put("version", version);
		if (StringUtils.isNotEmpty(modality.toString()))
			params.put("mode", modality.toString());
		if (StringUtils.isNotEmpty(style))
			params.put("style", style);
		if (StringUtils.isNotEmpty(device))
			params.put("device", device);
		IResourceDataHandler resourceDataHandler = DBAccessFactory
				.getResourceDataHandler(DBTypes.mongoDB);
		Object response = rb.getMetadataRepresentation(context.getRealPath(""),
				uriInfo, apiscolInstanceName, metadataId, includeDescription,
				params, resourceDataHandler, editUri);

		String mediaType = rb.getMediaType().toString();
		return Response
				.ok(response, mediaType)
				.header(HttpHeaders.ETAG,
						TimeUtils.toRFC3339(Long
								.parseLong(ResourceDirectoryInterface
										.getTimeStamp(metadataId)))).build();
	}

	@GET
	@Path("/{mdid}")
	@Produces({ MediaType.TEXT_HTML, MediaType.APPLICATION_XHTML_XML,
			MediaType.APPLICATION_ATOM_XML, MediaType.APPLICATION_XML,
			"application/javascript" })
	public Response getMetadata(
			@Context HttpServletRequest request,
			@Context HttpServletResponse httpServletResponse,
			@PathParam(value = "mdid") final String metadataId,
			@DefaultValue("") @QueryParam(value = "mode") final String mode,
			@DefaultValue("") @QueryParam(value = "style") final String style,
			@DefaultValue("") @QueryParam(value = "device") final String device,
			@DefaultValue("false") @QueryParam(value = "desc") final boolean includeDescription,
			@QueryParam(value = "format") final String format

	) throws MetadataNotFoundException, IncorrectMetadataKeySyntaxException,
			DBAccessException {
		return getMetadataRepresentation(request, httpServletResponse,
				metadataId, format, style, device, context, uriInfo,
				GetModalities.fromString(mode), includeDescription);

	}

	@GET
	@Path("/{mdid}/snippet")
	@Produces({ MediaType.APPLICATION_XML, MediaType.TEXT_HTML,
			MediaType.APPLICATION_XHTML_XML, "application/javascript" })
	public Response getMetadataSnippet(@Context HttpServletRequest request,
			@Context HttpServletResponse httpServletResponse,
			@PathParam(value = "mdid") final String metadataId,
			@QueryParam(value = "format") final String format)
			throws MetadataNotFoundException,
			IncorrectMetadataKeySyntaxException {
		checkMdidSyntax(metadataId);
		ResourceDirectoryInterface.checkMetadataExistence(metadataId);
		String requestedFormat = guessRequestedFormat(request, format);
		IEntitiesRepresentationBuilder<?> rb = EntitiesRepresentationBuilderFactory
				.getRepresentationBuilder(requestedFormat, context);
		Object response = rb.getMetadataSnippetRepresentation(
				context.getRealPath(""), uriInfo, apiscolInstanceName,
				metadataId, version);

		String mediaType = rb.getMediaType().toString();
		return Response
				.ok(response, mediaType)
				.header(HttpHeaders.ETAG,
						ResourceDirectoryInterface.getTimeStamp(metadataId))
				.build();
	}

	@GET
	@Path("/suggestions")
	@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_ATOM_XML,
			MediaType.TEXT_HTML, MediaType.APPLICATION_XHTML_XML })
	public Response getQuerySuggestions(@Context HttpServletRequest request,
			@QueryParam(value = "format") final String format,
			@QueryParam(value = "query") final String query)
			throws SearchEngineErrorException, NumberFormatException,
			DBAccessException {
		String requestedFormat = guessRequestedFormat(request, format);
		IEntitiesRepresentationBuilder<?> rb = EntitiesRepresentationBuilderFactory
				.getRepresentationBuilder(requestedFormat, context);
		if (StringUtils.isBlank(query))
			return Response
					.status(Status.BAD_REQUEST)
					.entity("You  cannot ask for suggestions with a blank query string")
					.type(MediaType.TEXT_PLAIN).build();
		else {
			Object result = searchEngineQueryHandler
					.processSpellcheckQuery(query.trim());
			ISearchEngineResultHandler handler = searchEngineFactory
					.getResultHandler();
			handler.parse(result);
			return Response.ok(
					rb.selectMetadataFollowingCriterium(
							context.getRealPath(""), uriInfo,
							apiscolInstanceName, apiscolInstanceLabel, handler,
							0, 10, false, null, editUri, version),
					rb.getMediaType()).build();
		}
	}

	@DELETE
	@Path("/{mdid}")
	@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_ATOM_XML })
	public Response deleteContent(@Context HttpServletRequest request,
			@PathParam(value = "mdid") final String metadataId,
			@QueryParam(value = "format") final String format)
			throws MetadataNotFoundException,
			IncorrectMetadataKeySyntaxException, InvalidEtagException,
			DBAccessException, DeletionNotAllowedException {
		checkMdidSyntax(metadataId);
		String requestedFormat = guessRequestedFormat(request, format);
		IEntitiesRepresentationBuilder<?> rb = EntitiesRepresentationBuilderFactory
				.getRepresentationBuilder(requestedFormat, context);
		ResponseBuilder response = null;
		StringBuilder warnings = new StringBuilder();
		takeAndReleaseGlobalLock();
		KeyLock keyLock = null;
		try {
			keyLock = keyLockManager.getLock(metadataId);
			keyLock.lock();
			try {
				logger.info(String
						.format("Entering critical section with mutual exclusion for metadata %s",
								metadataId));
				checkFreshness(request.getHeader(HttpHeaders.IF_MATCH),
						metadataId);
				List<String> packsContainingMetadata = ResourceDirectoryInterface
						.getPacksContainingMetadata(metadataId);
				if (packsContainingMetadata.size() > 0) {
					throw new DeletionNotAllowedException(metadataId,
							packsContainingMetadata.get(0));
				}
				List<String> otherAffectedRelations = ResourceDirectoryInterface
						.removePackRelations(metadataId, uriInfo);
				for (Iterator<String> iterator = otherAffectedRelations
						.iterator(); iterator.hasNext();) {
					refreshMetadata(iterator.next());
				}
				String url = rb.getMetadataUri(uriInfo, metadataId);
				boolean successFullFileDeletion = ResourceDirectoryInterface
						.deleteMetadataFile(metadataId);
				if (successFullFileDeletion) {
					try {
						searchEngineQueryHandler.processDeleteQuery(url);
						searchEngineQueryHandler.processCommitQuery();
					} catch (SearchEngineCommunicationException e1) {
						logger.error(String
								.format("Connexion problem with the search engine while trying to erase from index doc with id %s, with message %s",
										url, e1.getMessage()));
					} catch (SearchEngineErrorException e1) {
						logger.error(String
								.format("Exception thrown by the search engine while trying to erase from index doc with id %s, with message %s",
										url, e1.getMessage()));
					}

				} else {
					String errorReport = String
							.format("Failed to delete file for metadata %s",
									metadataId);
					warnings.append(errorReport);
					logger.error(errorReport);
					if (response == null)
						response = Response
								.status(Status.INTERNAL_SERVER_ERROR)
								.entity(warnings.toString())
								.type(MediaType.TEXT_PLAIN);
				}
				if (successFullFileDeletion)
					deleteMetadataEntryInDatabase(metadataId);

				if (response == null) {

					response = Response.ok(rb
							.getMetadataSuccessfulDestructionReport(
									realPath(context), uriInfo,
									apiscolInstanceName, metadataId,
									warnings.toString()), rb.getMediaType());

				}

			} finally {
				keyLock.unlock();
			}
		} finally {
			if (keyLock != null) {
				keyLock.release();
			}
			logger.info(String
					.format("Leaving critical section with mutual exclusion for metadata %s",
							metadataId));
		}

		return response.build();
	}

	private void checkFreshness(String providedEtag, String metadataId)
			throws MetadataNotFoundException, InvalidEtagException {
		String storedEtag = TimeUtils
				.toRFC3339(Long.parseLong(ResourceDirectoryInterface
						.getTimeStamp(metadataId)));
		if (!StringUtils.equals(providedEtag, storedEtag))
			throw new InvalidEtagException(metadataId, providedEtag);

	}

	private void checkMdidSyntax(String metadataId)
			throws IncorrectMetadataKeySyntaxException {
		if (!MetadataKeySyntax.metadataIdIsCorrect(metadataId))
			throw new IncorrectMetadataKeySyntaxException(metadataId);

	}

	private void initializeStaticParameters() {
		apiscolInstanceName = getProperty(ParametersKeys.apiscolInstanceName,
				context);
		apiscolInstanceLabel = getProperty(ParametersKeys.apiscolInstanceLabel,
				context);
	}

	private void createSearchEngineQueryHandler(ServletContext context) {
		String solrAddress = getProperty(ParametersKeys.solrAddress, context);
		String solrSearchPath = getProperty(ParametersKeys.solrSearchPath,
				context);
		String solrUpdatePath = getProperty(ParametersKeys.solrUpdatePath,
				context);
		String solrExtractPath = getProperty(ParametersKeys.solrExtractPath,
				context);
		String solrSuggestPath = getProperty(ParametersKeys.solrSuggestPath,
				context);
		try {
			searchEngineFactory = AbstractSearchEngineFactory
					.getSearchEngineFactory(AbstractSearchEngineFactory.SearchEngineType.SOLRJ);
		} catch (Exception e) {
			e.printStackTrace();
		}
		searchEngineQueryHandler = searchEngineFactory.getQueryHandler(
				solrAddress, solrSearchPath, solrUpdatePath, solrExtractPath,
				solrSuggestPath);
	}

	@PUT
	@Path("/{mdid}/parts")
	@Produces({ MediaType.APPLICATION_ATOM_XML, MediaType.APPLICATION_XML })
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response defineRelations(@Context HttpServletRequest request,
			@PathParam(value = "mdid") final String packMetadataId,
			@FormParam(value = "packid") final String packId,
			@FormParam("mdids") String metadataIds)
			throws FileSystemAccessException,
			InvalidProvidedMetadataFileException, MetadataNotFoundException,
			InvalidEtagException, DBAccessException {
		String requestedFormat = request.getHeader(HttpHeaders.ACCEPT);
		java.lang.reflect.Type collectionType = new TypeToken<List<String>>() {
		}.getType();
		IEntitiesRepresentationBuilder<?> rb = EntitiesRepresentationBuilderFactory
				.getRepresentationBuilder(requestedFormat, context);
		KeyLock keyLock = null;
		ResponseBuilder response = null;
		try {
			keyLock = keyLockManager.getLock(KeyLockManager.GLOBAL_LOCK_KEY);
			keyLock.lock();
			try {
				logger.info(String
						.format("Passing through mutual exclusion for all the content service"));

				checkFreshness(request.getHeader(HttpHeaders.IF_MATCH),
						packMetadataId);
				List<String> metadataUriList = null;
				List<String> partsMetadataIds = new ArrayList<String>();
				String filePath = "";
				try {
					metadataUriList = new Gson().fromJson(metadataIds,
							collectionType);
				} catch (Exception e) {
					String message = String
							.format("The list of metadata %s is impossible to parse as JSON",
									metadataIds);
					logger.warn(message);
					metadataUriList = new ArrayList<String>();
				}
				for (Iterator<String> iterator = metadataUriList.iterator(); iterator
						.hasNext();) {
					String metadata = (String) iterator.next();
					if (!metadata.startsWith(uriInfo.getBaseUri().toString()))
						continue;
					String id = metadata.replaceAll(uriInfo.getBaseUri()
							.toString(), "");
					partsMetadataIds.add(id);
				}
				ResourceDirectoryInterface.setAggregationLevel(packMetadataId,
						2);
				List<String> relationstoBeRemovedIds = ResourceDirectoryInterface
						.addPartsToPackMetadata(packMetadataId, packId,
								partsMetadataIds, uriInfo);
				ResourceDirectoryInterface.renewJsonpFile(packMetadataId);
				updateMetadataEntryInDataBase(packMetadataId);
				filePath = ResourceDirectoryInterface
						.getFilePath(packMetadataId);
				try {
					searchEngineQueryHandler.processDeleteQuery(filePath);
					searchEngineQueryHandler.processAddQuery(filePath);
				} catch (SearchEngineCommunicationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SearchEngineErrorException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				for (Iterator<String> iterator = partsMetadataIds.iterator(); iterator
						.hasNext();) {
					String partMetadataId = (String) iterator.next();
					ResourceDirectoryInterface.renewJsonpFile(partMetadataId);
					updateMetadataEntryInDataBase(partMetadataId);
					filePath = ResourceDirectoryInterface
							.getFilePath(partMetadataId);
					try {
						searchEngineQueryHandler.processDeleteQuery(filePath);
						searchEngineQueryHandler.processAddQuery(filePath);
					} catch (SearchEngineErrorException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (SearchEngineCommunicationException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					ResourceDirectoryInterface.setAggregationLevel(
							partMetadataId, 1);
				}
				relationstoBeRemovedIds.removeAll(partsMetadataIds);
				for (Iterator<String> iterator = relationstoBeRemovedIds
						.iterator(); iterator.hasNext();) {
					String next = iterator.next();
					if (StringUtils.isEmpty(next))
						continue;
					refreshMetadata(next);
				}

				IResourceDataHandler resourceDataHandler = DBAccessFactory
						.getResourceDataHandler(DBTypes.mongoDB);
				Object representation = rb.getMetadataRepresentation(
						context.getRealPath(""), uriInfo, packMetadataId,
						packMetadataId, false,
						Collections.<String, String> emptyMap(),
						resourceDataHandler, requestedFormat);
				response = Response.status(Status.OK).entity(representation);

			} finally {
				keyLock.unlock();
			}
		} finally {
			if (keyLock != null) {
				keyLock.release();
			}
			logger.info(String
					.format("Leaving critical section with mutual exclusion for metadata %s",
							packMetadataId));
		}
		return response.build();
	}

	private void refreshMetadata(String relationToBeRemovedId)
			throws DBAccessException, MetadataNotFoundException {

		ResourceDirectoryInterface.renewJsonpFile(relationToBeRemovedId);
		updateMetadataEntryInDataBase(relationToBeRemovedId);
		String filePath = ResourceDirectoryInterface
				.getFilePath(relationToBeRemovedId);
		try {
			searchEngineQueryHandler.processDeleteQuery(filePath);
			searchEngineQueryHandler.processAddQuery(filePath);
		} catch (SearchEngineErrorException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SearchEngineCommunicationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
