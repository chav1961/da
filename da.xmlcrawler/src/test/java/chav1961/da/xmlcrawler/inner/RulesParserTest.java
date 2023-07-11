package chav1961.da.xmlcrawler.inner;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.Predicate;

import org.junit.Assert;
import org.junit.Test;

import chav1961.da.xmlcrawler.inner.RulesParser;
import chav1961.da.xmlcrawler.inner.TriPredicate;
import chav1961.purelib.basic.Utils;
import chav1961.purelib.basic.exceptions.SyntaxException;

public class RulesParserTest {
	@Test
	public void formatParserTest() throws SyntaxException, IllegalArgumentException, NullPointerException {
		final Map<String,String>	vars = new HashMap<>();
		
		vars.put("subst", "value");
	
		Assert.assertEquals(" before value after ", print(RulesParser.buildSupplier(0, " before ${subst} after ", vars), vars));
		Assert.assertEquals("value after ", print(RulesParser.buildSupplier(0, "${subst} after ", vars), vars));
		Assert.assertEquals(" before value", print(RulesParser.buildSupplier(0, " before ${subst}", vars), vars));
		
		vars.remove("subst");
		try{RulesParser.buildSupplier(0, " before ${subst} after ", vars);
			Assert.fail("Mandatory exception was not detected (unknown variable)");
		} catch (SyntaxException exc) {
		}
	}

	@Test
	public void preprocesorPredicateParserTest() throws SyntaxException, IllegalArgumentException, NullPointerException {
		final Map<String,String>	vars = new HashMap<>();
		
		vars.put("exists", null);
		vars.put("key1", "value1");
		vars.put("key2", "value2");
		
		Predicate<Map<String, String>> pred = RulesParser.buildPredicate(0, "exists?", vars);
		Assert.assertFalse(pred.test(vars));
		
		pred = RulesParser.buildPredicate(0, "~exists?", vars);
		Assert.assertTrue(pred.test(vars));

		pred = RulesParser.buildPredicate(0, "key1 = \"value1\"", vars);
		Assert.assertTrue(pred.test(vars));
		pred = RulesParser.buildPredicate(0, "key1 = \"value2\"", vars);
		Assert.assertFalse(pred.test(vars));
		pred = RulesParser.buildPredicate(0, "key1 =* \"\\\\w*\\\\d\"", vars);
		Assert.assertTrue(pred.test(vars));

		pred = RulesParser.buildPredicate(0, "key1 = key2", vars);
		Assert.assertFalse(pred.test(vars));
		pred = RulesParser.buildPredicate(0, "key1 > key2", vars);
		Assert.assertFalse(pred.test(vars));
		pred = RulesParser.buildPredicate(0, "key1 >= key2", vars);
		Assert.assertFalse(pred.test(vars));

		pred = RulesParser.buildPredicate(0, "key1 < key2", vars);
		Assert.assertTrue(pred.test(vars));
		pred = RulesParser.buildPredicate(0, "key1 <= key2", vars);
		Assert.assertTrue(pred.test(vars));
		pred = RulesParser.buildPredicate(0, "key1 <> key2", vars);
		Assert.assertTrue(pred.test(vars));
		
		pred = RulesParser.buildPredicate(0, "key1 < key2 & ~exists?", vars);
		Assert.assertTrue(pred.test(vars));
		pred = RulesParser.buildPredicate(0, "key1 < key2 & exists?", vars);
		Assert.assertFalse(pred.test(vars));
		
		pred = RulesParser.buildPredicate(0, "key1 > key2 | ~exists?", vars);
		Assert.assertTrue(pred.test(vars));
		pred = RulesParser.buildPredicate(0, "key1 > key2 | exists?", vars);
		Assert.assertFalse(pred.test(vars));
	}
	
