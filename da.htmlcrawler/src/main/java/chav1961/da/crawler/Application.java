package chav1961.da.crawler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import chav1961.da.util.AbstractZipProcessor;
import chav1961.da.util.Constants;
import chav1961.da.util.DAUtils;
import chav1961.purelib.basic.ArgParser;
import chav1961.purelib.basic.MimeType;
import chav1961.purelib.basic.PureLibSettings;
import chav1961.purelib.basic.SubstitutableProperties;
import chav1961.purelib.basic.Utils;
import chav1961.purelib.basic.exceptions.CommandLineParametersException;
import chav1961.purelib.basic.exceptions.SyntaxException;
import chav1961.purelib.basic.interfaces.LoggerFacade;
import chav1961.purelib.basic.interfaces.LoggerFacade.Severity;
import nu.validator.htmlparser.common.XmlViolationPolicy;
import nu.validator.htmlparser.sax.HtmlParser;
import nu.validator.htmlparser.sax.XmlSerializer;

public class Application extends AbstractZipProcessor {
	public static final String		ARG_URI = "uri";
	public static final String		ARG_HEADERS = "headers";
	public static final String		ARG_ROBOTS = "robots";
	public static final String		ARG_SITEMAP = "sitemap";
	public static final String		ARG_DEPTH = "depth";
	
	private static final String		FILE_ROBOTS = "robots.txt";
	private static final String		FILE_SITEMAP = "sitemap.xml";

