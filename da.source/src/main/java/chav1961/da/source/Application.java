package chav1961.da.source;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.regex.Pattern;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import chav1961.da.util.Constants;
import chav1961.da.util.RenamingClass;
import chav1961.da.util.ZipProcessingClass;
import chav1961.da.util.interfaces.RenamingInterface;
import chav1961.purelib.basic.ArgParser;
import chav1961.purelib.basic.SubstitutableProperties;
import chav1961.purelib.basic.SystemErrLoggerFacade;
import chav1961.purelib.basic.exceptions.CommandLineParametersException;
import chav1961.purelib.basic.exceptions.ContentException;

public class Application {
	public static final String	ARG_START_PIPE = "startPipe";
	public static final String	ARG_APPEND = "append";
	
	public static void main(final String[] args) {
		System.exit(main(System.in,args,System.out,System.err));
	}
	
	public static int main(final InputStream is, final String[] args, final OutputStream os, final PrintStream err) {
		final ArgParser		parserTemplate = new ApplicationArgParser();
		
		try{final ArgParser			parser = parserTemplate.parse(args);
			final InputStream		source;	
		
			if (parser.getValue(ARG_START_PIPE, boolean.class)) {
				final SubstitutableProperties	props = new SubstitutableProperties();
				
				props.load(is);
				source = ZipProcessingClass.createZipTemplate(props);
			}
			else {
				source = is;
			}
			
			if (parser.getValue(Constants.ARG_ZIP, boolean.class) || parser.getValue(ARG_START_PIPE, boolean.class)) {
				processZip(source, os, parser);
			}
			else {
				throw new CommandLineParametersException("Neither ["+Constants.ARG_ZIP+"] nor ["+ARG_START_PIPE+"] parameter were types");
			}
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

	public static void processZip(final InputStream source, final OutputStream os, final ArgParser parser) throws IOException, ContentException {
		final RenamingInterface	ri = parser.isTyped(Constants.ARG_RENAME) ? new RenamingClass(parser.getValue(Constants.ARG_RENAME, String.class)) : (s)->s;
		final MoverAndAppender	maa = new MoverAndAppender(ri, parser.isTyped(ARG_APPEND) ? parser.getValue(ARG_APPEND, String[].class) : new String[0]);
		
		try(final ZipInputStream	zis = new ZipInputStream(source);
			final ZipOutputStream	zos = new ZipOutputStream(os)) {
			
			ZipProcessingClass.parseZip(zis, zos, 
					parser.getValue(Constants.ARG_EXCLUDE, Pattern.class), 
					parser.getValue(Constants.ARG_PROCESS, Pattern.class),
					maa,
					parser.getValue(Constants.ARG_DEBUG, boolean.class));
		}
	}
	
	static class ApplicationArgParser extends ArgParser {
		private static final ArgParser.AbstractArg[]	KEYS = {
			new BooleanArg(Constants.ARG_DEBUG, false, "Turn on debug trace", false),
			new BooleanArg(Constants.ARG_ZIP, false, "Parse input as *.zip format", false),
			new PatternArg(Constants.ARG_EXCLUDE, false, "Skip input *.zip parts and remove then from output stream", "\uFFFF"),
			new PatternArg(Constants.ARG_PROCESS, false, "Process the given parts in the input *.zip. If missing,all the parts will be processed", ".*"),
			new StringArg(Constants.ARG_RENAME, false, false, "Rename entries in the *.zip input. Types as pattern->template[;...], see Java Pattern syntax and Java Mather.replaceAll(...) description"),
			new BooleanArg(ARG_START_PIPE, false, "Start pipe. Input must be content of the ["+Constants.PART_TICKET+"] in this case", false),
			new StringListArg(ARG_APPEND, false, false, "Append content to input stream in the input *.zip. Refs can be any valid URIs"),
		};
		
		ApplicationArgParser() {
			super(KEYS);
		}
	}
}
