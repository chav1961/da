package chav1961.da.xmlcrawler;

import java.io.IOException;
import java.io.Reader;

import org.yaml.snakeyaml.Yaml;

import chav1961.da.xmlcrawler.inner.RulesPattern;
import chav1961.purelib.basic.exceptions.ContentException;
import chav1961.purelib.basic.exceptions.SyntaxException;

public class RulesParserOld {
	private final RulesPattern	rules;
	
	public RulesParserOld(final Reader reader) throws IOException, NullPointerException, SyntaxException, ContentException {
		if (reader == null) {
			throw new NullPointerException("Reader can't be null");
		}
		else {
	        this.rules = new Yaml().loadAs(reader, RulesPattern.class);
	        rules.validate();
		}
	}
}
