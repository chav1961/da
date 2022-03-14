package chav1961.da.converter.input;

import java.io.IOException;
import java.io.Reader;
import java.net.URI;

import chav1961.da.converter.interfaces.ContentWriter;
import chav1961.da.converter.interfaces.InputConverterInterface;
import chav1961.da.util.interfaces.DAContentFormat;
import chav1961.purelib.basic.URIUtils;
import chav1961.purelib.basic.exceptions.EnvironmentException;
import chav1961.purelib.basic.interfaces.LoggerFacade;
import chav1961.purelib.basic.interfaces.SyntaxTreeInterface;

public class TurtleReader implements InputConverterInterface {
	private static final URI	SERVE_URI = URI.create(CONV_SCHEMA+":"+DAContentFormat.TURTLE.getSchema()+":/");
	
	public TurtleReader() {
	}
	
	@Override
	public boolean canServe(final URI resource) throws NullPointerException {
		if (resource == null) {
			throw new NullPointerException("Resource to test can'tbe null");
		}
		return URIUtils.canServeURI(resource, SERVE_URI);
	}

	@Override
	public InputConverterInterface newInstance(final URI resource) throws EnvironmentException, NullPointerException, IllegalArgumentException {
		if (resource == null) {
			throw new NullPointerException("Resource to test can'tbe null");
		}
		else if (canServe(resource)) {
			return new TurtleReader();
		}
		else {
			throw new EnvironmentException("Can't create instance for serving ["+resource+"]");
		}
	}

	@Override
	public void process(final Reader rdr, final SyntaxTreeInterface<char[]> tree, final ContentWriter writer, final LoggerFacade logger) throws IOException {
		// TODO Auto-generated method stub
		if (rdr == null) {
		
		}
		
	}
}
