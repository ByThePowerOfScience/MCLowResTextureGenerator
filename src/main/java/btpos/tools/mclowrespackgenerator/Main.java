package btpos.tools.mclowrespackgenerator;

import net.coobird.thumbnailator.Thumbnailator;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.file.PathUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

@SuppressWarnings("unchecked") public class Main {
	private static final Pattern ASSETS_PATTERN = Pattern.compile("^assets/[\\w_-]+/textures/(?:block|item)");
	
	private static Scanner input;
	
	private static Args cli;
	
	
	public static void main(String[] args) throws ParseException, IOException {
		input = new Scanner(System.in);
		
		Args arguments = handleArgs(args);
		cli = arguments;
		
		Path outputPath = cli.outputDir.toPath();
//		.toPath().resolve("downscaled").toFile()
		cli.outputDir = cli.outputDir.toPath().resolve("downscaled").toFile();
		cli.outputDir.mkdir();
		cli.outputDir = cli.outputDir.toPath().resolve("downscaled_pack{}.zip").toFile();
		
		arguments.inputFiles.stream()
		                    .map(Main::fileToZipFile)
		                    .filter(Objects::nonNull)
		                    .collect(Collectors.groupingBy(Main::getPackFormatFromZip))
		                    .forEach(Main::fileLogic);
	}
	
