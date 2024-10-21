package btpos.mcmods.haikutest.mixin;

import btpos.mcmods.haikutest.HaikuTest;
import btpos.mcmods.haikutest.Util;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.coobird.thumbnailator.Thumbnailator;
import net.minecraft.server.packs.resources.IoSupplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.zip.ZipEntry;

@Mixin(IoSupplier.class)
public class MIOSupplier {
    @ModifyReturnValue(
            method="lambda$create$1",
            at=@At("RETURN")
    )
    private static InputStream resizeIncomingImages(InputStream original, @Local ZipEntry entry) throws IOException {
//        if (entry.getSize() <= (512 * 512))
//            return original;
        // else downscale to 512 by 512
        
        Dimension size = Util.getImageDimension(entry.getName(), original, HaikuTest.LOGGER);
        
        if (size.width <= 512 && size.height <= 512)
            return original;
        
        
        int newWidth;
        int newHeight;
        
        if (size.width == size.height) {
            newWidth = 512;
            newHeight = 512;
        } else if (size.height > size.width) {
            newHeight = 512;
            newWidth = haikutest$scale(size.width, size.height);
        } else {
            newWidth = 512;
            newHeight = haikutest$scale(size.height, size.width);
        }
        
        PipedInputStream pipedInputStream = new PipedInputStream();
        Thumbnailator.createThumbnail(original, new PipedOutputStream(pipedInputStream), newWidth, newHeight); // TODO this also buffers the entire thing into memory eeeeeehhhhhhhh
        original.close();
        return pipedInputStream;
    }
    
    @Unique
    private static int haikutest$scale(int toScale, int other) {
        return (int) ((toScale * 512d) / other);
    }
}
