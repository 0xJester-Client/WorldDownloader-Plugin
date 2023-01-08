package me.third.right.worldDownloader.utils;

import net.minecraft.block.Block;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import java.util.Objects;
import java.util.Random;

import static net.minecraft.world.chunk.Chunk.NULL_BLOCK_STORAGE;

public class ChunkUtils {//TODO move this to main client in version 4.5


    public static boolean isChunkEmpty(Chunk chunk) {
        if (chunk.isEmpty() || chunk instanceof EmptyChunk) {
            return true;
        }

        final ExtendedBlockStorage[] array = chunk.getBlockStorageArray();
        for (int i = 1; i < array.length; i++) {
            if (array[i] != NULL_BLOCK_STORAGE) {
                return false;
            }
        }
        if (array[0] != NULL_BLOCK_STORAGE) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        int id = Block.getStateId(array[0].get(x, y, z));
                        id = (id & 0xFFF) << 4 | (id & 0xF000) >> 12;
                        if ((id > 0x00F) && (id < 0x1A0 || id > 0x1AF)) {
                            return false;
                        }
                    }
                }
            }
        }
        return false;
    }


    public static int createChunkHash(Chunk chunk) {
        int hash = Objects.hashCode(chunk.x) ^ Objects.hashCode(chunk.z);

        for(int i = 0; i < chunk.getBlockStorageArray().length; i++) {
            final ExtendedBlockStorage extendedBlockStorage = chunk.getBlockStorageArray()[i];
            if(extendedBlockStorage != NULL_BLOCK_STORAGE) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        for (int x = 0; x < 16; x++) {
                            hash = hash ^ extendedBlockStorage.get(x, y, z).getBlock().toString().hashCode();
                        }
                    }
                }
            }
        }

        return hash;
    }


    //https://minecraft.fandom.com/wiki/Slime
    public static boolean isSlimeChunk(long seed, int chunkX, int chunkZ) {
        final Random r = new Random(seed + ((long) chunkX * chunkX * 4987142) + (chunkX * 5947611L) + ((long) chunkZ * chunkZ) * 4392871L + (chunkZ * 389711L) ^ 987234911L);
        return r.nextInt(10) == 0;
    }
}
