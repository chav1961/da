package chav1961.da.sender;

import chav1961.da.util.Constants;
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
			new BooleanArg(Constants.ARG_ZIP, false, "Parse input as *.zip format", false),
			new StringArg(Constants.ARG_EXCLUDE, false, false, "Skip input *.zip parts and remove then from output stream"),
			new StringArg(Constants.ARG_PROCESS, false, "Process the given parts in the input *.zip. If missing,all the partswill be processed", "*"),
			new URIArg(ARG_SERVER_URI, true, true, "RDF server to load content to"),
			new EnumArg<RdfFormat>(ARG_SERVER_FORMAT, RdfFormat.class, false, "RDF server format. If missing, SPARQL defaults", RdfFormat.SPARQL),
		};
		
		ApplicationArgParser() {
			super(KEYS);
		}
	}
}
