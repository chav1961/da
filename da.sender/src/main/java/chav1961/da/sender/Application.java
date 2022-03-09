package chav1961.da.sender;

import chav1961.da.util.interfaces.RdfFormat;
import chav1961.purelib.basic.ArgParser;
import chav1961.purelib.basic.exceptions.CommandLineParametersException;

public class Application {
	public static final String	ARG_SERVER_URI = "action";
	public static final String	ARG_SERVER_FORMAT = "repoUri";
	
	public static void main(final String[] args) {
		// TODO Auto-generated method stub
		try {final ArgParser		parser = new ApplicationArgParser().parse(args);
		
		} catch (CommandLineParametersException exc) {
			
		}
	}

	private static class ApplicationArgParser extends ArgParser {
		private static final ArgParser.AbstractArg[]	KEYS = {
			new URIArg(ARG_SERVER_URI, true, true, "RDF server to load content to"),
			new EnumArg<RdfFormat>(ARG_SERVER_FORMAT, RdfFormat.class, false, "RDF server format. If missing, SPARQL defaults", RdfFormat.SPARQL),
		};
		
		ApplicationArgParser() {
			super(KEYS);
		}
	}
}
