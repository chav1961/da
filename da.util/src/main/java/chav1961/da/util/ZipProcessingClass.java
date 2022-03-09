package chav1961.da.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.concurrent.Flow.Processor;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import chav1961.da.util.interfaces.InputFormat;
import chav1961.purelib.basic.ArgParser;
import chav1961.purelib.basic.PureLibSettings;
import chav1961.purelib.basic.SubstitutableProperties;
import chav1961.purelib.basic.SystemErrLoggerFacade;
import chav1961.purelib.basic.Utils;
import chav1961.purelib.basic.exceptions.CommandLineParametersException;
import chav1961.purelib.basic.exceptions.ContentException;
import chav1961.purelib.basic.interfaces.LoggerFacade;

public class ZipProcessingClass {
	public static final EntityProcessor	COPY_PROCESSOR = (reader, writer, partName, format, logger, debug) -> Utils.copyStream(reader, writer);
	public static final Pattern			ALL_PATTERN = Pattern.compile(".*"); 
	public static final Pattern			NONE_PATTERN = Pattern.compile("\uFFFF"); 
	
	@FunctionalInterface
	public interface EntityProcessor {
		void process(InputStream reader, OutputStream writer, String partName, InputFormat format, LoggerFacade logger, boolean debug) throws IOException;
		
		default String rename(String partName) {
			return partName;
		}

		default void append(ZipOutputStream writer, LoggerFacade logger, boolean debug) throws IOException {
		}
	}
	
	public static boolean checkZipParameters(final ArgParser parser) throws CommandLineParametersException, IllegalStateException, NullPointerException {
		if (parser == null) {
			throw new NullPointerException("Parse can't be null"); 
		}
		else if (!parser.getValue(Constants.ARG_ZIP, boolean.class)) {
			if (parser.isTyped(Constants.ARG_SKIP)) {
				throw new CommandLineParametersException("Argument ["+Constants.ARG_SKIP+"] must be used in conjunction with ["+Constants.ARG_ZIP+"] only");  
			}
			else if (parser.isTyped(Constants.ARG_PROCESS)) {
				throw new CommandLineParametersException("Argument ["+Constants.ARG_PROCESS+"] must be used in conjunction with ["+Constants.ARG_ZIP+"] only");  
			}
			else {
				return false;
			}
		}
		else {
			return true;
		}
	}

	public static void parseZip(final InputStream is, final OutputStream os, final Pattern skip, final Pattern process, final EntityProcessor processor) throws NullPointerException, ContentException {
		parseZip(is, os, skip, process, processor, false);
	}
	
	public static void parseZip(final InputStream is, final OutputStream os, final Pattern skip, final Pattern process, final EntityProcessor processor, final boolean debug) throws NullPointerException, ContentException {
		if (is == null) {
			throw new NullPointerException("Input stream can't be null");
		}
		else if (os == null) {
			throw new NullPointerException("Output stream can't be null");
		}
		else if (skip == null) {
			throw new NullPointerException("Skip pattern can't be null");
		}
		else if (process == null) {
			throw new NullPointerException("Process pattern can't be null");
		}
		else if (processor == null) {
			throw new NullPointerException("Part processor can't be null");
		}
		else {
			try(final Writer		swr = new StringWriter();
				final PrintWriter	pw = new PrintWriter(swr);
				final LoggerFacade	logger = new SystemErrLoggerFacade(pw)) {

				final ZipInputStream	zis = new ZipInputStream(is);
				final ZipOutputStream	zos = new ZipOutputStream(os);
				ZipEntry				zeIn, zeOut;
				
				if ((zeIn = zis.getNextEntry()) != null) {
					if (Constants.PART_TICKET.equals(zeIn.getName())) {
						final SubstitutableProperties	props = new SubstitutableProperties();
						
						props.load(zis);
						
						while ((zeIn = zis.getNextEntry()) != null && !Constants.PART_LOG.equals(zeIn)) {
							if (!skip.matcher(zeIn.getName()).matches()) {
								if (process.matcher(zeIn.getName()).matches()) {
									zeOut = new ZipEntry(processor.rename(zeIn.getName()));
									zeOut.setMethod(ZipEntry.DEFLATED);
									zos.putNextEntry(zeOut);
									processor.process(zis, zos, zeOut.getName(), null, logger, debug);
									zos.closeEntry();
								}
								else {
									zeOut = new ZipEntry(zeIn.getName());
									zeOut.setMethod(ZipEntry.DEFLATED);
									zos.putNextEntry(zeOut);
									Utils.copyStream(zis, zos);
									zos.closeEntry();
								}
							}
						}
						if (zeIn != null) {
							processor.append(zos, logger, debug);
							zeOut = new ZipEntry(zeIn.getName());
							zeOut.setMethod(ZipEntry.DEFLATED);
							zos.putNextEntry(zeOut);
							Utils.copyStream(zis, zos);
							pw.flush();
							zos.write(swr.toString().getBytes(PureLibSettings.DEFAULT_CONTENT_ENCODING));
							zos.closeEntry();
							
							if ((zeIn = zis.getNextEntry()) != null) {
								throw new ContentException("Input *.zip corrupted - the same last entity in the *.zip must be ["+Constants.PART_LOG+"]"); 
							}
							else {
								zos.finish();
							}
						}
						else {
							throw new ContentException("Input *.zip corrupted - the same last entity in the *.zip must be ["+Constants.PART_LOG+"]"); 
						}
					}
					else {
						throw new ContentException("Input *.zip corrupted - the same first entity in the *.zip must be ["+Constants.PART_TICKET+"]"); 
					}
				}
				else {
					throw new ContentException("Input *.zip corrupted - missing any content"); 
				}
			} catch (IOException e) {
			}
		}
	}

	public static void copyZip(final InputStream is, final OutputStream os, final boolean debug) throws NullPointerException, ContentException {
		parseZip(is, os, NONE_PATTERN, ALL_PATTERN, COPY_PROCESSOR, debug);
	}
}
