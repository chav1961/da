package chav1961.da.source;

import chav1961.da.util.Constants;
import chav1961.purelib.basic.ArgParser;
import chav1961.purelib.basic.exceptions.CommandLineParametersException;

public class Application {
	public static final String	ARG_START_PIPE = "startPipe";
	public static final String	ARG_APPEND = "append";
	
	public static void main(final String[] args) {
		// TODO Auto-generated method stub
		try {final ArgParser		parser = new ApplicationArgParser().parse(args);
		
		} catch (CommandLineParametersException exc) {
			
		}
	}

	private static class ApplicationArgParser extends ArgParser {
		private static final ArgParser.AbstractArg[]	KEYS = {
			new BooleanArg(Constants.ARG_ZIP, false, "Parse input as *.zip format", false),
			new StringArg(Constants.ARG_SKIP, false, false, "Skip input *.zip parts and remove then from output stream"),
			new StringArg(Constants.ARG_PROCESS, false, "Process the given parts in the input *.zip. If missing,all the partswill be processed", "*"),
			new BooleanArg(ARG_START_PIPE, false, "Start pipe. Input must be content of the ["+Constants.PART_TICKET+"] in this case", false),
			new StringListArg(ARG_APPEND, false, false, "Append content to input stream in the input *.zip. Refs can be any valid URIs"),
		};
		
		ApplicationArgParser() {
			super(KEYS);
		}
	}
}
