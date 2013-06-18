package fr.ac_versailles.crdp.apiscol.meta.representations;

import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import fr.ac_versailles.crdp.apiscol.database.DBAccessException;
import fr.ac_versailles.crdp.apiscol.meta.dataBaseAccess.IResourceDataHandler;
import fr.ac_versailles.crdp.apiscol.meta.fileSystemAccess.MetadataNotFoundException;
import fr.ac_versailles.crdp.apiscol.meta.searchEngine.ISearchEngineResultHandler;

public interface IEntitiesRepresentationBuilder<T> {

	MediaType getMediaType();

	String getMetadataDownloadUri(UriInfo uriInfo, String metadataId);

	T getMetadataSuccessfulDestructionReport(Object realPath, UriInfo uriInfo,
			String apiscolInstanceName, String metadataId, String warnings);

	T getSuccessfullOptimizationReport(String requestedFormat, UriInfo uriInfo);

	String getMetadataUri(UriInfo uriInfo, String metadataId);

	T getSuccessfulGlobalDeletionReport();

	T getSuccessfulRecoveryReport();

	String getMetadataJsonpDownloadUri(UriInfo uriInfo, String metadataId);

	T getMetadataSnippetRepresentation(String realPath, UriInfo uriInfo,
			String apiscolInstanceName, String metadataId, String version);

	String getMetadataSnippetUri(UriInfo uriInfo, String metadataId);

	T selectMetadataFollowingCriterium(String realPath, UriInfo uriInfo,
			String apiscolInstanceName, String apiscolInstanceLabel,
			ISearchEngineResultHandler handler, int start, int rows,
			boolean includeDescription,
			IResourceDataHandler resourceDataHandler, String editUri,
			String version) throws NumberFormatException, DBAccessException;

	T getMetadataRepresentation(String realPath, UriInfo uriInfo,
			String apiscolInstanceName, String resourceId,
			boolean includeDescription, Map<String, String> params,
			IResourceDataHandler resourceDataHandler, String editUri)
			throws MetadataNotFoundException, DBAccessException;

	T getCompleteMetadataListRepresentation(String realPath, UriInfo uriInfo,
			String apiscolInstanceName, String apiscolInstanceLabel, int start,
			int rows, boolean includeDescription,
			IResourceDataHandler resourceDataHandler, String editUri,
			String version) throws DBAccessException;

}
