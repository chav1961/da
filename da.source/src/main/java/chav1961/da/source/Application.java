package chav1961.da.source;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URL;
import java.util.Properties;
import java.util.regex.Pattern;
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
import chav1961.purelib.basic.interfaces.LoggerFacade.Severity;
import chav1961.purelib.enumerations.ContinueMode;
import chav1961.purelib.fsys.FileSystemFactory;
import chav1961.purelib.fsys.interfaces.FileSystemInterface;

/**
 * <p>This class is a main class to use as pipe element in Data Acquisition pipe. The class can be used:</p>
 * <ul>
 * <li>as the same first element in Data Acquisition pipe (type -startPipe parameter in the command line)
 * <li>as the middle element in Data Acquisition pipe.
 * </ul>
 * <p>In both the cases the class can be used to append any files into Data Acquisition pipe (type -append &lt;file_regex&gt; in the command line).
 * It also supported a subset of standard command line keys:</p>
 * <ul>
 * <li> -remove &lt;file_regex&gt; to remove some files from Data Acquisition pipe
 * <li> -rename &lt;name_regex->new_name&gt; to rename some files in Data Acquisition pipe
 * <li> -debug turn on debug trace to the System.err stream
 * </ul>
 * <p>Source stream for the pipe element is the System.in, destination stream for the pipe element is System.out. When the -startPipe parameter was typed,
 * Source stream must contain data in the {@linkplain Properties} format. The data will be used as content for new {@value Constants#PART_TICKET} part of the
 * Data Acquisiton pipe. When the -startPipe is missing, source stream must contain Data Acquisition pipe content. In both the cases destination stream will contains
 * Data Acquisition pipe content.</p>  
 * @author Alexander Chernomyrdin aka chav1961
 * @since 0.0.1
 */
public class Application extends AbstractZipProcessor {
	public static final String	ARG_START_PIPE = "startPipe";
	public static final String	ARG_APPEND = "append";
	public static final String	ARG_JOIN = "join";
	
	private final String[]		appends;
	private final String 		partName;
	private final PrintStream	ps;
	private final boolean		join;
	private final boolean		debug;
	
	Application(final String[] removeMask, final String[][] renameMask, final String[] appends, final PrintStream err, final boolean join, final String partName, final boolean debug) throws SyntaxException {
		super(Constants.MASK_NONE, Constants.MASK_ANY, removeMask, renameMask);
		this.appends = appends;
		this.ps = err;
		this.join = join;
		this.partName = partName;
		this.debug = debug;
	}
	
	@Override
	protected void processPart(final String part, final SubstitutableProperties props, final LoggerFacade logger, final InputStream source, final OutputStream target) throws IOException {
		Utils.copyStream(source, target);
	}

	@Override
	protected void processAppending(final SubstitutableProperties props, final LoggerFacade logger, final OutputStream zos) throws IOException {
		if (join) {
			newEntry(partName, zos);
		}
		for (String item : appends) {
			if (debug) {
				message(ps, "Append %1$s...", item);
			}
			final URI	uri;
			
			try{
				uri = URI.create(item);
			} catch (IllegalArgumentException exc) {
				throw new IOException("Illegal URI format ["+item+"] to append content"); 
			}
		
			if (uri.isAbsolute()) {
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
			else if (item.indexOf('*') >= 0 || item.indexOf('?') >= 0) {
				final File		file = new File(item);
				final Pattern	pattern = Pattern.compile(Utils.fileMask2Regex(file.getName()));
				final File[]	list = file.getParentFile().listFiles((File f)->pattern.matcher(f.getName()).find());
				
				if (list != null) {
					for (File toAppend : list) {
						if (toAppend.isFile()) {
							appendFile(toAppend, zos, logger);
						}
					}
				}
			}
			else {
				appendFile(new File(item), zos, logger);
			}
		}
		if (join) {
			closeEntry(zos);
		}
	}
	
	private void appendFile(final File toAppend, final OutputStream zos, final LoggerFacade logger) throws IOException {
		if (toAppend.exists() && toAppend.isFile() && toAppend.canRead()) {
			try(final InputStream	is = new FileInputStream(toAppend)) {
				if (join) {
					Utils.copyStream(is, zos);
				}
				else {
					append(toAppend.getName(), is, zos);
				}
			}
		}
		else {
			logger.message(Severity.warning, "File ["+toAppend.getAbsolutePath()+"] not exists, is directory or is not accessible for you");
		}
	}

	public static void main(final String[] args) {
		System.exit(main(System.in, args, System.out, System.err));
	}
	
	public static int main(final InputStream is, final String[] args, final OutputStream os, final PrintStream err) {
		final ArgParser		parserTemplate = new ApplicationArgParser();
		
		try{final ArgParser	parser = parserTemplate.parse(args);
			final boolean	debug = parser.getValue(Constants.ARG_DEBUG, boolean.class);
			final boolean	join = parser.isTyped(ARG_JOIN);
			
			if (join && !parser.isTyped(ARG_APPEND)) {
				throw new CommandLineParametersException("["+ARG_JOIN+"] parameter can be used in conjuntion with ["+ARG_APPEND+"] parameter only"); 
			}
			else {
				final Application		app = new Application(
												parser.isTyped(Constants.ARG_REMOVE) ? new String[] {parser.getValue(Constants.ARG_REMOVE, String.class)} : new String[0] ,
												parser.isTyped(Constants.ARG_RENAME) ? DAUtils.parseRenameArgument(parser.getValue(Constants.ARG_RENAME, String.class)) : new String[0][],
												parser.isTyped(ARG_APPEND) ? parser.getValue(ARG_APPEND, String[].class) : new String[] {},
												err,
												join, 
												join ? parser.getValue(ARG_JOIN, String.class) : "", 
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
			}
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
		ps.flush();
	}
	
	static class ApplicationArgParser extends ArgParser {
		private static final ArgParser.AbstractArg[]	KEYS = {
			new BooleanArg(Constants.ARG_DEBUG, false, "Turn on debug trace", false),
			new PatternArg(Constants.ARG_REMOVE, false, false, "Remove entries from the *.zip input. Types as pattern[,...]. This option is processed AFTER processing/passing part"),
			new StringArg(Constants.ARG_RENAME, false, false, "Rename entries in the *.zip input. Types as pattern->template[;...], see Java Pattern syntax and Java Mather.replaceAll(...) description. This option is processed AFTER processing/passing part"),
			new BooleanArg(ARG_START_PIPE, false, "Start new pipe. Standard input must contain content of the ["+Constants.PART_TICKET+"] in this case", false),
			new StringListArg(ARG_APPEND, false, false, "Append content to input stream in the input *.zip. Refs must be any valid URIs"),
			new StringListArg(ARG_JOIN, false, false, "Join appended content to one part with the given name"),
		};
		
		ApplicationArgParser() {
			super(KEYS);
		}
	}
}
