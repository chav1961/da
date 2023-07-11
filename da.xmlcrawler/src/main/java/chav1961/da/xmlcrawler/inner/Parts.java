package chav1961.da.xmlcrawler.inner;

import chav1961.da.xmlcrawler.OldApplication;

public enum Parts {
	HEAD(OldApplication.KEY_HEAD),
	BODY(OldApplication.KEY_BODY),
	TAIL(OldApplication.KEY_TAIL);
	
	private final String	partLabel;
	
	private Parts(final String partLabel) {
		this.partLabel = partLabel;
	}
	
	public String getPartLabel() {
		return partLabel;
	}
}