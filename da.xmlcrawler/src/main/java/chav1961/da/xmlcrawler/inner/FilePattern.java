package chav1961.da.xmlcrawler.inner;

import java.io.IOException;

import chav1961.purelib.basic.exceptions.ContentException;
import chav1961.purelib.basic.exceptions.SyntaxException;

public class FilePattern extends AbstractPattern {
	private String		filePattern;
	private XMLPattern	xmlPattern;
	
	public FilePattern() {
	}

	public String getFilePattern() {
		return filePattern;
	}

	public void setFilePattern(String filePattern) {
		this.filePattern = filePattern;
	}

	public XMLPattern getXmlPattern() {
		return xmlPattern;
	}

	public void setXmlPattern(XMLPattern xmlPattern) {
		this.xmlPattern = xmlPattern;
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
		return "FilePattern [filePattern=" + filePattern + ", xmlPattern=" + xmlPattern + "]";
	}

}
