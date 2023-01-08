package me.third.right.worldDownloader.utils;

import me.third.right.worldDownloader.mixins.IAnvilChunkLoader;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.*;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraft.world.storage.SaveHandler;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @Description: Save chunks to Anvil format.
 * @CopyOf: net.minecraft.world.chunk.storage.AnvilChunkLoader
 * @TODO:
 */
public class AnvilChunkWDL extends AnvilChunkLoader {
    //Vars

    //Overrides

    public AnvilChunkWDL(File file) {
        super(file, null);
    }

    public static AnvilChunkWDL create(SaveHandler saveHandler, WorldProvider world) {
        return new AnvilChunkWDL(getWorldSaveFolder(saveHandler, world));
    }

    private static File getWorldSaveFolder(SaveHandler handler, WorldProvider provider) {
        File baseFolder = handler.getWorldDirectory();

        if (provider instanceof WorldProviderHell) {
            File file = new File(baseFolder, "DIM-1");
            file.mkdirs();
            return file;
        } else if (provider instanceof WorldProviderEnd) {
            File file = new File(baseFolder, "DIM1");
            file.mkdirs();
            return file;
        }

        return baseFolder;
    }

    @Override
    public void saveChunk(World world, Chunk chunk) throws MinecraftException {
        world.checkSessionLock();

        NBTTagCompound levelTag = writeChunkToNBT(chunk, world);

        NBTTagCompound rootTag = new NBTTagCompound();
        rootTag.setTag("Level", levelTag);
        rootTag.setInteger("DataVersion", 1343);

        addChunkToPending(chunk.getPos(), rootTag);

    }

    private NBTTagCompound writeChunkToNBT(Chunk chunk, World world) {
        NBTTagCompound compound = new NBTTagCompound();

        compound.setByte("V", (byte) 1);
        compound.setInteger("xPos", chunk.x);
        compound.setInteger("zPos", chunk.z);
        compound.setLong("LastUpdate", world.getTotalWorldTime());
        compound.setIntArray("HeightMap", chunk.getHeightMap());
        compound.setBoolean("TerrainPopulated", true);  // We always want this
        compound.setBoolean("LightPopulated", chunk.isLightPopulated());
        compound.setLong("InhabitedTime", chunk.getInhabitedTime());
        ExtendedBlockStorage[] blockStorageArray = chunk.getBlockStorageArray();
        NBTTagList blockStorageList = new NBTTagList();
        boolean hasSky = world.provider.hasSkyLight();

        for (ExtendedBlockStorage blockStorage : blockStorageArray) {
            if (blockStorage != null) {
                NBTTagCompound blockData = new NBTTagCompound();
                blockData.setByte("Y",
                        (byte) (blockStorage.getYLocation() >> 4 & 255));
                byte[] buffer = new byte[4096];
                NibbleArray nibblearray = new NibbleArray();
                NibbleArray nibblearray1 = blockStorage.getData()
                        .getDataForNBT(buffer, nibblearray);
                blockData.setByteArray("Blocks", buffer);
                blockData.setByteArray("Data", nibblearray.getData());

                if (nibblearray1 != null) {
                    blockData.setByteArray("Add", nibblearray1.getData());
                }

                NibbleArray blocklightArray = blockStorage.getBlockLight();
                int lightArrayLen = blocklightArray.getData().length;
                blockData.setByteArray("BlockLight", blocklightArray.getData());

                if (hasSky) {
                    NibbleArray skylightArray = blockStorage.getSkyLight();
                    if (skylightArray != null) {
                        blockData.setByteArray("SkyLight", skylightArray.getData());
                    } else {
                        // Shouldn't happen, but if it does, handle it smoothly.
                        //LOGGER.error("[WDL] Skylight array for chunk at " + chunk.x + ", " + chunk.z + " is null despite VersionedProperties " + "saying it shouldn't be!");
                        blockData.setByteArray("SkyLight", new byte[lightArrayLen]);
                    }
                } else {
                    blockData.setByteArray("SkyLight", new byte[lightArrayLen]);
                }

                blockStorageList.appendTag(blockData);
            }
        }

        compound.setTag("Sections", blockStorageList);
        compound.setByteArray("Biomes", chunk.getBiomeArray());
        chunk.setHasEntities(false);

        NBTTagList entityList = getEntityList(chunk);
        compound.setTag("Entities", entityList);

        compound.setTag("TileEntities", entityList);

        List<NextTickListEntry> list = world.getPendingBlockUpdates(chunk, false);
        if (list != null) {
            long j = world.getTotalWorldTime();
            NBTTagList nbttaglist3 = new NBTTagList();
            Iterator var28 = list.iterator();

            while(var28.hasNext()) {
                NextTickListEntry nextticklistentry = (NextTickListEntry)var28.next();
                NBTTagCompound nbttagcompound1 = new NBTTagCompound();
                ResourceLocation resourcelocation = Block.REGISTRY.getNameForObject(nextticklistentry.getBlock());
                nbttagcompound1.setString("i", resourcelocation == null ? "" : resourcelocation.toString());
                nbttagcompound1.setInteger("x", nextticklistentry.position.getX());
                nbttagcompound1.setInteger("y", nextticklistentry.position.getY());
                nbttagcompound1.setInteger("z", nextticklistentry.position.getZ());
                nbttagcompound1.setInteger("t", (int)(nextticklistentry.scheduledTime - j));
                nbttagcompound1.setInteger("p", nextticklistentry.priority);
                nbttaglist3.appendTag(nbttagcompound1);
            }

            compound.setTag("TileTicks", nbttaglist3);
        }

        return compound;
    }

    public NBTTagList getEntityList(Chunk chunk) {
        NBTTagList entityList = new NBTTagList();

        // Build a list of all entities in the chunk.
        List<Entity> entities = new ArrayList<>();
        // Add the entities already in the chunk.
        for (ClassInheritanceMultiMap<Entity> map : chunk.getEntityLists()) {
            entities.addAll(map);
        }

        for (Entity entity : entities) {
            if (entity == null) {
                continue;
            }

            NBTTagCompound entityData = new NBTTagCompound();
            try {
                if (entity.writeToNBTOptional(entityData)) {
                    chunk.setHasEntities(true);
                    entityList.appendTag(entityData);
                }
            } catch (Exception ignored) {
            }
        }

        return entityList;
    }

    public Map<ChunkPos, NBTTagCompound> getChunksToSave() {
        return ((IAnvilChunkLoader)this).getChunksToSave();
    }

    public File getSaveLocation() {
        return ((IAnvilChunkLoader)this).getSaveLocation();
    }


}