	private static final Pattern	URL_REF_PATTERN = Pattern.compile(
									        "(?:^|[\\W])((ht|f)tp(s?):\\/\\/|www\\.)"
							                + "(([\\w\\-]+\\.){1,}?([\\w\\-.~]+\\/?)*"
							                + "[\\p{Alnum}.,%_=?&#\\-+()\\[\\]\\*$~@!:/{};']*)",
									        Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
	
	private final Pattern[]		toProcess;
	private final URI			siteAddress;
	private final Properties	headers;
	private final PrintStream	err;
	private final RobotsTxt		robots;
	private final SiteMap		sitemap;
	private final int			depth;
	private final boolean		debug;
	
	protected Application(final String[] processMask, final String[] removeMask, final String[][] renameMask, final URI siteAddress, final Properties headers, final PrintStream err, final RobotsTxt robots, final SiteMap sitemap, final int depth, final boolean debug) throws SyntaxException, IllegalArgumentException {
		super(Constants.MASK_NONE, Constants.MASK_ANY, removeMask, renameMask);
		this.toProcess = new Pattern[processMask.length];
		this.siteAddress = siteAddress;
		this.headers = headers;
		this.err = err;
		this.robots = robots;
		this.sitemap = sitemap;
		this.depth = depth;
		this.debug = debug;
		
		for(int index = 0; index < toProcess.length; index++) {
			toProcess[index] = Pattern.compile(processMask[index]);
		}
	}

	@Override
	protected void processPart(final String part, final SubstitutableProperties props, final LoggerFacade logger, final InputStream source, final OutputStream target) throws IOException {
		Utils.copyStream(source, target);
	}
	
	@Override
	protected void processAppending(final SubstitutableProperties props, final LoggerFacade logger, final OutputStream target) throws IOException {

//	        try {
//
//	            HttpGet request = new HttpGet("https://httpbin.org/get");
//
//	            // add request headers
//	            request.addHeader("custom-key", "mkyong");
//	            request.addHeader(HttpHeaders.USER_AGENT, "Googlebot");
//
//	            CloseableHttpResponse response = httpClient.execute(request);
//
//	            try {
//
//	                // Get HttpResponse Status
//	                System.out.println(response.getProtocolVersion());              // HTTP/1.1
//	                System.out.println(response.getStatusLine().getStatusCode());   // 200
//	                System.out.println(response.getStatusLine().getReasonPhrase()); // OK
//	                System.out.println(response.getStatusLine().toString());        // HTTP/1.1 200 OK
//
//	                HttpEntity entity = response.getEntity();
//	                if (entity != null) {
//	                    // return it as a String
//	                    String result = EntityUtils.toString(entity);
//	                    System.out.println(result);
//	                }
//
//	            } finally {
//	                response.close();
//	            }
//	        } finally {
//	            httpClient.close();
//	        }
//		
		
		
        try(final CloseableHttpClient httpClient = HttpClients.createDefault()) {
			super.processAppending(props, logger, target);
			final Set<String> processed = new HashSet<>();
			
			for (URI item : sitemap.getURIs()) {
				crawl(httpClient, item, depth, processed, logger, target);
			}
	    }
	}

	private void crawl(final CloseableHttpClient httpClient, final URI uri, final int depth, final Set<String> processed, final LoggerFacade logger, final OutputStream target) throws IOException {
		final String 	path = uri.getPath();
		
		if (depth >= 0 && path != null && !processed.contains(path) && canServe(path.substring(path.lastIndexOf('/')+1))) {
			processed.add(path);
			
            final HttpGet request = new HttpGet(path);
			
            for(Entry<Object, Object> item : headers.entrySet()) {
            	request.addHeader(item.getKey().toString(), item.getValue().toString());
            }
            
			try(CloseableHttpResponse response = httpClient.execute(request)) {
				final HttpEntity 	entity = response.getEntity();
				
				if (entity.getContentType().getValue().equals(MimeType.MIME_HTML_TEXT.toString())) {
					final String xmlContent = convert(entity.getContent(), logger);
					
					append(path, new ByteArrayInputStream(xmlContent.getBytes(PureLibSettings.DEFAULT_CONTENT_ENCODING)), target);
					
					for (URI item : extractURIs(xmlContent)) {
						crawl(httpClient, item, depth-1, processed, logger, target);
					}
				}
				else {
					append(path, entity.getContent(), target);
				}
			}
		}
	}

	private URI[] extractURIs(final String content) {
		final List<URI>	result = new ArrayList<>();
		final Matcher 	matcher = URL_REF_PATTERN.matcher(content);
		
		while (matcher.find()) {
		    int matchStart = matcher.start(1);
		    int matchEnd = matcher.end();

		    result.add(URI.create(content.substring(matchStart, matchEnd)));
		}
		return result.toArray(new URI[result.size()]);
	}

	private boolean canServe(final String path) {
		if (robots.canParse(path)) {
			for (Pattern item : toProcess) {
				if (item.matcher(path).find()) {
					return true;
				}
			}
		}
		return false;
	}

	public static void main(final String[] args) {
		System.exit(main(System.in, args, System.out, System.err));
	}

	public static int main(final InputStream is, final String[] args, final OutputStream os, final PrintStream err) {
		final ArgParser				parserTemplate = new ApplicationArgParser();
		
		try{final ArgParser		parser = parserTemplate.parse(args);
			final boolean		debug = parser.getValue(Constants.ARG_DEBUG, boolean.class);
			final URI			uri = parser.getValue(ARG_ROBOTS, URI.class);
			final long 			startTime = System.currentTimeMillis();
			final Properties	props = new Properties();
			
			message(err, "Preload settings from [%1$s]...", uri);
			
			if (parser.isTyped(ARG_HEADERS)) {
				try(final InputStream	pis = parser.getValue(ARG_HEADERS, URI.class).toURL().openStream()) {
					props.load(pis);
				}
			}
			final RobotsTxt			robots = new RobotsTxt(Utils.fromResource((parser.isTyped(ARG_ROBOTS) 
														? parser.getValue(ARG_ROBOTS, URI.class) 
														: uri.resolve(FILE_ROBOTS)).toURL())
													);
			final SiteMap			sitemap = new SiteMap (Utils.fromResource((parser.isTyped(ARG_SITEMAP) 
														? parser.getValue(ARG_SITEMAP, URI.class) 
														: (!Utils.checkEmptyOrNullString(robots.getSiteMapAddress())
															? URI.create(robots.getSiteMapAddress())
															: uri.resolve(FILE_SITEMAP))
														).toURL()
													)
												);
			final Application		app = new Application(											
										parser.isTyped(Constants.ARG_PROCESS) ? new String[] {parser.getValue(Constants.ARG_PROCESS, String.class)} : Constants.MASK_ANY ,
										parser.isTyped(Constants.ARG_REMOVE) ? new String[] {parser.getValue(Constants.ARG_REMOVE, String.class)} : Constants.MASK_NONE ,
										parser.isTyped(Constants.ARG_RENAME) ? DAUtils.parseRenameArgument(parser.getValue(Constants.ARG_RENAME, String.class)) : new String[0][],
										uri,
										props,
										err,
										robots,
										sitemap,
										parser.getValue(ARG_DEPTH, int.class),
										debug
									);
			
			try(final ZipInputStream	zis = new ZipInputStream(is);
				final ZipOutputStream	zos = new ZipOutputStream(os)) {
			
				message(err, "Processing standard input...");
				app.process(zis, zos);
				zos.finish();
			}
			message(err, "Processing terminated, duration=%1$d msec", (System.currentTimeMillis() - startTime));
			return 0;
		} catch (CommandLineParametersException exc) {
			err.println(exc.getLocalizedMessage());
			err.println(parserTemplate.getUsage("da.htmlcrowler"));
			return 128;
		} catch (IOException | SyntaxException exc) {
			exc.printStackTrace(err);
			return 129;
		}
	}

	static String convert(final InputStream is, final LoggerFacade logger) throws IOException {
		try {
			final StringWriter		os = new StringWriter();
			final ContentHandler 	serializer = new XmlSerializer(os);
			final HtmlParser 		parser = new HtmlParser(XmlViolationPolicy.ALTER_INFOSET);
	
			parser.setErrorHandler(new ErrorHandler() {
				@Override
				public void warning(final SAXParseException exc) throws SAXException {
					logger.message(Severity.warning, exc.getLocalizedMessage());
				}
	
				@Override
				public void error(final SAXParseException exc) throws SAXException {
					logger.message(Severity.error, exc.getLocalizedMessage());
				}
	
				@Override
				public void fatalError(final SAXParseException exc) throws SAXException {
					logger.message(Severity.severe, exc.getLocalizedMessage());
				}
			});
			parser.setContentHandler(serializer);
			parser.setProperty("http://xml.org/sax/properties/lexical-handler", serializer);
			parser.parse(new InputSource(is));
			os.flush();
			return os.toString();
		} catch (SAXException exc) {
			throw new IOException(exc.getLocalizedMessage(), exc);
		}
	}
	
	private static void message(final PrintStream ps, final String format, final Object... parameters) {
		ps.println("da.htmlcrowler: "+String.format(format, parameters));
	}
	
	static class ApplicationArgParser extends ArgParser {
		private static final ArgParser.AbstractArg[]	KEYS = {
			new BooleanArg(Constants.ARG_DEBUG, false, "Turn on debug trace", false),
			new PatternArg(Constants.ARG_PROCESS, false, "Load the given files from site. Types as pattern[,...]. If missing, all the files will be loaded.", ".*"),
			new PatternArg(Constants.ARG_REMOVE, false, false, "Remove entries from the *.zip input. Types as pattern[,...]. This option is processed AFTER processing/passing part"),
			new StringArg(Constants.ARG_RENAME, false, false, "Rename entries in the *.zip input. Types as pattern->template[;...], see Java Pattern syntax and Java Mather.replaceAll(...) description. This option is processed AFTER processing/passing part"),
			new URIArg(ARG_ROBOTS, false, false, "Emulation of robots.txt file content. If mising, robots.txt from the site will be used"),
			new URIArg(ARG_SITEMAP, false, false, "Emulation of sitemap.xml file content. If missing, sitemap.xml from the site will be used"),
			new IntegerArg(ARG_DEPTH, false, "Depth to resolve references inside html content.", 0),
			new URIArg(ARG_URI, true, true, "URI to crowl site"),
		};
		
		ApplicationArgParser() {
			super(KEYS);
		}
	}

	static class RobotsTxt {
		private final String siteMap;
		private final String[] allowed;
		private final Pattern[] allow;
		private final Pattern[] disallow;
		
		RobotsTxt(final String content) throws SyntaxException {
			if (Utils.checkEmptyOrNullString(content)) {
				throw new IllegalArgumentException("Content can't be null"); 
			}
			else {
				final List<String>	forAllowed = new ArrayList<>();
				final List<Pattern>	forAllow = new ArrayList<>();
				final List<Pattern>	forDisallow = new ArrayList<>();
				String forSiteMap = "";
				int lineNo = 1;
				
				for(String line : content.split("\n")) {
					final String trim = line.trim();
					
					if (!Utils.checkEmptyOrNullString(trim) && trim.startsWith("#")) {
						final int colonIndex = trim.indexOf(':');
						
						if (colonIndex < 0) {
							throw new SyntaxException(lineNo, 0, "Illegal robots.txt format - (:) is missing");
						}
						else {
							final String	key = trim.substring(0, colonIndex).trim(), value = trim.substring(colonIndex + 1).trim();
							
							try {
								switch (key.toLowerCase()) {
									case "allow"	:
										forAllowed.add(value);
										forAllow.add(Pattern.compile(Utils.fileMask2Regex(value)));
										break;
									case "disallow"	:
										forAllow.add(Pattern.compile(Utils.fileMask2Regex(value)));
										break;
									case "sitemap"	:
										forSiteMap = value;
										break;
									default:
										break;
								}
							} catch (IllegalArgumentException exc) {
								throw new SyntaxException(lineNo, 0, "Illegal path mask"); 
							}
						}
					}
					lineNo++;
				}
				this.siteMap = forSiteMap;
				this.allowed = forAllowed.toArray(new String[forAllowed.size()]);
				this.allow = forAllow.toArray(new Pattern[forAllow.size()]);
				this.disallow = forDisallow.toArray(new Pattern[forDisallow.size()]);
			}			
		}
		
		public boolean canParse(final String path) {
			if (Utils.checkEmptyOrNullString(path)) {
				throw new IllegalArgumentException("Path to check can't be null"); 
			}
			else {
				for (Pattern item : allow) {
					if (item.matcher(path).find()) {
						return true;
					}
				}
				for (Pattern item : disallow) {
					if (item.matcher(path).find()) {
						return false;
					}
				}
				return true;
			}
		}
		
		public String getSiteMapAddress() {
			return siteMap;
		}

		public String[] getAllowed() {
			return allowed;
		}
		
		@Override
		public String toString() {
			return "RobotsTxt [siteMap=" + siteMap + ", allow=" + Arrays.toString(allow) + ", disallow=" + Arrays.toString(disallow) + "]";
		}
	}
	
	static class SiteMap {
		private static final String	JAXP_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";
		private static final String	SCHEMA_SITEMAP_INDEX_SOURCE = "http://www.sitemaps.org/schemas/sitemap/0.9/siteindex.xsd";
		private static final String	SCHEMA_SITEMAP_SOURCE = "http://www.sitemaps.org/schemas/sitemap/0.9/sitemap.xsd";
		
		private final URI[]	uris;
		
		SiteMap(final String content) throws SyntaxException {
			if (Utils.checkEmptyOrNullString(content)) {
				throw new IllegalArgumentException("Content can't be null");
			}
			else if (content.contains("<?xml")) {
				try{
					if (content.contains("<sitemapindex")) {
						final List<URI>	temp = new ArrayList<>();
						
						for(URI uri : extractXml(content, SCHEMA_SITEMAP_INDEX_SOURCE, "loc")) {
							temp.addAll(Arrays.asList(extractXml(Utils.fromResource(uri.toURL()), SCHEMA_SITEMAP_SOURCE, "loc")));
						}
						this.uris = temp.toArray(new URI[temp.size()]);
					}
					else if (content.contains("<urlset")) {
						this.uris = extractXml(content, SCHEMA_SITEMAP_SOURCE, "loc");
					}
					else {
						this.uris = extractXml(content, null, "link");
					}
				} catch (ParserConfigurationException | IOException | SAXException | DOMException | URISyntaxException e) {
					throw new SyntaxException(0, 0, e.getLocalizedMessage(), e);
				}
			}
			else {
				this.uris = extractPlain(content);
			}
		}

		public URI[] getURIs() {
			return uris;
		}
		
		@Override
		public String toString() {
			return "SiteMap [uris=" + Arrays.toString(uris) + "]";
		}

		private URI[] extractXml(final String content, final String schema, final String tag) throws ParserConfigurationException, SAXException, IOException, DOMException, URISyntaxException {
			final DocumentBuilderFactory 	factory = DocumentBuilderFactory.newInstance();
			
			if (!Utils.checkEmptyOrNullString(schema)) {
				factory.setAttribute(JAXP_SCHEMA_SOURCE, SCHEMA_SITEMAP_SOURCE);
			}
			
			final DocumentBuilder 	builder = factory.newDocumentBuilder();
			final Document			doc = builder.parse(new InputSource(new StringReader(content)));
			final NodeList 			list = doc.getElementsByTagName(tag);
			final List<URI>			uris = new ArrayList<>(); 
			
			for (int index = 0; index < list.getLength(); index++) {
				uris.add(new URI(list.item(index).getTextContent()));
			}
			return uris.toArray(new URI[uris.size()]);
		}

		private URI[] extractPlain(final String content) throws SyntaxException {
			final String[] 	paths = content.split("\n");
			final URI[]		result = new URI[paths.length];
			
			for(int index = 0; index < result.length; index++) {
				final String temp = paths[index].trim();
				
				if (!Utils.checkEmptyOrNullString(temp)) {
					try {
						result[index] = URI.create(temp);
					} catch (IllegalArgumentException exc) {
						throw new SyntaxException(index, 0, exc.getLocalizedMessage(), exc); 
					}
				}
			}
			return result;
		}
	}
}
