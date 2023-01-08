package me.third.right.worldDownloader.hacks;

import me.third.right.ThirdMod;
import me.third.right.modules.Hack;
import me.third.right.utils.client.enums.Category;
import me.third.right.utils.client.manage.ThreadManager;
import me.third.right.utils.client.objects.Pair;
import me.third.right.worldDownloader.utils.CImagerRunnable;
import net.minecraft.world.chunk.Chunk;

@Hack.HackInfo(name = "ImagerTest", description = "Test ChunkImager.", category = Category.DEBUG)
public class ImagerTest extends Hack {

    @Override
    public void onEnable() {
        if(nullCheck()) return;

        final Chunk chunk = mc.world.getChunk(mc.player.getPosition());
        ThreadManager.INSTANCE.submit(new CImagerRunnable(new Pair<>(ThirdMod.configFolder.resolve("ImagerTest.png"), chunk)));
        disable();
    }
}
