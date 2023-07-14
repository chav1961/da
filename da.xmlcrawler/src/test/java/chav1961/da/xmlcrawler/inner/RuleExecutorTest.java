package chav1961.da.xmlcrawler.inner;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import chav1961.purelib.basic.exceptions.SyntaxException;

public class RuleExecutorTest {
	@Test
	public void lifeCycleTest() throws SyntaxException, IOException {
		final Map<String,String>	names = new HashMap<>();
		final Map<String,String>	vars = new HashMap<>();
		final RuleExecutor			re = new RuleExecutor(RulesParser.parseTemplate(0, "root/tag[@x=${x}]/${tail}", names), RulesParser.buildSupplier(0, "x=${x},tail=${tail}", names), vars);
		
		vars.clear();
		Assert.assertTrue(re.canServe());
		Assert.assertFalse(re.collectingRequired());
		
		re.push("root", (v)->null);
		Assert.assertTrue(re.canServe());
		Assert.assertFalse(re.collectingRequired());
		re.push("tag", (v)->"123");
		
		Assert.assertTrue(re.canServe());
		Assert.assertTrue(re.collectingRequired());
		
		re.setVar(re.getContentVarName(), "assa");
		final Writer	wr = new StringWriter();
		
		re.print(wr);
		Assert.assertEquals("x=123,tail=assa", wr.toString());
		
		re.pop();
		re.pop();
	}

}
