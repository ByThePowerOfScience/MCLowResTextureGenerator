package btpos.tools.mclowrespackgenerator;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.file.PathUtils;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.EventQueue;
import java.awt.FileDialog;
import java.awt.Frame;
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

public class ArgHandler {
	public static final int DEFAULT_MAX_SIZE = 256;
	
	public Scanner input;
	
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
		
		//		Option widthOption = Option.builder("w")
		//		                           .longOpt("width")
		//		                           .desc("Maximum width (pixels) of textures after resize. Default: 128.")
		//		                           .hasArg()
		//		                           .type(Integer.class)
		//		                           .build();
		//
		//		Option heightOption = Option.builder("h")
		//		                            .longOpt("height")
		//		                            .desc("Maximum height (pixels) of textures after resize. Default: 128.")
		//		                            .hasArg()
		//		                            .type(Integer.class)
		//		                            .build();
		
		Options o = new Options().addOption(inputFilesOption)
		                         .addOption(outputFileOption);
		//		                         .addOption(widthOption)
		//		                         .addOption(heightOption);
		
		CommandLine parsed = new DefaultParser().parse(o, args);
		
		
		return new Args(
				null,
				null,
				//				parsed.getParsedOptionValue(widthOption),
				//				parsed.getParsedOptionValue(heightOption),
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
		
//		if (in.maxWidth == null)
//			in.maxWidth = getWidth();
//
//		if (in.maxHeight == null)
//			in.maxHeight = getHeight();
		
		input.close();
		
		return in;
	}
	
	public Integer getWidth() {
		System.out.print("Enter maximum texture width [blank=256]: ");
		String s = input.nextLine();
		int value;
		if (s.isEmpty())
			value = DEFAULT_MAX_SIZE;
		else
			value = Integer.parseInt(s);
		return value;
	}
	
	public Integer getHeight() {
		System.out.print("Enter maximum texture height [blank=256]: ");
		String s = input.nextLine();
		int l;
		if (s.isEmpty())
			l = DEFAULT_MAX_SIZE;
		else
			l = Integer.parseInt(s);
		return l;
	}
	
	public List<File> getInputFiles() {
		final JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
		chooser.setDialogTitle("Select input files:");
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		chooser.setMultiSelectionEnabled(true);
		chooser.setFileFilter(new FileNameExtensionFilter("Mod/Pack (.zip, .jar)", "zip", "jar"));
		System.out.println("Requesting input files");
		EventQueue.invokeAndWait(() -> {
			int result = chooser.showOpenDialog(null);
			if (result != JFileChooser.APPROVE_OPTION) {
				System.exit(0);
			}
			System.out.println("Got input files");
		});
		return Arrays.asList(chooser.getSelectedFiles());
	}
	
	public File getOutputFile() {
		final JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
		chooser.setDialogTitle("Save output as:");
		chooser.setDialogType(JFileChooser.SAVE_DIALOG);
		chooser.setFileFilter(new FileNameExtensionFilter("Resource Pack (.zip)", "zip"));
		chooser.setSelectedFile(new File("downscaled.zip"));
		EventQueue.invokeAndWait(() -> {
			int result = chooser.showSaveDialog(null);
			if (result != JFileChooser.APPROVE_OPTION) {
				System.exit(0);
			}
		});
		return chooser.getSelectedFile();
	}
}