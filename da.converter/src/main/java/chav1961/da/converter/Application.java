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

import org.eclipse.rdf4j.common.lang.FileFormat;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;

import chav1961.da.util.AbstractZipProcessor;
import chav1961.da.util.Constants;
import chav1961.da.util.DAUtils;
import chav1961.da.util.interfaces.ContentFormat;
import chav1961.purelib.basic.ArgParser;
import chav1961.purelib.basic.PureLibSettings;
import chav1961.purelib.basic.SubstitutableProperties;
import chav1961.purelib.basic.Utils;
import chav1961.purelib.basic.exceptions.CommandLineParametersException;
import chav1961.purelib.basic.exceptions.ContentException;
import chav1961.purelib.basic.exceptions.SyntaxException;
import chav1961.purelib.basic.interfaces.LoggerFacade;
import chav1961.purelib.basic.interfaces.LoggerFacade.Severity;

/**
 * <p>This class is a main class to use as RDF data converter in Data Acquisition pipe. The class is a middle or terminal element in the Data Acquisition pipe.
 * To convert RDF part content type, use following parameters:</p>
 * <ul>
 * <li> -if &lt;content_type&gt; to type source format. If the parameter is missing, {@linkplain} Constants#PART_KEY_CONTENT_TYPE} parameter from {@value Constants#PART_TICKET}
 * part will be used
 * <li> -if &lt;content_type&gt; to type destination format.
 * </ul>
 * <p>This class also supported all standard command line keys:</p>
 * <ul>
 * <li> -process &lt;file_regex&gt; to process some files in Data Acquisition pipe
 * <li> -pass &lt;file_regex&gt; to skip processing some files in Data Acquisition pipe
 * <li> -remove &lt;file_regex&gt; to remove some files from Data Acquisition pipe
 * <li> -rename &lt;name_regex->new_name&gt; to rename some files in Data Acquisition pipe
 * <li> -debug turn on debug trace to the System.err stream
 * </ul>
 * <p>Source stream for the pipe element is the System.in, destination stream for the pipe element is System.out. Both the streams must contain Data Acquisition pipe content.</p>  
 *  
 * @author Alexander Chernomyrdin aka chav1961
 * @since 0.0.1
 */
public class Application extends AbstractZipProcessor {
	public static final String	ARG_INPUT_FORMAT = "if";
	public static final String	ARG_OUTPUT_FORMAT = "of";

	private final ContentFormat		fromFormat; 
	private final RDFFormat			fromNative; 
	private final ContentFormat		toFormat; 
	private final RDFFormat			toNative;
	private final PrintStream		ps;
	private final boolean			debug;
	
	Application(final String[] processMask, final String[] passMask, final String[] removeMask, final String[][] renameMask, final ContentFormat from, final ContentFormat to, final PrintStream ps, final boolean debug) throws SyntaxException {
		super(processMask, passMask, removeMask, renameMask);
		this.fromFormat = from;
		this.fromNative = toRdfFormat(from);
		this.toFormat = to;
		this.toNative = toRdfFormat(to);
		this.ps = ps;
		this.debug = debug;
	}

	@Override
	protected void processTicket(final SubstitutableProperties props, final LoggerFacade logger) throws IOException {
		if (props.getProperty(Constants.PART_KEY_CONTENT_TYPE, ContentFormat.class) != fromFormat) {
			logger.message(Severity.warning, 
					"Content type [%1$s] from %2$s is differ than typed in the command string. [%3$s] will be used",
					props.getProperty(Constants.PART_KEY_CONTENT_TYPE, ContentFormat.class),
					Constants.PART_TICKET,
					fromFormat);
		}
		super.processTicket(props, logger);
		props.setProperty(Constants.PART_KEY_CONTENT_TYPE, toFormat.name());
	}
	
	@Override
	protected void processPart(final String part, final SubstitutableProperties props, final LoggerFacade logger, final InputStream source, final OutputStream target) throws IOException {
		if (fromFormat == toFormat) {
			Utils.copyStream(source, target);
		}
		else {
			final Reader	rdr = new InputStreamReader(source, PureLibSettings.DEFAULT_CONTENT_ENCODING);
			final RDFParser parser = Rio.createParser(fromNative);
			final Writer	wr = new OutputStreamWriter(target, PureLibSettings.DEFAULT_CONTENT_ENCODING);
			final RDFWriter writer = Rio.createWriter(toNative, wr);
			
			if (debug) {
				message(ps, "Processing part ["+part+"]...", part);
			}
			
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
										parser.isTyped(Constants.ARG_PROCESS) ? new String[] {parser.getValue(Constants.ARG_PROCESS, String.class)} : Constants.MASK_NONE ,
										parser.isTyped(Constants.ARG_PASS) ? new String[] {parser.getValue(Constants.ARG_PASS, String.class)} : Constants.MASK_NONE ,
										parser.isTyped(Constants.ARG_REMOVE) ? new String[] {parser.getValue(Constants.ARG_REMOVE, String.class)} : Constants.MASK_NONE ,
										parser.isTyped(Constants.ARG_RENAME) ? DAUtils.parseRenameArgument(parser.getValue(Constants.ARG_RENAME, String.class)) : new String[0][],
										parser.getValue(ARG_INPUT_FORMAT, ContentFormat.class),
										parser.getValue(ARG_OUTPUT_FORMAT, ContentFormat.class),
										err, 
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

	private static RDFFormat toRdfFormat(final ContentFormat format) throws SyntaxException {
		try{
			return (RDFFormat) RDFFormat.class.getField(format.getNativeName()).get(null);
		} catch (IllegalAccessException | NoSuchFieldException | SecurityException e) {
			throw new SyntaxException(0, 0, "Unknown format ["+format+"]");
		}
	}
	
	private static void message(final PrintStream ps, final String format, final Object... parameters) {
		ps.println("da.converter: "+String.format(format, parameters));
		ps.flush();
	}
	
	private static class ApplicationArgParser extends ArgParser {
		private static final ArgParser.AbstractArg[]	KEYS = {
			new BooleanArg(Constants.ARG_DEBUG, false, "Turn on debug trace", false),
			new PatternArg(Constants.ARG_PROCESS, false, "Process the given parts in the input *.zip. Types as pattern[,...]. If missing, all the parts will be processed. Mutually exclusive with "+Constants.ARG_PASS+" argument", ".*"),
			new PatternArg(Constants.ARG_PASS, false, "Pass the given parts in the input *.zip without processing. Types as pattern[,...]. If missing, all the parts will be processed. Mutually exclusive with "+Constants.ARG_PROCESS+" argument", ""),
			new PatternArg(Constants.ARG_REMOVE, false, false, "Remove entries from the *.zip input. Types as pattern[,...]. This option is processed AFTER processing/passing part"),
			new StringArg(Constants.ARG_RENAME, false, false, "Rename entries in the *.zip input. Types as pattern->template[;...], see Java Pattern syntax and Java Mather.replaceAll(...) description. This option is processed AFTER processing/passing part"),
			new EnumArg<ContentFormat>(ARG_INPUT_FORMAT, ContentFormat.class, false, false, "Input format. When typed, replaces 'ticket.txt' key before conversion"),
			new EnumArg<ContentFormat>(ARG_OUTPUT_FORMAT, ContentFormat.class, false, false, "Output format. When typed, replaces 'ticket.txt' key after conversion'."),
		};
		
		ApplicationArgParser() {
			super(KEYS);
		}
	}
}