	@Test
	public void preprocessedFormatParserTest() throws SyntaxException, IllegalArgumentException, NullPointerException {
		final Map<String,String>	vars = new HashMap<>();
		
		vars.put("subst", "value1");
		Assert.assertEquals(" true\n", print(RulesParser.buildPreprocessedSupplier(0, ".if subst = \"value1\"\n true\n.else\n false\n.endif\n", vars), vars));

		vars.put("subst", "value2");
		Assert.assertEquals(" false\n", print(RulesParser.buildPreprocessedSupplier(0, ".if subst = \"value1\"\n true\n.else\n false\n.endif\n", vars), vars));
		
		vars.put("nest", "value1");
		Assert.assertEquals("before\n true21\nafter\n", print(RulesParser.buildPreprocessedSupplier(0, "before\n.if subst = \"value1\"\n.if nest = \"value1\"\n true11\n.else\n false11\n.endif\n.else\n.if nest = \"value1\"\n true21\n.else\n false21\n.endif\n.endif\nafter\n", vars), vars));
	}

	@Test
	public void parseAttributesTest() throws SyntaxException, IllegalArgumentException, NullPointerException {
		final Map<String, String>	vars = new HashMap<>();
		final Map<String, String>	result = new HashMap<>();
		final Properties			props = new Properties();
		
		vars.clear();
		TriPredicate<String, Function<String,String>, Map<String,String>>	tp = RulesParser.parseAttributes(0, "@x1=${x}", vars, new HashSet<>());
		
		props.clear();
		props.setProperty("x1", "val1");
		result.clear();
		Assert.assertTrue(tp.test("", (name)->props.getProperty(name), result));
		Assert.assertEquals("val1", result.get("x"));
		
		vars.clear();
		tp = RulesParser.parseAttributes(0, "@x1=${x},@x2=${x}", vars, new HashSet<>());
		props.clear();
		props.setProperty("x1", "val1");
		props.setProperty("x2", "val1");
		result.clear();
		Assert.assertTrue(tp.test("", (name)->props.getProperty(name), result));
		Assert.assertEquals("val1", result.get("x"));

		vars.clear();
		tp = RulesParser.parseAttributes(0, "@x1=${x},@@x2=${y}", vars, new HashSet<>());
		props.clear();
		props.setProperty("x1", "val1");
		props.setProperty("x2", "val1");
		result.clear();
		Assert.assertTrue(tp.test("", (name)->props.getProperty(name), result));
		Assert.assertEquals("val1", result.get("x"));
		Assert.assertEquals("val1", result.get("y"));

		vars.clear();
		tp = RulesParser.parseAttributes(0, "@x1=${x},@@x2=${y}", vars, new HashSet<>());
		props.clear();
		props.setProperty("x1", "val1");
		result.clear();
		Assert.assertTrue(tp.test("", (name)->props.getProperty(name), result));
		Assert.assertEquals("val1", result.get("x"));
		Assert.assertNull(result.get("y"));
		
		vars.clear();
		tp = RulesParser.parseAttributes(0, "@x1=${x},@@x2=${x}", vars, new HashSet<>());
		props.clear();
		props.setProperty("x1", "val1");
		props.setProperty("x2", "val1");
		result.clear();
		Assert.assertTrue(tp.test("", (name)->props.getProperty(name), result));
		Assert.assertEquals("val1", result.get("x"));

		vars.clear();
		tp = RulesParser.parseAttributes(0, "@x1=${x},@@x2=${x}", vars, new HashSet<>());
		props.clear();
		props.setProperty("x1", "val1");
		result.clear();
		Assert.assertTrue(tp.test("", (name)->props.getProperty(name), result));
		Assert.assertEquals("val1", result.get("x"));
		
		vars.clear();
		tp = RulesParser.parseAttributes(0, "@x1=${x},@x2=\"val1\"", vars, new HashSet<>());
		props.clear();
		props.setProperty("x1", "val1");
		props.setProperty("x2", "val1");
		result.clear();
		Assert.assertTrue(tp.test("", (name)->props.getProperty(name), result));
		Assert.assertEquals("val1", result.get("x"));

		vars.clear();
		tp = RulesParser.parseAttributes(0, "@x1=${x},@@x2=\"val1\"", vars, new HashSet<>());
		props.clear();
		props.setProperty("x1", "val1");
		props.setProperty("x2", "val1");
		result.clear();
		Assert.assertTrue(tp.test("", (name)->props.getProperty(name), result));
		Assert.assertEquals("val1", result.get("x"));

		vars.clear();
		tp = RulesParser.parseAttributes(0, "@x1=${x},@@x2=\"val1\"", vars, new HashSet<>());
		props.clear();
		props.setProperty("x1", "val1");
		result.clear();
		Assert.assertTrue(tp.test("", (name)->props.getProperty(name), result));
		Assert.assertEquals("val1", result.get("x"));
	}	

