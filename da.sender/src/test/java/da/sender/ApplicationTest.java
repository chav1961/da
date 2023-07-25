package da.sender;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Properties;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import com.sun.net.httpserver.HttpServer;

import chav1961.da.sender.Application;
import chav1961.da.util.Constants;
import chav1961.purelib.basic.PureLibSettings;
import chav1961.purelib.basic.SubstitutableProperties;
import chav1961.purelib.basic.Utils;
import chav1961.purelib.basic.exceptions.SyntaxException;
import chav1961.purelib.basic.interfaces.LoggerFacade;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.junit.Assert;
import org.junit.Test;

public class ApplicationTest {
	@Test
	public void sendTest() throws IOException, SyntaxException {
		final HttpServer 		server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
		
		try{final URI			sa = URI.create("http:/"+server.getAddress()+"/test");
		
			server.createContext("/test", new  MyHttpHandler());
			server.start();
			
			final PseudoApp		app = new PseudoApp(System.err, sa, Utils.mkProps("key","value"), false, false);
			
			app.processPart("part", new SubstitutableProperties(Utils.mkProps("key","value")), PureLibSettings.CURRENT_LOGGER, 
					new ByteArrayInputStream("test string".getBytes()), 
					new OutputStream() {
						@Override public void write(int b) throws IOException {}
					}
			);
		} finally {
			server.stop(0);
		}
	}

	@Test
	public void multipartSendTest() throws IOException, SyntaxException {
		final HttpServer 		server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
		
		try{final URI			sa = URI.create("http:/"+server.getAddress()+"/test");
		
			server.createContext("/test", new  MyMultipartHttpHandler());
			server.start();
			
			final PseudoApp		app = new PseudoApp(System.err, sa, Utils.mkProps("key","value"), true, false);
			
			app.processPart("part1", new SubstitutableProperties(Utils.mkProps("key","value")), PureLibSettings.CURRENT_LOGGER, 
					new ByteArrayInputStream("test string".getBytes()), 
					new OutputStream() {
						@Override public void write(int b) throws IOException {}
					}
			);
			app.processPart("part2", new SubstitutableProperties(Utils.mkProps("key","value")), PureLibSettings.CURRENT_LOGGER, 
					new ByteArrayInputStream("test string".getBytes()), 
					new OutputStream() {
						@Override public void write(int b) throws IOException {}
					}
			);
			app.processAppending(new SubstitutableProperties(Utils.mkProps("key","value")), PureLibSettings.CURRENT_LOGGER, 
					new OutputStream() {
						@Override public void write(int b) throws IOException {}
					}
			);
		} finally {
			server.stop(0);
		}
	}
	
	private static class PseudoApp extends Application {

		public PseudoApp(final PrintStream err, final URI server, final Properties headers, final boolean multipart, final boolean debug) throws SyntaxException, IllegalArgumentException {
			super(Constants.MASK_ANY, Constants.MASK_NONE, Constants.MASK_NONE, new String[0][], err, server, headers, multipart, debug);
		}
		
		@Override
		public void processPart(final String part, final SubstitutableProperties props, final LoggerFacade logger, final InputStream source, final OutputStream target) throws IOException {
			super.processPart(part, props, logger, source, target);
		}

		@Override
		public void processAppending(final SubstitutableProperties props, final LoggerFacade logger, final OutputStream target) throws IOException {
			super.processAppending(props, logger, target);
		}
	}
	
	private static class MyHttpHandler implements HttpHandler {
		@Override
		public void handle(final HttpExchange ex) throws IOException {
			Assert.assertEquals("POST", ex.getRequestMethod());
			Assert.assertEquals("value", ex.getRequestHeaders().getOrDefault("key", new ArrayList<>()).get(0));
			Assert.assertEquals("test string", Utils.fromResource(new InputStreamReader(ex.getRequestBody())));
			ex.sendResponseHeaders(200, 0);
			final OutputStream os = ex.getResponseBody();
			os.write("response".getBytes());
			os.flush();
			os.close();
		} 
	}

	private static class MyMultipartHttpHandler implements HttpHandler {
		@Override
		public void handle(final HttpExchange ex) throws IOException {
			Assert.assertEquals("POST", ex.getRequestMethod());
			Assert.assertEquals("value", ex.getRequestHeaders().getOrDefault("key", new ArrayList<>()).get(0));
			
			try {
				final ByteArrayDataSource 	datasource = new ByteArrayDataSource(ex.getRequestBody(), "multipart/form-data");
				final MimeMultipart 		multipart = new MimeMultipart(datasource);
	
			    Assert.assertEquals(2, multipart.getCount());
			    for (int i = 0; i < multipart.getCount(); i++) {
			        final BodyPart bodyPart = multipart.getBodyPart(i);
			        if (bodyPart.isMimeType("text/plain")) {
			        	Assert.assertEquals("test string", bodyPart.getContent());
			        } else {
			        	Assert.fail("Illegalt content type, text/plain awaited");
			        }
			    }			
			} catch (MessagingException e) {
				throw new IOException(e);
			}
			
			ex.sendResponseHeaders(200, 0);
			final OutputStream os = ex.getResponseBody();
			os.write("response".getBytes());
			os.flush();
			os.close();
		} 
	}
}
