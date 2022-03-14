package chav1961.da.converter;

import java.io.IOException;

import org.junit.Test;

import chav1961.da.util.interfaces.DAContentFormat;

public class SpiTest {

	@Test
	public void test() throws IOException {
		for (DAContentFormat item : DAContentFormat.values()) {
			Application.seekInputConverter(item);
			Application.seekOutputConverter(item);
		}
	}
}
