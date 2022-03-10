package chav1961.da.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.junit.Assert;
import org.junit.Test;

import chav1961.da.util.interfaces.EntityProcessor;
import chav1961.da.util.interfaces.InputFormat;
import chav1961.purelib.basic.ArgParser;
import chav1961.purelib.basic.SubstitutableProperties;
import chav1961.purelib.basic.URIUtils;
import chav1961.purelib.basic.Utils;
import chav1961.purelib.basic.exceptions.CommandLineParametersException;
import chav1961.purelib.basic.exceptions.ContentException;
import chav1961.purelib.basic.interfaces.LoggerFacade;
import chav1961.purelib.basic.interfaces.LoggerFacade.Severity;

public class ZipProcessingClassTest {

	@Test
	public void checkZipParametersTest() throws CommandLineParametersException {
		final ArgParser	parser = new TestArgParser();
		
		Assert.assertFalse(ZipProcessingClass.checkZipParameters(parser.parse()));
		Assert.assertTrue(ZipProcessingClass.checkZipParameters(parser.parse("-"+Constants.ARG_ZIP)));
		Assert.assertTrue(ZipProcessingClass.checkZipParameters(parser.parse("-"+Constants.ARG_ZIP,"-"+Constants.ARG_PROCESS,".*","-"+Constants.ARG_EXCLUDE,".*")));

		try{ZipProcessingClass.checkZipParameters(null);
			Assert.fail("Mandatory exception was not detected (null1-st argument)");
		} catch (NullPointerException exc) {
		}
		
		try{ZipProcessingClass.checkZipParameters(parser.parse("-"+Constants.ARG_EXCLUDE,".*"));
			Assert.fail("Mandatory exception was not detected (-exclude without -zip)");
		} catch (CommandLineParametersException exc) {
		}

		try{ZipProcessingClass.checkZipParameters(parser.parse("-"+Constants.ARG_PROCESS,".*"));
			Assert.fail("Mandatory exception was not detected (-exclude without -zip)");
		} catch (CommandLineParametersException exc) {
		}
	}
	
	@Test
	public void createZipTemplateTest() throws IOException {
		final SubstitutableProperties	props = new SubstitutableProperties(), newProps = new SubstitutableProperties();
		
		props.setProperty("key", "value");
		
		try(final InputStream		is = ZipProcessingClass.createZipTemplate(props);
			final ZipInputStream	zis = new ZipInputStream(is)) {
			
			ZipEntry	ze = zis.getNextEntry();
			
			Assert.assertEquals(Constants.PART_TICKET, ze.getName());
			newProps.load(zis);
			Assert.assertEquals(props, newProps);
			
			ze = zis.getNextEntry();
			
			Assert.assertEquals(Constants.PART_LOG, ze.getName());
			Assert.assertNull(zis.getNextEntry());
		}
		
		try{ZipProcessingClass.createZipTemplate(null);
			Assert.fail("Mandatory exception was not detected (null1-st argument)");
		} catch (NullPointerException exc) {
		}
	}	