	private static void fileLogic(Integer packFormat, List<ZipFile> packs) {
		if (packFormat == null || packFormat == -1)
			return;
		
		File f = new File(cli.outputDir.getName().replace("{}", packFormat.toString()));
		if (f.exists()) {
			System.out.println("File already exists: " + f.getPath());
			return;
		}
		try {
			f.createNewFile();
		} catch (IOException e) {
			System.out.println("Unable to create file: " + f.getPath());
			return;
		}
		
		AtomicBoolean wrote = new AtomicBoolean(false);
		
		try (ZipOutputStream outFile = new ZipOutputStream(Files.newOutputStream(f.toPath()))) {
			// add mcmeta for pack format
			JSONObject packmcmeta = makeMcMeta(packFormat);
			
			outFile.putNextEntry(new ZipEntry("pack.mcmeta"));
			outFile.write(packmcmeta.toJSONString().getBytes(StandardCharsets.UTF_8));
			outFile.closeEntry();
			
			packs.stream()
			     .flatMap(Main::mapZipToEntries)
			     .map(Main::mapStreamToBufferedImage)
			     .filter(Objects::nonNull)
			     .filter(entry_bufimg -> entry_bufimg.right.getWidth() > cli.maxWidth || entry_bufimg.right.getHeight() > cli.maxHeight)
			     .map(entry_bufimg -> {
				     System.out.println("Compressing: " + entry_bufimg.left.getName());
				     return entry_bufimg.r(Thumbnailator.createThumbnail(entry_bufimg.right, cli.maxWidth, cli.maxHeight));
			     })
			     .forEach(entry_bufimg -> {
				     try {
					     outFile.putNextEntry(new ZipEntry(entry_bufimg.left.getName()));
					     ImageIO.write(entry_bufimg.right, PathUtils.getExtension(Paths.get(entry_bufimg.left.getName())), outFile);
					     outFile.closeEntry();
				     } catch (IOException e) {
					     e.printStackTrace(System.err);
				     }
				     wrote.set(true);
			     });
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		if (!wrote.get()) {
			System.out.println("Didn't write anything to " + f.getPath() + "; deleting.");
			f.delete();
		}
	}
	
	private static JSONObject makeMcMeta(Integer key) {
		JSONObject inner = new JSONObject();
		inner.put("pack_format", key);
		inner.put("description", "Generated compressed resource pack for mods with pack format " + key);
		JSONObject packmcmeta = new JSONObject();
		packmcmeta.put("pack", inner);
		return packmcmeta;
	}
	
	private static Pair<? extends ZipEntry, BufferedImage> mapStreamToBufferedImage(Pair<? extends ZipEntry, InputStream> entry_stream) {
		try {
			BufferedImage img = ImageIO.read(entry_stream.right);
			
			if (img == null) {
				throw new IOException("Image null somehow.");
			}
			
			return entry_stream.r(img);
		} catch (IOException e) {
			fileBad(entry_stream, e);
			return null;
		} finally {
			try {
				entry_stream.right.close();
			} catch (IOException e) {
				fileBad(entry_stream.left, e);
			}
		}
	}
	
	private static int getPackFormatFromZip(ZipFile zip) {
		try {
			ZipEntry entry = zip.getEntry("pack.mcmeta");
			try (InputStream is = zip.getInputStream(entry);) {
				Object parse = new JSONParser().parse(new InputStreamReader(is));
				if (!(parse instanceof JSONObject))
					throw new IOException("Unable to parse pack.mcmeta");
				
				JSONObject obj = ((JSONObject) parse);
				return Math.toIntExact((Long) ((JSONObject) obj.get("pack")).get("pack_format"));
			}
		} catch (Exception e) {
			fileBad(zip.getName(), e);
			return -1;
		}
	}
	
	private static void fileBad(ZipEntry entry, Exception e) {
		fileBad(entry.getName(), e);
	}
	
	private static void fileBad(Pair<? extends ZipEntry, ?> entry, Exception e) {
		fileBad(entry.left.getName(), e);
	}
	
	private static void fileBad(String name, Exception e) {
		System.out.println("Error reading file: " + name);
		e.printStackTrace(System.err);
	}
	
	static Args handleArgs(String[] args) throws ParseException {
		Option inputFilesOption = Option.builder("i")
		                                .longOpt("input")
		                                .required()
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
		
		List<File> inputFiles = Arrays.stream(parsed.getOptionValues(inputFilesOption))
		                              .map(File::new)
		                              .collect(Collectors.toList());
		
		Integer width = parsed.getParsedOptionValue(widthOption, () -> getArg("maximum width", Integer::valueOf));
		
		Integer height = parsed.getParsedOptionValue(heightOption, () -> getArg("maximum height", Integer::valueOf));
		
		if (inputFiles.isEmpty())
			throw new MissingArgumentException(inputFilesOption);
		
		File outputFile = new File(parsed.getParsedOptionValue(
				outputFileOption,
				() -> getArg(
						"folder to put the output pack(s) in (e.g. \"C:\\Users\\me\\Desktop\\\" [without quotes]) (\".\" or blank = current working directory)",
						(String s) -> s.isEmpty() || ".".equals(s) ? System.getProperty("user.dir") : s
				)
		));
		
		return validateArgs(new Args(width, height, inputFiles, outputFile));
	}
	
	private static <T> T getArg(String text, Function<String, T> converter) {
		System.out.print("Enter " + text + ": ");
		return converter.apply(input.nextLine());
	}
	
	private static Args validateArgs(Args args) {
		// verify all input files exist
		AtomicBoolean foundNonExistentFile = new AtomicBoolean(false);
		args.inputFiles.stream()
		               .filter(file -> !file.exists() && !file.isDirectory())
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
		try {
			Files.createDirectories(args.outputDir.toPath().getParent());
		} catch (IOException e) {
			System.err.println("Unable to create directories for " + args.outputDir.toPath());
			throw new RuntimeException(e);
		}
		
		if (!args.outputDir.getName().endsWith(".zip"))
			args.outputDir = new File(args.outputDir.toPath() + ".zip");
		
		return args;
	}
	
	private static ZipFile fileToZipFile(File file) {
		try {
			return new ZipFile(file);
		} catch (IOException e) {
			fileBad(file.getName(), e);
			return null;
		}
	}
	
	private static Stream<? extends Pair<? extends ZipEntry, InputStream>> mapZipToEntries(ZipFile zip) {
		System.out.println("\nOpening " + zip.getName() + ":");
		return Util.enumerationAsStream(zip.entries())
		           .filter(entry ->
				                   ASSETS_PATTERN.matcher(entry.getName()).find()
						                   && entry.getName().endsWith(".png")
		           )
		           .map(entry -> {
			           try {
				           InputStream inputStream = Optional.ofNullable(zip.getInputStream(entry))
				                                             .orElseThrow(IOException::new);
				           return new Pair<>(entry, inputStream);
			           } catch (IOException e) {
				           fileBad(zip.getName() + ":" + entry.getName(), e);
				           return null;
			           }
		           })
		           .filter(Objects::nonNull);
	}
	
	public static class Args {
		public final int maxWidth;
		public final int maxHeight;
		public List<File> inputFiles;
		public File outputDir;
		public String fileName = "downscaled.zip";
		
		public Args(int maxWidth, int maxHeight, List<File> inputFiles, File outputDir) {
			this.maxWidth = maxWidth;
			this.maxHeight = maxHeight;
			this.inputFiles = inputFiles;
			this.outputDir = outputDir;
		}
	}
	
	public static class Pair<T, U> {
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
	
	public static class Util {
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
}

