package chav1961.da.converter.interfaces;

import java.io.IOException;
import java.io.Writer;

import chav1961.purelib.basic.interfaces.SyntaxTreeInterface;

public interface ContentWriter extends ContentProcessor {
	/**
	 * <p>Attach writer and syntax tree to content writer.</p>
	 * @param wr writer to attach. Can't be null
	 * @param tree syntax tree to attach. Can't be null
	 * @throws IOException on any I/O errors
	 */
	void attach(final Writer wr, final SyntaxTreeInterface<char[]> tree) throws IOException;
	
	/**
	 * <p>Detach writer 
	 * @return
	 * @throws IOException
	 */
	Writer detach() throws IOException;
}
