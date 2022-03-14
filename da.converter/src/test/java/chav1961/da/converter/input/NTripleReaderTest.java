package chav1961.da.converter.input;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.Assert;
import org.junit.Test;

import chav1961.da.converter.interfaces.ContentWriter;
import chav1961.da.converter.interfaces.InputConverterInterface;
import chav1961.da.util.interfaces.DAContentFormat;
import chav1961.purelib.basic.AndOrTree;
import chav1961.purelib.basic.exceptions.EnvironmentException;
import chav1961.purelib.basic.interfaces.SyntaxTreeInterface;

public class NTripleReaderTest {
	@Test
	public void test() throws EnvironmentException, NullPointerException, IllegalArgumentException, IOException {
		final NTripleReader				rdrFactory = new NTripleReader(); 
		final InputConverterInterface	ici = rdrFactory.newInstance(URI.create(InputConverterInterface.CONV_SCHEMA+":"+DAContentFormat.N_TRIPLES.getSchema()+":/"));
		
		try(final InputStream		is = this.getClass().getResourceAsStream("ntriples.zip");
			final ZipInputStream	zis = new ZipInputStream(is)) {
			ZipEntry				ze;
			
			while((ze = zis.getNextEntry()) != null) {
				switch (ze.getName()) {
					case "literal.nt" :
						testLiteral(zis, ici);
						break;
					case "literal_all_controls.nt" :						
						testLiteralAllControls(zis, ici);
						break;
					case "literal_all_punctuation.nt" :
						testLiteralAllPunctuation(zis, ici);
						break;
					case "literal_false.nt" :						
						testLiteralFalse(zis, ici);
						break;
					case "literal_true.nt" :						
						testLiteralTrue(zis, ici);
						break;
					default :
						System.err.println("Skip "+ze.getName());
				}
			}
		}
	}

	private void testLiteral(final InputStream	is, final InputConverterInterface ici) throws IOException {
		final Reader						rdr = new InputStreamReader(is);
		final SyntaxTreeInterface<char[]>	tree = new AndOrTree<>();
		final ContentWriter					cwr = new ContentWriter() {
												@Override public void attach(Writer wr, SyntaxTreeInterface<char[]> tree) throws IOException {}
												@Override public Writer detach() throws IOException {return null;}
												
												@Override
												public void process(long[] longContent, char[][] charContent) throws IOException {
													Assert.assertEquals("http://a.example/s", tree.getName(longContent[InputConverterInterface.SUBJ_INDEX]));
													Assert.assertEquals("http://a.example/p", tree.getName(longContent[InputConverterInterface.PRED_INDEX]));
													Assert.assertEquals("x", tree.getName(longContent[InputConverterInterface.OBJ_INDEX]));
													Assert.assertEquals(InputConverterInterface.DUMMY_VALUE, longContent[InputConverterInterface.TYPE_INDEX]);
												}
											};
		
		ici.process(rdr, tree, cwr);
	}

	private void testLiteralAllControls(final InputStream	is, final InputConverterInterface ici) throws IOException {
		final Reader						rdr = new InputStreamReader(is);
		final SyntaxTreeInterface<char[]>	tree = new AndOrTree<>();
		final ContentWriter					cwr = new ContentWriter() {
												@Override public void attach(Writer wr, SyntaxTreeInterface<char[]> tree) throws IOException {}
												@Override public Writer detach() throws IOException {return null;}
												
												@Override
												public void process(long[] longContent, char[][] charContent) throws IOException {
													Assert.assertEquals("http://a.example/s", tree.getName(longContent[InputConverterInterface.SUBJ_INDEX]));
													Assert.assertEquals("http://a.example/p", tree.getName(longContent[InputConverterInterface.PRED_INDEX]));
													Assert.assertEquals("\\u0000\\u0001\\u0002\\u0003\\u0004\\u0005\\u0006\\u0007\\u0008\\t\\u000B\\u000C\\u000E\\u000F\\u0010\\u0011\\u0012\\u0013\\u0014\\u0015\\u0016\\u0017\\u0018\\u0019\\u001A\\u001B\\u001C\\u001D\\u001E\\u001F", new String(charContent[InputConverterInterface.OBJ_INDEX]));
													Assert.assertEquals(InputConverterInterface.DUMMY_VALUE, longContent[InputConverterInterface.TYPE_INDEX]);
												}
											};
		
		ici.process(rdr, tree, cwr);
	}

