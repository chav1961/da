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
import chav1961.purelib.basic.exceptions.SyntaxException;

public class DAUtils {
	private static final String		RENAME_DIVIDER = "->";
	
	private DAUtils() {}
	
	public static InputStream newEmptyZip(final SubstitutableProperties props) throws IOException {
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
