package chav1961.da.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import chav1961.purelib.basic.PureLibSettings;
import chav1961.purelib.basic.SubstitutableProperties;
import chav1961.purelib.basic.Utils;
import chav1961.purelib.basic.exceptions.SyntaxException;
import chav1961.purelib.basic.interfaces.LoggerFacade;

/**
 * <p>This class supports basic functionality to process ZIP stream in Data Acquisition pipe. It's strongly recommended to use this class as parent for
 * your own application to process Data Acquisition pipe</p> 
 * @author Alexander Chernomyrdin aka chav1961
 * @since 0.0.1
 */
public abstract class AbstractZipProcessor {
	private static final int	STATE_BEFORE_TICKET = 0;
	private static final int	STATE_INSIDE_CONTENT = 1;
	private static final int	STATE_BEFORE_LOG = 2;
	
	private static final OutputStream	NULL_OUTPUT = new OutputStream() {
											@Override public void write(int b) throws IOException {}
										};
	
	private final Pattern[]		toProcess;
	private final Pattern[]		toPass;
	private final Pattern[]		toRemove;
	private final Pattern[]		toRename;
	private final String[]		renameFormat;
										
	/**
	 * <p>Constructor of the class. The first three arguments in the constructor must be valid {@linkplain Pattern} values. The fourth argument format is described {@linkplain DAUtils#parseRenameArgument(String) here}.</p>
	 * @param processMask parsed argument from {@value Constants#ARG_PROCESS} command line parameter. Can't be null but can be empty.
	 * @param passMask parsed argument from {@value Constants#ARG_PASS} command line parameter. Can't be null but can be empty.
	 * @param removeMask parsed argument from {@value Constants#ARG_REMOVE} command line parameter. Can't be null but can be empty.
	 * @param renameMask parsed argument from {@value Constants#ARG_RENAME} command line parameter. Can't be null but can be empty.
	 * @throws IllegalArgumentException on any argument errors
	 * @throws SyntaxException when any pattern error was detected.
	 */
	protected AbstractZipProcessor(final String[] processMask, final String[] passMask, final String[] removeMask, final String[][] renameMask) throws SyntaxException, IllegalArgumentException {
		if (processMask == null || processMask.length > 0 && Utils.checkArrayContent4Nulls(processMask, true) >= 0) {
			throw new IllegalArgumentException("Process mask is null or contains nulls/empties inside");
		}
		else if (passMask == null || passMask.length > 0 && Utils.checkArrayContent4Nulls(passMask, true) >= 0) {
			throw new IllegalArgumentException("Pass mask is null or contains nulls/empties inside");
		}
		else if (processMask.length == 0 && passMask.length == 0) {
			throw new IllegalArgumentException("Neither process mask nor pass mask is defined");
		}
		else if (removeMask == null || removeMask.length > 0 && Utils.checkArrayContent4Nulls(removeMask, true) >= 0) {
			throw new IllegalArgumentException("Remove mask is null or contains nulls/empties inside");
		}
		else if (renameMask == null || renameMask.length > 0 && Utils.checkArrayContent4Nulls(renameMask) >= 0) {
			throw new IllegalArgumentException("Rename mask is null or contains nulls inside");
		}
		else {
			
			for (String[] item : renameMask) {
				if (item == null || Utils.checkArrayContent4Nulls(item, true) >= 0) {
					throw new IllegalArgumentException("Rename mask is null or contains nulls/empties inside");
				}
				else if (item.length != 2) {
					throw new IllegalArgumentException("Rename mask must contains exactly tww strings for every rename item");
				}
			}
			this.toProcess = new Pattern[processMask.length];
			this.toPass = new Pattern[passMask.length];
			this.toRemove = new Pattern[removeMask.length];
			this.toRename = new Pattern[renameMask.length];
			this.renameFormat = new String[renameMask.length];
			
			int count = 0;
			for(String item : processMask) {
				try {
					toProcess[count++] = Pattern.compile(item);
				} catch (PatternSyntaxException exc) {
					throw new SyntaxException(count, 0, "processMask pattern ["+item+"] error : "+exc.getLocalizedMessage());
				}
			}
			count = 0;
			for(String item : passMask) {
				try {
					toPass[count++] = Pattern.compile(item);
				} catch (PatternSyntaxException exc) {
					throw new SyntaxException(count, 0, "passMask pattern ["+item+"] error : "+exc.getLocalizedMessage());
				}
			}
			count = 0;
			for(String item : removeMask) {
				try {
					toRemove[count++] = Pattern.compile(item);
				} catch (PatternSyntaxException exc) {
					throw new SyntaxException(count, 0, "removeMask pattern ["+item+"] error : "+exc.getLocalizedMessage());
				}
			}
			count = 0;
			for(String[] item : renameMask) {
				try {
					toRename[count] = Pattern.compile(item[0]);
					renameFormat[count] = item[1];
				} catch (PatternSyntaxException exc) {
					throw new SyntaxException(count, 0, "renameMask pattern ["+item[0]+"] error : "+exc.getLocalizedMessage());
				}
			}
		}
	}
	
