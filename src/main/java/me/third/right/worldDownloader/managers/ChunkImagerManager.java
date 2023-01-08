package me.third.right.worldDownloader.managers;

import me.third.right.ThirdMod;
import me.third.right.utils.client.manage.ThreadManager;
import me.third.right.utils.client.objects.Pair;
import me.third.right.utils.client.utils.ChatUtils;
import me.third.right.utils.client.utils.FileUtils;
import me.third.right.worldDownloader.utils.CImagerRunnable;
import net.minecraft.client.Minecraft;
import net.minecraft.world.chunk.Chunk;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static me.third.right.worldDownloader.utils.ChunkUtils.isChunkEmpty;

public class ChunkImagerManager {
    protected final Minecraft mc = Minecraft.getMinecraft();
    private final Queue<Pair<Path, Chunk>> queue = new ConcurrentLinkedQueue<>();
    protected ThreadManager threadManager;
    private String serverIP = "";
    private Path imagePathDir;
    private boolean isReady = false;


    public void init() {
        threadManager = ThreadManager.INSTANCE;
        imagePathDir = ThirdMod.configFolder.resolve("ChunkImages");
        FileUtils.folderExists(
                imagePathDir,
                imagePathDir.resolve(serverIP),
                imagePathDir.resolve(serverIP).resolve("Overworld"),
                imagePathDir.resolve(serverIP).resolve("Nether"),
                imagePathDir.resolve(serverIP).resolve("End")
        );
        queue.clear();
        isReady = true;
    }

    public void onTick() {
        if(mc.player == null || mc.world == null) return;

        final String currentServerIP = ChatUtils.getFormattedServerIP();
        if(currentServerIP.isEmpty()) return;

        if(!serverIP.equals(currentServerIP)) {
            serverIP = currentServerIP;
            init();
        }

        if(threadManager == null || queue.isEmpty()) return;
        if(threadManager.getQueueSize() < threadManager.getPoolSize() * 2) {
            Pair<Path, Chunk>[] pairs = new Pair[queue.size()];
            for(int i = 0; i < pairs.length; i++) {
                pairs[i] = queue.poll();
            }
            threadManager.submit(new CImagerRunnable(pairs));
        }
    }

    public void chunkToImageFiltered(Chunk chunk) {
        if(chunk == null) return;
        if(isChunkEmpty(chunk)) return;
        chunkToImage(chunk);
    }

    public void chunkToImage(Chunk chunk) {
        final Path finalPath;
        switch (mc.player.dimension) {
            case 0:
                finalPath = imagePathDir.resolve(serverIP).resolve("Overworld").resolve(chunk.x + "," + chunk.z + ".png");
                break;
            case -1:
                finalPath = imagePathDir.resolve(serverIP).resolve("Nether").resolve(chunk.x + "," + chunk.z + ".png");
                break;
            case 1:
                finalPath = imagePathDir.resolve(serverIP).resolve("End").resolve(chunk.x + "," + chunk.z + ".png");
                break;
            default:
                finalPath = null;
                break;

        }

        if(threadManager == null || finalPath == null) return;
        if(threadManager.getQueueSize() >= threadManager.getPoolSize() * 2) {
            final Pair<Path, Chunk> run = new Pair<>(finalPath, chunk);
            if (!queue.contains(run)) {
                queue.add(run);
            }
        } else {
            threadManager.submit(new CImagerRunnable(new Pair<>(finalPath, chunk)));
        }
    }

    public File getImage(int chunkX, int chunkZ) {
        return getImage(chunkX, chunkZ, mc.player.dimension);
    }

    public File getImage(int chunkX, int chunkZ, int dimension) {
        final File file;
        switch (dimension) {
            case 0:
                file = imagePathDir.resolve(serverIP).resolve("Overworld").resolve(chunkX + "," + chunkZ + ".png").toFile();
                break;
            case -1:
                file = imagePathDir.resolve(serverIP).resolve("Nether").resolve(chunkX + "," + chunkZ + ".png").toFile();
                break;
            case 1:
                file = imagePathDir.resolve(serverIP).resolve("End").resolve(chunkX + "," + chunkZ + ".png").toFile();
                break;
            default:
                return null;
        }

        if(Files.exists(file.toPath())) {
            return file;
        } else {
            chunkToImageFiltered(mc.world.getChunk(chunkX, chunkZ));
            return null;
        }
    }

    public int getQueueSize() {
        return queue.size();
    }

    public boolean isReady() {
        return isReady;
    }

    public void setReady(boolean ready) {
        isReady = ready;
    }
}
