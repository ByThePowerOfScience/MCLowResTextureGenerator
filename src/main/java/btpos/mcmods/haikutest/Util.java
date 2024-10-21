package btpos.mcmods.haikutest;

import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;

import java.awt.Dimension;

public class Util {
    private static List<ImageReader> pngReaders = ImmutableList.copyOf(ImageIO.getImageReadersBySuffix("png"));
    
    /**
     * Source: <a href="https://stackoverflow.com/a/12164026">...</a>
     * Gets image dimensions for given file
     * @return dimensions of image
     * @throws IOException if the file is not a known image
     */
    public static Dimension getImageDimension(String path, InputStream is, Logger log) throws IOException {
        for (ImageReader reader : pngReaders) {
            try {
                ImageInputStream stream = ImageIO.createImageInputStream(is);
                reader.setInput(stream);
                int width = reader.getWidth(reader.getMinIndex());
                int height = reader.getHeight(reader.getMinIndex());
                
                return new Dimension(width, height);
            } catch (IOException e) {
                log.warn("Error reading: " + path, e);
            } finally {
                reader.reset();
            }
        }
        
        throw new IOException("Not a known image file: " + path);
    }
    
    // TODO: Call when all textures are done being read in
    public static void closeImageReaders() {
        pngReaders.forEach(ImageReader::dispose);
        pngReaders = null;
    }
}
