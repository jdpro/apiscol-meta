package fr.ac_versailles.crdp.apiscol.meta;

import fr.ac_versailles.crdp.apiscol.ApiscolException;


public class IncorrectMetadataKeySyntaxException extends ApiscolException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public IncorrectMetadataKeySyntaxException(String resourceId) {
		super(String.format(
				"%s is not the valid syntax for apiscol content identifiers .",
				resourceId));
	}

}
