package chav1961.da.converter.interfaces;

import java.io.IOException;
import java.io.Writer;

import chav1961.purelib.basic.interfaces.SyntaxTreeInterface;

public interface ContentWriter {
	void attach(final Writer wr, final SyntaxTreeInterface<char[]> tree) throws IOException;
	void process(final long[] longContent, final char[][] charContent) throws IOException;
	Writer detach() throws IOException;
}
