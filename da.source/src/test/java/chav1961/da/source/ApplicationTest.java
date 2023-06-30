package chav1961.da.source;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.junit.Assert;
import org.junit.Test;

import chav1961.da.util.Constants;
import chav1961.da.util.DAUtils;
import chav1961.purelib.basic.ArgParser;
import chav1961.purelib.basic.SubstitutableProperties;
import chav1961.purelib.basic.Utils;
import chav1961.purelib.basic.exceptions.ContentException;

public class ApplicationTest {

	@Test
	public void basicTest() throws IOException, ContentException {
		final ArgParser	ap = new Application.ApplicationArgParser().parse("-"+Application.ARG_START_PIPE,"-"+Application.ARG_APPEND,"fsys:"+new File("./src/test/resources/folder1").toURI());
		
		try(final InputStream			is = DAUtils.newEmptyZip(new SubstitutableProperties(Utils.mkProps("key","value")));
			final ZipInputStream		zis = new ZipInputStream(is);
			final ByteArrayOutputStream	baos = new ByteArrayOutputStream();
			final ZipOutputStream		zos = new ZipOutputStream(baos)) {
			final Application			app = new Application(new String[0], new String[0][], ap.getValue(Application.ARG_APPEND, String[].class), System.err, false, "", false);
			
			app.process(zis, zos);
			zos.finish();
			
			try(final InputStream		isR = new ByteArrayInputStream(baos.toByteArray());
				final ZipInputStream	zisR = new ZipInputStream(isR)) {
				ZipEntry	ze;
			
				ze = zisR.getNextEntry();
				Assert.assertEquals(Constants.PART_TICKET, ze.getName());

				ze = zisR.getNextEntry();
				Assert.assertEquals("/x.txt", ze.getName());

				ze = zisR.getNextEntry();
				Assert.assertEquals(Constants.PART_LOG, ze.getName());
				
				Assert.assertNull(zis.getNextEntry());
			}
		}
	}
	
	@Test
	public void mainTest() throws IOException, ContentException {
		try(final InputStream			source = new ByteArrayInputStream("key=value\n".getBytes());
			final ByteArrayOutputStream	baos = new ByteArrayOutputStream()) {
			
			Application.main(source, new String[]{"-"+Application.ARG_START_PIPE,"-"+Application.ARG_APPEND,"fsys:"+new File("./src/test/resources/folder1").toURI()}, baos, System.err);

			try(final InputStream		is = new ByteArrayInputStream(baos.toByteArray());
				final ZipInputStream	zis = new ZipInputStream(is)) {
				final Properties		props = new Properties();
				ZipEntry	ze;
			
				ze = zis.getNextEntry();
				Assert.assertEquals(Constants.PART_TICKET, ze.getName());
				props.load(zis);
				
				Assert.assertEquals(Utils.mkProps("key","value"), props);

				ze = zis.getNextEntry();
				Assert.assertEquals("/x.txt", ze.getName());

				ze = zis.getNextEntry();
				Assert.assertEquals(Constants.PART_LOG, ze.getName());
				
				Assert.assertNull(zis.getNextEntry());
			}
		}
	}	
}
