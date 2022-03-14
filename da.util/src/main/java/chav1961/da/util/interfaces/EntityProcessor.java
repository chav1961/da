package chav1961.da.util.interfaces;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipOutputStream;

import chav1961.purelib.basic.SubstitutableProperties;
import chav1961.purelib.basic.interfaces.LoggerFacade;

@FunctionalInterface
public interface EntityProcessor extends RenamingInterface {
	void processEntry(InputStream reader, OutputStream writer, String partName, DAContentFormat format, LoggerFacade logger, boolean debug) throws IOException;
	
	default SubstitutableProperties processTicket(final SubstitutableProperties props) throws IOException {
		return props;
	}
	
	default String renameEntry(String partName) {
		return partName;
	}
	
	default void appendEntries(ZipOutputStream writer, LoggerFacade logger, boolean debug) throws IOException {
	}
}