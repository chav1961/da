package chav1961.da.xmlcrawler.inner;

import java.io.IOException;
import java.util.Arrays;

import chav1961.purelib.basic.exceptions.ContentException;
import chav1961.purelib.basic.exceptions.SyntaxException;

public class RulesPattern extends AbstractPattern {
	private String			head = "";
	private FilePattern[]	rules = null;
	private String			tail = "";
	
	public RulesPattern() {
	}

	public String getHead() {
		return head;
	}

	public void setHead(final String head) {
		this.head = head;
	}

	public FilePattern[] getRules() {
		return rules;
	}

	public void setRules(final FilePattern[] rules) {
		this.rules = rules;
	}

	public String getTail() {
		return tail;
	}

	public void setTail(final String tail) {
		this.tail = tail;
	}
	
	@Override
	public void validate() throws SyntaxException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void prepare() throws ContentException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void process() throws IOException, ContentException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String toString() {
		return "RulesContainer [head=" + head + ", rules=" + Arrays.toString(rules) + ", tail=" + tail + "]";
	}
}
