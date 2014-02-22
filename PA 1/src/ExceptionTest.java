import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;


public class ExceptionTest {
	
	public static void main(String[] args) {
		try {
			System.out.println("outer try");
			try {
				System.out.println("inner try");
				
				Path path = FileSystems.getDefault().getPath("./testFolder", "test.txt");
				System.out.println("Current directory is: " + path.toString());
				String testing = "testing";
				
				if (Files.exists(path)) {
					
				}
				
				try (BufferedWriter writer = Files.newBufferedWriter(path, Charset.forName("US-ASCII"))) {
					writer.write(testing, 0, testing.length());
				} catch (IOException e) {
					System.out.println("exception message: " + e.getMessage());
				}
				
				throw new Exception();
			} finally {
				System.out.println("inner finally");
			}
		} catch (Exception e) {
			System.out.println("outer catch");
		}
	}
	
}
