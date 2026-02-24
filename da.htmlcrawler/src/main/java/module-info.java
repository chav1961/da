module da.htmlcrawler {
	requires transitive chav1961.purelib;
	requires da.util;
	requires java.base;
	requires org.apache.httpcomponents.httpcore;
	requires org.apache.httpcomponents.httpclient;
	requires htmlparser;
	
	exports chav1961.da.crawler;
}
