package chav1961.da.util;

import org.junit.Assert;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.Test;

import chav1961.purelib.basic.PureLibSettings;
import chav1961.purelib.basic.SubstitutableProperties;
import chav1961.purelib.basic.Utils;

public class DAUtilsTest {
	@Test
	public void newEmptyZipTest() throws IOException {
		final Map<String,String> result = loadZipContent(DAUtils.newEmptyZip(new SubstitutableProperties()));
		
		Assert.assertTrue(result.containsKey(Constants.PART_TICKET));
		Assert.assertTrue(result.containsKey(Constants.PART_LOG));
		
		try{DAUtils.newEmptyZip(null);
			Assert.fail("Mandatory exception was not detected (null 1-st argument)");
		} catch (NullPointerException exc) {
		}
	}

	public static Map<String,String> loadZipContent(final InputStream is) {
		final Map<String,String>	result = new HashMap<>(); 
		
		try(final ZipInputStream zis = new ZipInputStream(is)) {
			ZipEntry	ze;
			
			while ((ze = zis.getNextEntry()) != null) {
				final Reader	rdr = new InputStreamReader(zis, PureLibSettings.DEFAULT_CONTENT_ENCODING);
				final Writer	wr = new StringWriter();
				
				Utils.copyStream(rdr, wr);
				result.put(ze.getName(), wr.toString());
			}
		} catch (IOException e) {
		}
		return result;
	}
}
