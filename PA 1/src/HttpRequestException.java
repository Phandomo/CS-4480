import javax.xml.ws.http.HTTPException;

/**
 * HTTPRequestException.java - Exception class for invalidd HTTP requests
 * 
 * @author Rob Johansen
 */
public class HttpRequestException extends HTTPException
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String message;
	
	/**
	 * @param statusCode
	 */
	public HttpRequestException(int statusCode, String message)
	{
		super(statusCode);
		this.message = message;
	}
	
	@Override
	public String getMessage()
	{
		return message;
	}
}
