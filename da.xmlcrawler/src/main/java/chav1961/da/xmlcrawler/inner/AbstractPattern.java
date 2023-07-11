package chav1961.da.xmlcrawler.inner;

import java.io.IOException;
import java.util.List;

import chav1961.purelib.basic.exceptions.ContentException;
import chav1961.purelib.basic.exceptions.SyntaxException;

public abstract class AbstractPattern {
	public abstract void validate() throws SyntaxException;
	public abstract void prepare() throws ContentException; 
	public abstract void process() throws IOException, ContentException;
	
	protected static void parseFilePattern(final String filePattern, final List<String> substitutions) {
		
	}

	protected static void parseXmlPattern(final String xmlPattern, final List<String> substitutions) {
		
	}
}
