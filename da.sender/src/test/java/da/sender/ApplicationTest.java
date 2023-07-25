package da.sender;

import java.io.IOException;
import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.junit.Assert;
import org.junit.Test;

public class ApplicationTest {
	@Test
	public void sendTest() throws IOException {
		final HttpServer 		server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
		final InetSocketAddress	addr = server.getAddress();
		
		server.createContext("/test", new  MyHttpHandler());
		server.start();
		server.stop(0);
	}

	
	private class MyHttpHandler implements HttpHandler {

		@Override
		public void handle(HttpExchange exchange) throws IOException {
			// TODO Auto-generated method stub
			
		} 
		
	}
}
