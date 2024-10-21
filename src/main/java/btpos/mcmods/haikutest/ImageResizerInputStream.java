package btpos.mcmods.haikutest;

import net.coobird.thumbnailator.Thumbnailator;
import net.minecraftforge.common.util.Lazy;

import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

public class ImageResizerInputStream extends InputStream {
	private static final int THRESHOLD = 512;
	private static final double SCALE_DOWN = 0.5;
	
	private final Lazy<InputStream> thumbnailStream;
	
	public ImageResizerInputStream(InputStream internal, String path) {
		 thumbnailStream = Lazy.of(() -> {
			 try {
				 return this.getThumbnailStream(internal);
			 } catch (IOException e) {
				 throw new RuntimeException("Error loading file: " + path, e);
			 }
		 });
	}
	
	@Override
	public int read() throws IOException {
		return thumbnailStream.get().read();
	}
	
	private static int getScaledSize(int toScale, int other) {
		return (int) (((double) toScale * THRESHOLD) / other);
	}
	
	private InputStream getThumbnailStream(InputStream is) throws IOException {
		Dimension size = Util.getImageDimension(is);
		
		if (size.width <= THRESHOLD && size.height <= THRESHOLD)
			return is;
		
		int newWidth;
		int newHeight;
		
		if (size.width == size.height) {
			newWidth = THRESHOLD;
			newHeight = THRESHOLD;
		} else if (size.height > size.width) {
			newHeight = THRESHOLD;
			newWidth = getScaledSize(size.width, size.height);
		} else {
			newWidth = THRESHOLD;
			newHeight = getScaledSize(size.height, size.width);
		}
		
		PipedInputStream pipedInputStream = new PipedInputStream();
		Thumbnailator.createThumbnail(is, new PipedOutputStream(pipedInputStream), newWidth, newHeight);
		return pipedInputStream;
	}
}
