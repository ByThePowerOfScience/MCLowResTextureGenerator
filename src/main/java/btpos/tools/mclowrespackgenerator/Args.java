package btpos.tools.mclowrespackgenerator;


import org.apache.commons.io.FileUtils;
import org.apache.commons.io.file.PathUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class Args {
	public Integer maxScale;
	public List<File> inputFiles;
	public File outputFile;
	
	public Args(Integer maxScale, List<File> inputFiles, File outputFile) {
		this.maxScale = maxScale;
		this.inputFiles = inputFiles;
		this.outputFile = outputFile;
		
		if (inputFiles != null)
			validateInputFiles(this.inputFiles);
		
		if (outputFile != null)
			validateOutputFile(this.outputFile);
		
		if (maxScale != null)
			validateMaxScale(this.maxScale);
	}
	
	private void validateMaxScale(Integer maxScale) {}
	
	private void validateInputFiles(List<File> inputFiles) throws IllegalArgumentException {
		inputFiles.forEach(f -> {
			if (!f.exists())
				throw new IllegalArgumentException(new FileNotFoundException(f.getPath()));
			if (f.isDirectory())
				throw new IllegalArgumentException(f.getPath() + " is not a file!");
		});
		
		System.out.println("Input files: " + inputFiles.stream().map(File::toString).collect(Collectors.joining(", ")));
	}
	
	private void validateOutputFile(File f) {
		Path path = f.toPath();
		if (!PathUtils.getExtension(path).equals("zip")) {
			System.out.println("Output file was not a zip file, renaming to " + this.outputFile.getPath());
			this.outputFile = new File(path + ".zip");
		}
		try {
			FileUtils.createParentDirectories(f);
		} catch (IOException e) {
			throw new RuntimeException("Unable to create parent directories for " + f.getAbsolutePath(), e);
		}
		
		System.out.println("Output file: " + outputFile.toString());
	}
}
