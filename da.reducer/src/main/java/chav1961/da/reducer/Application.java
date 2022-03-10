package chav1961.da.reducer;

import chav1961.da.util.Constants;
import chav1961.purelib.basic.ArgParser;
import chav1961.purelib.basic.exceptions.CommandLineParametersException;

public class Application {
	public static final String	ARG_ACTION = "action";
	public static final String	ARG_REPO_URI = "repoUri";
	
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
			new EnumArg<Action>(ARG_ACTION, Action.class, true, true, "Action"),
			new URIArg(ARG_REPO_URI, false, true, "version repository"),
		};
		
		ApplicationArgParser() {
			super(KEYS);
		}
	}
}
