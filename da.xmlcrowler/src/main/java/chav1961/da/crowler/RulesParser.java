package chav1961.da.crowler;

import java.io.IOException;
import java.io.Reader;

import org.yaml.snakeyaml.Yaml;

import chav1961.da.crowler.inner.RulesPattern;
import chav1961.purelib.basic.exceptions.ContentException;
import chav1961.purelib.basic.exceptions.SyntaxException;

public class RulesParser {
	private final RulesPattern	rules;
	
	public RulesParser(final Reader reader) throws IOException, NullPointerException, SyntaxException, ContentException {
		if (reader == null) {
			throw new NullPointerException("Reader can't be null");
		}
		else {
	        this.rules = new Yaml().loadAs(reader, RulesPattern.class);
	        rules.validate();
		}
	}
}
