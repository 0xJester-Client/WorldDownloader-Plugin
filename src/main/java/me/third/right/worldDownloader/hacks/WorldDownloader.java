package me.third.right.worldDownloader.hacks;

import me.bush.eventbus.annotation.EventListener;
import me.third.right.ThirdMod;
import me.third.right.events.client.PacketEvent;
import me.third.right.events.client.TickEvent;
import me.third.right.modules.Hack;
import me.third.right.modules.HackStandard;
import me.third.right.settings.setting.CheckboxSetting;
import me.third.right.settings.setting.EnumSetting;
import me.third.right.settings.setting.StringSetting;
import me.third.right.utils.client.enums.Category;
import me.third.right.utils.client.utils.ChatUtils;
import me.third.right.utils.client.utils.LoggerUtils;
import me.third.right.worldDownloader.managers.DatabaseManager;
import me.third.right.worldDownloader.mixins.IChunkProviderClient;
import me.third.right.worldDownloader.utils.AnvilChunkWDL;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.nbt.*;
import net.minecraft.network.play.server.SPacketChunkData;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.storage.SaveHandler;
import net.minecraft.world.storage.ThreadedFileIOBase;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import static me.third.right.worldDownloader.utils.ChunkUtils.isChunkEmpty;

@Hack.HackInfo(name = "WorldDownloader", description = "Downloads the world you are in", category = Category.OTHER)
public class WorldDownloader extends HackStandard {
    //Vars
    public static WorldDownloader INSTANCE;
    private AnvilChunkWDL anvilChunkWDL;
    private SaveHandler saveHandler;
    private int newChunks = 0;
    private int maxChunks = 0;
    private boolean isDownloading = false;
    private String serverIP = "";
    private DatabaseManager databaseManager;

    private enum NameType { Name, ServerIP, Both }
    private enum Page { WorldDownloader, Database }
    //Settings
    private final EnumSetting<Page> page = setting(new EnumSetting<>("Page", Page.values(), Page.WorldDownloader));
    // * WorldDownloader
    private final EnumSetting<NameType> nameType = setting(new EnumSetting<>("NameType", "The name given to the world that's being downloaded.", NameType.values(), NameType.Name, X -> !page.getSelected().equals(Page.WorldDownloader)));
    private final StringSetting worldName = setting(new StringSetting("WorldName", "The name of the world that's being downloaded.", "WorldDownloader", s -> nameType.getSelected().equals(NameType.ServerIP) || !page.getSelected().equals(Page.WorldDownloader)));

    // * Database
    private final CheckboxSetting useDatabase = setting(new CheckboxSetting("UseDatabase", "Whether or not to use the database. Stores more data about chunks.", false, X -> !page.getSelected().equals(Page.Database)));
    private final CheckboxSetting saveNewChunks = setting(new CheckboxSetting("SaveNewChunks", "Logs the new chunks into a database.", false, X -> !useDatabase.isChecked() || !page.getSelected().equals(Page.Database)));
    private final CheckboxSetting saveBlockCounts = setting(new CheckboxSetting("SaveBlockCounts", "Saves the block counts of the chunks.", false, X -> !useDatabase.isChecked() || !page.getSelected().equals(Page.Database)));

    //Overrides
    public WorldDownloader() {
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        ThirdMod.EVENT_PROCESSOR.subscribe(this);
        if(nullCheck()) {
            return;
        }

        serverIP = ChatUtils.getFormattedServerIP();
        maxChunks = (mc.gameSettings.renderDistanceChunks * 16) / 4;
        startDownload();
    }

    @Override
    public void onDisable() {
        ThirdMod.EVENT_PROCESSOR.unsubscribe(this);
        if(nullCheck()) return;

        stopDownload();
    }

    @Override
    public String setHudInfo() {
        return String.format("DL: %s | %s/%s", isDownloading, newChunks, maxChunks);
    }

    //Events

    @EventListener
    public void onTick(TickEvent event) {
        if(nullCheck()) {
            isDownloading = false;
            return;
        } else {
            isDownloading = true;
        }

        final String currentServerIP = ChatUtils.getFormattedServerIP();
        if(!serverIP.equals(currentServerIP)) {
            serverIP = currentServerIP;
            startDownload();//Rebuild everything
        }

        maxChunks = (mc.gameSettings.renderDistanceChunks * 16) / 4;

        if(isDownloading && databaseManager != null) {
            databaseManager.onTick();
        }
    }

