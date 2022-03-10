package chav1961.da.util;

import org.junit.Assert;
import org.junit.Test;

import chav1961.da.util.interfaces.RenamingInterface;
import chav1961.purelib.basic.exceptions.SyntaxException;

public class RenaminClassTest {
	@Test
	public void test() throws SyntaxException {
		RenamingInterface	ri = new RenamingClass("(\\d*)->$1$1");
		
		Assert.assertEquals("as", ri.renameEntry("as"));
		Assert.assertEquals("1212", ri.renameEntry("12"));

		ri = new RenamingClass("(\\d)(\\p{Lower})->$2$1;(\\d+)->$1$1");
		
		Assert.assertEquals("a1", ri.renameEntry("1a"));
		Assert.assertEquals("1212", ri.renameEntry("12"));

		try{new RenamingClass(null);
			Assert.fail("Mandatory exception was not detected (null 1-st argument)");
		} catch (IllegalArgumentException exc) {
		}
		try{new RenamingClass("");
			Assert.fail("Mandatory exception was not detected (empty 1-st argument)");
		} catch (IllegalArgumentException exc) {
		}
		
		try{new RenamingClass("as;");
			Assert.fail("Mandatory exception was not detected (missing '->')");
		} catch (SyntaxException exc) {
		}
		try{new RenamingClass("a->b->c");
			Assert.fail("Mandatory exception was not detected (missing ';' before '->')");
		} catch (SyntaxException exc) {
		}
		try{new RenamingClass("{->a");
			Assert.fail("Mandatory exception was not detected (illegal pattern syntax)");
		} catch (SyntaxException exc) {
		}
	}
}
