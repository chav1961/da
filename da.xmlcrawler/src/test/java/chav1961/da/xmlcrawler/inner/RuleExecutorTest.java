package chav1961.da.xmlcrawler.inner;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import chav1961.purelib.basic.exceptions.SyntaxException;

public class RuleExecutorTest {
	@Test
	public void lifeCycleTest() throws SyntaxException {
		final Map<String,String>	names = new HashMap<>();
		final RuleExecutor			re = new RuleExecutor(RulesParser.parseTemplate(0, "root/tag[@x=${x}]/${tail}", names),RulesParser.buildSupplier(0, "x=${x}", names), names);
		
		final Map<String,String>	vars = new HashMap<>();
		vars.clear();
		Assert.assertTrue(re.canServe());
		Assert.assertFalse(re.collectingRequired());
		
		Assert.assertTrue(re.test("root", (v)->null, vars));
		Assert.assertTrue(re.canServe());
		Assert.assertFalse(re.collectingRequired());
		Assert.assertTrue(re.test("tag", (v)->"123", vars));
		Assert.assertEquals("123", vars.get("x"));
		
		Assert.assertTrue(re.canServe());
		Assert.assertTrue(re.collectingRequired());
		
		Assert.assertEquals("x=123", RulesParserTest.print(re.getFormat(), vars));
		
		re.pop();
		re.pop();
	}

}