	@Test
	public void parseTemplateTest() throws SyntaxException, IllegalArgumentException, NullPointerException {
		final Map<String, String>	vars = new HashMap<>();
		
		vars.clear();
		TriPredicate<String, Function<String,String>, Map<String,String>>[]	templ = RulesParser.parseTemplate(0, "root/subroot[@x1=${x}]/${tail}", vars);
		Assert.assertEquals(3, templ.length);
		Assert.assertTrue(vars.containsKey("x"));
		Assert.assertTrue(vars.containsKey("tail"));
		Assert.assertTrue(templ[2].charContentRequired());
		Assert.assertFalse(templ[2].subtreeContentRequired());
		Assert.assertEquals("tail",templ[2].getContentVarName());

		vars.clear();
		templ = RulesParser.parseTemplate(0, "root/subroot[@x1=${x}]/${tail}*", vars);
		Assert.assertEquals(3, templ.length);
		Assert.assertTrue(vars.containsKey("x"));
		Assert.assertTrue(vars.containsKey("tail"));
		Assert.assertFalse(templ[2].charContentRequired());
		Assert.assertTrue(templ[2].subtreeContentRequired());
		Assert.assertEquals("tail",templ[2].getContentVarName());

		vars.clear();
		vars.put("predef", null);
		templ = RulesParser.parseTemplate(0, "${predef}/subroot[@x1=${x}]/${tail}*", vars);
		Assert.assertEquals(3, templ.length);
		Assert.assertTrue(vars.containsKey("x"));
		Assert.assertTrue(vars.containsKey("tail"));
		Assert.assertTrue(vars.containsKey("predef"));
		Assert.assertFalse(templ[2].charContentRequired());
		Assert.assertTrue(templ[2].subtreeContentRequired());
		Assert.assertEquals("tail",templ[2].getContentVarName());

		vars.clear();
		vars.put("predef", "123");
		templ = RulesParser.parseTemplate(0, "${predef}/subroot[@x1=${x}]/${tail}*", vars);
		Assert.assertEquals(3, templ.length);
		Assert.assertTrue(vars.containsKey("x"));
		Assert.assertTrue(vars.containsKey("tail"));
		Assert.assertTrue(vars.containsKey("predef"));
		Assert.assertFalse(templ[2].charContentRequired());
		Assert.assertTrue(templ[2].subtreeContentRequired());
		Assert.assertEquals("tail",templ[2].getContentVarName());
		
		vars.clear();
		vars.put("predef", null);
		templ = RulesParser.parseTemplate(0, "root/${predef}[@x1=${x}]/${tail}*", vars);
		Assert.assertEquals(3, templ.length);
		Assert.assertTrue(vars.containsKey("x"));
		Assert.assertTrue(vars.containsKey("tail"));
		Assert.assertTrue(vars.containsKey("predef"));
		Assert.assertFalse(templ[2].charContentRequired());
		Assert.assertTrue(templ[2].subtreeContentRequired());
		Assert.assertEquals("tail",templ[2].getContentVarName());

		vars.clear();
		vars.put("predef", "123");
		templ = RulesParser.parseTemplate(0, "root/${predef}[@x1=${x}]/${tail}*", vars);
		Assert.assertEquals(3, templ.length);
		Assert.assertTrue(vars.containsKey("x"));
		Assert.assertTrue(vars.containsKey("tail"));
		Assert.assertTrue(vars.containsKey("predef"));
		Assert.assertFalse(templ[2].charContentRequired());
		Assert.assertTrue(templ[2].subtreeContentRequired());
		Assert.assertEquals("tail",templ[2].getContentVarName());
	}	
	
	static String print(final Function<Map<String,String>, char[]>[] supp, final Map<String,String> vars) {
		final StringBuilder	sb = new StringBuilder();
		
		for(Function<Map<String, String>, char[]> item : supp) {
			sb.append(item.apply(vars));
		}
		return sb.toString();
	}
}

