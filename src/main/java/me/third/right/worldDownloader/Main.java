package me.third.right.worldDownloader;

import me.third.right.commands.Command;
import me.third.right.hud.Hud;
import me.third.right.modules.Hack;
import me.third.right.plugins.PluginBase;
import me.third.right.worldDownloader.hacks.ChunkHashTest;
import me.third.right.worldDownloader.hacks.ImagerTest;
import me.third.right.worldDownloader.hacks.MiniMap;
import me.third.right.worldDownloader.hacks.WorldDownloader;
import me.third.right.worldDownloader.huds.MiniMapElement;
import me.third.right.worldDownloader.huds.PerformanceElement;

@PluginBase.PluginInfo(name = "WorldDownloader", author = "ThirdRight", version = "1.2")
public class Main extends PluginBase {

    public static Main INSTANCE;

    public Main() {
        INSTANCE = this;
    }

    @Override
    public Hack[] registerHacks() {
        return new Hack[] {
                new WorldDownloader(),
                new ImagerTest(),
                new MiniMap(),
                new ChunkHashTest()
        };
    }

    @Override
    public Hud[] registerHuds() {
        return new Hud[] {
                new PerformanceElement(),
                new MiniMapElement()
        };
    }

    @Override
    public Command[] registerCommands() {
        return new Command[0];
    }
}
