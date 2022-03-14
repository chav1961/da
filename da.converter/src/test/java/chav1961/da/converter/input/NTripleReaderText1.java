package chav1961.da.converter.input;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.Assert;
import org.junit.Test;

import chav1961.da.converter.interfaces.InputConverterInterface;
import chav1961.da.util.interfaces.DAContentFormat;
import chav1961.purelib.basic.exceptions.EnvironmentException;

public class NTripleReaderText1 {
	@Test
	public void basicTest() throws EnvironmentException, IOException {
		final NTripleReader				rdrFactory = new NTripleReader(); 
		final InputConverterInterface	ici = rdrFactory.newInstance(URI.create(InputConverterInterface.CONV_SCHEMA+":"+DAContentFormat.N_TRIPLES.getSchema()+":/"));
		
		try(final InputStream		is = this.getClass().getResourceAsStream("ntriples.zip");
			final ZipInputStream	zis = new ZipInputStream(is)) {
			ZipEntry				ze;
			
			while((ze = zis.getNextEntry()) != null) {
				switch (ze.getName()) {
					case "literal.nt" :
						break;
					default :
						System.err.println("Skip "+ze.getName());
				}
			}
		}
		Assert.fail("Not yet implemented");
	}
	
}
