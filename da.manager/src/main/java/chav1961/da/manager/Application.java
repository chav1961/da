package chav1961.da.manager;

import chav1961.purelib.basic.ArgParser;
import chav1961.purelib.basic.exceptions.CommandLineParametersException;

public class Application {
	public static final String	ARG_SERVER_CONFIG = "config";
	public static final String	ARG_LISTENER_PORT = "port";
	
	public static void main(final String[] args) {
		// TODO Auto-generated method stub
		try {final ArgParser		parser = new ApplicationArgParser().parse(args);
		
		} catch (CommandLineParametersException exc) {
			
		}
	}

	private static class ApplicationArgParser extends ArgParser {
		private static final ArgParser.AbstractArg[]	KEYS = {
			new ConfigArg(ARG_SERVER_CONFIG, true, true, "Configuration file"),
			new IntegerArg(ARG_LISTENER_PORT, false, "Listener port. If missing, 12500 will be used", 12500),
		};
		
		ApplicationArgParser() {
			super(KEYS);
		}
	}
}
