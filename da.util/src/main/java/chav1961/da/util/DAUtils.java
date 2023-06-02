package chav1961.da.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import chav1961.purelib.basic.SubstitutableProperties;

public class DAUtils {
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
}
