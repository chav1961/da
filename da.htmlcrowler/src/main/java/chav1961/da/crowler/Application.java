package chav1961.da.crowler;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import chav1961.da.crowler.Application.ApplicationArgParser;
import chav1961.da.util.AbstractZipProcessor;
import chav1961.da.util.Constants;
import chav1961.da.util.DAUtils;
import chav1961.purelib.basic.ArgParser;
import chav1961.purelib.basic.PureLibSettings;
import chav1961.purelib.basic.SubstitutableProperties;
import chav1961.purelib.basic.Utils;
import chav1961.purelib.basic.exceptions.CommandLineParametersException;
import chav1961.purelib.basic.exceptions.SyntaxException;
import chav1961.purelib.basic.interfaces.LoggerFacade;

public class Application extends AbstractZipProcessor {
	public static final String	ARG_URI = "uri";
	public static final String	ARG_ROBOTS = "robots";
	public static final String	ARG_SITEMAP = "sitemap";
	
	private static final String	FILE_ROBOTS = "robots.txt";
	private static final String	FILE_SITEMAP = "sitemap.xml";
	

	private final String[]		toProcess;
	private final URI			siteAddress;
	private final PrintStream	err;
	private final String		robots;
	private final String		sitemap;
	private final boolean		debug;
	
	protected Application(final String[] processMask, final String[] removeMask, final String[][] renameMask, final URI siteAddress, final PrintStream err, final String robots, final String sitemap, final boolean debug) throws SyntaxException, IllegalArgumentException {
		super(Constants.MASK_NONE, Constants.MASK_ANY, removeMask, renameMask);
		this.toProcess = processMask;
		this.siteAddress = siteAddress;
		this.err = err;
		this.robots = robots;
		this.sitemap = sitemap;
		this.debug = debug;
	}

	@Override
	protected void processPart(final String part, final SubstitutableProperties props, final LoggerFacade logger, final InputStream source, final OutputStream target) throws IOException {
		Utils.copyStream(source, target);
	}
	
	@Override
	protected void processAppending(final SubstitutableProperties props, final LoggerFacade logger, final OutputStream target) throws IOException {
		super.processAppending(props, logger, target);
	}

	public static void main(final String[] args) {
		System.exit(main(System.in, args, System.out, System.err));
	}

	public static int main(final InputStream is, final String[] args, final OutputStream os, final PrintStream err) {
		final ArgParser				parserTemplate = new ApplicationArgParser();
		
		try{final ArgParser			parser = parserTemplate.parse(args);
			final boolean			debug = parser.getValue(Constants.ARG_DEBUG, boolean.class);
			final URI				uri = parser.getValue(ARG_ROBOTS, URI.class);
			final long 				startTime = System.currentTimeMillis();
			
			message(err, "Preload settings from [%1$s]...", uri);
			
			final String			robots = Utils.fromResource((parser.isTyped(ARG_ROBOTS) ? parser.getValue(ARG_ROBOTS, URI.class) : uri.resolve(FILE_ROBOTS)).toURL());
			final String			sitemap = Utils.fromResource((parser.isTyped(ARG_SITEMAP) ? parser.getValue(ARG_SITEMAP, URI.class) : uri.resolve(FILE_SITEMAP)).toURL());
			final Application		app = new Application(											
										parser.isTyped(Constants.ARG_PROCESS) ? new String[] {parser.getValue(Constants.ARG_PROCESS, String.class)} : Constants.MASK_ANY ,
										parser.isTyped(Constants.ARG_REMOVE) ? new String[] {parser.getValue(Constants.ARG_REMOVE, String.class)} : Constants.MASK_NONE ,
										parser.isTyped(Constants.ARG_RENAME) ? DAUtils.parseRenameArgument(parser.getValue(Constants.ARG_RENAME, String.class)) : new String[0][],
										uri,
										err,
										robots,
										sitemap,
										debug
									);
			try(final ZipInputStream	zis = new ZipInputStream(is);
				final ZipOutputStream	zos = new ZipOutputStream(os)) {
			
				message(err, "Processing standard input...");
				app.process(zis, zos);
				zos.finish();
			}
			message(err, "Processing terminated, duration=%1$d msec", (System.currentTimeMillis() - startTime));
			return 0;
		} catch (CommandLineParametersException exc) {
			err.println(exc.getLocalizedMessage());
			err.println(parserTemplate.getUsage("da.htmlcrowler"));
			return 128;
		} catch (IOException | SyntaxException exc) {
			exc.printStackTrace(err);
			return 129;
		}
	}
	
	private static void message(final PrintStream ps, final String format, final Object... parameters) {
		ps.println("da.htmlcrowler: "+String.format(format, parameters));
	}
	
	static class ApplicationArgParser extends ArgParser {
		private static final ArgParser.AbstractArg[]	KEYS = {
			new BooleanArg(Constants.ARG_DEBUG, false, "Turn on debug trace", false),
			new PatternArg(Constants.ARG_PROCESS, false, "Load the given files from site. Types as pattern[,...]. If missing, all the files will be loaded.", ".*"),
			new PatternArg(Constants.ARG_REMOVE, false, false, "Remove entries from the *.zip input. Types as pattern[,...]. This option is processed AFTER processing/passing part"),
			new StringArg(Constants.ARG_RENAME, false, false, "Rename entries in the *.zip input. Types as pattern->template[;...], see Java Pattern syntax and Java Mather.replaceAll(...) description. This option is processed AFTER processing/passing part"),
			new URIArg(ARG_ROBOTS, false, false, "Emulation of robots.txt file content. If mising, robots.txt from the site will be used"),
			new URIArg(ARG_SITEMAP, false, false, "Emulation of sitemap.xml file content. If missing, sitemap.xml from the site will be used"),
			new URIArg(ARG_URI, true, true, "URI to crowl site"),
		};
		
		ApplicationArgParser() {
			super(KEYS);
		}
	}
}