	/**
	 * <p>This is a callback to process part content. The callback will be called for all the parts matched to {@value Constants#ARG_PROCESS} or <i>not</i> matched to {@value Constants#ARG_PASS} command line argument.
	 * The method will be called after calling {@linkplain #processTicket(SubstitutableProperties, LoggerFacade)} method</p>
	 * <p>Don't close source or target stream during processing (including implicit closing by using <b>try-with-resource</b> inside the method).</p> 
	 * @param part part name. Can't be null or empty.
	 * @param props content of the {@value Constants#PART_TICKET} part. Don't change it's content can't during this method call. Can't be null
	 * @param logger logger to print any processing errors into. Can't be null
	 * @param source source input stream. Can't be null. Avoid using cast to {@linkplain ZipInputStream} because it can be changed in the future. 
	 * @param target target output stream. Can't be null. Avoid using cast to {@linkplain ZipOutputStream} because it can be changed in the future.
	 * @throws IOException on any I/O errors.
	 */
	protected abstract void processPart(final String part, final SubstitutableProperties props, final LoggerFacade logger, final InputStream source, final OutputStream target) throws IOException; 

	/**
	 * <p>This is a callback to process {@linkplain Constants#PART_TICKET} part content. This method will be called as same first during ZIP processing.</p>
	 * @param props content of the {@linkplain Constants#PART_TICKET} part. Can't be null. Any changes you make with it will be reflected in the output ZIP content
	 * @param logger logger to print any processing errors into. Can't be null
	 * @throws IOException on any I/O errors.
	 */
	protected void processTicket(final SubstitutableProperties props, final LoggerFacade logger) throws IOException {
	}

	/**
	 * <p>This is a callback to process appending any data into Data Acquisition pipe. This method will be called as same last during ZIP processing.</p>
	 * @param props content of the {@value Constants#PART_TICKET} part. Don't change it's content can't during this method call. Can't be null
	 * @param logger logger to print any processing errors into. Can't be null
	 * @param target target output stream. Can't be null. Avoid using cast to {@linkplain ZipOutputStream} because it can be changed in the future.
	 * @throws IOException on any I/O errors.
	 */
	protected void processAppending(final SubstitutableProperties props, final LoggerFacade logger, final OutputStream target) throws IOException {
	}

	/**
	 * <p>This is an utility method to append new part into ZIP stream. Call this method from {@linkplain #processAppending(SubstitutableProperties, LoggerFacade, OutputStream)}
	 * method only</p>
	 * @param part part name to append. Can' be null or empty
	 * @param is content to append into ZIP stream. Can't be null
	 * @param target target output stream. Can't be null. Avoid using cast to {@linkplain ZipOutputStream} because it can be changed in the future.
	 * @throws IOException on any I/O errors
	 */
	protected void append(final String part, final InputStream is, final OutputStream target) throws IOException {
		newEntry(part, target);
		Utils.copyStream(is, target);
		closeEntry(target);
	}

	/**
	 * <p>Create new part entry in the output stream</p>
	 * @param part part name to create new entry. Can't be null or empty
	 * @param target output stream to create part into. Can't be null
	 * @throws IOException on any I/O errors
	 */
	protected void newEntry(final String part, final OutputStream target) throws IOException {
		final ZipEntry			zeOut = new ZipEntry(part);
		
		zeOut.setMethod(ZipEntry.DEFLATED);
		((ZipOutputStream)target).putNextEntry(zeOut);
	}
	
