package btpos.tools.mclowrespackgenerator;

import net.coobird.thumbnailator.Thumbnailator;
import org.apache.commons.cli.ParseException;
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
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

@SuppressWarnings("unchecked") public class Main {
	private static final Pattern ASSETS_PATTERN = Pattern.compile("^assets/[\\w_-]+/textures/(?:block|item)");
	public static final int DEFAULT_MAX_SIZE = 256;
	
	private static Args cli;
	
	
	public static void main(String[] args) throws ParseException {
		ArgHandler argHandler = new ArgHandler();
		cli = argHandler.getDefaultArgs(argHandler.checkCliArgs(args));
		
		
		cli.inputFiles.stream()
		              .map(Main::fileToZipFile)
		              .filter(Objects::nonNull)
		              .collect(Collectors.groupingBy(Main::getPackFormatFromZip))
		              .forEach(Main::fileLogic);
	}
	
	private static void fileLogic(Integer packFormat, List<ZipFile> packs) {
		if (packFormat == null || packFormat == -1)
			return;
		
		File f = new File(cli.outputFile.getName().replace("{}", packFormat.toString()));
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
	
	
	private static Args validateArgs(Args args) {
		
		// Handle output file
		try {
			Files.createDirectories(args.outputFile.toPath().getParent());
		} catch (IOException e) {
			System.err.println("Unable to create directories for " + args.outputFile.toPath());
			throw new RuntimeException(e);
		}
		
		if (!args.outputFile.getName().endsWith(".zip"))
			args.outputFile = new File(args.outputFile.toPath() + ".zip");
		
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
		public Integer maxWidth;
		public Integer maxHeight;
		public List<File> inputFiles;
		public File outputFile;
		
		public Args(int maxWidth, int maxHeight, List<File> inputFiles, File outputFile) {
			this.maxWidth = maxWidth;
			this.maxHeight = maxHeight;
			this.inputFiles = inputFiles;
			this.outputFile = outputFile;
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

