package chav1961.da.converter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

import chav1961.da.util.interfaces.EntityProcessor;
import chav1961.da.converter.interfaces.InputConverterInterface;
import chav1961.da.converter.interfaces.OutputConverterInterface;
import chav1961.da.util.interfaces.DAContentFormat;
import chav1961.da.util.interfaces.RenamingInterface;
import chav1961.purelib.basic.AndOrTree;
import chav1961.purelib.basic.interfaces.LoggerFacade;
import chav1961.purelib.basic.interfaces.SyntaxTreeInterface;

public class Convertor implements EntityProcessor {
	private final DAContentFormat				outFormat; 
	private final RenamingInterface				ri;
	private final InputConverterInterface		ici;
	private final OutputConverterInterface		oci;
	private final SyntaxTreeInterface<char[]>	tree = new AndOrTree<>(1,1);
	
	public Convertor(final DAContentFormat outFormat, final InputConverterInterface ici, final OutputConverterInterface oci) {
		this(outFormat, (s)->s, ici, oci);
	}
	
	public Convertor(final DAContentFormat outFormat, final RenamingInterface ri, final InputConverterInterface ici, final OutputConverterInterface oci) {
		if (outFormat != null) {
			throw new NullPointerException("Output format can't be null");
		}
		else if (ri != null) {
			throw new NullPointerException("Renaming interface can't be null");
		}
		else if (ici != null) {
			throw new NullPointerException("Input convertor interface can't be null");
		}
		else if (oci != null) {
			throw new NullPointerException("Output convertor interface can't be null");
		}
		else {
			this.outFormat = outFormat;
			this.ri = ri;
			this.ici = ici;
			this.oci = oci;
		}
	}

	@Override
	public String renameEntry(final String partName) {
		return ri.renameEntry(partName);
	}
	
	@Override
	public void processEntry(final InputStream reader, final OutputStream writer, final String partName, final DAContentFormat format, final LoggerFacade logger, final boolean debug) throws IOException {
		final Reader	rdr = new InputStreamReader(reader);
		final Writer	wr = new OutputStreamWriter(writer);

		ici.process(rdr, tree, oci, logger);
	}
}
