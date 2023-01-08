package me.third.right.worldDownloader.utils;

import me.third.right.ThirdMod;
import me.third.right.utils.client.objects.Pair;
import me.third.right.utils.client.utils.BlockUtils;
import me.third.right.utils.client.utils.ChatUtils;
import me.third.right.worldDownloader.events.CImageCompleteEvent;
import me.third.right.worldDownloader.managers.PerformanceTracker;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockCarpet;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import org.apache.commons.lang3.time.StopWatch;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static me.third.right.worldDownloader.utils.ChunkUtils.isChunkEmpty;

public class CImagerRunnable implements Runnable {
    protected final Minecraft mc = Minecraft.getMinecraft();
    private final Pair<Path, Chunk>[] chunks;

    public CImagerRunnable(Pair<Path, Chunk>... chunks) {
        this.chunks = chunks;
    }

    public CImagerRunnable(Pair<Path, Chunk> chunks) {
        this.chunks = new Pair[]{chunks};
    }

    @Override
    public void run() {
        if(mc.player == null || mc.world == null) return;

        for(Pair<Path, Chunk> pair : chunks) {
            final Chunk chunk = pair.getSecond();
            if (chunk == null) {
                return;
            }

            if (isChunkEmpty(chunk)) {
                return;
            }

            final StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            final File outfile = pair.getFirst().toFile();
            final BufferedImage bufferedImage = new BufferedImage(16, 16, 2);
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {

                    int skyY = 256;
                    while (skyY != 0) {
                        BlockPos pos = new BlockPos(chunk.x * 16 + x, skyY, chunk.z * 16 + z);
                        IBlockState state = mc.world.getBlockState(pos);
                        Block block = state.getBlock();
                        if (block instanceof BlockAir) {
                            skyY--;
                            continue;
                        }
                        //TODO add Nether but split the chunk it layered segments.

                        if (BlockUtils.isSolid(block) || block instanceof BlockCarpet) {
                            int colour = state.getMapColor(chunk.getWorld(), pos).colorValue;

                            int searchDepth = 10;
                            int newY = skyY + 1;
                            while (searchDepth != 0) {
                                BlockPos pos2 = new BlockPos(chunk.x * 16 + x, newY, chunk.z * 16 + z).add(EnumFacing.NORTH.getDirectionVec());
                                IBlockState state2 = mc.world.getBlockState(pos2);
                                Block block2 = state2.getBlock();

                                if (block2 instanceof BlockAir) {
                                    break;
                                } else {
                                    colour = darken(colour, 15);//Darken the colour.
                                    newY++;
                                    searchDepth--;
                                }
                            }

                            bufferedImage.setRGB(x, z, colour | 255 << 24);//Set to block map colour.
                            break;
                        } else if (BlockUtils.isLiquid(block)) {
                            int colourBlend = state.getMapColor(chunk.getWorld(), pos).colorValue;
                            if (skyY <= 1) {
                                bufferedImage.setRGB(x, z, colourBlend | 255 << 24);//Set to liquid colour if there is only one block below.
                                break;
                            }

                            int searchDepth = 10;
                            int newY = skyY;
                            while (searchDepth != 0) {
                                BlockPos pos2 = new BlockPos(chunk.x * 16 + x, newY, chunk.z * 16 + z);
                                IBlockState state2 = mc.world.getBlockState(pos2);
                                Block block2 = state2.getBlock();
                                if (block2 instanceof BlockAir) {
                                    newY--;
                                    searchDepth--;
                                    continue;
                                }

                                if (BlockUtils.isSolid(block2)) {
                                    colourBlend = blendColour(colourBlend, state2.getMapColor(chunk.getWorld(), pos2).colorValue);//Blend the colour with the block below.
                                    break;
                                } else if (BlockUtils.isLiquid(block2)) {
                                    colourBlend = darken(colourBlend, 3);//Darken the colour.
                                    newY--;
                                    searchDepth--;
                                }
                            }

                            bufferedImage.setRGB(x, z, colourBlend | 255 << 24);//Set to liquid colour if there is only one block below.
                            break;
                        } else {
                            skyY--;
                        }
                    }

                    if (skyY == 0) {
                        bufferedImage.setRGB(x, z, -16777216);//Set to black
                    }
                }
            }

            try {
                ImageIO.write(bufferedImage, "png", outfile);
            } catch (IOException var5) {
                ChatUtils.error("Failed to save ChunkImage...");
            }


            ThirdMod.EVENT_PROCESSOR.post(new CImageCompleteEvent(chunk.getPos()));

            stopWatch.stop();
            PerformanceTracker.addTime(stopWatch.getTime(TimeUnit.MILLISECONDS));
        }
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof CImagerRunnable && obj.hashCode() == hashCode();
    }

    @Override
    public int hashCode() {
        int hash = 0;
        for(Pair<Path, Chunk> chunk : chunks) {
            hash += Objects.hashCode(chunk.getSecond().x) ^ Objects.hashCode(chunk.getSecond().z);
        }
        return hash;
    }

    //TODO move to main client colour class.
    private int darken(int colour, int amount) {
        int r = (colour >> 16) & 0xFF;
        int g = (colour >> 8) & 0xFF;
        int b = colour & 0xFF;

        r = Math.max(0, r - amount);
        g = Math.max(0, g - amount);
        b = Math.max(0, b - amount);

        return (r << 16) | (g << 8) | b;
    }

    private int blendColour(int colour1, int colour2) {//Auto generated by Github Copilot
        int r1 = (colour1 >> 16) & 0xFF;
        int g1 = (colour1 >> 8) & 0xFF;
        int b1 = (colour1) & 0xFF;

        int r2 = (colour2 >> 16) & 0xFF;
        int g2 = (colour2 >> 8) & 0xFF;
        int b2 = (colour2) & 0xFF;

        int r = (r1 + r2) / 2;
        int g = (g1 + g2) / 2;
        int b = (b1 + b2) / 2;

        return (r << 16) | (g << 8) | b;
    }

}
