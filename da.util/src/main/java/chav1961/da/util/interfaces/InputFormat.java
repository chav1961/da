package chav1961.da.util.interfaces;

public enum InputFormat {
	RDF_XML("application/rdf+xml"), 
	TURTLE("text/turtle"),
	RDF_JSON("application/rdf+json"),
	N3("text/n3"),
	N_TRIPLES("application/n-triples"),
	N_QUADS("application/n-quads"),
	TRIG("application/trig");
	
	private final String	mime;
	
	InputFormat(final String mime) {
		this.mime = mime;
	}
	
	public String getMimeType() {
		return mime;
	}
}
