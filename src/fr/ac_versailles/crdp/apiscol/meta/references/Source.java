package fr.ac_versailles.crdp.apiscol.meta.references;

public enum Source {
	LOMV10("LOMv1.0"),SCOLOMFRv10("SCOLOMFRv1.0");
	private String value;

	private Source(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return value;
	}

}
