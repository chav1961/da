package chav1961.da.converter;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.util.ServiceLoader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import chav1961.da.converter.interfaces.InputConverterInterface;
import chav1961.da.converter.interfaces.OutputConverterInterface;
import chav1961.da.util.Constants;
import chav1961.da.util.interfaces.InputFormat;
import chav1961.purelib.basic.AndOrTree;
import chav1961.purelib.basic.ArgParser;
import chav1961.purelib.basic.PureLibSettings;
import chav1961.purelib.basic.SubstitutableProperties;
import chav1961.purelib.basic.SystemErrLoggerFacade;
import chav1961.purelib.basic.Utils;
import chav1961.purelib.basic.exceptions.CommandLineParametersException;
import chav1961.purelib.basic.exceptions.EnvironmentException;
import chav1961.purelib.basic.interfaces.LoggerFacade;
import chav1961.purelib.basic.interfaces.SyntaxTreeInterface;

public class Application {
	public static final String	ARG_INPUT_FORMAT = "if";
	public static final String	ARG_OUTPUT_FORMAT = "of";

	public static void main(final String[] args) {
		// TODO Auto-generated method stub
		final ArgParser	parserTemplate = new ApplicationArgParser();
		
		try{final ArgParser						parser = parserTemplate.parse(args);
			final SyntaxTreeInterface<char[]>	tree = new AndOrTree<>(1,1);
			final URI							inputType = parser.isTyped(ARG_INPUT_FORMAT) ? URI.create(InputConverterInterface.CONV_SCHEMA+':'+parser.getValue(ARG_INPUT_FORMAT, InputFormat.class).name()+":/") : null;
			final URI							outputType = parser.isTyped(ARG_OUTPUT_FORMAT) ? URI.create(OutputConverterInterface.CONV_SCHEMA+':'+parser.getValue(ARG_OUTPUT_FORMAT, InputFormat.class).name()+":/") : inputType;

			if (inputType == null) {
				try(final Writer			sw  = new StringWriter();
					final PrintWriter		pw = new PrintWriter(sw);
					final LoggerFacade		logger = new SystemErrLoggerFacade(pw)) {
					
					try(final ZipInputStream	zis = new ZipInputStream(System.in);
						final ZipOutputStream	zos = new ZipOutputStream(System.out)) {
						
						ZipEntry				ze = zis.getNextEntry();
						
						if (Constants.PART_TICKET.equals(ze.getName())) {
							if (outputType == null) {
								throw new CommandLineParametersException("Output format key is mandatory for input type *.zip, but it is not typed in the command line parameters");
							}
							else {
								final SubstitutableProperties	props = new SubstitutableProperties();
								
								props.load(zis);
								
								final URI		inputZipType = URI.create(InputConverterInterface.CONV_SCHEMA+':'+props.getProperty(ARG_INPUT_FORMAT)+":/");
								
								while ((ze = zis.getNextEntry()) != null && !Constants.PART_LOG.equals(ze.getName())) {
									final Reader	rdr = new InputStreamReader(zis, PureLibSettings.DEFAULT_CONTENT_ENCODING);
									final ZipEntry	zeOut = new ZipEntry(ze.getName());
									
									zeOut.setMethod(ZipEntry.DEFLATED);
									zos.putNextEntry(zeOut);
									
									final Writer	wr = new OutputStreamWriter(zos, PureLibSettings.DEFAULT_CONTENT_ENCODING);
									
									processEntry(rdr, wr, inputZipType, outputType, tree, logger);
								}
								
								if (ze != null) {
									pw.flush();
									
									final Reader	rdr = new InputStreamReader(zis, PureLibSettings.DEFAULT_CONTENT_ENCODING);
									final ZipEntry	zeOut = new ZipEntry(ze.getName());
									
									zeOut.setMethod(ZipEntry.DEFLATED);
									zos.putNextEntry(zeOut);
									
									final Writer	wr = new OutputStreamWriter(zos, PureLibSettings.DEFAULT_CONTENT_ENCODING);
									Utils.copyStream(rdr, wr);
									Utils.copyStream(new StringReader(sw.toString()), wr);
								}
								zos.finish();
							}
						}
						else {
							throw new IOException("The same first part of *.zip must be ["+Constants.PART_TICKET+"]");
						}
					}
				}
			}
			else {
				final Reader	rdr = new InputStreamReader(System.in, PureLibSettings.DEFAULT_CONTENT_ENCODING);
				final Writer	wr = new OutputStreamWriter(System.out, PureLibSettings.DEFAULT_CONTENT_ENCODING);
				
				processEntry(rdr, wr, inputType, outputType, tree, PureLibSettings.CURRENT_LOGGER);
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(129);
		} catch (CommandLineParametersException exc) {
			System.err.println(parserTemplate.getUsage("da.converter"));
			System.exit(128);
		}
	}

	public static void processEntry(final Reader rdr, final Writer wr, final URI inputType, final URI outputType, final SyntaxTreeInterface<char[]> tree, final LoggerFacade logger) throws IOException {
		for (InputConverterInterface inputItem : ServiceLoader.load(InputConverterInterface.class)) {
			if (inputItem.canServe(inputType)) {
				for (OutputConverterInterface outputItem : ServiceLoader.load(OutputConverterInterface.class)) {
					if (outputItem.canServe(outputType)) {
						try{final InputConverterInterface	ici = inputItem.newInstance(inputType);
							final OutputConverterInterface	oci = outputItem.newInstance(outputType);
							
							ici.process(rdr, tree, oci, logger);
							return;
						} catch (EnvironmentException e) {
							throw new IOException(e.getLocalizedMessage(), e);
						}
					}
				}
				throw new IOException("Output format ["+outputType+"] is not supported");
			}
		}
		throw new IOException("Input format ["+inputType+"] is not supported");
	}
	
	private static class ApplicationArgParser extends ArgParser {
		private static final ArgParser.AbstractArg[]	KEYS = {
			new BooleanArg(Constants.ARG_ZIP, false, "Parse input as *.zip format", false),
			new StringArg(Constants.ARG_EXCLUDE, false, false, "Skip input *.zip parts and remove then from output stream"),
			new StringArg(Constants.ARG_PROCESS, false, "Process the given parts in the input *.zip. If missing,all the partswill be processed", "*"),
			new EnumArg<InputFormat>(ARG_INPUT_FORMAT, InputFormat.class, false, false, "Input format. When missing, treated as *.zip with the same first 'ticket.txt' part"),
			new EnumArg<InputFormat>(ARG_OUTPUT_FORMAT, InputFormat.class, false, false, "Output format. When missing, treated as 'no conversion'. When input format is *.zip, the output format is also *.zip"),
		};
		
		ApplicationArgParser() {
			super(KEYS);
		}
	}
}
