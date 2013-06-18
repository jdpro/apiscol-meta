package fr.ac_versailles.crdp.apiscol.meta.exceptionMappers;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import fr.ac_versailles.crdp.apiscol.meta.fileSystemAccess.InvalidProvidedMetadataFileException;

@Provider
public class InvalidProvidedMetadataFileExceptionMapper implements
		ExceptionMapper<InvalidProvidedMetadataFileException> {
	@Override
	public Response toResponse(InvalidProvidedMetadataFileException e) {
		return Response.status(Status.BAD_REQUEST)
				.type(MediaType.APPLICATION_XML).entity(e.getXMLMessage())
				.build();
	}
}