package chav1961.da.converter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import chav1961.da.util.AbstractZipProcessor;
import chav1961.da.util.Constants;
import chav1961.da.util.interfaces.DAContentFormat;
import chav1961.purelib.basic.ArgParser;
import chav1961.purelib.basic.SubstitutableProperties;
import chav1961.purelib.basic.exceptions.CommandLineParametersException;
import chav1961.purelib.basic.exceptions.SyntaxException;
import chav1961.purelib.basic.interfaces.LoggerFacade;

public class Application extends AbstractZipProcessor {
	public static final String	ARG_INPUT_FORMAT = "if";
	public static final String	ARG_OUTPUT_FORMAT = "of";
	
	public Application(final String[] processMask, final String[] passMask, final String[] removeMask, final String[][] renameMask) throws SyntaxException {
		super(processMask, passMask, removeMask, renameMask);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void processTicket(final SubstitutableProperties props, final LoggerFacade logger) throws IOException {
		// TODO Auto-generated method stub
		super.processTicket(props, logger);
	}
	
	@Override
	protected void processPart(final String part, final SubstitutableProperties props, final LoggerFacade logger, final InputStream source, final OutputStream target) throws IOException {
		// TODO Auto-generated method stub
		
	}
	
	public static void main(final String[] args) {
		final ArgParser	parserTemplate = new ApplicationArgParser();
		
		try{final ArgParser		parser = parserTemplate.parse(args);

		} catch (CommandLineParametersException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static class ApplicationArgParser extends ArgParser {
		private static final ArgParser.AbstractArg[]	KEYS = {
			new BooleanArg(Constants.ARG_DEBUG, false, "Turn on debug trace", false),
			new StringArg(Constants.ARG_PROCESS, false, "Process the given parts in the input *.zip. Types as pattern[,...]. If missing, all the parts will be processed. Mutually exclusive with "+Constants.ARG_PASS+" argument", ".*"),
			new StringArg(Constants.ARG_PASS, false, "Pass the given parts in the input *.zip without processing. Types as pattern[,...]. If missing, all the parts will be processed. Mutually exclusive with "+Constants.ARG_PROCESS+" argument", ""),
			new StringArg(Constants.ARG_REMOVE, false, false, "Remove entries from the *.zip input. Types as pattern[,...]. This option is processed AFTER processing/passing part"),
			new StringArg(Constants.ARG_RENAME, false, false, "Rename entries in the *.zip input. Types as pattern->template[;...], see Java Pattern syntax and Java Mather.replaceAll(...) description. This option is processed AFTER processing/passing part"),
			new EnumArg<DAContentFormat>(ARG_INPUT_FORMAT, DAContentFormat.class, false, false, "Input format. When typed, replaces 'ticket.txt' key before conversion"),
			new EnumArg<DAContentFormat>(ARG_OUTPUT_FORMAT, DAContentFormat.class, false, false, "Output format. When typed, replaces 'ticket.txt' key after conversion'."),
		};
		
		ApplicationArgParser() {
			super(KEYS);
		}
	}
}
