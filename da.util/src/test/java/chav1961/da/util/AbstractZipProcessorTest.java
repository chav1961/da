package chav1961.da.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.Map;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.junit.Assert;
import org.junit.Test;

import chav1961.purelib.basic.SubstitutableProperties;
import chav1961.purelib.basic.Utils;
import chav1961.purelib.basic.exceptions.SyntaxException;
import chav1961.purelib.basic.interfaces.LoggerFacade;
import chav1961.purelib.basic.interfaces.LoggerFacade.Severity;

public class AbstractZipProcessorTest {
	static final String		PART_PROCESS = "partProcess";
	static final String		PART_PASS = "partPass";
	static final String		PART_REMOVE = "partRemove";
	static final String		PART_RENAME = "partRename";
	
	private static final String[]		EMPTY_STRING = new String[] {""};
	private static final String[][]		NULL_STRING_PAIR = new String[0][];
	private static final OutputStream	NULL_STREAM = new OutputStream() {
											@Override public void write(int b) throws IOException {}
										};
										
	
	@Test
	public void basicTest() throws SyntaxException, IOException {
		final AbstractZipProcessor	azp = new PseudoZipProcessor(Constants.MASK_ANY, Constants.MASK_NONE, Constants.MASK_NONE, NULL_STRING_PAIR);

		try(final ByteArrayOutputStream	baos = new ByteArrayOutputStream()) {
			try(final ZipInputStream	zis = new ZipInputStream(DAUtils.newEmptyZip(new SubstitutableProperties()));
				final ZipOutputStream	zos = new ZipOutputStream(baos)) {
				
				azp.process(zis, zos);
			} catch (IOException exc) {
			}
			final Map<String, String>	result = DAUtilsTest.loadZipContent(new ByteArrayInputStream(baos.toByteArray()));
			
			Assert.assertTrue(result.containsKey(Constants.PART_TICKET));
			Assert.assertTrue(result.containsKey(Constants.PART_LOG));
		}
		
		try(final ZipInputStream	zis = new ZipInputStream(this.getClass().getResourceAsStream("/illegal.zip"));
			final ZipOutputStream	zos = new ZipOutputStream(NULL_STREAM)) {
			
			azp.process(zis, zos);
			Assert.fail("Mandatory exception was not detected (ZIP content corrupted)");
		} catch (IOException exc) {
		}
		
		try{new PseudoZipProcessor(null, Constants.MASK_NONE, Constants.MASK_NONE, NULL_STRING_PAIR);
			Assert.fail("Mandatory exception was not detected (null 1-st argument)");
		} catch (IllegalArgumentException exc) {
		}
		try{new PseudoZipProcessor(EMPTY_STRING, Constants.MASK_NONE, Constants.MASK_NONE, NULL_STRING_PAIR);
			Assert.fail("Mandatory exception was not detected (empty 1-st argument)");
		} catch (IllegalArgumentException exc) {
		}
		
		try{new PseudoZipProcessor(Constants.MASK_NONE, null, Constants.MASK_NONE, NULL_STRING_PAIR);
			Assert.fail("Mandatory exception was not detected (null 2-st argument)");
		} catch (IllegalArgumentException exc) {
		}
		try{new PseudoZipProcessor(Constants.MASK_NONE, EMPTY_STRING, Constants.MASK_NONE, NULL_STRING_PAIR);
			Assert.fail("Mandatory exception was not detected (empty 2-st argument)");
		} catch (IllegalArgumentException exc) {
		}
		try{new PseudoZipProcessor(Constants.MASK_NONE, Constants.MASK_NONE, Constants.MASK_NONE, NULL_STRING_PAIR);
			Assert.fail("Mandatory exception was not detected (both 1-st and 2-st arguments are missing)");
		} catch (IllegalArgumentException exc) {
		}
		
		try{new PseudoZipProcessor(Constants.MASK_NONE, Constants.MASK_NONE, null, NULL_STRING_PAIR);
			Assert.fail("Mandatory exception was not detected (null 3-rd argument)");
		} catch (IllegalArgumentException exc) {
		}
		try{new PseudoZipProcessor(Constants.MASK_NONE, Constants.MASK_NONE, EMPTY_STRING, NULL_STRING_PAIR);
			Assert.fail("Mandatory exception was not detected (empty 3-rd argument)");
		} catch (IllegalArgumentException exc) {
		}
		
		try{new PseudoZipProcessor(Constants.MASK_NONE, Constants.MASK_NONE, Constants.MASK_NONE, null);
			Assert.fail("Mandatory exception was not detected (null 4-th argument)");
		} catch (IllegalArgumentException exc) {
		}
		try{new PseudoZipProcessor(Constants.MASK_NONE, Constants.MASK_NONE, Constants.MASK_NONE, NULL_STRING_PAIR);
			Assert.fail("Mandatory exception was not detected (empty pairs in 4-th argument)");
		} catch (IllegalArgumentException exc) {
		}	
	}
	
