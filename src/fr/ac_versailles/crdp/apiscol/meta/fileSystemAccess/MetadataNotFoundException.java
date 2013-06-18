package fr.ac_versailles.crdp.apiscol.meta.fileSystemAccess;

import fr.ac_versailles.crdp.apiscol.ApiscolException;

public class MetadataNotFoundException extends ApiscolException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public MetadataNotFoundException(String metadataId) {
		super(String.format("No file was found for the metadata id %s.", metadataId));
	}

	

}
