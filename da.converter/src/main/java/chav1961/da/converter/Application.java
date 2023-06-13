package chav1961.da.converter;


import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Reader;
import java.io.Writer;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.openrdf.model.Statement;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.Rio;
import org.openrdf.rio.RDFFormat;

import chav1961.da.util.AbstractZipProcessor;
import chav1961.da.util.Constants;
import chav1961.da.util.DAUtils;
import chav1961.da.util.interfaces.DAContentFormat;
import chav1961.purelib.basic.ArgParser;
import chav1961.purelib.basic.PureLibSettings;
import chav1961.purelib.basic.SubstitutableProperties;
import chav1961.purelib.basic.Utils;
import chav1961.purelib.basic.exceptions.CommandLineParametersException;
import chav1961.purelib.basic.exceptions.ContentException;
import chav1961.purelib.basic.exceptions.SyntaxException;
import chav1961.purelib.basic.interfaces.LoggerFacade;
import chav1961.purelib.basic.interfaces.LoggerFacade.Severity;

public class Application extends AbstractZipProcessor {
	public static final String	ARG_INPUT_FORMAT = "if";
	public static final String	ARG_OUTPUT_FORMAT = "of";
	
	private final RDFFormat			fromFormat; 
	private final RDFFormat			toFormat;
	private final PrintStream		ps;
	private final boolean			debug;
	
	Application(final String[] processMask, final String[] passMask, final String[] removeMask, final String[][] renameMask, final DAContentFormat from, final DAContentFormat to, final PrintStream ps, final boolean debug) throws SyntaxException {
		super(processMask, passMask, removeMask, renameMask);
		this.fromFormat = toRdfFormat(from);
		this.toFormat = toRdfFormat(to);
		this.ps = ps;
		this.debug = debug;
	}

	@Override
	protected void processTicket(final SubstitutableProperties props, final LoggerFacade logger) throws IOException {
		try {
			if (toRdfFormat(props.getProperty(Constants.PART_KEY_CONTENT_TYPE, DAContentFormat.class)) != fromFormat) {
				logger.message(Severity.warning, "Content type [%1$s] from %2$s is differ than typed in the command string. [%3$s] will be used",
						props.getProperty(Constants.PART_KEY_CONTENT_TYPE, DAContentFormat.class),
						Constants.PART_TICKET,
						fromFormat);
			}
			super.processTicket(props, logger);
		} catch (SyntaxException e) {
			throw new IOException(e); 
		}
	}
	
	@Override
	protected void processPart(final String part, final SubstitutableProperties props, final LoggerFacade logger, final InputStream source, final OutputStream target) throws IOException {
		if (fromFormat == toFormat) {
			Utils.copyStream(source, target);
		}
		else {
			final Reader	rdr = new InputStreamReader(source, PureLibSettings.DEFAULT_CONTENT_ENCODING);
			final RDFParser parser = Rio.createParser(fromFormat);
			final Writer	wr = new OutputStreamWriter(target, PureLibSettings.DEFAULT_CONTENT_ENCODING);
			final RDFWriter writer = Rio.createWriter(toFormat, wr);
			
			parser.setRDFHandler(new RDFHandler() {
				@Override
				public void startRDF() throws RDFHandlerException {
					writer.startRDF();
				}
				
				@Override
				public void handleStatement(final Statement st) throws RDFHandlerException {
					writer.handleStatement(st);
				}
				
				@Override
				public void handleNamespace(final String prefix, final String uri) throws RDFHandlerException {
					writer.handleNamespace(prefix, uri);
				}
				
				@Override
				public void handleComment(final String comment) throws RDFHandlerException {
					writer.handleComment(comment);
				}
				
				@Override
				public void endRDF() throws RDFHandlerException {
					writer.endRDF();
				}
			}).parse(rdr, "");
			wr.flush();
		}
	}

	public static void main(final String[] args) {
		System.exit(main(System.in, args, System.out, System.err));
	}
	