	@Test
	public void processingTest() throws SyntaxException, IOException {
		final AbstractZipProcessor	prep = new PrepareZipProcessor();
		final byte[]				content;
		
		try(final ByteArrayOutputStream	baos = new ByteArrayOutputStream()) {
			try(final InputStream		is = DAUtils.newEmptyZip(new SubstitutableProperties());
				final ZipInputStream	zis = new ZipInputStream(is);
				final ZipOutputStream	zos = new ZipOutputStream(baos)) {
				
				prep.process(zis, zos);
			}
			content = baos.toByteArray();
		}
		
		try(final ByteArrayOutputStream	baos = new ByteArrayOutputStream()) {
			try(final InputStream		is = new ByteArrayInputStream(content);
				final ZipInputStream	zis = new ZipInputStream(is);
				final ZipOutputStream	zos = new ZipOutputStream(baos)) {
				
				new PseudoZipProcessor(Constants.MASK_ANY, Constants.MASK_NONE, Constants.MASK_NONE, NULL_STRING_PAIR).process(zis, zos);
			}
			final Map<String, String>	result = DAUtilsTest.loadZipContent(new ByteArrayInputStream(baos.toByteArray()));
			
			Assert.assertTrue(result.containsKey(Constants.PART_TICKET));
			Assert.assertTrue(result.containsKey(Constants.PART_LOG));
			Assert.assertTrue(result.containsKey(PART_PROCESS));
			Assert.assertTrue(result.containsKey(PART_PASS));
			Assert.assertTrue(result.containsKey(PART_REMOVE));
			Assert.assertTrue(result.containsKey(PART_RENAME));
			
			Assert.assertEquals(PART_PROCESS.toUpperCase(), result.get(PART_PROCESS).trim());
			Assert.assertEquals(PART_PASS.toUpperCase(), result.get(PART_PASS).trim());
			Assert.assertEquals(PART_REMOVE.toUpperCase(), result.get(PART_REMOVE).trim());
			Assert.assertEquals(PART_RENAME.toUpperCase(), result.get(PART_RENAME).trim());
			Assert.assertTrue(result.get(Constants.PART_LOG).contains("the end"));
		}

		try(final ByteArrayOutputStream	baos = new ByteArrayOutputStream()) {
			try(final InputStream		is = new ByteArrayInputStream(content);
				final ZipInputStream	zis = new ZipInputStream(is);
				final ZipOutputStream	zos = new ZipOutputStream(baos)) {
				
				new PseudoZipProcessor(Constants.MASK_NONE, Constants.MASK_ANY, Constants.MASK_NONE, NULL_STRING_PAIR).process(zis, zos);
			}
			final Map<String, String>	result = DAUtilsTest.loadZipContent(new ByteArrayInputStream(baos.toByteArray()));
			
			Assert.assertTrue(result.containsKey(Constants.PART_TICKET));
			Assert.assertTrue(result.containsKey(Constants.PART_LOG));
			Assert.assertTrue(result.containsKey(PART_PROCESS));
			Assert.assertTrue(result.containsKey(PART_PASS));
			Assert.assertTrue(result.containsKey(PART_REMOVE));
			Assert.assertTrue(result.containsKey(PART_RENAME));
			
			Assert.assertEquals(PART_PROCESS, result.get(PART_PROCESS).trim());
			Assert.assertEquals(PART_PASS, result.get(PART_PASS).trim());
			Assert.assertEquals(PART_REMOVE, result.get(PART_REMOVE).trim());
			Assert.assertEquals(PART_RENAME, result.get(PART_RENAME).trim());
		}

		try(final ByteArrayOutputStream	baos = new ByteArrayOutputStream()) {
			try(final InputStream		is = new ByteArrayInputStream(content);
				final ZipInputStream	zis = new ZipInputStream(is);
				final ZipOutputStream	zos = new ZipOutputStream(baos)) {
				
				new PseudoZipProcessor(Constants.MASK_NONE, Constants.MASK_ANY, new String[] {PART_REMOVE}, NULL_STRING_PAIR).process(zis, zos);
			}
			final Map<String, String>	result = DAUtilsTest.loadZipContent(new ByteArrayInputStream(baos.toByteArray()));
			
			Assert.assertTrue(result.containsKey(Constants.PART_TICKET));
			Assert.assertTrue(result.containsKey(Constants.PART_LOG));
			Assert.assertTrue(result.containsKey(PART_PROCESS));
			Assert.assertTrue(result.containsKey(PART_PASS));
			Assert.assertFalse(result.containsKey(PART_REMOVE));
			Assert.assertTrue(result.containsKey(PART_RENAME));
			
			Assert.assertEquals(PART_PROCESS, result.get(PART_PROCESS).trim());
			Assert.assertEquals(PART_PASS, result.get(PART_PASS).trim());
			Assert.assertEquals(PART_RENAME, result.get(PART_RENAME).trim());
		}

		try(final ByteArrayOutputStream	baos = new ByteArrayOutputStream()) {
			try(final InputStream		is = new ByteArrayInputStream(content);
				final ZipInputStream	zis = new ZipInputStream(is);
				final ZipOutputStream	zos = new ZipOutputStream(baos)) {
				
				new PseudoZipProcessor(Constants.MASK_NONE, Constants.MASK_ANY, Constants.MASK_NONE, new String[][] {new String[] {PART_RENAME, "assa"}}).process(zis, zos);
			}
			final Map<String, String>	result = DAUtilsTest.loadZipContent(new ByteArrayInputStream(baos.toByteArray()));
			
			Assert.assertTrue(result.containsKey(Constants.PART_TICKET));
			Assert.assertTrue(result.containsKey(Constants.PART_LOG));
			Assert.assertTrue(result.containsKey(PART_PROCESS));
			Assert.assertTrue(result.containsKey(PART_PASS));
			Assert.assertTrue(result.containsKey(PART_REMOVE));
			Assert.assertFalse(result.containsKey(PART_RENAME));
			Assert.assertTrue(result.containsKey("assa"));
			
			Assert.assertEquals(PART_PROCESS, result.get(PART_PROCESS).trim());
			Assert.assertEquals(PART_PASS, result.get(PART_PASS).trim());
			Assert.assertEquals(PART_REMOVE, result.get(PART_REMOVE).trim());
			Assert.assertEquals(PART_RENAME, result.get("assa").trim());
		}
	}	
}


