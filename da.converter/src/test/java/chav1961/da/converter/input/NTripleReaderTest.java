package chav1961.da.converter.input;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.Assert;
import org.junit.Test;

import chav1961.da.converter.interfaces.ContentWriter;
import chav1961.da.converter.interfaces.InputConverterInterface;
import chav1961.da.util.interfaces.DAContentFormat;
import chav1961.purelib.basic.AndOrTree;
import chav1961.purelib.basic.exceptions.EnvironmentException;
import chav1961.purelib.basic.interfaces.SyntaxTreeInterface;

public class NTripleReaderTest {
	@Test
	public void test() throws EnvironmentException, NullPointerException, IllegalArgumentException, IOException {
		final NTripleReader				rdrFactory = new NTripleReader(); 
		final InputConverterInterface	ici = rdrFactory.newInstance(URI.create(InputConverterInterface.CONV_SCHEMA+":"+DAContentFormat.N_TRIPLES.getSchema()+":/"));
		
		try(final InputStream		is = this.getClass().getResourceAsStream("ntriples.zip");
			final ZipInputStream	zis = new ZipInputStream(is)) {
			ZipEntry				ze;
			
			while((ze = zis.getNextEntry()) != null) {
				switch (ze.getName()) {
					case "literal.nt" :
						testLiteral(zis, ici);
						break;
					default :
						System.err.println("Skip "+ze.getName());
				}
			}
		}
	}

	private void testLiteral(final InputStream	is, final InputConverterInterface ici) throws IOException {
		final Reader						rdr = new InputStreamReader(is);
		final SyntaxTreeInterface<char[]>	tree = new AndOrTree<>();
		
		ici.process(rdr, tree, new ContentWriter() {
			private Writer						wr = null;
			private SyntaxTreeInterface<char[]>	tree = null;
			
			@Override
			public void attach(final Writer wr, final SyntaxTreeInterface<char[]> tree) throws IOException{
				this.wr = wr;
				this.tree = tree;
			}
			
			@Override
			public void process(long[] longContent, char[][] charContent) throws IOException {
				// TODO Auto-generated method stub
				Assert.fail("Not yet implemented");
			}
			
			@Override
			public Writer detach() throws IOException {
				final Writer	oldWr = wr;
				
				wr = null;
				return oldWr;
			}
		});
	}
}
