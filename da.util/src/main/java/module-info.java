module da.util {
	requires transitive chav1961.purelib;
	requires java.base;
	
	exports chav1961.da.util to da.xmlcrawler, da.source, da.converter, da.infer, da.sender, da.htmlcrawler;
	exports chav1961.da.util.interfaces to da.xmlcrawler, da.source, da.converter, da.infer, da.sender, da.htmlcrawler;
}
