package chav1961.da.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URL;
import java.util.Date;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Flow.Processor;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import chav1961.da.util.interfaces.EntityProcessor;
import chav1961.da.util.interfaces.RenamingInterface;
import chav1961.purelib.basic.ArgParser;
import chav1961.purelib.basic.PureLibSettings;
import chav1961.purelib.basic.SubstitutableProperties;
import chav1961.purelib.basic.SystemErrLoggerFacade;
import chav1961.purelib.basic.URIUtils;
import chav1961.purelib.basic.Utils;
import chav1961.purelib.basic.exceptions.CommandLineParametersException;
import chav1961.purelib.basic.exceptions.ContentException;
import chav1961.purelib.basic.interfaces.LoggerFacade;
import chav1961.purelib.enumerations.ContinueMode;
import chav1961.purelib.fsys.interfaces.FileSystemInterface;

public class ZipProcessingClass {
	public static final EntityProcessor	COPY_PROCESSOR = (reader, writer, partName, format, logger, debug) -> Utils.copyStream(reader, writer);
	public static final Pattern			ALL_PATTERN = Pattern.compile(".*"); 
	public static final Pattern			NONE_PATTERN = Pattern.compile("\uFFFF"); 
	
	public static boolean checkZipParameters(final ArgParser parser) throws CommandLineParametersException, IllegalStateException, NullPointerException {
		if (parser == null) {
			throw new NullPointerException("Parse can't be null"); 
		}
		else if (!parser.getValue(Constants.ARG_ZIP, boolean.class)) {
			if (parser.isTyped(Constants.ARG_EXCLUDE)) {
				throw new CommandLineParametersException("Argument ["+Constants.ARG_EXCLUDE+"] must be used in conjunction with ["+Constants.ARG_ZIP+"] only");  
			}
			else if (parser.isTyped(Constants.ARG_PROCESS)) {
				throw new CommandLineParametersException("Argument ["+Constants.ARG_PROCESS+"] must be used in conjunction with ["+Constants.ARG_ZIP+"] only");  
			}
			else if (parser.isTyped(Constants.ARG_RENAME)) {
				throw new CommandLineParametersException("Argument ["+Constants.ARG_RENAME+"] must be used in conjunction with ["+Constants.ARG_ZIP+"] only");  
			}
			else {
				return false;
			}
		}
		else {
			return true;
		}
	}

	public static InputStream createZipTemplate(final Properties props, final URI... content) throws IOException, NullPointerException {
		if (props == null) {
			throw new NullPointerException("Properties can't be null");
		}
		else if (content == null || Utils.checkArrayContent4Nulls(content) >= 0) {
			throw new NullPointerException("Properties is null or contain nulls inside");
		}
		else {
			try(final ByteArrayOutputStream	baos = new ByteArrayOutputStream()) {
				try(final ZipOutputStream	zos = new ZipOutputStream(baos)) {
					ZipEntry	ze = new ZipEntry(Constants.PART_TICKET);
					
					ze.setMethod(ZipEntry.DEFLATED);
					zos.putNextEntry(ze);
					props.store(zos, "Created "+new Date(System.currentTimeMillis()));
					zos.closeEntry();

					for (URI item : content) {
						copyZip(item, zos);
					}
					
					ze = new ZipEntry(Constants.PART_LOG);
					ze.setMethod(ZipEntry.DEFLATED);
					zos.putNextEntry(ze);
					zos.closeEntry();
					
					zos.finish();
				} catch (ContentException e) {
					throw new IOException(e.getLocalizedMessage(), e);
				}
				
				return new ByteArrayInputStream(baos.toByteArray());
			}
		}
	}
	
	public static void parseZip(final ZipInputStream is, final ZipOutputStream os, final Pattern skip, final Pattern process, final EntityProcessor processor) throws NullPointerException, ContentException {
		parseZip(is, os, skip, process, processor, false);
	}
	
