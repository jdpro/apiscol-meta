package fr.ac_versailles.crdp.apiscol.meta.representations;

import javax.servlet.ServletContext;
import javax.ws.rs.core.MediaType;

import fr.ac_versailles.crdp.apiscol.CustomMediaType;

public class EntitiesRepresentationBuilderFactory {

	public static IEntitiesRepresentationBuilder<?> getRepresentationBuilder(
			String requestedFormat, ServletContext context) {
		if (requestedFormat.equals(MediaType.APPLICATION_XML)
				|| requestedFormat.equals(MediaType.APPLICATION_ATOM_XML)) {
			return new XMLRepresentationBuilder();
		} else if (requestedFormat.equals(CustomMediaType.JSONP.toString())) {
			return new JSonPRepresentationBuilder();
		} else if (requestedFormat.contains(MediaType.APPLICATION_XHTML_XML)
				|| requestedFormat.contains(MediaType.TEXT_HTML)) {
			return new XHTMLRepresentationBuilder();
		}
		throw new UnknownMediaTypeForResponseException(requestedFormat);
	}

}
