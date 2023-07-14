package chav1961.da.xmlcrawler.inner;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import chav1961.purelib.basic.Utils;

public class RulesHandler extends DefaultHandler {
	private final Writer								wr;
	private final Map<String,String>					globalVars;
	private final Function<Map<String,String>,char[]>[]	before;
	private final Function<Map<String,String>,char[]>[]	after;
	private final RuleExecutor[]						rules;
	private final StringBuilder		collector = new StringBuilder();
	
	public RulesHandler(final Writer wr, final Map<String,String> globalVars, final Function<Map<String,String>,char[]>[] before, final Function<Map<String,String>,char[]>[] after, final RuleExecutor... rules) {
		if (wr == null) {
			throw new NullPointerException("Writer can't be null");
		}
		else if (globalVars == null) {
			throw new NullPointerException("Global variables can't be null");
		}
		else if (before == null || Utils.checkArrayContent4Nulls(before) >= 0) {
			throw new IllegalArgumentException("Before functions list is null or contains nulls inside");
		}
		else if (after == null || Utils.checkArrayContent4Nulls(after) >= 0) {
			throw new IllegalArgumentException("After functions list is null or contains nulls inside");
		}
		else if (rules == null || Utils.checkArrayContent4Nulls(rules) >= 0) {
			throw new IllegalArgumentException("Rules list is null or contains nulls inside");
		}
		else {
			this.wr = wr;
			this.globalVars = new HashMap<>(globalVars);
			this.before = before;
			this.after = after;
			this.rules = rules;
		}
	}

	@Override
	public void startDocument() throws SAXException {
		print(before, globalVars);
	}
	
	@Override
	public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) throws SAXException {
		for(RuleExecutor item : rules) {
			item.push(qName, (name)->attributes.getValue(name));
		}
		collector.setLength(0);
	}

	@Override
	public void characters(final char[] ch, final int start, final int length) throws SAXException {
		collector.append(ch, start, length);
	}

	
	@Override
	public void endElement(final String uri, final String localName, final String qName) throws SAXException {
		for(RuleExecutor item : rules) {
			if (item.canServe()) {
				if (item.charContentRequired()) {
					item.setVar(item.getContentVarName(), collector.toString());
				}
				try{
					item.print(wr);
				} catch (IOException e) {
					throw new SAXException(e.getLocalizedMessage(), e);
				}
			}
			item.pop();
		}
	}
	
	@Override
	public void endDocument() throws SAXException {
		print(after, globalVars);
		try {
			wr.flush();
		} catch (IOException e) {
			throw new SAXException(e.getLocalizedMessage(), e);
		}
	}
	
	private void print(final Function<Map<String, String>, char[]>[] func, Map<String, String> vars) throws SAXException {
		for(Function<Map<String, String>, char[]> item : func) {
			try{
				wr.write(item.apply(vars));
			} catch (IOException e) {
				throw new SAXException(e.getLocalizedMessage(), e);
			}
		}
	}
}
