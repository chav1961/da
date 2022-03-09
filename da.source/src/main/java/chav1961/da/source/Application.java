package chav1961.da.source;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import chav1961.da.util.Constants;
import chav1961.da.util.ZipProcessingClass;
import chav1961.purelib.basic.ArgParser;
import chav1961.purelib.basic.SubstitutableProperties;
import chav1961.purelib.basic.SystemErrLoggerFacade;
import chav1961.purelib.basic.exceptions.CommandLineParametersException;
import chav1961.purelib.basic.exceptions.ContentException;

public class Application {
	public static final String	ARG_START_PIPE = "startPipe";
	public static final String	ARG_APPEND = "append";
	
	public static void main(final String[] args) {
		final ArgParser		parserTemplate = new ApplicationArgParser();
		
		try{final ArgParser			parser = parserTemplate.parse(args);
			final MoverAndAppender	maa = new MoverAndAppender(parser.isTyped(ARG_APPEND) ? parser.getValue(ARG_APPEND, String[].class) : new String[0]);
			final InputStream		source;	
		
			if (parser.getValue(ARG_START_PIPE, boolean.class)) {
				final SubstitutableProperties	props = new SubstitutableProperties();
				
				props.load(System.in);
				source = buildZipTemplate(props);
			}
			else {
				source = System.in;
			}
			
			if (parser.getValue(Constants.ARG_ZIP, boolean.class) || parser.getValue(ARG_START_PIPE, boolean.class)) {
				try(final ZipInputStream	zis = new ZipInputStream(source);
					final ZipOutputStream	zos = new ZipOutputStream(System.out)) {
					
					ZipProcessingClass.parseZip(zis, zos, 
							parser.getValue(Constants.ARG_SKIP, Pattern.class), 
							parser.getValue(Constants.ARG_PROCESS, Pattern.class),
							maa,
							parser.getValue(Constants.ARG_DEBUG, boolean.class));
				}
			}
			else {
				maa.process(source, System.out, "source", null, new SystemErrLoggerFacade(), parser.getValue(Constants.ARG_DEBUG, boolean.class));
			}
		} catch (CommandLineParametersException exc) {
			System.err.println(exc.getLocalizedMessage());
			System.err.println(parserTemplate.getUsage("da.source"));
			System.exit(128);
		} catch (IOException | ContentException exc) {
			exc.printStackTrace();
			System.exit(129);
		}
	}

	static InputStream buildZipTemplate(final SubstitutableProperties props) throws IOException {
		try(final ByteArrayOutputStream	baos = new ByteArrayOutputStream()) {
			try(final ZipOutputStream	zos = new ZipOutputStream(baos)) {
				ZipEntry	ze = new ZipEntry(Constants.PART_TICKET);
				
				ze.setMethod(ZipEntry.DEFLATED);
				zos.putNextEntry(ze);
				props.store(zos, "Created "+new Date(System.currentTimeMillis()));
				zos.closeEntry();
				
				ze = new ZipEntry(Constants.PART_LOG);
				ze.setMethod(ZipEntry.DEFLATED);
				zos.putNextEntry(ze);
				zos.closeEntry();
				
				zos.finish();
			}
			
			return new ByteArrayInputStream(baos.toByteArray());
		}
	}

	private static class ApplicationArgParser extends ArgParser {
		private static final ArgParser.AbstractArg[]	KEYS = {
			new BooleanArg(Constants.ARG_DEBUG, false, "Turn on debug trace", false),
			new BooleanArg(Constants.ARG_ZIP, false, "Parse input as *.zip format", false),
			new StringArg(Constants.ARG_SKIP, false, "Skip input *.zip parts and remove then from output stream", "\uFFFF"),
			new StringArg(Constants.ARG_PROCESS, false, "Process the given parts in the input *.zip. If missing,all the parts will be processed", ".*"),
			new BooleanArg(ARG_START_PIPE, false, "Start pipe. Input must be content of the ["+Constants.PART_TICKET+"] in this case", false),
			new StringListArg(ARG_APPEND, false, false, "Append content to input stream in the input *.zip. Refs can be any valid URIs"),
		};
		
		ApplicationArgParser() {
			super(KEYS);
		}
	}
}
