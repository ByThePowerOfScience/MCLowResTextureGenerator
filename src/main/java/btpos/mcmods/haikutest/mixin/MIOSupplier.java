package btpos.mcmods.haikutest.mixin;

import btpos.mcmods.haikutest.HaikuTest;
import btpos.mcmods.haikutest.ImageResizerInputStream;
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
    private static InputStream resizeIncomingImages(InputStream original, @Local(argsOnly = true) ZipEntry entry) {
        return new ImageResizerInputStream(original, entry.getName());
    }
}
