package chav1961.da.xmlcrawler.inner;

public enum Parts {
	HEAD("head"),
	BODY("body"),
	TAIL("tail");
	
	private final String	partLabel;
	
	private Parts(final String partLabel) {
		this.partLabel = partLabel;
	}
	
	public String getPartLabel() {
		return partLabel;
	}
}