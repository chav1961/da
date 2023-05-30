package chav1961.da.util.interfaces;

public enum DAContentFormat {
	RDF_XML("application/rdf+xml", "xml", ""), 
	TURTLE("text/turtle", "turtle", "ttf"),
	RDF_JSON("application/rdf+json", "json", "json"),
	N3("text/n3", "n3", "n3"),
	N_TRIPLES("application/n-triples", "triples", "nt"),
	N_QUADS("application/n-quads", "quads", ""),
	TRIG("application/trig", "trig", "");
	
	private final String	mime;
	private final String	schema;
	private final String	extension;
	
	DAContentFormat(final String mime, final String schema, final String extension) {
		this.mime = mime;
		this.schema = schema;
		this.extension = extension;
	}
	
	public String getMimeType() {
		return mime;
	}
	
	public String getSchema() {
		return schema;
	}
	
	public String getExtension() {
		return extension;
	}
}
