package chav1961.da.converter.output;

import java.io.IOException;
import java.io.Writer;
import java.net.URI;

import chav1961.da.converter.interfaces.OutputConverterInterface;
import chav1961.da.util.interfaces.DAContentFormat;
import chav1961.purelib.basic.URIUtils;
import chav1961.purelib.basic.exceptions.EnvironmentException;
import chav1961.purelib.basic.interfaces.SyntaxTreeInterface;

public class TrigWriter implements OutputConverterInterface {
	private static final URI	SERVE_URI = URI.create(CONV_SCHEMA+":"+DAContentFormat.TRIG.getSchema()+":/");

	public TrigWriter() {
	}
	
	@Override
	public boolean canServe(final URI resource) throws NullPointerException {
		return URIUtils.canServeURI(resource, SERVE_URI);
	}

	@Override
	public OutputConverterInterface newInstance(URI resource) throws EnvironmentException, NullPointerException, IllegalArgumentException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void attach(final Writer wr, final SyntaxTreeInterface<char[]> tree) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Writer detach() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void process(int marks, long[] longContent, char[] objectContent) throws IOException {
		// TODO Auto-generated method stub
		
	}
}
