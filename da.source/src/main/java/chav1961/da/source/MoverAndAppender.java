package chav1961.da.source;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import chav1961.da.util.ZipProcessingClass;
import chav1961.da.util.ZipProcessingClass.EntityProcessor;
import chav1961.da.util.interfaces.InputFormat;
import chav1961.purelib.basic.Utils;
import chav1961.purelib.basic.exceptions.CommandLineParametersException;
import chav1961.purelib.basic.exceptions.ContentException;
import chav1961.purelib.basic.interfaces.LoggerFacade;
import chav1961.purelib.enumerations.ContinueMode;
import chav1961.purelib.fsys.FileSystemFactory;
import chav1961.purelib.fsys.FileSystemOnFile;
import chav1961.purelib.fsys.interfaces.FileSystemInterface;

public class MoverAndAppender implements EntityProcessor {
	private final URI[]	uris;
	
	public MoverAndAppender(final String... uris) throws CommandLineParametersException {
		if (uris == null || Utils.checkArrayContent4Nulls(uris, true) >= 0) {
			throw new IllegalArgumentException("Uri string list can't be null and can't contains nulls or empties inside");
		}
		else {
			this.uris = new URI[uris.length];
			
			for (int index = 0; index < uris.length; index++) {
				this.uris[index] = URI.create(uris[index]);
			}
		}
	}
	
	@Override
	public void process(final InputStream reader, final OutputStream writer, final String partName, final InputFormat format, final LoggerFacade logger, final boolean debug) throws IOException {
		Utils.copyStream(reader, writer);
	}

	@Override
	public void append(final ZipOutputStream writer, final LoggerFacade logger, final boolean debug) throws IOException {
		for(URI item : uris) {
			if (item.isAbsolute()) { 
				if(FileSystemInterface.FILESYSTEM_URI_SCHEME.equals(item.getScheme())) {
					try(final FileSystemInterface	fsi = FileSystemFactory.createFileSystem(item)) {
						append(fsi, writer, logger, debug);
					}
				}
				else if(!"jar".equals(item.getScheme()) || item.getRawPath().contains("!")) {
					try(final InputStream	is = item.toURL().openStream()) {
						final String		name = rename(item.getPath());
						final ZipEntry		ze = new ZipEntry(name);
						
						ze.setMethod(ZipEntry.DEFLATED);
						writer.putNextEntry(ze);
						Utils.copyStream(is, writer);
						writer.closeEntry();
					}
				}
				else {
					try(final InputStream	is = item.toURL().openStream()) {
						ZipProcessingClass.copyZip(is, writer, debug);
					} catch (ContentException e) {
						throw new IOException(e.getLocalizedMessage(), e);
					}
				}
			}
			else {
				try(final FileSystemInterface	fsi = new FileSystemOnFile(new File(item.toString()).toURI())) {
					append(fsi, writer, logger, debug);
				}
			}
		}
	}

	private void append(final FileSystemInterface fsi, final ZipOutputStream writer, final LoggerFacade logger, boolean debug) throws IOException {
		if (fsi.isDirectory()) {
			fsi.list((i)->{
				append(i, writer, logger, debug);
				return ContinueMode.CONTINUE;
			});
		}
		else {
			try(final InputStream	is = fsi.read()) {
				final ZipEntry		ze = new ZipEntry(fsi.getPath());
				
				ze.setMethod(ZipEntry.DEFLATED);
				writer.putNextEntry(ze);
				Utils.copyStream(is, writer);
				writer.closeEntry();
			}
		}
	}
	
	
}
