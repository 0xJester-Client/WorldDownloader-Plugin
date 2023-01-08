package me.third.right.worldDownloader.mixins;


import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.io.File;
import java.util.Map;

@Mixin(net.minecraft.world.chunk.storage.AnvilChunkLoader.class)
public interface IAnvilChunkLoader {

    @Accessor("chunksToSave")
    Map<ChunkPos, NBTTagCompound> getChunksToSave();

    @Accessor("chunkSaveLocation")
    File getSaveLocation();

}