	/**
	 * <p>Close current entry in the output stream</p>
	 * @param target output stream to close part in. Can't be null
	 * @throws IOException on any I/O errors
	 */
	protected void closeEntry(final OutputStream target) throws IOException {
		((ZipOutputStream)target).closeEntry();
	}
	
	/**
	 * <p>Process Data Acquisition pipe.</p>
	 * @param zis input ZIP to process. Can't be null
	 * @param zos Output ZIP to process. Can't be null
	 * @throws NullPointerException any argument is null
	 * @throws IOException on any I/O errors
	 */
	public void process(final ZipInputStream zis, final ZipOutputStream zos) throws IOException, NullPointerException {
		if (zis == null) {
			throw new NullPointerException("Input stream can't be null"); 
		}
		else if (zos == null) {
			throw new NullPointerException("Output stream can't be null"); 
		}
		else {
			final LoggerFacade				slf = LoggerFacade.Factory.newInstance(URI.create(LoggerFacade.LOGGER_SCHEME+":string:/"));
			final SubstitutableProperties	props = new SubstitutableProperties();
			String		logContent = "";
			int 		state = STATE_BEFORE_TICKET;
			ZipEntry	ze;
			
			while ((ze = zis.getNextEntry()) != null) {
				switch (state) {
					case STATE_BEFORE_TICKET	:
						if (!Constants.PART_TICKET.equals(ze.getName())) {
							throw new IOException("Zip strem structure corrupted: the same first part in the stream must be ["+Constants.PART_TICKET+"]");
						}
						else {
							props.load(zis);
							processTicket(props, slf);
							newEntry(Constants.PART_TICKET, zos);
							props.store(zos, null);
							closeEntry(zos);
							state = STATE_INSIDE_CONTENT;
						}
						break;
					case STATE_INSIDE_CONTENT	:
						if (Constants.PART_LOG.equals(ze.getName())) {
							try(final StringWriter	wr = new StringWriter()) {
								Utils.copyStream(new InputStreamReader(zis), wr);
								logContent = wr.toString().trim();
							}
							state = STATE_BEFORE_LOG;
						}
						else if (mustPass(ze.getName())) {
							if (!mustRemove(ze.getName())) {
								append(renameIfRequired(ze.getName()), zis, zos);
							}
						}
						else {
							if (!mustRemove(ze.getName())) {
								newEntry(renameIfRequired(ze.getName()), zos);
								processPart(ze.getName(), props, slf, zis, zos);
								closeEntry(zos);
							}
							else {
								processPart(ze.getName(), props, slf, zis, NULL_OUTPUT);
							}
						}
						break;
					default :
						throw new UnsupportedOperationException("State ["+state+"] is not supported yet"); 
				}
			}
			if (state != STATE_BEFORE_LOG) {
				throw new IOException("Zip strem structure corrupted: the same last part in the stream must be ["+Constants.PART_LOG+"]");
			}
			else {
				processAppending(props, slf, zos);
				
				newEntry(Constants.PART_LOG, zos);
				
				final Writer wr = new OutputStreamWriter(zos, PureLibSettings.DEFAULT_CONTENT_ENCODING);
				final String newLogContent = slf.toString().trim(); 
						
				if (!Utils.checkEmptyOrNullString(logContent)) {
					wr.write(logContent);
					wr.write(System.lineSeparator());
				}
				if (!Utils.checkEmptyOrNullString(newLogContent)) {
					wr.write(newLogContent);
					wr.write(System.lineSeparator());
				}
				wr.flush();
				closeEntry(zos);
			}
		}
	}

	private boolean mustPass(final String part) {
		for (Pattern item : toPass) {
			if (item.matcher(part).find()) {
				return true;
			}
		}
		for (Pattern item : toProcess) {
			if (item.matcher(part).find()) {
				return false;
			}
		}
		return true;
	}

	private boolean mustRemove(final String part) {
		for (Pattern item : toRemove) {
			if (item.matcher(part).find()) {
				return true;
			}
		}
		return false;
	}

	private String renameIfRequired(final String source) {
		for (int index = 0; index < toRename.length; index++) {
			final Matcher	m = toRename[index].matcher(source);
			
			if (m.find()) {
				return m.replaceAll(renameFormat[index]);
			}
		}
		return source;
	}
}
