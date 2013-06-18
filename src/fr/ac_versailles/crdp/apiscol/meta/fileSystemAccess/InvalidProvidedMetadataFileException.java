package fr.ac_versailles.crdp.apiscol.meta.fileSystemAccess;

import fr.ac_versailles.crdp.apiscol.ApiscolException;

public class InvalidProvidedMetadataFileException extends ApiscolException {

	public InvalidProvidedMetadataFileException(String message) {
		super(message, true);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

}
