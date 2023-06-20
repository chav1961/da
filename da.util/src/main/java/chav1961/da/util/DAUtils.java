package chav1961.da.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import chav1961.purelib.basic.SubstitutableProperties;
import chav1961.purelib.basic.Utils;
import chav1961.purelib.basic.exceptions.CommandLineParametersException;

/**
 * <p>This class contanns a set of useful methods to use in Data Acquisition project.</p> 
 * @author Alexander Chernomyrdin aka chav1961
 * @since 0.0.1
 */
public class DAUtils {
	private static final String		RENAME_DIVIDER = "->";
	
	private DAUtils() {}
	
	/**
	 * <p>Create new empty ZIP source according to Data Acquisition pipe standard</p>
	 * @param props properties to use as {@value Constants#PART_TICKET} content. Can't be null</p>
	 * @return ZIP source. Can't be null.
	 * @throws NullPointerException when properties is null
	 * @throws IOException on any I/O errors
	 */
	public static InputStream newEmptyZip(final SubstitutableProperties props) throws NullPointerException, IOException {
		if (props == null) {
			throw new NullPointerException("Properteis can't be null");
		}
		else {
			try(final ByteArrayOutputStream		baos = new ByteArrayOutputStream()) {
				try(final ZipOutputStream		zos = new ZipOutputStream(baos)) {
				
					ZipEntry	ze = new ZipEntry(Constants.PART_TICKET);
					
					ze.setMethod(ZipEntry.DEFLATED);
					zos.putNextEntry(ze);
					props.store(zos, null);
					zos.closeEntry();
					
					ze = new ZipEntry(Constants.PART_LOG);
					
					ze.setMethod(ZipEntry.DEFLATED);
					zos.putNextEntry(ze);
					zos.closeEntry();
				}
				return new ByteArrayInputStream(baos.toByteArray());
			}
		}
	}

	/**
	 * <p>Parse {@value Constants#ARG_RENAME} argument from the command line. Argument string must be typed as:</p>
	 * <code>
	 * argument = arg (';' argument)*
	 * arg = pattern '->' format
	 * </code>
	 * <p>Pattern and format values see  {@linkplain String#replaceAll(String, String)} method description.</p>
	 * @param rename rename argument value. Can't be null or empty
	 * @return arguments parsed. Will be String[*][2] = {{pattern, format}, ... }. Can't be null or empty array
	 * @throws IllegalArgumentException string is null or empty
	 * @throws CommandLineParametersException on any format errors
	 */
	public static String[][] parseRenameArgument(final String rename) throws IllegalArgumentException, CommandLineParametersException {
		if (Utils.checkEmptyOrNullString(rename)) {
			throw new IllegalArgumentException("Rename string can't be null or empty");
		}
		else {
			final String[]		source = rename.split(";");
			
			if (source.length == 0 || Utils.checkArrayContent4Nulls(source, true) >= 0) {
				throw new CommandLineParametersException("Rename list contains empties inside"); 
			}
			else {
				final String[][]	result = new String[source.length][];
				
				for(int index = 0; index < result.length; index++) {
					result[index] = parseRenameArgumentInternal(source[index]);
				}
				return result;
			}
		}
	}
	
	private static String[] parseRenameArgumentInternal(final String arg) throws CommandLineParametersException {
		final int 	index = arg.indexOf(RENAME_DIVIDER);
		
		if (index < 0) {
			throw new CommandLineParametersException("Illegal rename argument ["+arg+"]: missing '->'"); 
		}
		else {
			return new String[] {arg.substring(0, index).trim(), arg.substring(index + RENAME_DIVIDER.length()).trim()};
		}
	}
}
