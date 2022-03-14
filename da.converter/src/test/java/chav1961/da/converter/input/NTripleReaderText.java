package chav1961.da.converter.input;

import java.net.URI;

import org.junit.Assert;
import org.junit.Test;

import chav1961.da.converter.interfaces.InputConverterInterface;
import chav1961.da.util.interfaces.DAContentFormat;
import chav1961.purelib.basic.exceptions.EnvironmentException;

public class NTripleReaderText {
	@Test
	public void basicTest() throws EnvironmentException {
		final NTripleReader				rdrFactory = new NTripleReader(); 
		final InputConverterInterface	ici = rdrFactory.newInstance(URI.create(InputConverterInterface.CONV_SCHEMA+":"+DAContentFormat.N_TRIPLES.getSchema()+":/"));
		
		Assert.fail("Not yet implemented");
	}
	
}
