package me.third.right.worldDownloader.events;

import me.bush.eventbus.event.Event;
import net.minecraft.util.math.ChunkPos;

public class CImageCompleteEvent extends Event {

    private final ChunkPos chunkPos;

    public CImageCompleteEvent(ChunkPos chunkPos) {
        this.chunkPos = chunkPos;
    }

    public ChunkPos getChunkPos() {
        return chunkPos;
    }

    @Override
    protected boolean isCancellable() {
        return false;
    }
}
