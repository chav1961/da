package chav1961.da.source;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URL;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import chav1961.da.util.AbstractZipProcessor;
import chav1961.da.util.Constants;
import chav1961.da.util.DAUtils;
import chav1961.purelib.basic.ArgParser;
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
	public static final String	ARG_START_PIPE = "startPipe";
	public static final String	ARG_APPEND = "append";
	
	private final String[]		appends;
	private final PrintStream	ps;
	private final boolean		debug;
	
	Application(final String[] removeMask, final String[][] renameMask, final String[] appends, final PrintStream err, final boolean debug) throws SyntaxException {
		super(new String[0], new String[] {".+"}, removeMask, renameMask);
		this.appends = appends;
		this.ps = err;
		this.debug = debug;
	}
	
	@Override
	protected void processPart(final String part, final SubstitutableProperties props, final LoggerFacade logger, final InputStream source, final OutputStream target) throws IOException {
		Utils.copyStream(source, target);
	}

	@Override
	protected void processAppending(final SubstitutableProperties props, final LoggerFacade logger, final ZipOutputStream zos) throws IOException {
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
									append(file.getPath(), is, zos);
								}
							}
							return ContinueMode.CONTINUE;
						});
					}
					else {
						try(final InputStream	is = fsi.read()) {
							append(fsi.getPath(), is, zos);
						}
					}
				}
			}
			else {
				final URL	url = uri.toURL();
				
				try(final InputStream	is = url.openStream()) {
					append(url.getPath(), is, zos);
				}
			}
		}
	}
	
	public static void main(final String[] args) {
		System.exit(main(System.in, args, System.out, System.err));
	}
	
	public static int main(final InputStream is, final String[] args, final OutputStream os, final PrintStream err) {
		final ArgParser		parserTemplate = new ApplicationArgParser();
		
		try{final ArgParser			parser = parserTemplate.parse(args);
			final boolean			debug = parser.getValue(Constants.ARG_DEBUG, boolean.class);
			final Application		app = new Application(
											parser.isTyped(Constants.ARG_REMOVE) ? new String[] {parser.getValue(Constants.ARG_REMOVE, String.class)} : new String[0] ,
											parser.isTyped(Constants.ARG_RENAME) ? DAUtils.parseRenameArgument(parser.getValue(Constants.ARG_RENAME, String.class)) : new String[0][],
											parser.isTyped(ARG_APPEND) ? parser.getValue(ARG_APPEND, String[].class) : new String[] {},
											err,
											debug
									);
			final InputStream		source;	
		
			if (parser.getValue(ARG_START_PIPE, boolean.class)) {
				final SubstitutableProperties	props = new SubstitutableProperties();
				
				message(err, "%1$s argument typed - new empty stream created from the standard input...", ARG_START_PIPE);
				props.load(is);
				source = DAUtils.newEmptyZip(props);
			}
			else {
				message(err, "Process standard input...");
				source = is;
			}

			final long	startTime = System.currentTimeMillis();
			
			try(final ZipInputStream	zis = new ZipInputStream(source);
				final ZipOutputStream	zos = new ZipOutputStream(os)) {
				
				app.process(zis, zos);
			}
			message(err, "Processing terminated, duration=%1$d msec", System.currentTimeMillis() - startTime);
			return 0;
		} catch (CommandLineParametersException exc) {
			err.println(exc.getLocalizedMessage());
			err.println(parserTemplate.getUsage("da.source"));
			return 128;
		} catch (IOException | ContentException exc) {
			exc.printStackTrace(err);
			return 129;
		}
	}

	private static void message(final PrintStream ps, final String format, final Object... parameters) {
		ps.println("da.source: "+String.format(format, parameters));
	}
	
	static class ApplicationArgParser extends ArgParser {
		private static final ArgParser.AbstractArg[]	KEYS = {
			new BooleanArg(Constants.ARG_DEBUG, false, "Turn on debug trace", false),
			new PatternArg(Constants.ARG_REMOVE, false, false, "Remove entries from the *.zip input. Types as pattern[,...]. This option is processed AFTER processing/passing part"),
			new StringArg(Constants.ARG_RENAME, false, false, "Rename entries in the *.zip input. Types as pattern->template[;...], see Java Pattern syntax and Java Mather.replaceAll(...) description. This option is processed AFTER processing/passing part"),
			new BooleanArg(ARG_START_PIPE, false, "Start new pipe. Standard input must contain content of the ["+Constants.PART_TICKET+"] in this case", false),
			new StringListArg(ARG_APPEND, false, false, "Append content to input stream in the input *.zip. Refs must be any valid URIs"),
		};
		
		ApplicationArgParser() {
			super(KEYS);
		}
	}
}
