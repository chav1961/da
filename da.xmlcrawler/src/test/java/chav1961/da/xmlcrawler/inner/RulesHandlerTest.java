package chav1961.da.xmlcrawler.inner;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import chav1961.purelib.basic.exceptions.SyntaxException;

public class RulesHandlerTest {
	@Test
	public void basicTest() throws ParserConfigurationException, SAXException, IOException, SyntaxException {
		final Writer				wr = new StringWriter();
		final SAXParserFactory 		saxParserFactory = SAXParserFactory.newInstance();
		final SAXParser 			saxParser = saxParserFactory.newSAXParser();
		
		System.setProperty("key", "value");
		
		final RulesParser			parser = new RulesParser(this.getClass().getResourceAsStream("simpleRules.txt"));
		final DefaultHandler		handler = new RulesHandler(wr, parser.getVariables(), parser.getHeadContent(), parser.getTailContent());

		saxParser.parse(new InputSource(this.getClass().getResourceAsStream("content.xml")), handler);
		Assert.assertEquals("# comment value\n# the end\n", wr.toString().replace("\r", ""));
	}

	@Test
	public void rulesTest() throws ParserConfigurationException, SAXException, IOException, SyntaxException {
		final Writer				wr = new StringWriter();
		final SAXParserFactory 		saxParserFactory = SAXParserFactory.newInstance();
		final SAXParser 			saxParser = saxParserFactory.newSAXParser();
		
		System.setProperty("key", "value");
		
		final RulesParser			parser = new RulesParser(this.getClass().getResourceAsStream("simpleRules.txt"));
		final RuleExecutor			exec = new RuleExecutor(parser.getRules()[0], parser.getVariables());
		final DefaultHandler		handler = new RulesHandler(wr, parser.getVariables(), parser.getHeadContent(), parser.getTailContent(), exec);

		saxParser.parse(new InputSource(this.getClass().getResourceAsStream("content.xml")), handler);
		Assert.assertEquals("# comment value\n# the end\n", wr.toString().replace("\r", ""));
	}
}
