package fr.ac_versailles.crdp.apiscol.meta;

import fr.ac_versailles.crdp.apiscol.ApiscolException;

public class DeletionNotAllowedException extends ApiscolException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public DeletionNotAllowedException(String metadataId, String packMedatadaId) {
		super(
				String.format(
						"The metadata %s belongs to a the manifest %s, it cannot be deleted.",
						metadataId, packMedatadaId));
	}

}
