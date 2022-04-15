package chav1961.da.converter.input;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.net.URI;

import org.junit.Assert;
import org.junit.Test;

import chav1961.da.converter.interfaces.ContentProcessor;
import chav1961.da.converter.interfaces.ContentWriter;
import chav1961.da.converter.interfaces.InputConverterInterface;
import chav1961.da.util.interfaces.DAContentFormat;
import chav1961.purelib.basic.AndOrTree;
import chav1961.purelib.basic.exceptions.EnvironmentException;
import chav1961.purelib.basic.interfaces.SyntaxTreeInterface;

public class TurtleReaderTest {

	@Test
	public void basicTest() throws EnvironmentException, IOException {
		final TurtleReader					rdrFactory = new TurtleReader(); 
		final SyntaxTreeInterface<char[]>	tree = new AndOrTree<>();
		final InputConverterInterface		ici = rdrFactory.newInstance(URI.create(InputConverterInterface.CONV_SCHEMA+":"+DAContentFormat.TURTLE.getSchema()+":/"));
		final int[]		count = new int[] {0};

		checkContent("<http://a.example/s> <http://a.example/p> \"x\" .\n", ici, tree, (marks, longContent, objectContent)->{
			Assert.assertEquals(tree.seekName("http://a.example/s"), longContent[ContentProcessor.SUBJ_INDEX]);
			Assert.assertEquals(tree.seekName("http://a.example/p"), longContent[ContentProcessor.PRED_INDEX]);
			Assert.assertEquals(tree.seekName("x"), longContent[ContentProcessor.OBJ_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.LANG_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.TYPE_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.CONTEXT_INDEX]);
		});

		checkContent("<http://a.example/s> a \"x\" .\n", ici, tree, (marks, longContent, objectContent)->{
			Assert.assertEquals(tree.seekName("http://a.example/s"), longContent[ContentProcessor.SUBJ_INDEX]);
			Assert.assertEquals(tree.seekName("a"), longContent[ContentProcessor.PRED_INDEX]);
			Assert.assertEquals(tree.seekName("x"), longContent[ContentProcessor.OBJ_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.LANG_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.TYPE_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.CONTEXT_INDEX]);
		});

		checkContent("@prefix :  <http://example.org/base1#> .\n@prefix a: <http://example.org/base2#> .\n@prefix b: <http://example.org/base3#> .\n:a a:a b:a .\n", ici, tree, (marks, longContent, objectContent)->{
			Assert.assertEquals(tree.seekName(":a"), longContent[ContentProcessor.SUBJ_INDEX]);
			Assert.assertEquals(tree.seekName("a:a"), longContent[ContentProcessor.PRED_INDEX]);
			Assert.assertEquals(tree.seekName("b:a"), longContent[ContentProcessor.OBJ_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.LANG_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.TYPE_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.CONTEXT_INDEX]);
		});

		count[0] = 0;
		checkContent("@prefix : <http://example.org/base#> .\n:a :b :c,\n:d,\n:e .\n", ici, tree, (marks, longContent, objectContent)->{
			switch (count[0]) {
				case 0 :
					Assert.assertEquals(tree.seekName(":a"), longContent[ContentProcessor.SUBJ_INDEX]);
					Assert.assertEquals(tree.seekName(":b"), longContent[ContentProcessor.PRED_INDEX]);
					Assert.assertEquals(tree.seekName(":c"), longContent[ContentProcessor.OBJ_INDEX]);
					break;
				case 1 :
					Assert.assertEquals(tree.seekName(":a"), longContent[ContentProcessor.SUBJ_INDEX]);
					Assert.assertEquals(tree.seekName(":b"), longContent[ContentProcessor.PRED_INDEX]);
					Assert.assertEquals(tree.seekName(":d"), longContent[ContentProcessor.OBJ_INDEX]);
					break;
				case 2 :
					Assert.assertEquals(tree.seekName(":a"), longContent[ContentProcessor.SUBJ_INDEX]);
					Assert.assertEquals(tree.seekName(":b"), longContent[ContentProcessor.PRED_INDEX]);
					Assert.assertEquals(tree.seekName(":e"), longContent[ContentProcessor.OBJ_INDEX]);
					break;
				default :
					Assert.fail("Extreme call!");
			}	
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.LANG_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.TYPE_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.CONTEXT_INDEX]);
			count[0]++;
		});
		
		count[0] = 0;
		checkContent("@prefix : <http://example.org/base#> .\n:a :b :c ;\n:d :e ;\n:f :g .\n", ici, tree, (marks, longContent, objectContent)->{
			switch (count[0]) {
				case 0 :
					Assert.assertEquals(tree.seekName(":a"), longContent[ContentProcessor.SUBJ_INDEX]);
					Assert.assertEquals(tree.seekName(":b"), longContent[ContentProcessor.PRED_INDEX]);
					Assert.assertEquals(tree.seekName(":c"), longContent[ContentProcessor.OBJ_INDEX]);
					break;
				case 1 :
					Assert.assertEquals(tree.seekName(":a"), longContent[ContentProcessor.SUBJ_INDEX]);
					Assert.assertEquals(tree.seekName(":d"), longContent[ContentProcessor.PRED_INDEX]);
					Assert.assertEquals(tree.seekName(":e"), longContent[ContentProcessor.OBJ_INDEX]);
					break;
				case 2 :
					Assert.assertEquals(tree.seekName(":a"), longContent[ContentProcessor.SUBJ_INDEX]);
					Assert.assertEquals(tree.seekName(":f"), longContent[ContentProcessor.PRED_INDEX]);
					Assert.assertEquals(tree.seekName(":g"), longContent[ContentProcessor.OBJ_INDEX]);
					break;
				default :
					Assert.fail("Extreme call!");
			}	
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.LANG_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.TYPE_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.CONTEXT_INDEX]);
			count[0]++;
		});
	}

	private void checkContent(final String content, final InputConverterInterface ici, final SyntaxTreeInterface<char[]> tree, final ContentProcessor cp) throws IOException {
		final ContentWriter					cwr = new ContentWriter() {
												@Override public void attach(Writer wr, SyntaxTreeInterface<char[]> tree) throws IOException {}
												@Override public Writer detach() throws IOException {return null;}
												@Override public void process(final int marks, final long[] longContent, final char[] objectContent) throws IOException {cp.process(marks, longContent, objectContent);}
											};
		
		try(final Reader			rdr = new StringReader(content)) {

			ici.process(rdr, tree, cwr);
		}
	}
	
}
