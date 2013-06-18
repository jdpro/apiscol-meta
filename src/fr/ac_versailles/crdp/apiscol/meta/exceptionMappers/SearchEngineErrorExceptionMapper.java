package fr.ac_versailles.crdp.apiscol.meta.exceptionMappers;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import fr.ac_versailles.crdp.apiscol.meta.searchEngine.SearchEngineErrorException;

@Provider
public class SearchEngineErrorExceptionMapper implements
		ExceptionMapper<SearchEngineErrorException> {
	@Override
	public Response toResponse(SearchEngineErrorException e) {
		return Response.status(Status.BAD_REQUEST)
				.type(MediaType.APPLICATION_XML).entity(e.getXMLMessage())
				.build();
	}
}