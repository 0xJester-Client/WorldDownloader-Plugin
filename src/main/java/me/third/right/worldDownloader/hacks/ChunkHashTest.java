package me.third.right.worldDownloader.hacks;

import me.third.right.modules.Hack;
import me.third.right.utils.client.enums.Category;
import me.third.right.utils.client.utils.ChatUtils;
import me.third.right.worldDownloader.utils.ChunkUtils;
import net.minecraft.world.chunk.Chunk;
import org.apache.commons.lang3.time.StopWatch;

import java.util.concurrent.TimeUnit;

@Hack.HackInfo(name = "ChunkHashTest", description = "Tests the chunk hash.", category = Category.DEBUG)
public class ChunkHashTest extends Hack {

    @Override
    public void onEnable() {
        if(mc.player == null || mc.world == null) return;

        final Chunk chunk = mc.world.getChunk(mc.player.getPosition());

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        int hash = ChunkUtils.createChunkHash(chunk);

        stopWatch.stop();

        ChatUtils.debug("Chunk hash: " + hash + " Took: "+stopWatch.getTime(TimeUnit.MILLISECONDS));
        disable();
    }
}

