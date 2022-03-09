package chav1961.da.converter.interfaces;

import chav1961.purelib.basic.interfaces.SpiService;

public interface OutputConverterInterface extends SpiService<OutputConverterInterface>, ContentWriter {
	String	CONV_SCHEMA = "outputconverter";

}
