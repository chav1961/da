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

import chav1961.da.util.AbstractZipProcessor;
import chav1961.da.util.Constants;
import chav1961.da.util.DAUtils;
import chav1961.purelib.basic.ArgParser;
import chav1961.purelib.basic.PureLibSettings;
import chav1961.purelib.basic.SubstitutableProperties;
import chav1961.purelib.basic.Utils;
import chav1961.purelib.basic.exceptions.CommandLineParametersException;
import chav1961.purelib.basic.exceptions.ContentException;
import chav1961.purelib.basic.exceptions.SyntaxException;
import chav1961.purelib.basic.interfaces.LoggerFacade;
import chav1961.purelib.enumerations.ContinueMode;
import chav1961.purelib.fsys.FileSystemFactory;
import chav1961.purelib.fsys.interfaces.FileSystemInterface;


public class Application extends AbstractZipProcessor {
	private static final String	ARG_RULES_FILE = "rules";
	private static final String	ARG_APPEND = "append";
	
	private final RulesParser	parser;
	private final String[]		appends;
	private final PrintStream	ps;
	private final boolean 		debug;
	
	public Application(final String[] removeMask, final String[][] renameMask, final RulesParser parser, final String[] appends, final PrintStream ps, final boolean debug) throws SyntaxException, NullPointerException, IllegalArgumentException {
		super(Constants.MASK_NONE, Constants.MASK_ANY, removeMask, renameMask);
		if (parser == null) {
			throw new NullPointerException("Parser can't be null"); 
		}
		else if (appends == null || appends.length == 0) {
			throw new IllegalArgumentException("Appends mask can't be null or empty"); 
		}
		else {
			this.parser = parser;
			this.appends = appends;
			this.ps = ps;
			this.debug = debug;
		}
	}
	
	@Override
	protected void processPart(final String part, final SubstitutableProperties props, final LoggerFacade logger, final InputStream source, final OutputStream target) throws IOException {
		Utils.copyStream(source, target);
	}

	@Override
	protected void processAppending(final SubstitutableProperties props, final LoggerFacade logger, final OutputStream target) throws IOException {
		for (String item : appends) {
			if (debug) {
				message(ps, "Append %1$s...", item);
			}
			final URI	uri = URI.create(item);
		
			if (FileSystemInterface.FILESYSTEM_URI_SCHEME.equals(uri.getScheme())) {
				try (final FileSystemInterface	fsi = FileSystemFactory.createFileSystem(uri)) {
					if (fsi.exists() && fsi.isDirectory()) {
						fsi.list((FileSystemInterface file)->{
							if (file.exists() && file.isFile()) {
								try(final InputStream	is = file.read()) {
									processXML(file.getPath(), is, target);
								}
							}
							return ContinueMode.CONTINUE;
						});
					}
					else {
						try(final InputStream	is = fsi.read()) {
							processXML(fsi.getPath(), is, target);
						}
					}
				}
			}
			else {
				final URL	url = uri.toURL();
				
				try(final InputStream	is = url.openStream()) {
					processXML(url.getPath(), is, target);
				}
			}
		}
	}
	
	private void processXML(final String pathName, final InputStream is, final OutputStream os) throws IOException {
		append(pathName, is, os);
	}
	
	public static void main(final String[] args) {
		System.exit(main(System.in, args, System.out, System.err));
	}
	
	public static int main(final InputStream is, final String[] args, final OutputStream os, final PrintStream err) {
		final ArgParser				parserTemplate = new ApplicationArgParser();
		
		try{final ArgParser			parser = parserTemplate.parse(args);
			final boolean			debug = parser.getValue(Constants.ARG_DEBUG, boolean.class);
			final RulesParser		rules;
			
			message(err, "Parsing rules from [%1$s]...", parser.getValue(ARG_RULES_FILE, String.class));
			try(final InputStream	ruleStream = parser.getValue(ARG_RULES_FILE, URL.class).openStream();
				final Reader		rdr = new InputStreamReader(ruleStream, PureLibSettings.DEFAULT_CONTENT_ENCODING)) {
				
				rules = new RulesParser(rdr);
			}
			final Application		app = new Application(
											parser.isTyped(Constants.ARG_REMOVE) ? new String[] {parser.getValue(Constants.ARG_REMOVE, String.class)} : Constants.MASK_NONE ,
											parser.isTyped(Constants.ARG_RENAME) ? DAUtils.parseRenameArgument(parser.getValue(Constants.ARG_RENAME, String.class)) : new String[0][],
											rules, 
											parser.getValue(ARG_APPEND, String[].class), 
											err, debug
											);
			final long 				startTime = System.currentTimeMillis();
			
			try(final ZipInputStream	zis = new ZipInputStream(is);
				final ZipOutputStream	zos = new ZipOutputStream(os)) {
			
				message(err, "Processing standard input...");
				app.process(zis, zos);
				zos.finish();
			}
			message(err, "Processing terminated, duration=%1$d msec", (System.currentTimeMillis() - startTime));
		} catch (CommandLineParametersException exc) {
			err.println(exc.getLocalizedMessage());
			err.println(parserTemplate.getUsage("da.converter"));
			return 128;
		} catch (IOException | ContentException exc) {
			exc.printStackTrace(err);
			return 129;
		}
		return 0;
	}
	
	
	private static void message(final PrintStream ps, final String format, final Object... parameters) {
		ps.println("da.xmlcrowler: "+String.format(format, parameters));
	}
	
	static class ApplicationArgParser extends ArgParser {
		private static final ArgParser.AbstractArg[]	KEYS = {
			new BooleanArg(Constants.ARG_DEBUG, false, "Turn on debug trace", false),
			new PatternArg(Constants.ARG_REMOVE, false, false, "Remove entries from the *.zip input. Types as pattern[,...]. This option is processed AFTER processing/passing part"),
			new StringArg(Constants.ARG_RENAME, false, false, "Rename entries in the *.zip input. Types as pattern->template[;...], see Java Pattern syntax and Java Mather.replaceAll(...) description. This option is processed AFTER processing/passing part"),
			new FileArg(ARG_RULES_FILE, false, true, "Location if the rules file."),
			new StringListArg(ARG_APPEND, false, true, "Append content to input stream in the input *.zip. Refs must be any valid URIs"),
		};
		
		ApplicationArgParser() {
			super(KEYS);
		}
	}
}
