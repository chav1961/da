package chav1961.da.sender;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import chav1961.da.util.AbstractZipProcessor;
import chav1961.da.util.Constants;
import chav1961.da.util.DAUtils;
import chav1961.purelib.basic.ArgParser;
import chav1961.purelib.basic.SubstitutableProperties;
import chav1961.purelib.basic.exceptions.CommandLineParametersException;
import chav1961.purelib.basic.exceptions.SyntaxException;
import chav1961.purelib.basic.interfaces.LoggerFacade;

// https://mkyong.com/java/apache-httpclient-examples/
// https://www.baeldung.com/httpclient-multipart-upload

public class Application extends AbstractZipProcessor {
	public static final String	ARG_SERVER_URI = "uri";
	public static final String	ARG_SERVER_HEADERS = "headers";
	public static final String	ARG_MULTIPART_FORM = "mf";

	private final PrintStream	err;
	private final URI			server;
	private final Properties	headers;
	private final boolean		multipart;
	private final boolean		debug;
	private final MultipartEntityBuilder	meb;
	
	public Application(final String[] toProcess, final String[] toPass, final String[] toRemove, final String[][] toRename, final PrintStream err, final URI server, final Properties headers, final boolean multipart, final boolean debug) throws SyntaxException, IllegalArgumentException {
		super(toProcess, toPass, join(toRemove, toProcess), toRename);
		this.err = err;
		this.server = server;
		this.headers = headers;
		this.multipart = multipart;
		this.debug = debug;
		
		if (multipart) {
			this.meb = MultipartEntityBuilder.create();
			this.meb.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
		}
		else {
			this.meb = null;
		}
	}

	@Override
	protected void processPart(final String part, final SubstitutableProperties props, final LoggerFacade logger, final InputStream source, final OutputStream target) throws IOException {
		if (debug) {
			message(err, "Sending [%1$s]...", part);
		}
		if (multipart) {
			meb.addBinaryBody(part, source);
		}
		else {
			sendPOST(server, headers, new InputStreamEntity(source));
		}
	}
	
	@Override
	protected void processAppending(final SubstitutableProperties props, final LoggerFacade logger, final OutputStream target) throws IOException {
		super.processAppending(props, logger, target);
		if (multipart) {
			if (debug) {
				message(err, "Sending multipart content...");
			}
			sendPOST(server, headers, meb.build());
		}
	}	
	
	public static void main(final String[] args) {
		System.exit(main(System.in, args, System.out, System.err));
	}	
	
	public static int main(final InputStream is, final String[] args, final OutputStream os, final PrintStream ps) {
		final ArgParser	parserTemplate = new ApplicationArgParser();
		
		try{final ArgParser		parser = parserTemplate.parse(args);
			final boolean		debug = parser.getValue(Constants.ARG_DEBUG, boolean.class);
			final URI			server = parser.getValue(ARG_SERVER_URI, URI.class);
			final Properties	props = new Properties();
			
			if (parser.isTyped(ARG_SERVER_HEADERS)) {
				try(final InputStream	pis = parser.getValue(ARG_SERVER_HEADERS, URI.class).toURL().openStream()) {
					props.load(pis);
				}
			}
			
			final Application	app = new Application(
										parser.isTyped(Constants.ARG_PROCESS) ? new String[] {parser.getValue(Constants.ARG_PROCESS, String.class)} : Constants.MASK_NONE ,
										parser.isTyped(Constants.ARG_PASS) ? new String[] {parser.getValue(Constants.ARG_PASS, String.class)} : Constants.MASK_NONE ,
										parser.isTyped(Constants.ARG_REMOVE) ? new String[] {parser.getValue(Constants.ARG_REMOVE, String.class)} : Constants.MASK_NONE ,
										parser.isTyped(Constants.ARG_RENAME) ? DAUtils.parseRenameArgument(parser.getValue(Constants.ARG_RENAME, String.class)) : new String[0][],
										ps, 
										server,
										props,
										parser.getValue(ARG_MULTIPART_FORM, boolean.class),
										debug);
			final long			startTime = System.currentTimeMillis();

			try(final ZipInputStream	zis = new ZipInputStream(is);
				final ZipOutputStream	zos = new ZipOutputStream(os)) {
				
				message(ps, "Process sending standard input...");
				app.process(zis, zos);
				zos.finish();
			}
			message(ps, "Processing terminated, duration=%1$d msec", System.currentTimeMillis() - startTime);
			return 0;
		} catch (IOException | SyntaxException exc) {
			message(ps, exc.getLocalizedMessage());
			return 129;
		} catch (CommandLineParametersException exc) {
			message(ps, exc.getLocalizedMessage());
			message(ps, parserTemplate.getUsage("da.sender"));
			return 128;
		}
	}

	private static void message(final PrintStream ps, final String format, final Object... parameters) {
		ps.println("da.sender: "+String.format(format, parameters));
		ps.flush();
	}

    private static int sendPOST(final URI url, final Properties props, final HttpEntity content) throws IOException {
    	final HttpPost 				post = new HttpPost(url);
    	
        for (Entry<Object, Object> item : props.entrySet()) {
            post.setHeader(item.getKey().toString(), item.getValue().toString());
        }
        post.setEntity(content);

        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             CloseableHttpResponse response = httpClient.execute(post)) {

        	return response.getStatusLine().getStatusCode();
        }
    }
	
	private static String[] join(final String[] toRemove, final String[] toProcess) {
		final String[] result = new String[toRemove.length + toProcess.length];

		System.arraycopy(toRemove, 0, result, 0, toRemove.length);
		System.arraycopy(toProcess, 0, result, toRemove.length, toProcess.length);
		return result;
	}
	
	private static class ApplicationArgParser extends ArgParser {
		private static final ArgParser.AbstractArg[]	KEYS = {
			new BooleanArg(Constants.ARG_DEBUG, false, "Turn on debug trace", false),
			new PatternArg(Constants.ARG_PROCESS, false, "Process the given parts in the input *.zip. Types as pattern[,...]. If missing, all the parts will be processed. Mutually exclusive with "+Constants.ARG_PASS+" argument. Parts processed will be removed from output stream", ".*"),
			new PatternArg(Constants.ARG_PASS, false, "Pass the given parts in the input *.zip without processing. Types as pattern[,...]. If missing, all the parts will be processed. Mutually exclusive with "+Constants.ARG_PROCESS+" argument", ""),
			new PatternArg(Constants.ARG_REMOVE, false, false, "Remove entries from the *.zip input. Types as pattern[,...]. This option is processed AFTER processing/passing part"),
			new StringArg(Constants.ARG_RENAME, false, false, "Rename entries in the *.zip input. Types as pattern->template[;...], see Java Pattern syntax and Java Mather.replaceAll(...) description. This option is processed AFTER processing/passing part"),
			new URIArg(ARG_SERVER_URI, true, true, "Server URI to send content to."),
			new URIArg(ARG_SERVER_HEADERS, false, false, "Request headers to send to the server URI."),
			new BooleanArg(ARG_MULTIPART_FORM, false, "Pack all the parts processing to the multipart/form-data format", false),
		};
		
		ApplicationArgParser() {
			super(KEYS);
		}
	}
}
