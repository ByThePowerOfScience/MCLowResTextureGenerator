This is an app that I spent _WAY_ too long on.  

# What it do

If you can't load a large modpack or texture pack because Minecraft says you need a smaller resource pack, or if you just like crunchy textures, this tool is for you.

(Common error message: `Unable to fit: bloodmagic:models/alchemyarrays/suppressionsigil - size: 512x512 - Maybe try a lower resolution resourcepack?`)

This tool essentially does these things:
* Goes through every mod/resource pack you give it,
* If you specified a size, it compresses all block and item textures it can find to that size.
* Otherwise, it tries to compress as few items as possible to let your GPU load all the textures. 
  * Most standard graphics cards only support a maximum 16384x16384 texture atlas, so it just tries to get the total pixel count below 16384 * 16384.
  * If your GPU won't work with the default autoscaling, you can set the value lower with `-t <value>`. Something like 16000*16000 should be more than plenty unless some _weirdly_-spaced textures are in there.


# Installation
This is a 90s-style Java app: run it with [Java 8](https://www.java.com/), and it should work fully standalone on any operating system. It also comes with command line arguments too for the super-nerds like me. (Another thing I spent far too long on.)

Basically just download the jar and double-click to run it, or use `java -jar <name>` in the terminal.  A cool little log window will pop up if it worked.

# How to use
(Note: for some reason all of the file pickers default to System32 if it isn't run from the command line. I literally put a line of code in there to try to prevent that, but it keeps happening. If you know how to fix that, let me know, but I'm too lazy to fix it. Just press the "home" button at the top to go to your user folder.)

1. In the first file picker that pops up, select all of the mods/resource packs you want the tool to scan.
2. Set the resource pack that should be generated in the next file picker. 
3. Select if you want to use the autoscaler or use a set maximum size. (only powers of 2 are supported for obvious reasons, but the command line allows any size)
   - (shoot, I forgot to account for long animated textures on the manual sizer...)
4. Wait for the resource pack to finish generating, then move it to your resource packs folder.
5. If you can load the game, select the resource pack in-game. Otherwise you'll need to manually add it in `options.txt` so MC doesn't try to load the old oversized ones.