class PseudoZipProcessor extends AbstractZipProcessor {
	public PseudoZipProcessor(final String[] processMask, final String[] passMask, final String[] removeMask, final String[][] renameMask) throws SyntaxException {
		super(processMask, passMask, removeMask, renameMask);
	}

	@Override
	protected void processPart(final String part, final SubstitutableProperties props, final LoggerFacade logger, final InputStream source, final OutputStream target) throws IOException {
		final Reader			rdr = new InputStreamReader(source);
		final BufferedReader	brdr = new BufferedReader(rdr);
		final PrintWriter		pwr = new PrintWriter(target);
		
		String line;
		while ((line = brdr.readLine()) != null) {
			pwr.println(line.toUpperCase());
		}
		pwr.flush();
		logger.message(Severity.info, "the end");
	}
	
	@Test
	public void processingTest() throws SyntaxException, IOException {
	}	
}

class PrepareZipProcessor extends AbstractZipProcessor {
	public PrepareZipProcessor() throws SyntaxException {
		super(new String[] {".*"}, new String[0], new String[0], new String[0][]);
	}

	@Override
	protected void processPart(final String part, final SubstitutableProperties props, final LoggerFacade logger, final InputStream source, final OutputStream target) throws IOException {
		Utils.copyStream(source, target);
	}
	
	@Override
	protected void processAppending(final SubstitutableProperties props, final LoggerFacade logger, final OutputStream zos) throws IOException {
		append(AbstractZipProcessorTest.PART_PROCESS, new ByteArrayInputStream(AbstractZipProcessorTest.PART_PROCESS.getBytes()), zos);
		append(AbstractZipProcessorTest.PART_PASS, new ByteArrayInputStream(AbstractZipProcessorTest.PART_PASS.getBytes()), zos);
		append(AbstractZipProcessorTest.PART_REMOVE, new ByteArrayInputStream(AbstractZipProcessorTest.PART_REMOVE.getBytes()), zos);
		append(AbstractZipProcessorTest.PART_RENAME, new ByteArrayInputStream(AbstractZipProcessorTest.PART_RENAME.getBytes()), zos);
	}
}
