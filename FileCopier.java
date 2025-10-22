import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class FileCopier {
	
	public static void main(String[] args)
			throws Exception {
		if (args.length != 2) {
			throw new IllegalArgumentException(FileCopier.class.getName() + " requires 2 arguments (source path and target path)");
		}
		Path target = Files.copy(Path.of(args[0]), Path.of(args[1]), StandardCopyOption.REPLACE_EXISTING);
		System.out.println("File " + args[0] + " has been copied to " + target.toAbsolutePath());
	}
	
}
