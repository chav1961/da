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
										
	protected AbstractZipProcessor(final String[] processMask, final String[] passMask, final String[] removeMask, final String[][] renameMask) throws SyntaxException {
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
	
	protected abstract void processPart(final String part, final SubstitutableProperties props, final LoggerFacade logger, final InputStream source, final OutputStream target) throws IOException; 
	
	protected void processTicket(final SubstitutableProperties props, final LoggerFacade logger) throws IOException {
	}

	protected void processAppending(final SubstitutableProperties props, final LoggerFacade logger, final ZipOutputStream zos) throws IOException {
	}
	
	protected void append(final String part, final InputStream is, final ZipOutputStream zos) throws IOException {
		final ZipEntry	zeOut = new ZipEntry(part);
		
		zeOut.setMethod(ZipEntry.DEFLATED);
		zos.putNextEntry(zeOut);
		Utils.copyStream(is, zos);
		zos.closeEntry();
	}
	
	public void process(final ZipInputStream zis, final ZipOutputStream zos) throws IOException {
		final LoggerFacade				slf = LoggerFacade.Factory.newInstance(URI.create(LoggerFacade.LOGGER_SCHEME+":string:/"));
		final SubstitutableProperties	props = new SubstitutableProperties();
		String		logContent = "";
		int 		state = STATE_BEFORE_TICKET;
		ZipEntry	ze, zeOut;
		
		while ((ze = zis.getNextEntry()) != null) {
			switch (state) {
				case STATE_BEFORE_TICKET	:
					if (!Constants.PART_TICKET.equals(ze.getName())) {
						throw new IOException("Zip strem structure corrupted: the same first part in the stream must be ["+Constants.PART_TICKET+"]");
					}
					else {
						props.load(zis);
						processTicket(props, slf);
						
						zeOut = new ZipEntry(Constants.PART_TICKET);
						zeOut.setMethod(ZipEntry.DEFLATED);
						zos.putNextEntry(zeOut);
						props.store(zos, null);
						zos.closeEntry();
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
							zeOut = new ZipEntry(renameIfRequired(ze.getName()));
							zeOut.setMethod(ZipEntry.DEFLATED);
							zos.putNextEntry(zeOut);
							processPart(ze.getName(), props, slf, zis, zos);
							zos.closeEntry();
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
			
			zeOut = new ZipEntry(Constants.PART_LOG);
			zeOut.setMethod(ZipEntry.DEFLATED);
			zos.putNextEntry(zeOut);
			
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
			zos.closeEntry();
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
