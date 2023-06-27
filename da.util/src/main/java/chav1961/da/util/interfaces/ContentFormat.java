package chav1961.da.util.interfaces;

/**
 * <p>This enumerations describes Data Acquisition pipe RDF content type</p>   
 * @author Alexander Chernomyrdin aka chav1961
 * @since 0.0.1
 */
public enum ContentFormat {
	RDFXML("RDFXML"),
	NTRIPLES("NTRIPLES"),
	TURTLE("TURTLE"),
	N3("N3"),
	TRIX("TRIX"),
	TRIG("TRIG"),
	BINARY("BINARY"),
	NQUADS("NQUADS"),
	JSONLD("JSONLD"),
	RDFA("RDFA");
	
	private final String	nativeName;
	
	private ContentFormat(final String nativeName) {
		this.nativeName = nativeName;
	}

	public String getNativeName() {
		return nativeName;
	}
}