	@Test
	public void parseAndCopyZipTest() throws IOException, ContentException {
		final SubstitutableProperties	props = new SubstitutableProperties(), newProps = new SubstitutableProperties();
		final boolean[]					entered = new boolean[] {false};
		
		
		props.setProperty("key", "value");
		
		try(final ByteArrayOutputStream	baos = new ByteArrayOutputStream();
			final ZipOutputStream		zos = new ZipOutputStream(baos)){
			
			try(final InputStream		is = ZipProcessingClass.createZipTemplate(props);
				final ZipInputStream	zis = new ZipInputStream(is)) {
				
				final EntityProcessor	ep = new EntityProcessor() {
											@Override
											public void processEntry(InputStream reader, OutputStream writer, String partName, InputFormat format, LoggerFacade logger, boolean debug) throws IOException {
												Assert.fail("Unwaited call!");
											}
											
											@Override
											public void appendEntries(final ZipOutputStream writer, final LoggerFacade logger, boolean debug) throws IOException {
												try(final InputStream	is = new ByteArrayInputStream("test string".getBytes())) {
													
													ZipProcessingClass.copyZip(is, "addon", zos, logger, debug);
													
													try{ZipProcessingClass.copyZip(null, "addon", zos, logger, debug);
														Assert.fail("Mandatory exception was not detected (null 1-st argument)");
													} catch (NullPointerException exc) {
													}
													try{ZipProcessingClass.copyZip(is, null, zos, logger, debug);
														Assert.fail("Mandatory exception was not detected (null 2-nd argument)");
													} catch (IllegalArgumentException exc) {
													}
													try{ZipProcessingClass.copyZip(is, "", zos, logger, debug);
														Assert.fail("Mandatory exception was not detected (empty 2-nd argument)");
													} catch (IllegalArgumentException exc) {
													}
													try{ZipProcessingClass.copyZip(is, "addon", null, logger, debug);
														Assert.fail("Mandatory exception was not detected (null 3-rd argument)");
													} catch (NullPointerException exc) {
													}
													try{ZipProcessingClass.copyZip(is, "addon", zos, null, debug);
														Assert.fail("Mandatory exception was not detected (null 4-th argument)");
													} catch (NullPointerException exc) {
													}
													
													logger.message(Severity.info, "test message");
												} catch (ContentException e) {
													throw new IOException(e);
												}
											}
										};
				
				ZipProcessingClass.parseZip(zis, zos, ZipProcessingClass.NONE_PATTERN, ZipProcessingClass.ALL_PATTERN, ep);
				
				try{ZipProcessingClass.parseZip(null, zos, ZipProcessingClass.NONE_PATTERN, ZipProcessingClass.ALL_PATTERN, ep);
					Assert.fail("Mandatory exception was not detected (null 1-st argument)");
				} catch (NullPointerException exc) {
				}
				try{ZipProcessingClass.parseZip(zis, null, ZipProcessingClass.NONE_PATTERN, ZipProcessingClass.ALL_PATTERN, ep);
					Assert.fail("Mandatory exception was not detected (null 2-nd argument)");
				} catch (NullPointerException exc) {
				}
				try{ZipProcessingClass.parseZip(zis, zos, null, ZipProcessingClass.ALL_PATTERN, ep);
					Assert.fail("Mandatory exception was not detected (null 3-rd argument)");
				} catch (NullPointerException exc) {
				}
				try{ZipProcessingClass.parseZip(zis, zos, ZipProcessingClass.NONE_PATTERN, null, ep);
					Assert.fail("Mandatory exception was not detected (null 4-th argument)");
				} catch (NullPointerException exc) {
				}
				try{ZipProcessingClass.parseZip(zis, zos, ZipProcessingClass.NONE_PATTERN, ZipProcessingClass.ALL_PATTERN, null);
					Assert.fail("Mandatory exception was not detected (null 5-th argument)");
				} catch (NullPointerException exc) {
				}
			}
			
			try(final InputStream		is = new ByteArrayInputStream(baos.toByteArray());
				final ZipInputStream	zis = new ZipInputStream(is)) {
				ZipEntry	ze = zis.getNextEntry();
				
				Assert.assertEquals(Constants.PART_TICKET, ze.getName());
				newProps.load(zis);
				Assert.assertEquals(props, newProps);

				ze = zis.getNextEntry();
				Assert.assertEquals("addon", ze.getName());
				Assert.assertEquals("test string", Utils.fromResource(new InputStreamReader(zis)));
				
				ze = zis.getNextEntry();
				
				Assert.assertEquals(Constants.PART_LOG, ze.getName());
				Assert.assertEquals("System.err.logger[info]: test message\r\n", Utils.fromResource(new InputStreamReader(zis)));
				
				Assert.assertNull(zis.getNextEntry());
			}
			
			try(final InputStream			is = new ByteArrayInputStream(baos.toByteArray());
				final ZipInputStream		zis = new ZipInputStream(is);
				final ByteArrayOutputStream	os = new ByteArrayOutputStream();
				final ZipOutputStream		dummy = new ZipOutputStream(os)) {

				final EntityProcessor		ep = new EntityProcessor() {
												@Override
												public void processEntry(final InputStream reader, final OutputStream writer, final String partName, final InputFormat format, final LoggerFacade logger, final boolean debug) throws IOException {
													Assert.assertEquals("addon",partName);
													entered[0] = true;
												}
											};
				
				entered[0] = false;
				ZipProcessingClass.parseZip(zis, dummy, ZipProcessingClass.NONE_PATTERN, ZipProcessingClass.ALL_PATTERN, ep);
				Assert.assertTrue(entered[0]);
				Assert.assertEquals(3, calcEntryCount(os.toByteArray()));
			}			

			try(final InputStream			is = new ByteArrayInputStream(baos.toByteArray());
				final ZipInputStream		zis = new ZipInputStream(is);
				final ByteArrayOutputStream	os = new ByteArrayOutputStream();
				final ZipOutputStream		dummy = new ZipOutputStream(os)) {

				final EntityProcessor		ep = new EntityProcessor() {
												@Override
												public void processEntry(final InputStream reader, final OutputStream writer, final String partName, final InputFormat format, final LoggerFacade logger, final boolean debug) throws IOException {
													Assert.assertEquals("addon",partName);
													entered[0] = true;
												}
											};
				
				entered[0] = false;
				ZipProcessingClass.parseZip(zis, dummy, ZipProcessingClass.ALL_PATTERN, ZipProcessingClass.ALL_PATTERN, ep);
				Assert.assertFalse(entered[0]);
				Assert.assertEquals(2, calcEntryCount(os.toByteArray()));
			}			

			try(final InputStream			is = new ByteArrayInputStream(baos.toByteArray());
				final ZipInputStream		zis = new ZipInputStream(is);
				final ByteArrayOutputStream	os = new ByteArrayOutputStream();
				final ZipOutputStream		dummy = new ZipOutputStream(os)) {

				final EntityProcessor		ep = new EntityProcessor() {
												@Override
												public void processEntry(final InputStream reader, final OutputStream writer, final String partName, final InputFormat format, final LoggerFacade logger, final boolean debug) throws IOException {
													Assert.assertEquals("addon",partName);
													entered[0] = true;
												}
											};
				
				entered[0] = false;
				ZipProcessingClass.parseZip(zis, dummy, ZipProcessingClass.NONE_PATTERN, ZipProcessingClass.NONE_PATTERN, ep);
				Assert.assertFalse(entered[0]);
				Assert.assertEquals(3, calcEntryCount(os.toByteArray()));
			}			
		}

		try(final ByteArrayOutputStream	baos = new ByteArrayOutputStream(); 
			final ZipOutputStream		zos = new ZipOutputStream(baos)) {
			try(final InputStream		is = ZipProcessingClass.createZipTemplate(Utils.mkProps("key","value"));
				final ZipInputStream	zis = new ZipInputStream(is)) {
				final EntityProcessor	ep = new EntityProcessor() {
											@Override
											public void processEntry(InputStream reader, OutputStream writer, String partName, InputFormat format, LoggerFacade logger, boolean debug) throws IOException {
											}
											
											@Override
											public void appendEntries(final ZipOutputStream writer, final LoggerFacade logger, boolean debug) throws IOException {
												try(final InputStream		is = ZipProcessingClass.createZipTemplate(new Properties(), URIUtils.convert2selfURI("added", "test string".getBytes()));
													final ZipInputStream	zis = new ZipInputStream(is)) {
													
													ZipProcessingClass.copyZip(zis, writer, debug);
													
													try{ZipProcessingClass.copyZip(null, writer, debug);
														Assert.fail("Mandatory exception was not detected (null 1-st argument)");
													} catch (NullPointerException exc) {
													}
													try{ZipProcessingClass.copyZip(zis, null, debug);
														Assert.fail("Mandatory exception was not detected (null 2-nd argument)");
													} catch (NullPointerException exc) {
													}
												} catch (ContentException e) {
													throw new IOException(e);
												}
											}
										};
				
				ZipProcessingClass.parseZip(zis, zos, ZipProcessingClass.NONE_PATTERN, ZipProcessingClass.ALL_PATTERN, ep);
			}

			try(final InputStream			is = new ByteArrayInputStream(baos.toByteArray());
				final ZipInputStream		zis = new ZipInputStream(is);
				final ByteArrayOutputStream	os = new ByteArrayOutputStream();
				final ZipOutputStream		dummy = new ZipOutputStream(os)) {

				final EntityProcessor		ep = new EntityProcessor() {
												@Override
												public void processEntry(final InputStream reader, final OutputStream writer, final String partName, final InputFormat format, final LoggerFacade logger, final boolean debug) throws IOException {
													Assert.assertEquals("addon",partName);
													entered[0] = true;
												}
											};
				
				entered[0] = false;
				ZipProcessingClass.parseZip(zis, dummy, ZipProcessingClass.NONE_PATTERN, ZipProcessingClass.NONE_PATTERN, ep);
				Assert.assertFalse(entered[0]);
				Assert.assertEquals(3, calcEntryCount(os.toByteArray()));
			}			
		}
		
	}	

	private static int calcEntryCount(final byte[] content) throws IOException {
		int		count = 0;
		
		try(final InputStream		is = new ByteArrayInputStream(content);
			final ZipInputStream	zis = new ZipInputStream(is)) {
			
			while (zis.getNextEntry() != null) {
				count++;
			}
		}
		return count;
	}
	
	private static class TestArgParser extends ArgParser {
		private static final ArgParser.AbstractArg[]	KEYS = {
			new BooleanArg(Constants.ARG_ZIP, false, "Parse input as *.zip format", false),
			new PatternArg(Constants.ARG_EXCLUDE, false, "Skip input *.zip parts and remove then from output stream", "\uFFFF"),
			new PatternArg(Constants.ARG_PROCESS, false, "Process the given parts in the input *.zip. If missing,all the parts will be processed", ".*"),
		};
		
		TestArgParser() {
			super(KEYS);
		}
	}
}
