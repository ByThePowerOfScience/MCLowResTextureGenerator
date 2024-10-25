package btpos.tools.mclowrespackgenerator;

import btpos.tools.mclowrespackgenerator.Main.Args;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.file.PathUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ArgHandler {
	public static final ExtensionFilter zipFilter = new ExtensionFilter("ZIP archive", "*.zip");
	public static Scanner input;
	
	public Args checkCliArgs(String[] args) throws ParseException {
		Option inputFilesOption = Option.builder("i")
		                                .longOpt("input")
		                                .desc("Input files to create a resource pack for, or directory containing files.")
		                                .hasArgs()
		                                .type(String.class)
		                                .build();
		
		Option outputFileOption = Option.builder("o")
		                                .longOpt("output-zip")
		                                .desc("Name of the output zip or the output folder to put it in.")
		                                .hasArg()
		                                .argName("name")
		                                .type(String.class)
		                                .build();
		
		Option widthOption = Option.builder("w")
		                           .longOpt("width")
		                           .desc("Maximum width (pixels) of textures after resize. Default: 128.")
		                           .hasArg()
		                           .type(Integer.class)
		                           .build();
		
		Option heightOption = Option.builder("h")
		                            .longOpt("height")
		                            .desc("Maximum height (pixels) of textures after resize. Default: 128.")
		                            .hasArg()
		                            .type(Integer.class)
		                            .build();
		
		Options o = new Options().addOption(inputFilesOption)
		                         .addOption(outputFileOption)
		                         .addOption(widthOption)
		                         .addOption(heightOption);
		
		CommandLine parsed = new DefaultParser().parse(o, args);
		
		
		
		return new Args(
				parsed.getParsedOptionValue(widthOption),
				parsed.getParsedOptionValue(heightOption),
				Optional.ofNullable(parsed.getOptionValues(inputFilesOption))
				        .map(names -> Arrays.stream(names).map(File::new).peek(f -> {
							if (!f.exists())
								throw new IllegalArgumentException(new FileNotFoundException(f.getPath()));
							if (f.isDirectory())
								throw new IllegalArgumentException(f.getPath() + " is not a file!");
				        }).collect(Collectors.toList()))
				        .orElse(null),
				Optional.ofNullable(parsed.getOptionValue(outputFileOption))
				        .map(Paths::get)
				        .map(Path::toFile)
						.map(f -> {
							try {
								FileUtils.createParentDirectories(f);
							} catch (IOException e) {
								throw new RuntimeException("Unable to create parent directories for " + f.getAbsolutePath(), e);
							}
							return f;
						})
				        .orElse(null)
		);
	}
	
	public Args getDefaultArgs(Args in) {
		input = new Scanner(System.in);
		
		if (in.inputFiles == null)
			in.inputFiles = getInputFiles();
		
		System.out.println("Input files: " + in.inputFiles.stream().map(File::toString).collect(Collectors.joining(", ")));
		
		if (in.outputFile == null)
			in.outputFile = getOutputFile();
		
		System.out.println("Output file: " + in.outputFile.toString());
		
		if (in.maxWidth == null)
			in.maxWidth = getWidth();
		
		if (in.maxHeight == null)
			in.maxHeight = getHeight();
		
		System.out.println("Output file");
		
		input.close();
		
		return in;
	}
	
	public static Integer getWidth() {
		System.out.print("Enter maximum texture width (in pixels): ");
		int out = input.nextInt();
		input.nextLine();
		return out;
	}
	
	public static Integer getHeight() {
		System.out.print("Enter maximum texture height (in pixels): ");
		int out = input.nextInt();
		input.nextLine();
		return out;
	}
	
	public static List<File> getInputFiles() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Select mods to generate a pack for:");
		fileChooser.getExtensionFilters().addAll(new ExtensionFilter("Java JAR Archive", "*.jar"), zipFilter);
		return fileChooser.showOpenMultipleDialog(null);
	}
	
	public static File getOutputFile() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Select output file:");
		fileChooser.getExtensionFilters().add(zipFilter);
		fileChooser.setInitialDirectory(new File(System.getProperty("user.dir")));
		fileChooser.setInitialFileName("downscaled.zip");
		return fileChooser.showSaveDialog(null);
	}
}