	public static void parseZip(final ZipInputStream is, final ZipOutputStream os, final Pattern skip, final Pattern process, final EntityProcessor processor, final boolean debug) throws NullPointerException, ContentException {
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

				ZipEntry			zeIn, zeOut;
				
				if ((zeIn = is.getNextEntry()) != null) {
					if (Constants.PART_TICKET.equals(zeIn.getName())) {
						final SubstitutableProperties	props = new SubstitutableProperties(), newProps;
						
						props.load(is);		// Process ticket and move it to output
						newProps = processor.processTicket(props);
						zeOut = new ZipEntry(Constants.PART_TICKET);
						zeOut.setMethod(ZipEntry.DEFLATED);
						os.putNextEntry(zeOut);
						newProps.store(os, "");
						os.closeEntry();
						
						while ((zeIn = is.getNextEntry()) != null && !Constants.PART_LOG.equals(zeIn.getName())) {
							if (!skip.matcher(zeIn.getName()).matches()) {
								if (process.matcher(zeIn.getName()).matches()) {
									zeOut = new ZipEntry(processor.renameEntry(zeIn.getName()));
									zeOut.setMethod(ZipEntry.DEFLATED);
									os.putNextEntry(zeOut);
									processor.processEntry(is, os, zeOut.getName(), null, logger, debug);
									os.closeEntry();
								}
								else {
									zeOut = new ZipEntry(zeIn.getName());
									zeOut.setMethod(ZipEntry.DEFLATED);
									os.putNextEntry(zeOut);
									Utils.copyStream(is, os);
									os.closeEntry();
								}
							}
						}
						if (zeIn != null) {
							processor.appendEntries(os, logger, debug);
							zeOut = new ZipEntry(zeIn.getName());
							zeOut.setMethod(ZipEntry.DEFLATED);
							os.putNextEntry(zeOut);
							Utils.copyStream(is, os);
							pw.flush();
							os.write(swr.toString().getBytes(PureLibSettings.DEFAULT_CONTENT_ENCODING));
							os.closeEntry();
							
							if ((zeIn = is.getNextEntry()) != null) {
								throw new ContentException("Input *.zip corrupted - the same last entity in the *.zip must be ["+Constants.PART_LOG+"]"); 
							}
							else {
								os.finish();
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
				throw new ContentException(e.getLocalizedMessage(), e);
			}
		}
	}

	public static void copyZip(final ZipInputStream is, final ZipOutputStream os, final boolean debug) throws NullPointerException, ContentException, IOException {
		copyZip(is, os, NONE_PATTERN, (s)->s, PureLibSettings.CURRENT_LOGGER, debug);
	}
	
	public static void copyZip(final ZipInputStream is, final ZipOutputStream os, final Pattern skip, final RenamingInterface ri, final LoggerFacade logger, final boolean debug) throws NullPointerException, ContentException, IOException {
		if (is == null) {
			throw new NullPointerException("Input stream can't be null");
		}
		else if (os == null) {
			throw new NullPointerException("Output stream can't be null");
		}
		else if (skip == null) {
			throw new NullPointerException("Skip pattern can't be null");
		}
		else if (ri == null) {
			throw new NullPointerException("Renaming interface can't be null");
		}
		else if (logger == null) {
			throw new NullPointerException("Logger can't be null");
		}
		else {
			ZipEntry	ze;
			
			while((ze = is.getNextEntry()) != null) {
				if (!Constants.PART_TICKET.equals(ze.getName()) && !Constants.PART_LOG.equals(ze.getName()) && !skip.matcher(ze.getName()).matches()) {
					copyZip(is,ze.getName(),ri,os,logger,debug);
				}
			}
		}
	}

	public static void copyZip(final InputStream is, final String entryName, final ZipOutputStream os, final LoggerFacade logger, final boolean debug) throws NullPointerException, IllegalArgumentException, ContentException, IOException {
		copyZip(is, entryName, (s)->s, os, logger, debug);
	}
	
	public static void copyZip(final InputStream is, final String entryName, final RenamingInterface ri, final ZipOutputStream os, final LoggerFacade logger, final boolean debug) throws NullPointerException, IllegalArgumentException, ContentException, IOException {
		if (is == null) {
			throw new NullPointerException("Input stream can't be null");
		}
		else if (entryName == null || entryName.isEmpty()) {
			throw new IllegalArgumentException("Entry name can't be null or empty");
		}
		else if (ri == null) {
			throw new NullPointerException("Renaming interface can't be null");
		}
		else if (os == null) {
			throw new NullPointerException("Output stream can't be null");
		}
		else if (logger == null) {
			throw new NullPointerException("Logger can't be null");
		}
		else {
			final ZipEntry	ze = new ZipEntry(entryName);
			
			ze.setMethod(ZipEntry.DEFLATED);
			os.putNextEntry(ze);
			Utils.copyStream(is, os);
			os.closeEntry();
		}
	}

	public static void copyZip(final FileSystemInterface fsi, final ZipOutputStream os) throws NullPointerException, ContentException, IOException {
		copyZip(fsi, os, NONE_PATTERN, (s)->s, PureLibSettings.CURRENT_LOGGER, false);
	}	
	
	public static void copyZip(final FileSystemInterface fsi, final ZipOutputStream os, final Pattern skip, final RenamingInterface ri, final LoggerFacade logger, final boolean debug) throws NullPointerException, ContentException, IOException {
		if (fsi == null) {
			throw new NullPointerException("File system can't be null");
		}
		else if (os == null) {
			throw new NullPointerException("Output stream can't be null");
		}
		else if (skip == null) {
			throw new NullPointerException("Skip pattern stream can't be null");
		}
		else if (ri == null) {
			throw new NullPointerException("Renaming interface can't be null");
		}
		else if (logger == null) {
			throw new NullPointerException("Logger can't be null");
		}
		else {
			copyZipInternal(fsi, os, skip, ri, logger, debug);
		}
	}	

	private static void copyZipInternal(final FileSystemInterface fsi, final ZipOutputStream os, final Pattern skip, final RenamingInterface ri, final LoggerFacade logger, final boolean debug) throws NullPointerException, ContentException, IOException {
		if (fsi.isDirectory()) {
			fsi.list((i)->{
				try{copyZipInternal(i, os, skip, ri, logger, debug);
					return ContinueMode.CONTINUE;
				} catch (ContentException e) {
					throw new IOException(e.getLocalizedMessage());
				}
			});
		}
		else if (!skip.matcher(fsi.getPath()).matches()) {
			try(final InputStream	is = fsi.read()) {
				copyZip(is, fsi.getPath(), ri, os, logger, debug);
			}
		}
	}

	public static void copyZip(final URI uri, final ZipOutputStream os) throws NullPointerException, ContentException, IOException {
		copyZip(uri, os, NONE_PATTERN, (s)->s, PureLibSettings.CURRENT_LOGGER, false);
	}
	
	public static void copyZip(final URI uri, final ZipOutputStream os, final Pattern skip, final RenamingInterface ri, final LoggerFacade logger, final boolean debug) throws NullPointerException, ContentException, IOException {
		if (uri == null) {
			throw new NullPointerException("URI can't be null");
		}
		else if (os == null) {
			throw new NullPointerException("Output stream can't be null");
		}
		else if (skip == null) {
			throw new NullPointerException("Skip pattern stream can't be null");
		}
		else if (ri == null) {
			throw new NullPointerException("Renaming interface can't be null");
		}
		else if (logger == null) {
			throw new NullPointerException("Logger can't be null");
		}
		else if (!uri.isAbsolute()) {
			final File	f = new File(uri.getPath());
			
			if (!skip.matcher(f.getName()).matches()) {
				try(final InputStream	is = new FileInputStream(f)) {
					copyZip(is, f.getName(), ri, os, logger, debug);
				}
			}
		}
		else if (FileSystemInterface.FILESYSTEM_URI_SCHEME.equals(uri.getScheme())) {
			try(final FileSystemInterface	fsi = FileSystemInterface.Factory.newInstance(uri)) {
				
				copyZip(fsi, os, skip, ri, logger, debug);
			}
		}
		else if ("jar".equals(uri.getScheme())) {
			if (uri.toString().contains("!/")) {
				try(final InputStream		is = uri.toURL().openStream()) {
					final String			ssp = uri.getSchemeSpecificPart();
					
					copyZip(is, ssp.substring(ssp.indexOf("!/")+1), ri, os, logger, debug);
				}
			}
			else {
				final URI	nested = URI.create(uri.getSchemeSpecificPart());
				
				try(final InputStream		is = nested.toURL().openStream();
					final ZipInputStream	zis = new ZipInputStream(is)) {
					
					copyZip(zis, os, skip, ri, logger, debug);
				}
			}
		}
		else {
			try(final InputStream		is = uri.toURL().openStream()) {
				
				copyZip(is, uri.getPath(), ri, os, logger, debug);
			}
		}
	}
}
