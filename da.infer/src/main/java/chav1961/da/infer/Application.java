package chav1961.da.infer;

import chav1961.da.util.interfaces.RdfFormat;
import chav1961.purelib.basic.ArgParser;
import chav1961.purelib.basic.exceptions.CommandLineParametersException;

public class Application {
	public static final String	ARG_RDF_URI = "rdfUri";
	public static final String	ARG_RDF_SERVER = "rdfServer";
	public static final String	ARG_RDF_FORMAT = "rdfFormat";

	
	public static void main(final String[] args) {
		// TODO Auto-generated method stub
		try {final ArgParser		parser = new ApplicationArgParser().parse(args);
		
		} catch (CommandLineParametersException exc) {
			
		}
	}

	private static class ApplicationArgParser extends ArgParser {
		private static final ArgParser.AbstractArg[]	KEYS = {
			new URIArg(ARG_RDF_URI, false, true, "RDF schemas location. Must points to directory"),
			new URIArg(ARG_RDF_URI, false, false, "external RDF server URI. When missing, local server will be used"),
			new EnumArg<RdfFormat>(ARG_RDF_FORMAT, RdfFormat.class, false, "external RDF server exchange format. When missing, SPARQL will be used", RdfFormat.SPARQL),
		};
		
		ApplicationArgParser() {
			super(KEYS);
		}
	}
}
