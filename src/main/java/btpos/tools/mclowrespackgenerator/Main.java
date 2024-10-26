package btpos.tools.mclowrespackgenerator;

import net.coobird.thumbnailator.Thumbnailator;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.file.PathUtils;

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import java.awt.Dialog;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static btpos.tools.mclowrespackgenerator.Util.catcher;
import static btpos.tools.mclowrespackgenerator.Util.fileBad;

public class Main {
	
	private Args args;
	
	private static final int MAX_ATLAS_SIZE = 16384 * 16384;
	
	
	public static void main(String[] argsIn) {
		
		new Main().fileLogic(argsIn);
	}
	
	private static int getClosestPowerOf2(int width, int height) {
		if (width > height)
			return getClosestPowerOf2(width);
		else
			return getClosestPowerOf2(height);
	}
	
	private static int getClosestPowerOf2(final int i) {
		// there's a better way to do this but idc enough
		int out = 2;
		while (out < i) {
			out *= 2;
		}
		return out / 2;
	}
	
	
	public void fileLogic(String[] argsIn) {
		ArgHandler argHandler = new ArgHandler();
		args = argHandler.getDefaultArgs(argHandler.checkCliArgs(argsIn));
		
		args.outputFile.createNewFile();
		
		File outDirectory = Files.createTempDirectory("mcdownscaler").toFile();
		
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
			
			PriorityQueue<Pair<Integer, Pair<String, Supplier<InputStream>>>> texturesHeap = new PriorityQueue<>(Comparator.comparingInt(Pair<Integer, Pair<String, Supplier<InputStream>>>::getLeft).reversed());
			
			AtomicInteger totalSize = new AtomicInteger(0);
			
			name_to_stream.stream()
			              .map(name_stream -> new Pair<>(Util.getPngDimension(name_stream.left, name_stream.right.get()), name_stream))
			              .map(dim_pair -> dim_pair.lmap(dim_pair.left.width * dim_pair.left.height))
			              .peek(size_pair -> totalSize.addAndGet(size_pair.left))
			              .forEach(texturesHeap::add);
			
			boolean hasOperated = false;
			
			while (!texturesHeap.isEmpty() && totalSize.get() >= MAX_ATLAS_SIZE) {
				hasOperated = true;
				Pair<Integer, Pair<String, Supplier<InputStream>>> tuple = texturesHeap.poll();
				Integer oldSize = tuple.left;
				String name = tuple.right.left;
				Supplier<InputStream> is_get = tuple.right.right;
				
				try {
					BufferedImage img = ImageIO.read(is_get.get());
					if (img == null) {
						System.err.println("Image null somehow idk");
						continue;
					}
					
					int square = getClosestPowerOf2(img.getWidth(), img.getHeight());
					
					System.out.println("Compressing: " + name);
					img = Thumbnailator.createThumbnail(img, square, square);
					
					int newSize = img.getHeight() * img.getWidth();
					totalSize.addAndGet(newSize - oldSize); // update total size
					
					
					Path outPath = outDirectory.toPath().resolve(Paths.get(name));
					FileUtils.createParentDirectories(outPath.toFile());
					
					try (OutputStream outFile = Files.newOutputStream(outPath)) {
						ImageIO.write(img, PathUtils.getExtension(outPath), outFile);
					}
					
					texturesHeap.add(new Pair<>(
							newSize,
							new Pair<>(
									name,
									() -> catcher(() -> Files.newInputStream(outPath), null, e -> fileBad(outPath.toString(), e)))));
					
				} catch (IOException e) {
					fileBad(name, e);
				}
			}
			String displayText;
			if (hasOperated) {
				pack(outDirectory.getAbsolutePath(), args.outputFile.getAbsolutePath());
				displayText = "Created zip archive at " + args.outputFile.getAbsolutePath();
			}
			else {
				displayText = "Nothing to do!";
			}
			
			EventQueue.invokeAndWait(() -> JOptionPane.showMessageDialog(null, displayText));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
	}
	
	public static void pack(String sourceDirPath, String zipFilePath) throws IOException {
		File f = new File(zipFilePath);
		if (!f.exists())
			f.createNewFile();
		Path p = f.toPath();
		try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(p))) {
			Path pp = Paths.get(sourceDirPath);
			Files.walk(pp)
			     .filter(path -> !Files.isDirectory(path))
			     .forEach(path -> {
				     ZipEntry zipEntry = new ZipEntry(pp.relativize(path).toString());
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

