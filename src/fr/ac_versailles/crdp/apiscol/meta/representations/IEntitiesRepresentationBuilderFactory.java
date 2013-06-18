package fr.ac_versailles.crdp.apiscol.meta.representations;

import javax.servlet.ServletContext;

public interface IEntitiesRepresentationBuilderFactory {

	IEntitiesRepresentationBuilder<?> getRepresentationBuilder(
			String requestedFormat, ServletContext context);

}
