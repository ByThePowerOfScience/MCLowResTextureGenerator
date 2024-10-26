package btpos.tools.mclowrespackgenerator;

import java.io.File;
import java.util.List;

public class Args {
	public Integer maxWidth;
	public Integer maxHeight;
	public List<File> inputFiles;
	public File outputFile;
	
	public Args(Integer maxWidth, Integer maxHeight, List<File> inputFiles, File outputFile) {
		this.maxWidth = maxWidth;
		this.maxHeight = maxHeight;
		this.inputFiles = inputFiles;
		this.outputFile = outputFile;
	}
}
