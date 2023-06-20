package chav1961.da.converter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.junit.Assert;
import org.junit.Test;

import chav1961.da.util.Constants;
import chav1961.da.util.interfaces.ContentFormat;
import chav1961.purelib.basic.SubstitutableProperties;
import chav1961.purelib.basic.Utils;
import chav1961.purelib.basic.exceptions.SyntaxException;

public class ApplicationTest {

	@Test
	public void basicTest() throws SyntaxException, IOException {
		final Application	app = new Application(Constants.MASK_ANY, Constants.MASK_NONE, Constants.MASK_NONE, new String[0][], 
											ContentFormat.TURTLE, ContentFormat.NTRIPLES, System.err, false);
		final byte[]		content;
		
		try(final InputStream		is = this.getClass().getResourceAsStream("test.zip");
			final ZipInputStream	zis = new ZipInputStream(is);
			final OutputStream		os = new ByteArrayOutputStream();
			final ZipOutputStream	zos = new ZipOutputStream(os)) {
			
			app.process(zis, zos);
			zos.finish();
			content = ((ByteArrayOutputStream)os).toByteArray();
		}
		
		try(final InputStream		is = new ByteArrayInputStream(content);
			final ZipInputStream	zis = new ZipInputStream(is)) {
			
			ZipEntry	ze;
			
			while ((ze = zis.getNextEntry()) != null) {
				switch (ze.getName()) {
					case Constants.PART_TICKET :
						final SubstitutableProperties	props = SubstitutableProperties.of(zis);
						
						Assert.assertEquals("NTRIPLES", props.getProperty(Constants.PART_KEY_CONTENT_TYPE));
						break;
					case Constants.PART_LOG :
						final String	log = Utils.fromResource(new InputStreamReader(zis)).trim();

						Assert.assertEquals("", log);
						break;
					case "part.ttl.txt" :
						final String	converted = Utils.fromResource(new InputStreamReader(zis));
						final Pattern	pattern = Pattern.compile("(\\s*#.*|\\s*\\Q<http:\\E[^>]+>\\s*\\Q<http:\\E[^>]+>.*)");
						
						for (String item : converted.split("\n")) {
							final String	line = item.trim();
							
							if (!line.isEmpty()) {
								Assert.assertTrue(pattern.matcher(line).find());
							}
						}
						break;
					default :
						Assert.fail("Unknown part name ["+ze.getName()+"] in the source");
				}
			}
		}
	}
}
