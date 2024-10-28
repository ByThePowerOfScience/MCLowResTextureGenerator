package btpos.tools.mclowrespackgenerator;


import java.awt.Dimension;
import java.io.InputStream;
import java.util.function.Supplier;

public class TextureEntry {
	public final String path;
	public final Supplier<InputStream> getter;
	public final Dimension dimension;
	
	public TextureEntry(String path, Supplier<InputStream> getter) {
		this(path, getter, null);
	}
	
	public TextureEntry(String path, Supplier<InputStream> getter, Dimension dimension) {
		this.path = path;
		this.getter = getter;
		this.dimension = dimension != null ? dimension : findDimension(path, getter);
	}
	
	private static Dimension findDimension(String path, Supplier<InputStream> getter) {
		return Util.getPngDimension(path, getter.get());
	}
	
	public int getTotalSize() {
		return dimension.height * dimension.width;
	}
	
	public int getBestCap() {
		int bigger = Math.max(dimension.height, dimension.width);
		int smaller = Math.min(dimension.height, dimension.width);
		
		// handle animated textures that are one block stretched over many
		if (bigger != smaller
            && bigger % smaller == 0)
		{
			int ratio = bigger / smaller;
			return Util.getClosestPowerOf2(smaller) * ratio;
		} else {
			return Util.getClosestPowerOf2(bigger);
		}
	}
}
