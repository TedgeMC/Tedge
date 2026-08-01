# 🔮 TedgeMC
TedgeMC is an innovative Minecraft modloader made from scratch.<br/>
A user-friendly installer is not available yet.<br/>
If you want to make a mod, use the [Tedge Mod Template.](https://github.com/TedgeMC/Tedge-Mod-Template)

## 🦈 Why Tedge
1. **Auto-changed Modifiers**                                          <br/>
   Tedge makes all types, fields and methods of Minecraft **public**.<br/>
   Most of them are made non-final too.
   <br/><br/>
2. **Access Editors**<br/>
   Access Editors allow you to customize way more aspects of elements than accesswideners and accesstransformers.<br/>
   1. You can toggle almost all of the keywords;
   2. You can make a class:<br/><br/>
   - implement an interface,
   - extend a class,
   - use class composition on a class,
   - have a value of some of its constants changed.
   <br/><br/>
3. **Groovy Mixin Support**                                                   <br/>
   Tedge mods can write mixins in other JVM languages than Java, if there's a TedgeMixin extension for it!<br/>
   This cannot be done on Fabric and Forge; NeoForge status is unknown.<br/>
   The TedgeMC team will be working on supporting more languages in mixins, such as Kotlin, soon.
   <br/><br/>
4. **YAML instead of JSON**<br/>
   Many configurations in Tedge use YAML or JSON5 instead of pure JSON.<br/>
   This allows you to write comments in all your configs, and they're easier to read :D
   <br/><br/>
5. **Speed & Disk Space**<br/>
   Tedge is written with practical usage in mind.<br/>
   This may seem obvious, but it *isn't*:
   ###### (these numbers are regarding the development environment for each modloader)
   - Fabric needs <b>~181MB</b>:
     1. 35MB disk space per global version
     2. 74MB disk space per project version
     3. 72.5MB disk space per project sync after accesswidener change *(the jars it makes are never deleted... lol)*
     4. ~1 minute to setup
        <br/><br/>
   - Forge needs <b>~591MB</b>:
     1. 201MB disk space for its MCP bs
     2. 52.6MB disk space for its "Minecraft repository"
     3. 338MB disk space for its "Minecraft user repository"
     4. ~10 minutes to setup
        <br/><br/>
   - NeoForge needs <b>~812MB</b>:
     1. 32MB disk space per a mapped version
     2. uses ~780MB disk space in my gradle cache *(idk what for its very confusing just random uuids)*
     3. ~10-20 minutes to setup
        <br/><br/>
   - UniMined... I don't know what that is
     <br/><br/>

   Tedge consumes <u>~109MB</u>:
   - 92.7KB for the loader
   - 34MB per minecraft version
   - 74.7MB for libraries folder
   - ~~\~20KB per accesseditor class~~ no gensources yet
   - ~24 seconds to setup
