import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HttpRequest.java - Representation of an HTTP 1.0 request
 * 
 * @author Rob Johansen
 */
public class HttpRequest {
	
	// Constants for request codes and messages
	private final int    BAD_REQUEST_CODE                   = 400;
	private final String BAD_REQUEST_MESSAGE                = "Bad Request";
	private final int    NOT_IMPLEMENTED_CODE               = 501;
	private final String NOT_IMPLEMENTED_MESSAGE            = "Not Implemented";
	private final int    HTTP_VERSION_NOT_SUPPORTED_CODE    = 505;
	private final String HTTP_VERSION_NOT_SUPPORTED_MESSAGE = "HTTP Version Not Supported";
	
	// Constants for HTTP 1.0 methods
	private final String GET_METHOD  = "GET";
	private final String HEAD_METHOD = "HEAD";
	private final String POST_METHOD = "POST";
	
	// Constant for HTTP version number
	private final String HTTP_1_0_VERSION = "HTTP/1.0";
	
	private final String CRLF = "\r\n";
	private final int HTTP_PORT = 80;
	
	// Request parameters
	private String method;
	private String uri;
	private String version;
	private String headers = "";
	
	// uri tokens
	private String host;
	private int port;
	private String path;
	
	/** Create HttpRequest by reading it from the client socket */
	public HttpRequest(BufferedReader fromClient) throws IOException {
		String requestLine     = fromClient.readLine();
		String[] requestTokens = requestLine.split(" ");
		
		if (requestTokens.length < 3)
			throwHttpRequestException(BAD_REQUEST_CODE, BAD_REQUEST_MESSAGE);
		
		method  = requestTokens[0];
		uri     = requestTokens[1];
		version = requestTokens[2];
		
		// Return error code 501/400 if request method is not "GET"
		if (!method.equals(GET_METHOD)) {
			if (method.equals(HEAD_METHOD) || method.equals(POST_METHOD))
				throwHttpRequestException(NOT_IMPLEMENTED_CODE, NOT_IMPLEMENTED_MESSAGE);
			else
				throwHttpRequestException(BAD_REQUEST_CODE, BAD_REQUEST_MESSAGE);
		}
		
		// Return error code 400 if uri is not absolute (must begin with scheme followed by colon)
		Pattern uriPattern = Pattern.compile("^\\w+:\\/\\/(?<host>[\\w\\d\\.]+):?(?<port>\\d+)?(?<path>\\/?.*)$");
		Matcher uriMatcher = uriPattern.matcher(uri);
		
		if (uriMatcher.matches()) {
			host = uriMatcher.group("host");
			
			if (host == null)
				throwHttpRequestException(BAD_REQUEST_CODE, BAD_REQUEST_MESSAGE);
			
			if (uriMatcher.group("port") != null) {
				try {
					port = Integer.parseInt(uriMatcher.group("port"));
				}
				catch (NumberFormatException e) {
					throwHttpRequestException(BAD_REQUEST_CODE, BAD_REQUEST_MESSAGE);
				}
			}
			else {
				port = HTTP_PORT;
			}
			
			if (uriMatcher.group("path") != null && uriMatcher.group("path").length() > 0)
				path = uriMatcher.group("path");
			else
				path = "/";
		}
		else {
			throwHttpRequestException(BAD_REQUEST_CODE, BAD_REQUEST_MESSAGE);
		}
		
		// Return error code 505 if HTTP version is not 1.0
		if (!version.equals(HTTP_1_0_VERSION))
			throwHttpRequestException(HTTP_VERSION_NOT_SUPPORTED_CODE, HTTP_VERSION_NOT_SUPPORTED_MESSAGE);
		
		headers += "Host: " + host + ":" + port + CRLF;
		headers += "Connection: close" + CRLF;
		
		String line = fromClient.readLine();
		
		while (line.length() != 0) {
			// Valid characters in HTTP 1.0 header name: !#$%&'*+-.0-9A-Z^_`a-z|~
			Pattern header = Pattern.compile("^(?<name>[\\!\\#\\$\\%\\&\\'\\*\\+\\-\\.0-9A-Z\\^\\_\\`a-z\\|\\~]+): (?<value>.*)$");
			Matcher headerMatcher = header.matcher(line);
			
			if (headerMatcher.matches()) {
				String headerName  = headerMatcher.group("name");
				String headerValue = headerMatcher.group("value");
				
				if (headerName.toLowerCase().equals("host") || headerName.toLowerCase().equals("connection"))
					continue;
				
				headers += headerName + ": " + headerValue + CRLF;
			}
			else {
				throwHttpRequestException(BAD_REQUEST_CODE, BAD_REQUEST_MESSAGE);
			}
			
			line = fromClient.readLine();
		}
	}
	
	private void throwHttpRequestException(int statusCode, String message) throws HttpRequestException {
		throw new HttpRequestException(statusCode, message);
	}
	
	public String getUri() {
		return uri;
	}
	
	/** Return host for which this request is intended */
	public String getHost() {
		return host;
	}

	/** Return port */
	public int getPort() {
		return port;
	}
	
	/** Return path */
	public String getPath() {
		return path;
	}

	/**
	 * Convert request into a string for easy re-sending.
	 */
	public String toString() {
		String req = method + " " + path + " " + version + CRLF;
		req += headers;
		req += CRLF;
		
		return req;
	}
}