package chav1961.da.converter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.util.ServiceLoader;
import java.util.regex.Pattern;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import chav1961.da.converter.interfaces.InputConverterInterface;
import chav1961.da.converter.interfaces.OutputConverterInterface;
import chav1961.da.util.Constants;
import chav1961.da.util.RenamingClass;
import chav1961.da.util.ZipProcessingClass;
import chav1961.da.util.interfaces.DAContentFormat;
import chav1961.da.util.interfaces.RenamingInterface;
import chav1961.purelib.basic.AndOrTree;
import chav1961.purelib.basic.ArgParser;
import chav1961.purelib.basic.PureLibSettings;
import chav1961.purelib.basic.SystemErrLoggerFacade;
import chav1961.purelib.basic.exceptions.CommandLineParametersException;
import chav1961.purelib.basic.exceptions.ContentException;
import chav1961.purelib.basic.exceptions.EnvironmentException;
import chav1961.purelib.basic.interfaces.LoggerFacade;
import chav1961.purelib.basic.interfaces.SyntaxTreeInterface;

public class Application {
	public static final String	ARG_INPUT_FORMAT = "if";
	public static final String	ARG_OUTPUT_FORMAT = "of";

	public static void main(final String[] args) {
		System.exit(main(System.in, args, System.out, System.err));
	}

	public static int main(final InputStream is, final String[] args, final OutputStream os, final PrintStream err) {
		final ArgParser	parserTemplate = new ApplicationArgParser();
		
		try{final ArgParser						parser = parserTemplate.parse(args);
			final URI							inputType = parser.isTyped(ARG_INPUT_FORMAT) ? URI.create(InputConverterInterface.CONV_SCHEMA+':'+parser.getValue(ARG_INPUT_FORMAT, DAContentFormat.class).name()+":/") : null;
			final URI							outputType = parser.isTyped(ARG_OUTPUT_FORMAT) ? URI.create(OutputConverterInterface.CONV_SCHEMA+':'+parser.getValue(ARG_OUTPUT_FORMAT, DAContentFormat.class).name()+":/") : inputType;

			if (ZipProcessingClass.checkZipParameters(parser)) {
				if (outputType == null) {
					throw new CommandLineParametersException("Argument ["+ARG_OUTPUT_FORMAT+"] must be typed to process *.zip content");
				}
				else {
					processZip(System.in, System.out, inputType != null ? seekInputConverter(parser.getValue(ARG_INPUT_FORMAT, DAContentFormat.class)): null, seekOutputConverter(parser.getValue(ARG_OUTPUT_FORMAT, DAContentFormat.class)), parser);
				}
			}
			else {
				if (inputType == null || outputType == null) {
					throw new CommandLineParametersException("Both ["+ARG_INPUT_FORMAT+"] and ["+ARG_OUTPUT_FORMAT+"] parameters must be typed to process plain content");
				}
				else {
					processPlain(System.in, System.out, seekInputConverter(parser.getValue(ARG_INPUT_FORMAT, DAContentFormat.class)), seekOutputConverter(parser.getValue(ARG_OUTPUT_FORMAT, DAContentFormat.class)), new SystemErrLoggerFacade());
				}
			}
			return 0;
		} catch (IOException e) {
			e.printStackTrace(err);
			return 129;
		} catch (CommandLineParametersException exc) {
			err.println(parserTemplate.getUsage("da.converter"));
			return 128;
		}
	}

	public static void processZip(final InputStream is, final OutputStream os, final InputConverterInterface ici, final OutputConverterInterface oci, final ArgParser parser) throws IOException {
		try(final ZipInputStream	zis = new ZipInputStream(is);
			final ZipOutputStream	zos = new ZipOutputStream(os)) {
			final SyntaxTreeInterface<char[]>	tree = new AndOrTree<>(1,1);
			final RenamingInterface	ri = parser.isTyped(Constants.ARG_RENAME) ? new RenamingClass(parser.getValue(Constants.ARG_RENAME, String.class)) : (s)->s;
			final Convertor			conv = new Convertor(parser.getValue(ARG_OUTPUT_FORMAT, DAContentFormat.class), ri, ici, oci);

			ZipProcessingClass.parseZip(zis, zos, parser.getValue(Constants.ARG_EXCLUDE, Pattern.class), parser.getValue(Constants.ARG_PROCESS, Pattern.class), conv, false);
		} catch (ContentException e) {
			throw new IOException(e.getLocalizedMessage(), e); 
		}
	}
	
	public static void processPlain(final InputStream is, final OutputStream os, final InputConverterInterface ici, final OutputConverterInterface oci, final LoggerFacade logger) throws IOException {
		final Reader	rdr = new InputStreamReader(is, PureLibSettings.DEFAULT_CONTENT_ENCODING);
		final Writer	wr = new OutputStreamWriter(os, PureLibSettings.DEFAULT_CONTENT_ENCODING);
		final SyntaxTreeInterface<char[]>	tree = new AndOrTree<>(1,1);
		
		ici.process(rdr, tree, oci, logger);
	}
	
	public static InputConverterInterface seekInputConverter(final DAContentFormat format) throws IOException {
		final URI	uri = URI.create(InputConverterInterface.CONV_SCHEMA+':'+format.getSchema()+":/");
		
		for (InputConverterInterface item : ServiceLoader.load(InputConverterInterface.class)) {
			if (item.canServe(uri)) {
				return item; 
			}
		}
		throw new IOException("Input format ["+format+"] conversion is not supported");
	}

	public static OutputConverterInterface seekOutputConverter(final DAContentFormat format) throws IOException {
		final URI	uri = URI.create(OutputConverterInterface.CONV_SCHEMA+':'+format.getSchema()+":/");
		
		for (OutputConverterInterface item : ServiceLoader.load(OutputConverterInterface.class)) {
			if (item.canServe(uri)) {
				return item; 
			}
		}
		throw new IOException("Output format ["+format+"] conversion is not supported");
	}
	
	private static class ApplicationArgParser extends ArgParser {
		private static final ArgParser.AbstractArg[]	KEYS = {
			new BooleanArg(Constants.ARG_DEBUG, false, "Turn on debug trace", false),
			new BooleanArg(Constants.ARG_ZIP, false, "Parse input as *.zip format", false),
			new PatternArg(Constants.ARG_EXCLUDE, false, "Skip input *.zip parts and remove then from output stream", "\uFFFF"),
			new PatternArg(Constants.ARG_PROCESS, false, "Process the given parts in the input *.zip. If missing,all the partswill be processed", ".*"),
			new StringArg(Constants.ARG_RENAME, false, false, "Rename entries in the *.zip input. Types as pattern->template[;...], see Java Pattern syntax and Java Mather.replaceAll(...) description"),
			new EnumArg<DAContentFormat>(ARG_INPUT_FORMAT, DAContentFormat.class, false, false, "Input format. When missing, treated as *.zip with the same first 'ticket.txt' part"),
			new EnumArg<DAContentFormat>(ARG_OUTPUT_FORMAT, DAContentFormat.class, false, false, "Output format. When missing, treated as 'no conversion'. When input format is *.zip, the output format is also *.zip"),
		};
		
		ApplicationArgParser() {
			super(KEYS);
		}
	}
}
