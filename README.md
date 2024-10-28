This is an app that I spent _WAY_ too long on.  

# What it do

If you can't load a large modpack or texture pack because Minecraft says you need a smaller resource pack, or if you just like crunchy textures, this tool is for you.

This tool essentially does these things:
* Goes through every mod/resource pack you give it,
* If you specifid a size, it compresses all block and item textures it can find to that size.
* Otherwise, it tries to compress as few items as possible to let your GPU load all the textures. 
  * Most standard graphics cards only support a maximum 16384x16384 texture atlas, so it just tries to get the total pixel count below 16384 * 16384.
  * I'll probably add something to accurately  eventually, but that's not in the tool currently.


# Installation
This is a 90s-style Java app: run it with [Java 8](https://www.java.com/), and it should work fully standalone on any operating system. It also comes with command line arguments too for the super-nerds like me. (Another thing I spent far too long on.)

Basically just download the jar and double-click to run it, or use `java -jar <name>` in the terminal.  A cool little printout window will pop up if it worked.

# How to use