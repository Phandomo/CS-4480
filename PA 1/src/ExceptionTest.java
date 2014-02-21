
public class ExceptionTest {
	
	public static void main(String[] args) {
		try {
			System.out.println("outer try");
			try {
				System.out.println("inner try");
				throw new Exception();
			} finally {
				System.out.println("inner finally");
			}
		} catch (Exception e) {
			System.out.println("outer catch");
		}
	}
	
}
