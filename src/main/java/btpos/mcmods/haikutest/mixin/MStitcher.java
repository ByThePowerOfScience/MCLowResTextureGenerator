package btpos.mcmods.haikutest.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.texture.Stitcher;
import net.minecraft.client.renderer.texture.Stitcher.Entry;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

@Mixin(Stitcher.class)
public class MStitcher {
    @Shadow @Final private static Logger LOGGER;
    @Unique
    private static final List<String> haikutest$command = List.of("powershell", "-c", "\"$p = Get-Process java;((Get-Counter \\\"\\GPU Process Memory(pid_$($p.id)*)\\Local Usage\\\").CounterSamples | where CookedValue).CookedValue | foreach {Write-Output \\\"Process $($P.Name) GPU Process Memory $([math]::Round($_/1MB,2)) MB\\\"}; ((Get-Counter \\\"\\GPU Engine(pid_$($p.id)*engtype_3D)\\Utilization Percentage\\\").CounterSamples | where CookedValue).CookedValue | foreach {Write-Output \\\"Process $($p.Name) GPU Engine Usage $([math]::Round($_,2))%\\\"}\"");
    
    @Inject(
            method="stitch",
            at=@At(
                    target="java/lang/StringBuilder.<init> ()V",
                    value="INVOKE"
            )
    )
    private <T extends Entry> void inject(CallbackInfo ci, @Local Stitcher.Holder<T> holder) {
        ProcessBuilder proc = new ProcessBuilder().command(haikutest$command);
        try {
            Process p = proc.start();
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                int read;
                StringBuilder sb = new StringBuilder();
                while (p.isAlive()) {
                    if ((read = bufferedReader.read()) == -1)
                        continue;
                    sb.append((char)read);
                }
                LOGGER.error("[HAIKU GOOD] " + sb);
            }
        } catch (IOException e) {
            LOGGER.warn("[HAIKU ERROR]", e);
        }
    }
}
