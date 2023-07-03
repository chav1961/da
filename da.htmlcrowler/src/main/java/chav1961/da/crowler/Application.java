package chav1961.da.crowler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import chav1961.da.util.AbstractZipProcessor;
import chav1961.da.util.Constants;
import chav1961.purelib.basic.ArgParser;
import chav1961.purelib.basic.SubstitutableProperties;
import chav1961.purelib.basic.exceptions.SyntaxException;
import chav1961.purelib.basic.interfaces.LoggerFacade;

public class Application extends AbstractZipProcessor {
	public static final String	ARG_URIS = "uris";
	public static final String	ARG_CONF = "conf";

	protected Application(final String[] processMask, final String[] removeMask, final String[][] renameMask) throws SyntaxException, IllegalArgumentException {
		super(Constants.MASK_NONE, Constants.MASK_ANY, removeMask, renameMask);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void processPart(String part, SubstitutableProperties props, LoggerFacade logger, InputStream source, OutputStream target) throws IOException {
		// TODO Auto-generated method stub
		
	}

	public static void main(final String[] args) {
		System.exit(main(System.in, args, System.out, System.err));
	}

	public static int main(final InputStream is, final String[] args, final OutputStream os, final PrintStream ps) {
		return 0;
	}
	
	
	static class ApplicationArgParser extends ArgParser {
		private static final ArgParser.AbstractArg[]	KEYS = {
			new BooleanArg(Constants.ARG_DEBUG, false, "Turn on debug trace", false),
			new PatternArg(Constants.ARG_PROCESS, false, "Load the given files from site. Types as pattern[,...]. If missing, all the files will be loaded.", ".*"),
			new PatternArg(Constants.ARG_REMOVE, false, false, "Remove entries from the *.zip input. Types as pattern[,...]. This option is processed AFTER processing/passing part"),
			new StringArg(Constants.ARG_RENAME, false, false, "Rename entries in the *.zip input. Types as pattern->template[;...], see Java Pattern syntax and Java Mather.replaceAll(...) description. This option is processed AFTER processing/passing part"),
			new ConfigArg(ARG_CONF, false, false, "Configuration file reference."),
			new StringListArg(ARG_URIS, false, true, "URIs to crowl site(s)"),
		};
		
		ApplicationArgParser() {
			super(KEYS);
		}
	}
}