	private void testLiteralAllPunctuation(final InputStream	is, final InputConverterInterface ici) throws IOException {
		final Reader						rdr = new InputStreamReader(is);
		final SyntaxTreeInterface<char[]>	tree = new AndOrTree<>();
		final ContentWriter					cwr = new ContentWriter() {
												@Override public void attach(Writer wr, SyntaxTreeInterface<char[]> tree) throws IOException {}
												@Override public Writer detach() throws IOException {return null;}
												
												@Override
												public void process(long[] longContent, char[][] charContent) throws IOException {
													Assert.assertEquals("http://a.example/s", tree.getName(longContent[InputConverterInterface.SUBJ_INDEX]));
													Assert.assertEquals("http://a.example/p", tree.getName(longContent[InputConverterInterface.PRED_INDEX]));
													Assert.assertEquals(" !\\\"#$%&():;<=>?@[]^_`{|}~", tree.getName(longContent[InputConverterInterface.OBJ_INDEX]));
													Assert.assertEquals(InputConverterInterface.DUMMY_VALUE, longContent[InputConverterInterface.TYPE_INDEX]);
												}
											};
		
		ici.process(rdr, tree, cwr);
	}

	private void testLiteralFalse(final InputStream	is, final InputConverterInterface ici) throws IOException {
		final Reader						rdr = new InputStreamReader(is);
		final SyntaxTreeInterface<char[]>	tree = new AndOrTree<>();
		final ContentWriter					cwr = new ContentWriter() {
												@Override public void attach(Writer wr, SyntaxTreeInterface<char[]> tree) throws IOException {}
												@Override public Writer detach() throws IOException {return null;}
												
												@Override
												public void process(long[] longContent, char[][] charContent) throws IOException {
													Assert.assertEquals("http://a.example/s", tree.getName(longContent[InputConverterInterface.SUBJ_INDEX]));
													Assert.assertEquals("http://a.example/p", tree.getName(longContent[InputConverterInterface.PRED_INDEX]));
													Assert.assertEquals("false", tree.getName(longContent[InputConverterInterface.OBJ_INDEX]));
													Assert.assertEquals("http://www.w3.org/2001/XMLSchema#boolean", tree.getName(longContent[InputConverterInterface.TYPE_INDEX]));
												}
											};
		
		ici.process(rdr, tree, cwr);
	}

	private void testLiteralTrue(final InputStream	is, final InputConverterInterface ici) throws IOException {
		final Reader						rdr = new InputStreamReader(is);
		final SyntaxTreeInterface<char[]>	tree = new AndOrTree<>();
		final ContentWriter					cwr = new ContentWriter() {
												@Override public void attach(Writer wr, SyntaxTreeInterface<char[]> tree) throws IOException {}
												@Override public Writer detach() throws IOException {return null;}
												
												@Override
												public void process(long[] longContent, char[][] charContent) throws IOException {
													Assert.assertEquals("http://a.example/s", tree.getName(longContent[InputConverterInterface.SUBJ_INDEX]));
													Assert.assertEquals("http://a.example/p", tree.getName(longContent[InputConverterInterface.PRED_INDEX]));
													Assert.assertEquals("true", tree.getName(longContent[InputConverterInterface.OBJ_INDEX]));
													Assert.assertEquals("http://www.w3.org/2001/XMLSchema#boolean", tree.getName(longContent[InputConverterInterface.TYPE_INDEX]));
												}
											};
		
		ici.process(rdr, tree, cwr);
	}
}
