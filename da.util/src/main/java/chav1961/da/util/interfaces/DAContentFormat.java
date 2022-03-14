package chav1961.da.util.interfaces;

public enum DAContentFormat {
	RDF_XML("application/rdf+xml", "xml"), 
	TURTLE("text/turtle", "turtle"),
	RDF_JSON("application/rdf+json", "json"),
	N3("text/n3", "n3"),
	N_TRIPLES("application/n-triples", "triples"),
	N_QUADS("application/n-quads", "quads"),
	TRIG("application/trig", "trig");
	
	private final String	mime;
	private final String	schema;
	
	DAContentFormat(final String mime, final String schema) {
		this.mime = mime;
		this.schema = schema;
	}
	
	public String getMimeType() {
		return mime;
	}
	
	public String getSchema() {
		return schema;
	}
}
