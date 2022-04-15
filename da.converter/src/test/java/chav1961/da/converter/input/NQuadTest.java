package chav1961.da.converter.input;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Assert;
import org.junit.Test;

import chav1961.da.converter.interfaces.ContentProcessor;
import chav1961.da.converter.interfaces.ContentWriter;
import chav1961.da.converter.interfaces.InputConverterInterface;
import chav1961.da.util.interfaces.DAContentFormat;
import chav1961.purelib.basic.AndOrTree;
import chav1961.purelib.basic.URIUtils;
import chav1961.purelib.basic.exceptions.EnvironmentException;
import chav1961.purelib.basic.interfaces.SyntaxTreeInterface;

public class NQuadTest {
	@Test
	public void basicTest() throws EnvironmentException, NullPointerException, IllegalArgumentException, IOException {
		final NQuadReader					rdrFactory = new NQuadReader(); 
		final SyntaxTreeInterface<char[]>	tree = new AndOrTree<>();
		final InputConverterInterface		ici = rdrFactory.newInstance(URI.create(InputConverterInterface.CONV_SCHEMA+":"+DAContentFormat.N_QUADS.getSchema()+":/"));

		checkContent("<http://a.example/s> <http://a.example/p> \"x\" <http://a.example/c> .\n", ici, tree, (marks, longContent, objectContent)->{
			Assert.assertEquals(tree.seekName("http://a.example/s"), longContent[ContentProcessor.SUBJ_INDEX]);
			Assert.assertEquals(tree.seekName("http://a.example/p"), longContent[ContentProcessor.PRED_INDEX]);
			Assert.assertEquals(tree.seekName("x"), longContent[ContentProcessor.OBJ_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.LANG_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.TYPE_INDEX]);
			Assert.assertEquals(tree.seekName("http://a.example/c"), longContent[ContentProcessor.CONTEXT_INDEX]);
		});

		checkContent("<http://a.example/s> <http://a.example/p> \"false\"^^<http://www.w3.org/2001/XMLSchema#boolean> .\n", ici, tree, (marks, longContent, objectContent)->{
			Assert.assertEquals(tree.seekName("http://a.example/s"), longContent[ContentProcessor.SUBJ_INDEX]);
			Assert.assertEquals(tree.seekName("http://a.example/p"), longContent[ContentProcessor.PRED_INDEX]);
			Assert.assertEquals(tree.seekName("false"), longContent[ContentProcessor.OBJ_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.LANG_INDEX]);
			Assert.assertEquals(tree.seekName("http://www.w3.org/2001/XMLSchema#boolean"), longContent[ContentProcessor.TYPE_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.CONTEXT_INDEX]);
		});

		checkContent("<http://a.example/s> <http://a.example/p> \"true\"^^<http://www.w3.org/2001/XMLSchema#boolean> .\n", ici, tree, (marks, longContent, objectContent)->{
			Assert.assertEquals(tree.seekName("http://a.example/s"), longContent[ContentProcessor.SUBJ_INDEX]);
			Assert.assertEquals(tree.seekName("http://a.example/p"), longContent[ContentProcessor.PRED_INDEX]);
			Assert.assertEquals(tree.seekName("true"), longContent[ContentProcessor.OBJ_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.LANG_INDEX]);
			Assert.assertEquals(tree.seekName("http://www.w3.org/2001/XMLSchema#boolean"), longContent[ContentProcessor.TYPE_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.CONTEXT_INDEX]);
		});

		checkContent("<http://a.example/s> <http://a.example/p> \"x'y\" .\n", ici, tree, (marks, longContent, objectContent)->{
			Assert.assertEquals(tree.seekName("http://a.example/s"), longContent[ContentProcessor.SUBJ_INDEX]);
			Assert.assertEquals(tree.seekName("http://a.example/p"), longContent[ContentProcessor.PRED_INDEX]);
			Assert.assertEquals(tree.seekName("x'y"), longContent[ContentProcessor.OBJ_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.LANG_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.TYPE_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.CONTEXT_INDEX]);
		});
		
		checkContent("<http://a.example/s> <http://a.example/p> \"x''y\" .\n", ici, tree, (marks, longContent, objectContent)->{
			Assert.assertEquals(tree.seekName("http://a.example/s"), longContent[ContentProcessor.SUBJ_INDEX]);
			Assert.assertEquals(tree.seekName("http://a.example/p"), longContent[ContentProcessor.PRED_INDEX]);
			Assert.assertEquals(tree.seekName("x''y"), longContent[ContentProcessor.OBJ_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.LANG_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.TYPE_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.CONTEXT_INDEX]);
		});
		
		checkContent("<http://a.example/s> <http://a.example/p> \"x\\\"y\" .\n", ici, tree, (marks, longContent, objectContent)->{
			Assert.assertEquals(tree.seekName("http://a.example/s"), longContent[ContentProcessor.SUBJ_INDEX]);
			Assert.assertEquals(tree.seekName("http://a.example/p"), longContent[ContentProcessor.PRED_INDEX]);
			Assert.assertEquals(tree.seekName("x\\\"y"), longContent[ContentProcessor.OBJ_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.LANG_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.TYPE_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.CONTEXT_INDEX]);
		});
		
		checkContent("<http://a.example/s> <http://a.example/p> \"x\\\"\\\"y\" .\n", ici, tree, (marks, longContent, objectContent)->{
			Assert.assertEquals(tree.seekName("http://a.example/s"), longContent[ContentProcessor.SUBJ_INDEX]);
			Assert.assertEquals(tree.seekName("http://a.example/p"), longContent[ContentProcessor.PRED_INDEX]);
			Assert.assertEquals(tree.seekName("x\\\"\\\"y"), longContent[ContentProcessor.OBJ_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.LANG_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.TYPE_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.CONTEXT_INDEX]);
		});
		
		checkContent("<http://a.example/s> <http://a.example/p> \"\\b\" .\n", ici, tree, (marks, longContent, objectContent)->{
			Assert.assertEquals(tree.seekName("http://a.example/s"), longContent[ContentProcessor.SUBJ_INDEX]);
			Assert.assertEquals(tree.seekName("http://a.example/p"), longContent[ContentProcessor.PRED_INDEX]);
			Assert.assertEquals(tree.seekName("\\b"), longContent[ContentProcessor.OBJ_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.LANG_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.TYPE_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.CONTEXT_INDEX]);
		});
		
		checkContent("<http://a.example/s> <http://a.example/p> \"\\r\" .\n", ici, tree, (marks, longContent, objectContent)->{
			Assert.assertEquals(tree.seekName("http://a.example/s"), longContent[ContentProcessor.SUBJ_INDEX]);
			Assert.assertEquals(tree.seekName("http://a.example/p"), longContent[ContentProcessor.PRED_INDEX]);
			Assert.assertEquals(tree.seekName("\\r"), longContent[ContentProcessor.OBJ_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.LANG_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.TYPE_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.CONTEXT_INDEX]);
		});
		
		checkContent("<http://a.example/s> <http://a.example/p> \"\\f\" .\n", ici, tree, (marks, longContent, objectContent)->{
			Assert.assertEquals(tree.seekName("http://a.example/s"), longContent[ContentProcessor.SUBJ_INDEX]);
			Assert.assertEquals(tree.seekName("http://a.example/p"), longContent[ContentProcessor.PRED_INDEX]);
			Assert.assertEquals(tree.seekName("\\f"), longContent[ContentProcessor.OBJ_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.LANG_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.TYPE_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.CONTEXT_INDEX]);
		});

		checkContent("<http://a.example/s> <http://a.example/p> \"\\n\" .\n", ici, tree, (marks, longContent, objectContent)->{
			Assert.assertEquals(tree.seekName("http://a.example/s"), longContent[ContentProcessor.SUBJ_INDEX]);
			Assert.assertEquals(tree.seekName("http://a.example/p"), longContent[ContentProcessor.PRED_INDEX]);
			Assert.assertEquals(tree.seekName("\\n"), longContent[ContentProcessor.OBJ_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.LANG_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.TYPE_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.CONTEXT_INDEX]);
		});
		
		checkContent("<http://a.example/s> <http://a.example/p> \"\\t\" .\n", ici, tree, (marks, longContent, objectContent)->{
			Assert.assertEquals(tree.seekName("http://a.example/s"), longContent[ContentProcessor.SUBJ_INDEX]);
			Assert.assertEquals(tree.seekName("http://a.example/p"), longContent[ContentProcessor.PRED_INDEX]);
			Assert.assertEquals(tree.seekName("\\t"), longContent[ContentProcessor.OBJ_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.LANG_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.TYPE_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.CONTEXT_INDEX]);
		});
	
		checkContent("<http://a.example/s> <http://a.example/p> \"\\\\\" .\n", ici, tree, (marks, longContent, objectContent)->{
			Assert.assertEquals(tree.seekName("http://a.example/s"), longContent[ContentProcessor.SUBJ_INDEX]);
			Assert.assertEquals(tree.seekName("http://a.example/p"), longContent[ContentProcessor.PRED_INDEX]);
			Assert.assertEquals(tree.seekName("\\\\"), longContent[ContentProcessor.OBJ_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.LANG_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.TYPE_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.CONTEXT_INDEX]);
		});
		
		checkContent("<http://example.org/ns#s> <http://example.org/ns#p1> \"test-\\\\\" .\n", ici, tree, (marks, longContent, objectContent)->{
			Assert.assertEquals(tree.seekName("http://example.org/ns#s"), longContent[ContentProcessor.SUBJ_INDEX]);
			Assert.assertEquals(tree.seekName("http://example.org/ns#p1"), longContent[ContentProcessor.PRED_INDEX]);
			Assert.assertEquals(tree.seekName("test-\\\\"), longContent[ContentProcessor.OBJ_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.LANG_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.TYPE_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.CONTEXT_INDEX]);
		});
				
		
		checkContent("<http://a.example/s> <http://a.example/p> \"\\u006F\" .\n", ici, tree, (marks, longContent, objectContent)->{
			Assert.assertEquals(tree.seekName("http://a.example/s"), longContent[ContentProcessor.SUBJ_INDEX]);
			Assert.assertEquals(tree.seekName("http://a.example/p"), longContent[ContentProcessor.PRED_INDEX]);
			Assert.assertEquals(tree.seekName("\\u006F"), longContent[ContentProcessor.OBJ_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.LANG_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.TYPE_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.CONTEXT_INDEX]);
		});

		checkContent("<http://a.example/s> <http://a.example/p> \"\\U0000006F\" .\n", ici, tree, (marks, longContent, objectContent)->{
			Assert.assertEquals(tree.seekName("http://a.example/s"), longContent[ContentProcessor.SUBJ_INDEX]);
			Assert.assertEquals(tree.seekName("http://a.example/p"), longContent[ContentProcessor.PRED_INDEX]);
			Assert.assertEquals(tree.seekName("\\U0000006F"), longContent[ContentProcessor.OBJ_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.LANG_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.TYPE_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.CONTEXT_INDEX]);
		});
		
		checkContent("<http://a.example/s> <http://a.example/p> \"chat\"@en .\n", ici, tree, (marks, longContent, objectContent)->{
			Assert.assertEquals(tree.seekName("http://a.example/s"), longContent[ContentProcessor.SUBJ_INDEX]);
			Assert.assertEquals(tree.seekName("http://a.example/p"), longContent[ContentProcessor.PRED_INDEX]);
			Assert.assertEquals(tree.seekName("chat"), longContent[ContentProcessor.OBJ_INDEX]);
			Assert.assertEquals(tree.seekName("en"), longContent[ContentProcessor.LANG_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.TYPE_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.CONTEXT_INDEX]);
		});
		
		checkContent("<http://example.org/ex#a> <http://example.org/ex#b> \"Cheers\"@en-UK .\n", ici, tree, (marks, longContent, objectContent)->{
			Assert.assertEquals(tree.seekName("http://example.org/ex#a"), longContent[ContentProcessor.SUBJ_INDEX]);
			Assert.assertEquals(tree.seekName("http://example.org/ex#b"), longContent[ContentProcessor.PRED_INDEX]);
			Assert.assertEquals(tree.seekName("Cheers"), longContent[ContentProcessor.OBJ_INDEX]);
			Assert.assertEquals(tree.seekName("en-UK"), longContent[ContentProcessor.LANG_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.TYPE_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.CONTEXT_INDEX]);
		});
		
		checkContent("<http://a.example/s> <http://a.example/p> \"\\u0000\\u0001\\u0002\\u0003\\u0004\\u0005\\u0006\\u0007\\u0008\\t\\u000B\\u000C\\u000E\\u000F\\u0010\\u0011\\u0012\\u0013\\u0014\\u0015\\u0016\\u0017\\u0018\\u0019\\u001A\\u001B\\u001C\\u001D\\u001E\\u001F\" .\n", ici, tree, (marks, longContent, objectContent)->{
			Assert.assertEquals(tree.seekName("http://a.example/s"), longContent[ContentProcessor.SUBJ_INDEX]);
			Assert.assertEquals(tree.seekName("http://a.example/p"), longContent[ContentProcessor.PRED_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.OBJ_INDEX]);
			Assert.assertEquals("\\u0000\\u0001\\u0002\\u0003\\u0004\\u0005\\u0006\\u0007\\u0008\\t\\u000B\\u000C\\u000E\\u000F\\u0010\\u0011\\u0012\\u0013\\u0014\\u0015\\u0016\\u0017\\u0018\\u0019\\u001A\\u001B\\u001C\\u001D\\u001E\\u001F", new String(objectContent));
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.LANG_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.TYPE_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.CONTEXT_INDEX]);
		});

		checkContent("<http://example/s> <http://example/p> <http://example/o> .\n", ici, tree, (marks, longContent, objectContent)->{
			Assert.assertEquals(tree.seekName("http://example/s"), longContent[ContentProcessor.SUBJ_INDEX]);
			Assert.assertEquals(tree.seekName("http://example/p"), longContent[ContentProcessor.PRED_INDEX]);
			Assert.assertEquals(tree.seekName("http://example/o"), longContent[ContentProcessor.OBJ_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.LANG_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.TYPE_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.CONTEXT_INDEX]);
		});
		
		checkContent("<http://example/\\u0053> <http://example/p> <http://example/o> .\n", ici, tree, (marks, longContent, objectContent)->{
			Assert.assertEquals(tree.seekName("http://example/\\u0053"), longContent[ContentProcessor.SUBJ_INDEX]);
			Assert.assertEquals(tree.seekName("http://example/p"), longContent[ContentProcessor.PRED_INDEX]);
			Assert.assertEquals(tree.seekName("http://example/o"), longContent[ContentProcessor.OBJ_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.LANG_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.TYPE_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.CONTEXT_INDEX]);
		});

		checkContent("<http://example/\\U00000053> <http://example/p> <http://example/o> .\n", ici, tree, (marks, longContent, objectContent)->{
			Assert.assertEquals(tree.seekName("http://example/\\U00000053"), longContent[ContentProcessor.SUBJ_INDEX]);
			Assert.assertEquals(tree.seekName("http://example/p"), longContent[ContentProcessor.PRED_INDEX]);
			Assert.assertEquals(tree.seekName("http://example/o"), longContent[ContentProcessor.OBJ_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.LANG_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.TYPE_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.CONTEXT_INDEX]);
		});

		checkContent("<http://example/s> <http://example/p> <scheme:!$%25&'()*+,-./0123456789:/@ABCDEFGHIJKLMNOPQRSTUVWXYZ_abcdefghijklmnopqrstuvwxyz~?#> .\n", ici, tree, (marks, longContent, objectContent)->{
			Assert.assertEquals(tree.seekName("http://example/s"), longContent[ContentProcessor.SUBJ_INDEX]);
			Assert.assertEquals(tree.seekName("http://example/p"), longContent[ContentProcessor.PRED_INDEX]);
			Assert.assertEquals(tree.seekName("scheme:!$%25&'()*+,-./0123456789:/@ABCDEFGHIJKLMNOPQRSTUVWXYZ_abcdefghijklmnopqrstuvwxyz~?#"), longContent[ContentProcessor.OBJ_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.LANG_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.TYPE_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.CONTEXT_INDEX]);
		});
		
		checkContent("<http://example/s> <http://example/p> <http://example/o> . # comment\n", ici, tree, (marks, longContent, objectContent)->{
			Assert.assertEquals(tree.seekName("http://example/s"), longContent[ContentProcessor.SUBJ_INDEX]);
			Assert.assertEquals(tree.seekName("http://example/p"), longContent[ContentProcessor.PRED_INDEX]);
			Assert.assertEquals(tree.seekName("http://example/o"), longContent[ContentProcessor.OBJ_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.LANG_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.TYPE_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.CONTEXT_INDEX]);
		});
	}

	@Test
	public void anonTest() throws EnvironmentException, NullPointerException, IllegalArgumentException, IOException {
		final NQuadReader					rdrFactory = new NQuadReader(); 
		final SyntaxTreeInterface<char[]>	tree = new AndOrTree<>();
		final InputConverterInterface		ici = rdrFactory.newInstance(URI.create(InputConverterInterface.CONV_SCHEMA+":"+DAContentFormat.N_QUADS.getSchema()+":/"));

		checkContent("_:a  <http://example/p> <http://example/o> .\n", ici, tree, (marks, longContent, objectContent)->{
			Assert.assertEquals(tree.seekName("_:a"), longContent[ContentProcessor.SUBJ_INDEX]);
			Assert.assertEquals(tree.seekName("http://example/p"), longContent[ContentProcessor.PRED_INDEX]);
			Assert.assertEquals(tree.seekName("http://example/o"), longContent[ContentProcessor.OBJ_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.LANG_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.TYPE_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.CONTEXT_INDEX]);
		});
		
		checkContent("<http://example/s> <http://example/p> _:a .\n", ici, tree, (marks, longContent, objectContent)->{
			Assert.assertEquals(tree.seekName("http://example/s"), longContent[ContentProcessor.SUBJ_INDEX]);
			Assert.assertEquals(tree.seekName("http://example/p"), longContent[ContentProcessor.PRED_INDEX]);
			Assert.assertEquals(tree.seekName("_:a"), longContent[ContentProcessor.OBJ_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.LANG_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.TYPE_INDEX]);
			Assert.assertEquals(ContentProcessor.DUMMY_VALUE, longContent[ContentProcessor.CONTEXT_INDEX]);
		});
		
	}	
	
	@Test
	public void exceptionTest() throws EnvironmentException, NullPointerException, IllegalArgumentException, IOException {
		final NQuadReader					rdrFactory = new NQuadReader(); 
		final SyntaxTreeInterface<char[]>	tree = new AndOrTree<>();
		final InputConverterInterface		ici = rdrFactory.newInstance(URI.create(InputConverterInterface.CONV_SCHEMA+":"+DAContentFormat.N_QUADS.getSchema()+":/"));
		
		try{checkContent("@base <http://example/> .\n", ici, tree, (marks, longContent, objectContent)->{});
			Assert.fail("Mandatory exception was not detected (bad base)");
		} catch (IOException exc) {
		}

		try{checkContent("<http://example/s> <http://example/p> \"a\\zb\" .\n", ici, tree, (marks, longContent, objectContent)->{});
			Assert.fail("Mandatory exception was not detected (bad escape)");
		} catch (IOException exc) {
		}
		
		try{checkContent("<http://example/s> <http://example/p> \"\\uWXYZ\" .\n", ici, tree, (marks, longContent, objectContent)->{});
			Assert.fail("Mandatory exception was not detected (bad escape)");
		} catch (IOException exc) {
		}
		
		try{checkContent("<http://example/s> <http://example/p> \"\\U0000WXYZ\" .\n", ici, tree, (marks, longContent, objectContent)->{});
			Assert.fail("Mandatory exception was not detected (bad escape)");
		} catch (IOException exc) {
		}
		
		try{checkContent("<http://example/s> <http://example/p> \"string\"@1 .\n", ici, tree, (marks, longContent, objectContent)->{});
			Assert.fail("Mandatory exception was not detected (bad lang)");
		} catch (IOException exc) {
		}
		
		try{checkContent("<http://example/s> <http://example/p> 1 .\n", ici, tree, (marks, longContent, objectContent)->{});
			Assert.fail("Mandatory exception was not detected (bad number)");
		} catch (IOException exc) {
		}
		
		try{checkContent("<http://example/s> <http://example/p> 1.0 .\n", ici, tree, (marks, longContent, objectContent)->{});
			Assert.fail("Mandatory exception was not detected (bad number)");
		} catch (IOException exc) {
		}

		try{checkContent("<http://example/s> <http://example/p> 1.0e0 .\n", ici, tree, (marks, longContent, objectContent)->{});
			Assert.fail("Mandatory exception was not detected (bad number)");
		} catch (IOException exc) {
		}
		
		try{checkContent("@prefix : <http://example/> .\n", ici, tree, (marks, longContent, objectContent)->{});
			Assert.fail("Mandatory exception was not detected (bad prefix)");
		} catch (IOException exc) {
		}
		
		try{checkContent("<http://example/s> <http://example/p> \"abc' .\n", ici, tree, (marks, longContent, objectContent)->{});
			Assert.fail("Mandatory exception was not detected (bad string)");
		} catch (IOException exc) {
		}

		try{checkContent("<http://example/s> <http://example/p> 1.0 .\n", ici, tree, (marks, longContent, objectContent)->{});
			Assert.fail("Mandatory exception was not detected (bad string)");
		} catch (IOException exc) {
		}

		try{checkContent("<http://example/s> <http://example/p> 1.0e1 .\n", ici, tree, (marks, longContent, objectContent)->{});
			Assert.fail("Mandatory exception was not detected (bad string)");
		} catch (IOException exc) {
		}

		try{checkContent("<http://example/s> <http://example/p> '''abc''' .\n", ici, tree, (marks, longContent, objectContent)->{});
			Assert.fail("Mandatory exception was not detected (bad string)");
		} catch (IOException exc) {
		}

		try{checkContent("<http://example/s> <http://example/p> \"\"\"abc\"\"\" .\n", ici, tree, (marks, longContent, objectContent)->{});
			Assert.fail("Mandatory exception was not detected (bad string)");
		} catch (IOException exc) {
		}

		try{checkContent("<http://example/s> <http://example/p> \"abc .\n", ici, tree, (marks, longContent, objectContent)->{});
			Assert.fail("Mandatory exception was not detected (bad string)");
		} catch (IOException exc) {
		}

		try{checkContent("<http://example/s> <http://example/p> abc\" .\n", ici, tree, (marks, longContent, objectContent)->{});
			Assert.fail("Mandatory exception was not detected (bad string)");
		} catch (IOException exc) {
		}

		try{checkContent("<http://example/ space> <http://example/p> <http://example/o> .\n", ici, tree, (marks, longContent, objectContent)->{});
			Assert.fail("Mandatory exception was not detected (bad URI)");
		} catch (IOException exc) {
		}

		try{checkContent("<http://example/\\u00ZZ11> <http://example/p> <http://example/o> .\n", ici, tree, (marks, longContent, objectContent)->{});
			Assert.fail("Mandatory exception was not detected (bad URI)");
		} catch (IOException exc) {
		}

		try{checkContent("<http://example/\\U00ZZ1111> <http://example/p> <http://example/o> .\n", ici, tree, (marks, longContent, objectContent)->{});
			Assert.fail("Mandatory exception was not detected (bad URI)");
		} catch (IOException exc) {
		}

		try{checkContent("<http://example/\\n> <http://example/p> <http://example/o> .\n", ici, tree, (marks, longContent, objectContent)->{});
			Assert.fail("Mandatory exception was not detected (bad URI)");
		} catch (IOException exc) {
		}

		try{checkContent("<http://example/\\/> <http://example/p> <http://example/o> .\n", ici, tree, (marks, longContent, objectContent)->{});
			Assert.fail("Mandatory exception was not detected (bad URI)");
		} catch (IOException exc) {
		}

		try{checkContent("<s> <http://example/p> <http://example/o> .\n", ici, tree, (marks, longContent, objectContent)->{});
			Assert.fail("Mandatory exception was not detected (bad URI)");
		} catch (IOException exc) {
		}

		try{checkContent("<http://example/s> <p> <http://example/o> .\n", ici, tree, (marks, longContent, objectContent)->{});
			Assert.fail("Mandatory exception was not detected (bad URI)");
		} catch (IOException exc) {
		}

		try{checkContent("<http://example/s> <http://example/p> <o> .\n", ici, tree, (marks, longContent, objectContent)->{});
			Assert.fail("Mandatory exception was not detected (bad URI)");
		} catch (IOException exc) {
		}

		try{checkContent("<http://example/s> <http://example/p> \"foo\"^^<dt> .\n", ici, tree, (marks, longContent, objectContent)->{});
			Assert.fail("Mandatory exception was not detected (bad URI)");
		} catch (IOException exc) {
		}

		try{checkContent("<http://example/s> <http://example/p> _:1a .\n", ici, tree, (marks, longContent, objectContent)->{});
			Assert.fail("Mandatory exception was not detected (bad anon name)");
		} catch (IOException exc) {
		}
	}	

	@Test
	public void complexTest() throws EnvironmentException, NullPointerException, IllegalArgumentException, IOException, URISyntaxException {
		final NQuadReader					rdrFactory = new NQuadReader(); 
		final SyntaxTreeInterface<char[]>	tree = new AndOrTree<>();
		final InputConverterInterface		ici = rdrFactory.newInstance(URI.create(InputConverterInterface.CONV_SCHEMA+":"+DAContentFormat.N_QUADS.getSchema()+":/"));
		final char[]						content = URIUtils.loadCharsFromURI(this.getClass().getResource("nt-syntax-subm-01.nq").toURI());
		
		checkContent(new String(content), ici, tree, (marks, longContent, objectContent)->{});
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
