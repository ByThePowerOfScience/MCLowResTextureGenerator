package btpos.tools.mclowrespackgenerator;

import net.coobird.thumbnailator.Thumbnailator;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class Main {
	public static void main(String[] args) throws ParseException, IOException {
		Args arguments = handleArgs(args);
		
		
		arguments.outputFile.delete();
		arguments.outputFile.createNewFile();
		
		try (ZipOutputStream zipOut = new ZipOutputStream(Files.newOutputStream(arguments.outputFile.toPath()))) {
			arguments.inputFiles.stream()
			                    .map(file -> {
				                    try {
					                    return new ZipFile(file);
				                    } catch (IOException e) {
					                    throw new RuntimeException(e);
				                    }
			                    })
			                    .flatMap(zip -> Util.enumerationAsStream(zip.entries())
			                                        .map(entry -> {
				                                        try {
					                                        return new Pair<>(entry, zip.getInputStream(entry));
				                                        } catch (IOException e) {
					                                        throw new RuntimeException(e);
				                                        }
			                                        })
			                    )
			                    .map(entry_stream -> {
				                    try {
					                    return entry_stream.r(ImageIO.read(entry_stream.right));
				                    } catch (IOException e) {
					                    throw new RuntimeException(e);
				                    }
			                    })
			                    .filter(entry_bufimg -> entry_bufimg.right.getWidth() > arguments.maxWidth || entry_bufimg.right.getHeight() > arguments.maxHeight)
			                    .map(entry_bufimg -> entry_bufimg.r(Thumbnailator.createThumbnail(entry_bufimg.right, arguments.maxWidth, arguments.maxHeight)))
			                    .forEach(entry_bufimg -> {
				                    try {
					                    zipOut.putNextEntry(new ZipEntry(entry_bufimg.left.getName()));
										ImageIO.write(entry_bufimg.right, "png", zipOut);
										zipOut.closeEntry();
				                    } catch (IOException e) {
					                    throw new RuntimeException(e);
				                    }
			                    });
		}
		
	}
	
	static Args handleArgs(String[] args) throws ParseException {
		Option inputFilesOption = Option.builder("i")
		                                .longOpt("input")
		                                .desc("Input files to create a resource pack for, or directory containing files.")
		                                .hasArgs()
		                                .type(File[].class)
		                                .build();
		
		Option outputFileOption = Option.builder("o")
		                                .longOpt("output-zip")
		                                .desc("Name of the output zip or the output folder to put it in.")
		                                .hasArg()
		                                .argName("name")
		                                .type(File.class)
		                                .build();
		
		Option widthOption = Option.builder("w")
		                           .longOpt("width")
		                           .desc("Maximum width (pixels) of textures after resize.")
		                           .hasArg()
		                           .type(Integer.class)
		                           .build();
		Option heightOption = Option.builder("h")
		                            .longOpt("height")
		                            .desc("Maximum height (pixels) of textures after resize.")
		                            .hasArg()
		                            .type(Integer.class)
		                            .build();
		Options o = new Options().addOption(inputFilesOption)
		                         .addOption(outputFileOption)
		                         .addOption(widthOption)
		                         .addOption(heightOption);
		
		CommandLine parsed = new DefaultParser().parse(o, args);
		Integer width = parsed.getParsedOptionValue(widthOption, 128);
		Integer height = parsed.getParsedOptionValue(heightOption, 128);
		List<File> inputFiles = new ArrayList<>(Arrays.asList(parsed.getParsedOptionValue(inputFilesOption)));
		if (inputFiles.isEmpty())
			throw new MissingArgumentException(inputFilesOption);
		File outputFile = parsed.getParsedOptionValue(outputFileOption, Paths.get(System.getProperty("user.dir"), "downscaled.zip")).toFile();
		
		return validateArgs(new Args(width, height, inputFiles, outputFile));
	}
	
	private static Args validateArgs(Args args) {
		// verify all input files exist
		AtomicBoolean foundNonExistentFile = new AtomicBoolean(false);
		args.inputFiles.stream()
		               .filter(file -> !file.exists())
		               .forEach(f -> {
			               System.out.println("File not found: \"" + f + "\".");
			               foundNonExistentFile.set(true);
		               });
		
		if (foundNonExistentFile.get())
			throw new IllegalArgumentException("Input files must exist.");
		
		args.inputFiles = args.inputFiles.stream()
		                                 .flatMap(file -> {
			                                 if (file.isDirectory()) {
				                                 try {
					                                 return FileUtils.streamFiles(file, true, "zip", "jar");
				                                 } catch (IOException e) {
					                                 throw new RuntimeException(e);
				                                 }
			                                 } else {
				                                 return Stream.of(file);
			                                 }
		                                 })
		                                 .collect(Collectors.toList());
		
		
		// Handle output file
		if (args.outputFile.isDirectory())
			args.outputFile = args.outputFile.toPath().resolve("downscaled.zip").toFile();
		
		try {
			Files.createDirectories(args.outputFile.toPath());
		} catch (IOException e) {
			System.err.println("Unable to create directories for " + args.outputFile.toPath());
			throw new RuntimeException(e);
		}
		
		if (!args.outputFile.getName().endsWith(".zip"))
			args.outputFile = new File(args.outputFile.toPath() + ".zip");
		
		return args;
	}
}

class Args {
	public final int maxWidth;
	public final int maxHeight;
	public List<File> inputFiles;
	public File outputFile;
	
	public Args(int maxWidth, int maxHeight, List<File> inputFiles, File outputFile) {
		this.maxWidth = maxWidth;
		this.maxHeight = maxHeight;
		this.inputFiles = inputFiles;
		this.outputFile = outputFile;
	}
}

class Util {
	public static <T> Stream<T> enumerationAsStream(Enumeration<T> e) {
		return StreamSupport.stream(
				Spliterators.spliteratorUnknownSize(
						new Iterator<T>() {
							public T next() {
								return e.nextElement();
							}
							
							public boolean hasNext() {
								return e.hasMoreElements();
							}
						},
						Spliterator.ORDERED
				), false);
	}
	
}

class Pair<T, U> {
	public final T left;
	public final U right;
	
	public Pair(T left, U right) {
		this.left = left;
		this.right = right;
	}
	
	public <V> Pair<T, V> r(V v) {
		return new Pair<>(left, v);
	}
}