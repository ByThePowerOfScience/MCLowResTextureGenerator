package btpos.tools.mclowrespackgenerator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;

public class Util {
	private static final Pattern ASSETS_PATTERN = Pattern.compile("^assets/[\\w_-]+/textures/(?:block|item|gui)");
	
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
	
	/**
	 * NOTE: Only possible because of Manifold turning checked exceptions into unchecked ones.
	 * Otherwise I'd have to wrap the exceptions in the suppliers too.
	 */
	public static <T> T catcher(Supplier<T> canThrow, T ifThrows, Consumer<Exception> onCatch) {
		try {
			return canThrow.get();
		} catch (Exception e) {
			onCatch.accept(e);
			return ifThrows;
		}
	}
	
	
	/**
	 * Gets image dimensions for given file
	 * @param is image file
	 * @return dimensions of image
	 * @throws IOException if the file is not a known image
	 */
	public static Dimension getPngDimension(String name, InputStream is) throws IOException {
		Iterator<ImageReader> iter = ImageIO.getImageReadersBySuffix("png"); // TODO can I optimize this to not have to search every single time
		
		while(iter.hasNext()) {
			ImageReader reader = iter.next();
			
			try {
				ImageInputStream stream = ImageIO.createImageInputStream(is);
				reader.setInput(stream);
				int width = reader.getWidth(reader.getMinIndex());
				int height = reader.getHeight(reader.getMinIndex());
				stream.close();
				return new Dimension(width, height);
			} catch (IOException e) {
				System.err.println("Error reading: " + name);
				e.printStackTrace(System.err);
			} finally {
				reader.dispose();
			}
		}
		
		throw new IOException("Not a known image file: " + name);
	}
	
	static boolean isTexture(String name) {
		return ASSETS_PATTERN.matcher(name).find() && name.endsWith(".png");
	}
	
	static void fileBad(ZipEntry entry, Exception e) {
		fileBad(entry.getName(), e);
	}
	
	static void fileBad(Pair<? extends ZipEntry, ?> entry, Exception e) {
		fileBad(entry.left.getName(), e);
	}
	
	static void fileBad(String name, Exception e) {
		System.out.println("Error reading file: " + name);
		e.printStackTrace(System.err);
	}
	
	static int getClosestPowerOf2(final int i) {
		// there's a better way to do this but idc enough
		int out = 2;
		while (out < i) {
			out *= 2;
		}
		return out / 2;
	}
}
