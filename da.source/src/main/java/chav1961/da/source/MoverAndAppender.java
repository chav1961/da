package chav1961.da.source;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.zip.ZipOutputStream;

import chav1961.da.util.ZipProcessingClass;
import chav1961.da.util.interfaces.EntityProcessor;
import chav1961.da.util.interfaces.DAContentFormat;
import chav1961.da.util.interfaces.RenamingInterface;
import chav1961.purelib.basic.Utils;
import chav1961.purelib.basic.exceptions.CommandLineParametersException;
import chav1961.purelib.basic.exceptions.ContentException;
import chav1961.purelib.basic.interfaces.LoggerFacade;

public class MoverAndAppender implements EntityProcessor {
	private final URI[]				uris;
	private final RenamingInterface	ri;
	
	public MoverAndAppender(final String... uris) throws CommandLineParametersException, IllegalArgumentException, NullPointerException {
		this((s)->s, uris);
	}
	
	public MoverAndAppender(final RenamingInterface ren, final String... uris) throws CommandLineParametersException, IllegalArgumentException, NullPointerException {
		if (ren == null) {
			throw new NullPointerException("Renaming interface can't be null");
		}
		else if (uris == null || Utils.checkArrayContent4Nulls(uris, true) >= 0) {
			throw new IllegalArgumentException("Uri string list can't be null and can't contains nulls or empties inside");
		}
		else {
			this.ri = ren;
			this.uris = new URI[uris.length];
			
			for (int index = 0; index < uris.length; index++) {
				this.uris[index] = URI.create(uris[index]);
			}
		}
	}
	
	@Override
	public String renameEntry(final String partName) {
		return ri.renameEntry(partName);
	}
	
	@Override
	public void processEntry(final InputStream reader, final OutputStream writer, final String partName, final DAContentFormat format, final LoggerFacade logger, final boolean debug) throws IOException {
		Utils.copyStream(reader, writer);
	}

	@Override
	public void appendEntries(final ZipOutputStream writer, final LoggerFacade logger, final boolean debug) throws IOException {
		for(URI item : uris) {
			try{ZipProcessingClass.copyZip(item, writer, ZipProcessingClass.NONE_PATTERN, this, logger, debug);
			} catch (ContentException e) {
				throw new IOException(e.getLocalizedMessage(), e); 
			}
		}
	}
}