    @EventListener
    public void onPacketEvent(PacketEvent.Receive event) {
        if(nullCheck()) return;

        if(event.getPacket() instanceof SPacketChunkData) {
            final SPacketChunkData packet = (SPacketChunkData) event.getPacket();
            newChunks++;

            if(newChunks > maxChunks) {
                newChunks = 0;
                saveChunks();
            }

            if(useDatabase.isChecked() && isDownloading) {
                if(!packet.isFullChunk() && saveNewChunks.isChecked()) {
                    databaseManager.storeNewChunk(packet.getChunkX(), packet.getChunkZ());
                }
            }
        }
    }

    //Methods

    public void startDownload() {
        if(databaseManager != null) {
            databaseManager.disconnect();
            databaseManager = null;
        }
        //databaseManager = new DatabaseManager(serverIP);
        newChunks = 0;
        serverIP = ChatUtils.getFormattedServerIP();
        saveHandler = (SaveHandler) mc.getSaveLoader().getSaveLoader(getWorldName(), true);
        anvilChunkWDL = AnvilChunkWDL.create(saveHandler, mc.world.provider);
        System.gc();
        System.runFinalization();
        System.gc();
        System.runFinalization();
    }

    public void stopDownload() {//TODO add a first time check create all the player data in the world folder then downloader and update the player data when stopped properly.
        if(!isDownloading) return;
        if(nullCheck()) {
            LoggerUtils.moduleLog(this, "Potentially threw away a lot of data, cause you didn't stop the downloader before closing the game.");
            saveHandler = null;
            anvilChunkWDL = null;
            System.gc();//Prob not needed.
            return;
        }
        saveWorld();
        mc.getSaveLoader().flushCache();
        saveHandler.flush();
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public String getWorldName() {
        switch (nameType.getSelected()) {
            case Name:
                return worldName.getString();
            case ServerIP:
                if(nullCheck()) return "IP_LOST";
                return ChatUtils.getFormattedServerIP();
            case Both:
                return String.format("%s_%s", worldName.getString(), nullCheck() ? "IP_LOST" : ChatUtils.getFormattedServerIP());
            default:
                return "WorldDownloader";
        }
    }

    public void saveWorld() {
        NBTTagCompound playerNBT = savePlayer();

        saveWorldInfo(playerNBT);
        saveChunks();

        try {
            ThreadedFileIOBase.getThreadedIOInstance().waitForFinish();
        } catch (Exception e) {
            throw new RuntimeException("Threw exception waiting for asynchronous IO to finish. Hmmm.", e);
        }
    }

    public void saveChunks() {//NullPointException somewhere in here
        final ChunkProviderClient chunkProvider = mc.world.getChunkProvider();
        final List<Chunk> chunks;

        try {
            chunks = new ArrayList<>(((IChunkProviderClient) chunkProvider).getLoadedChunks().values());
        } catch (NullPointerException e) {
            e.printStackTrace();
            return;
        }

        if(chunks.isEmpty()) {
            return;
        }

        for(Chunk chunk : chunks) {

            if(chunk == null) {
                continue;
            }

            if(isChunkEmpty(chunk)) {
                continue;
            }

            try {
                if (useDatabase.isChecked() && saveBlockCounts.isChecked() && databaseManager != null) {
                    databaseManager.storeChunkInfo(chunk);
                }
            } catch (NullPointerException e) {
                e.printStackTrace();
                break;
            }

            try {
                anvilChunkWDL.saveChunk(mc.world, chunk);
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }
    }

    private void saveWorldInfo(NBTTagCompound playerInfoNBT) {

        mc.world.getWorldInfo().setSaveVersion(19133);

        NBTTagCompound worldInfoNBT = mc.world.getWorldInfo().cloneNBTCompound(playerInfoNBT);
        NBTTagCompound rootWorldInfoNBT = new NBTTagCompound();
        rootWorldInfoNBT.setTag("Data", worldInfoNBT);

        applyOverridesToWorldInfo(worldInfoNBT, rootWorldInfoNBT);

        File saveDirectory = saveHandler.getWorldDirectory();
        File dataFile = new File(saveDirectory, "level.dat_new");
        File dataFileBackup = new File(saveDirectory, "level.dat_old");
        File dataFileOld = new File(saveDirectory, "level.dat");

        try (FileOutputStream stream = new FileOutputStream(dataFile)) {
            CompressedStreamTools.writeCompressed(rootWorldInfoNBT, stream);

            if (dataFileBackup.exists()) {
                dataFileBackup.delete();
            }

            dataFileOld.renameTo(dataFileBackup);

            if (dataFileOld.exists()) {
                dataFileOld.delete();
            }

            dataFile.renameTo(dataFileOld);

            if (dataFile.exists()) {
                dataFile.delete();
            }
        } catch (Exception e) {
            throw new RuntimeException("Couldn't save the world metadata!", e);
        }
    }

    public void applyOverridesToWorldInfo(NBTTagCompound worldInfoNBT, NBTTagCompound rootWorldInfoNBT) {//TODO automate this and add settings for this.
        // LevelName
        worldInfoNBT.setString("LevelName", getWorldName());
        // Cheats
        worldInfoNBT.setBoolean("allowCommands", true);
        // GameType
        worldInfoNBT.setInteger("GameType", 1); // Creative
        // Time
        worldInfoNBT.setLong("Time", mc.world.getWorldTime());
        // RandomSeed
        long seed = 0;
        worldInfoNBT.setLong("RandomSeed", seed);
        // MapFeatures
        worldInfoNBT.setBoolean("MapFeatures", false);
        // generatorName
        worldInfoNBT.setString("generatorName", "flat");
        // generatorOptions
        worldInfoNBT.setString("generatorOptions", ";0");
        // generatorVersion
        worldInfoNBT.setInteger("generatorVersion", 0);
        // Weather
        worldInfoNBT.setBoolean("raining", false);
        worldInfoNBT.setInteger("rainTime", 0);
        worldInfoNBT.setBoolean("thundering", false);
        worldInfoNBT.setInteger("thunderTime", 0);
        // Spawn
        int x = MathHelper.floor(mc.player.posX);
        int y = MathHelper.floor(mc.player.posY);
        int z = MathHelper.floor(mc.player.posZ);
        worldInfoNBT.setInteger("SpawnX", x);
        worldInfoNBT.setInteger("SpawnY", y);
        worldInfoNBT.setInteger("SpawnZ", z);
        worldInfoNBT.setBoolean("initialized", true);


        // Gamerules (most of these are already populated)
/*
        NBTTagCompound gamerules = worldInfoNBT.getCompoundTag("GameRules");
        for (String prop : worldProps.stringPropertyNames()) {
            if (!prop.startsWith("GameRule.")) {
                continue;
            }
            String rule = prop.substring("GameRule.".length());
            gamerules.setString(rule, worldProps.getProperty(prop));
        }
 */
    }

    public NBTTagCompound savePlayer() {

        NBTTagCompound playerNBT = new NBTTagCompound();
        mc.player.writeToNBT(playerNBT);

        applyOverridesToPlayer(playerNBT);

        File playersDirectory = new File(saveHandler.getWorldDirectory(), "playerdata");
        File playerFileTmp = new File(playersDirectory, mc.player.getUniqueID() + ".dat.tmp");
        File playerFile = new File(playersDirectory, mc.player.getUniqueID() + ".dat");

        try (FileOutputStream stream = new FileOutputStream(playerFileTmp)) {

            CompressedStreamTools.writeCompressed(playerNBT, stream);

            // Remove the old player file to make space for the new one.
            if (playerFile.exists()) {
                playerFile.delete();
            }

            playerFileTmp.renameTo(playerFile);
        } catch (Exception e) {
            throw new RuntimeException("Couldn't save the player!", e);
        }

        return playerNBT;
    }

    private void applyOverridesToPlayer(NBTTagCompound playerNBT) {
        // Health
        playerNBT.setShort("Health", (short) mc.player.getHealth());

        // foodLevel, foodTimer, foodSaturationLevel, foodExhaustionLevel
        playerNBT.setInteger("foodLevel", 20);
        playerNBT.setInteger("foodTickTimer", 0);
        playerNBT.setFloat("foodSaturationLevel", 5.0f);

        // Player Position
        BlockPos playerPos = mc.player.getPosition();

        //Positions are offset to center of block,
        //or player height.
        NBTTagList pos = new NBTTagList();
        pos.appendTag(new NBTTagDouble(playerPos.getX() + 0.5D));
        pos.appendTag(new NBTTagDouble(playerPos.getY() + 0.621D));
        pos.appendTag(new NBTTagDouble(playerPos.getZ() + 0.5D));
        playerNBT.setTag("Pos", pos);
        NBTTagList motion = new NBTTagList();
        motion.appendTag(new NBTTagDouble(0.0D));
        //Force them to land on the ground?
        motion.appendTag(new NBTTagDouble(-0.0001D));
        motion.appendTag(new NBTTagDouble(0.0D));
        playerNBT.setTag("Motion", motion);
        NBTTagList rotation = new NBTTagList();
        rotation.appendTag(new NBTTagFloat(0.0f));
        rotation.appendTag(new NBTTagFloat(0.0f));
        playerNBT.setTag("Rotation", rotation);


        // If the player is able to fly, spawn them flying.
        // Helps ensure they don't fall out of the world.
        playerNBT.getCompoundTag("abilities").setBoolean("flying", true);
    }

}
