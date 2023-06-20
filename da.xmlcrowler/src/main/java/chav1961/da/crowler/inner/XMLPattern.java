package chav1961.da.crowler.inner;

import java.io.IOException;

import chav1961.purelib.basic.exceptions.ContentException;
import chav1961.purelib.basic.exceptions.SyntaxException;

public class XMLPattern extends AbstractPattern {
	private String	template;
	private String	format;
	
	public XMLPattern() {
	}

	public String getTemplate() {
		return template;
	}

	public void setTemplate(final String template) {
		this.template = template;
	}

	public String getFormat() {
		return format;
	}

	public void setFormat(final String format) {
		this.format = format;
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
		return "XMLPattern [template=" + template + ", format=" + format + "]";
	}
}
