package chav1961.da.converter.output;

import java.net.URI;

import org.junit.Assert;
import org.junit.Test;

import chav1961.da.converter.interfaces.OutputConverterInterface;
import chav1961.da.util.interfaces.DAContentFormat;
import chav1961.purelib.basic.exceptions.EnvironmentException;

public class NTripleWriterTest {

	@Test
	public void test() throws EnvironmentException {
		final NTripleWriter				rdrFactory = new NTripleWriter(); 
		final OutputConverterInterface	oci = rdrFactory.newInstance(URI.create(OutputConverterInterface.CONV_SCHEMA+":"+DAContentFormat.N_TRIPLES.getSchema()+":/"));

		Assert.fail("Not yet implemented");
	}

}
