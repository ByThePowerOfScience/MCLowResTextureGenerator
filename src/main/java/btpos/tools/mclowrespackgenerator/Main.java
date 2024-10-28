package btpos.tools.mclowrespackgenerator;

import net.coobird.thumbnailator.Thumbnailator;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.file.PathUtils;

import javax.imageio.ImageIO;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static btpos.tools.mclowrespackgenerator.Util.catcher;
import static btpos.tools.mclowrespackgenerator.Util.fileBad;

public class Main {
	
	private static final Path outDirectory = Files.createTempDirectory("mcdownscaler");
	private static Args args;
	
	private static final boolean IS_HEADLESS = java.awt.GraphicsEnvironment.isHeadless();
	
	public static final int MAX_ATLAS_SIZE = 16384 * 16384;
	
	private static final String packmcmeta = "{\n\t\"pack\":{\n\t\t\"pack_format\": %d,\n\t\t \"description\": \"Compressed textures\"\n\t}\n}";
	
	private static final UserInterfaceHandler uiHandler = UserInterfaceHandler.get(IS_HEADLESS);
	
	public static void main(String[] argsIn) {
		uiHandler.onStart();
		
		args = new ArgHandler(IS_HEADLESS).getArgs(argsIn);
		
		
		if (args.outputFile.exists()) {
			args.outputFile.createNewFile();
		}
		
		doCompression();
	}
	
	
	public static void doCompression() {
		try {
			List<Pair<String, Supplier<InputStream>>> name_to_stream = args.inputFiles.stream()
			                                                                          .map(file -> catcher(() -> new ZipFile(file), null, e -> Util.fileBad(file.getName(), e)))
			                                                                          .filter(Objects::nonNull)
			                                                                          .flatMap((ZipFile zip) -> {
				                                                                          System.out.println("\nIndexing " + zip.getName() + ":");
				                                                                          return Util.enumerationAsStream(zip.entries())
				                                                                                     .filter(entry -> Util.isTexture(entry.getName()))
				                                                                                     .map(entry -> new Pair<String, Supplier<InputStream>>(entry.getName(), () -> zip.getInputStream(entry)));
			                                                                          })
			                                                                          .collect(Collectors.toList());
			
			// max heap of total size in pixels
			PriorityQueue<TextureEntry> texturesHeap = new PriorityQueue<>(Comparator.comparingInt(TextureEntry::getTotalSize).reversed());
			
			AtomicInteger totalSize = new AtomicInteger(0);
			
			name_to_stream.stream()
			              .map(str_supp -> new TextureEntry(str_supp.left, str_supp.right))
			              .peek(entry -> totalSize.addAndGet(entry.getTotalSize()))
			              .forEach(texturesHeap::add);
			
			boolean hasOperated = false;
			
			System.out.println("Starting combined texture size (pixels): " + totalSize.get());
			long maxAtlasSize = (Long) args.ext.get(Args.atlas);
			
			while (!texturesHeap.isEmpty() && (!isAutoscale() || totalSize.get() >= maxAtlasSize)) {
				hasOperated = true;
				TextureEntry oldEntry = texturesHeap.poll();
				int oldSize = oldEntry.getTotalSize();
				
				TextureEntry newEntry;
				try {
					newEntry = compressTexture(oldEntry);
				} catch (Exception e) {
					fileBad(oldEntry.path, e);
					continue;
				}
				
				totalSize.addAndGet(newEntry.getTotalSize() - oldSize); // update total size
				
				if (isAutoscale())
					texturesHeap.add(newEntry); // put back on the heap for possible re-resizing if needed
			}
			
			
			String displayText;
			if (hasOperated) {
				File mcmeta = outDirectory.resolve("pack.mcmeta").toFile();
				FileUtils.write(mcmeta, String.format(packmcmeta, (Integer) args.ext.get(Args.fmt)), StandardCharsets.UTF_8);
				
				pack(outDirectory, args.outputFile.getAbsolutePath());
				displayText = "Created zip archive at " + args.outputFile.getAbsolutePath();
			} else {
				displayText = "Nothing to do!";
			}
			
			uiHandler.onFinish(displayText);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private static TextureEntry compressTexture(TextureEntry entry) {
		try (InputStream is = entry.getter.get()) {
			BufferedImage img = ImageIO.read(is);
			if (img == null) {
				throw new IOException("Image read as null: " + entry.path);
			}
			
			int square = isAutoscale() ? entry.getBestCap() : entry.getBestCap(args.maxScale);
			
			System.out.println("Compressing: " + entry.path);
			img = Thumbnailator.createThumbnail(img, square, square); // does keep the aspect ratio the same
			
			
			Path outPath = outDirectory.resolve(Paths.get(entry.path));
			FileUtils.createParentDirectories(outPath.toFile());
			
			try (OutputStream outFile = Files.newOutputStream(outPath)) {
				ImageIO.write(img, PathUtils.getExtension(outPath), outFile);
			}
			
			return new TextureEntry(
					entry.path,
					() -> catcher(() -> Files.newInputStream(outPath), null, e -> fileBad(outPath.toString(), e)),
					new Dimension(img.getWidth(), img.getHeight())
			);
		}
	}
	
	private static boolean isAutoscale() {
		return args.maxScale == null;
	}
	
	
	public static void pack(Path sourceDirPath, String zipFilePath) throws IOException {
		File f = new File(zipFilePath);
		if (!f.exists())
			f.createNewFile();
		
		Path p = f.toPath();
		try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(p));
		     Stream<Path> walk = Files.walk(sourceDirPath))
		{
			walk.filter(path -> !Files.isDirectory(path))
			    .forEach(path -> {
				    ZipEntry zipEntry = new ZipEntry(sourceDirPath.relativize(path).toString());
				    try {
					    zs.putNextEntry(zipEntry);
					    Files.copy(path, zs);
					    zs.closeEntry();
				    } catch (IOException e) {
					    e.printStackTrace(System.err);
				    }
			    });
		}
	}
}