	public static int main(final InputStream is, final String[] args, final OutputStream os, final PrintStream err) {
		final ArgParser	parserTemplate = new ApplicationArgParser();
		
		try{final ArgParser		parser = parserTemplate.parse(args);
			final boolean		debug = parser.getValue(Constants.ARG_DEBUG, boolean.class);
			final Application	app = new Application(
										parser.isTyped(Constants.ARG_REMOVE) ? new String[] {parser.getValue(Constants.ARG_REMOVE, String.class)} : new String[0] ,
										parser.isTyped(Constants.ARG_REMOVE) ? new String[] {parser.getValue(Constants.ARG_REMOVE, String.class)} : new String[0] ,
										parser.isTyped(Constants.ARG_REMOVE) ? new String[] {parser.getValue(Constants.ARG_REMOVE, String.class)} : new String[0] ,
										parser.isTyped(Constants.ARG_RENAME) ? DAUtils.parseRenameArgument(parser.getValue(Constants.ARG_RENAME, String.class)) : new String[0][],
										parser.getValue(ARG_INPUT_FORMAT, DAContentFormat.class),
										parser.getValue(ARG_OUTPUT_FORMAT, DAContentFormat.class),
										System.err, 
										debug);
			
			final long	startTime=  System.currentTimeMillis();
			try(final ZipInputStream	zis = new ZipInputStream(is);
				final ZipOutputStream	zos = new ZipOutputStream(os)) {
				
				message(err, "Process standard input...");
				app.process(zis, zos);
				zos.finish();
			}
			message(err, "Processing terminated, duration=%1$d msec", System.currentTimeMillis() - startTime);
			return 0;
		} catch (CommandLineParametersException exc) {
			err.println(exc.getLocalizedMessage());
			err.println(parserTemplate.getUsage("da.converter"));
			return 128;
		} catch (IOException | ContentException exc) {
			exc.printStackTrace(err);
			return 129;
		}
	}

	private static RDFFormat toRdfFormat(final DAContentFormat format) throws SyntaxException {
		try{
			return (RDFFormat) RDFFormat.class.getField(format.name()).get(null);
		} catch (IllegalAccessException | NoSuchFieldException | SecurityException e) {
			throw new SyntaxException(0, 0, "Unknown format ["+format+"]");
		}
	}
	
	private static void message(final PrintStream ps, final String format, final Object... parameters) {
		ps.println("da.converter: "+String.format(format, parameters));
	}
	
	private static class ApplicationArgParser extends ArgParser {
		private static final ArgParser.AbstractArg[]	KEYS = {
			new BooleanArg(Constants.ARG_DEBUG, false, "Turn on debug trace", false),
			new PatternArg(Constants.ARG_PROCESS, false, "Process the given parts in the input *.zip. Types as pattern[,...]. If missing, all the parts will be processed. Mutually exclusive with "+Constants.ARG_PASS+" argument", ".*"),
			new PatternArg(Constants.ARG_PASS, false, "Pass the given parts in the input *.zip without processing. Types as pattern[,...]. If missing, all the parts will be processed. Mutually exclusive with "+Constants.ARG_PROCESS+" argument", ""),
			new PatternArg(Constants.ARG_REMOVE, false, false, "Remove entries from the *.zip input. Types as pattern[,...]. This option is processed AFTER processing/passing part"),
			new StringArg(Constants.ARG_RENAME, false, false, "Rename entries in the *.zip input. Types as pattern->template[;...], see Java Pattern syntax and Java Mather.replaceAll(...) description. This option is processed AFTER processing/passing part"),
			new EnumArg<DAContentFormat>(ARG_INPUT_FORMAT, DAContentFormat.class, false, false, "Input format. When typed, replaces 'ticket.txt' key before conversion"),
			new EnumArg<DAContentFormat>(ARG_OUTPUT_FORMAT, DAContentFormat.class, false, false, "Output format. When typed, replaces 'ticket.txt' key after conversion'."),
		};
		
		ApplicationArgParser() {
			super(KEYS);
		}
	}
}
