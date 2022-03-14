package chav1961.da.converter.interfaces;

import java.io.IOException;
import java.io.Reader;

import chav1961.purelib.basic.PureLibSettings;
import chav1961.purelib.basic.interfaces.LoggerFacade;
import chav1961.purelib.basic.interfaces.SpiService;
import chav1961.purelib.basic.interfaces.SyntaxTreeInterface;

public interface InputConverterInterface extends SpiService<InputConverterInterface> {
	String	CONV_SCHEMA = "inputconverter";
	int		SUBJ_INDEX = 0;
	int		PRED_INDEX = 1;
	int		OBJ_INDEX = 2;
	int		TYPE_INDEX = 3;
	int		DUMMY_VALUE = -1;
	
	default void process(Reader rdr, SyntaxTreeInterface<char[]> tree, ContentWriter writer) throws IOException {
		process(rdr, tree, writer, PureLibSettings.CURRENT_LOGGER);
	}

	void process(Reader rdr, SyntaxTreeInterface<char[]> tree, ContentWriter writer, LoggerFacade logger) throws IOException;
}
