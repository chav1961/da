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
import chav1961.purelib.basic.exceptions.CommandLineParametersException;

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

	@Test
	public void parseRenameArgumentTest() throws IOException, CommandLineParametersException {
		String[][]	rename = DAUtils.parseRenameArgument("x::y");
		
		Assert.assertEquals(1, rename.length);
		Assert.assertArrayEquals(new String[] {"x","y"}, rename[0]);

		rename = DAUtils.parseRenameArgument("x::y;a::b");
		
		Assert.assertEquals(2, rename.length);
		Assert.assertArrayEquals(new String[] {"x","y"}, rename[0]);
		Assert.assertArrayEquals(new String[] {"a","b"}, rename[1]);
		
		try{DAUtils.parseRenameArgument(null);
			Assert.fail("Mandatory exception was not detected (null 1-st argument)");
		} catch (IllegalArgumentException exc) {
		}
		try{DAUtils.parseRenameArgument("");
			Assert.fail("Mandatory exception was not detected (empty 1-st argument)");
		} catch (IllegalArgumentException exc) {
		}
		try{DAUtils.parseRenameArgument(";");
			Assert.fail("Mandatory exception was not detected (1-st argument contains empties (;) inside)");
		} catch (CommandLineParametersException exc) {
		}
		try{DAUtils.parseRenameArgument("x");
			Assert.fail("Mandatory exception was not detected (1-st argument missing '->'");
		} catch (CommandLineParametersException exc) {
		}
	}	
	
	static Map<String,String> loadZipContent(final InputStream is) {
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
