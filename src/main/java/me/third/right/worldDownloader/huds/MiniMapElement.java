package me.third.right.worldDownloader.huds;

import me.third.right.ThirdMod;
import me.third.right.hud.Hud;
import me.third.right.modules.Render.Waypoints;
import me.third.right.utils.client.font.FontDrawing;
import me.third.right.utils.client.objects.Pair;
import me.third.right.utils.client.objects.Triplet;
import me.third.right.utils.client.utils.ChatUtils;
import me.third.right.utils.client.utils.LoggerUtils;
import me.third.right.utils.render.Render2D;
import me.third.right.worldDownloader.hacks.MiniMap;
import me.third.right.worldDownloader.managers.ChunkImagerManager;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;

import static me.third.right.utils.client.utils.ColourUtils.rgbToInt;
import static me.third.right.worldDownloader.utils.ChunkUtils.isSlimeChunk;

@Hud.HudInfo(name = "MiniMap")
public class MiniMapElement extends Hud {
    public static MiniMapElement INSTANCE;
    private final MiniMap miniMap = MiniMap.INSTANCE;
    private final Waypoints waypoints = Waypoints.INSTANCE;
    private final ChunkImagerManager chunkImagerManager = miniMap.getChunkImagerManager();
    private final HashMap<ChunkPos, Pair<ResourceLocation, Boolean>> chunkImageMap = new HashMap<>();
    private final int width = 16;
    private final int height = 16;
    private long seed = -1;
    private int index = 0;

    public MiniMapElement() {
        INSTANCE = this;
        setRequirements(MiniMap.INSTANCE);
    }

    @Override
    public void onRender() {
        if(guiHud.nullCheckFull()) return;

        GL11.glPushMatrix();
        GL11.glTranslated(getX() + getWindowWidth(), getY() + getWindowHeight(), 0);
        final double scale = miniMap.getScale();
        GL11.glScaled(scale, scale, 0);


        Render2D.drawRect(-(width * miniMap.getRenderDistance()) - 2,
                -(height * miniMap.getRenderDistance()) - 2,
                (width * miniMap.getRenderDistance()) + 2,
                (height * miniMap.getRenderDistance()) + 2,
                miniMap.getColour()
        );

        if(miniMap.isOutline()) {
            Render2D.drawOutlineRect(-(width * miniMap.getRenderDistance()) - 1,
                    -(height * miniMap.getRenderDistance()) - 1,
                    (width * miniMap.getRenderDistance()) + 1,
                    (height * miniMap.getRenderDistance()) + 1,
                    miniMap.getColourOutline(),
                    1.2F
            );
        }

        //Render2D.preRotation(0,0, (180 + MathHelper.wrapDegrees(mc.player.rotationYawHead)));
        renderMap();//TODO add rotation.
        //Render2D.postRotation(0,0);

        GL11.glScaled(1,1,1);
        GL11.glPopMatrix();
    }

    private void renderMap() {
        if(chunkImageMap.isEmpty()) return;

        for(int x = -miniMap.getRenderDistance(); x < miniMap.getRenderDistance(); x++) {
            for (int z = -miniMap.getRenderDistance(); z < miniMap.getRenderDistance(); z++) {
                final ChunkPos chunkPos = new ChunkPos(mc.player.getPosition().add(x * 16, 0, z * 16));

                if(!chunkImageMap.containsKey(chunkPos)) continue;

                final Pair<ResourceLocation, Boolean> pair = chunkImageMap.get(chunkPos);
                final ResourceLocation resourceLocation = pair.getFirst();
                if (resourceLocation == null) {
                    chunkImagerManager.chunkToImageFiltered(mc.world.getChunk(chunkPos.x, chunkPos.z));
                    continue;
                }

                int offsetX = (x * width);
                int offsetY = (z * height);

                boolean blend = GL11.glGetBoolean(GL11.GL_BLEND);
                GL11.glPushMatrix();
                if (!blend) GL11.glEnable(GL11.GL_BLEND);
                GL11.glColor4f(1, 1, 1, 1);
                mc.getTextureManager().bindTexture(resourceLocation);
                Gui.drawModalRectWithCustomSizedTexture(offsetX, offsetY, 0, 0, width, height, width, height);
                if (!blend) GL11.glDisable(GL11.GL_BLEND);
                GL11.glPopMatrix();

                if(miniMap.isSlimeChunks()) {
                    if(seed != -1 && mc.player.dimension == 0 && isSlimeChunk(seed, chunkPos.x, chunkPos.z)) {
                        Render2D.drawRect(offsetX, offsetY, offsetX + width, offsetY + height, 2146048 | 145 << 24);
                    }
                }

                if(miniMap.isShowUpdates()) {
                    if(pair.getSecond()) {
                        Render2D.drawRect(offsetX, offsetY, offsetX + width, offsetY + height, rgbToInt(255, 0, 0, 145));
                    }
                }
             }
        }

        if(!miniMap.isWaypoints()) return;

        final ChunkPos chunkPos = new ChunkPos(mc.player.getPosition());
        final String server = ChatUtils.getFormattedServerIP();

        if(server == null || server.equalsIgnoreCase("singleplayer")) return;

        for (Triplet<String, Integer, Vec3d> waypoint : waypoints.getWaypointList(ChatUtils.getFormattedServerIP())) {
            if(!waypoint.getSecond().equals(mc.player.dimension)) continue;
            final ChunkPos waypointPos = new ChunkPos(new BlockPos(waypoint.getThird()));

            int xDiff =  (waypointPos.x - chunkPos.x);
            int zDiff = (waypointPos.z - chunkPos.z);
            int distance = (int) Math.sqrt(xDiff * xDiff + zDiff * zDiff);
            if(distance > miniMap.getRenderDistance()) continue;

            int offsetX = ((waypointPos.x - chunkPos.x) * width);
            int offsetY = ((waypointPos.z - chunkPos.z) * height);

            //Render Chunk
            Render2D.drawRect(offsetX + 2, offsetY + 2, offsetX + width - 2, offsetY + height - 2, rgbToInt(255,0,255,145));

            //Render Name
            GL11.glPushMatrix();
            GL11.glTranslated(offsetX, offsetY, 0);
            final double scale = miniMap.getScale();
            GL11.glScaled(scale + 1, scale + 1, 0);

            String name = waypoint.getFirst();
            int nameWidth = FontDrawing.getStringWidth(FontDrawing.FontMode.LARGE, name, true);
            Render2D.drawRect(
                    -(nameWidth / 2) - 1,
                    -1,
                    (nameWidth / 2) + 4,
                    12, rgbToInt(0,0,0,145));

            FontDrawing.drawCenteredString(FontDrawing.FontMode.LARGE, name, 2, 2, rgbToInt(255,255,255,255), true, true);

            GL11.glScaled(1,1,1);
            GL11.glPopMatrix();
        }
    }


    @Override
    public void onUpdate() {//60 FPS when loading 15 Chunk distance. 1ms to 0ms was 10ms.
        if(guiHud.nullCheckFull()) return;

        if(seed == -1) {
            seed = ThirdMod.getSeedList().getSeed(ChatUtils.getFormattedServerIP());
        }

        mc.addScheduledTask(this::cleanUp);

        final int chunkDistance = miniMap.getRenderDistance() + 2;
        switch (index) {
            case 0:
                if(doFileSearch(0, chunkDistance, -chunkDistance, 0)) index++;
                break;
            case 1:
                if(doFileSearch(0, chunkDistance, 0, chunkDistance)) index++;
                break;
            case 2:
                if(doFileSearch(-chunkDistance, 0, -chunkDistance, 0)) index++;
                break;
            case 3:
                if(doFileSearch(-chunkDistance, 0, 0, chunkDistance)) index = 0;
                break;
        }
    }

    @Override
    public void onPinned() {
        reset();
    }

    @Override
    public void onUnpinned() {
        reset();
    }

    private boolean doFileSearch(int startX, int endX, int startZ, int endZ) {
        int maxReads = 4;
        for(int x = startX; x < endX; x++) {
            for (int z = startZ; z < endZ; z++) {
                if(maxReads == 0) return false;
                final BlockPos pos = mc.player.getPosition().add(x * 16, 0, z * 16);
                final ChunkPos chunkPos = new ChunkPos(pos);

                if(chunkImageMap.containsKey(chunkPos) && !chunkImageMap.get(chunkPos).getSecond()) {
                    continue;
                }

                final File path = chunkImagerManager.getImage(chunkPos.x, chunkPos.z);
                if(path == null) {
                    continue;
                }

                if(!Files.isReadable(path.toPath())) {
                    continue;
                }

                final BufferedImage bufferedImage = getImage(path, ImageIO::read);
                if (bufferedImage == null) continue;

                maxReads--;
                final DynamicTexture dynamicTexture = new DynamicTexture(bufferedImage);
                try {
                    dynamicTexture.loadTexture(mc.getResourceManager());
                } catch (IOException e) {
                    continue;
                }

                final Pair<ResourceLocation, Boolean> pair = new Pair<>(mc.getTextureManager().getDynamicTextureLocation("chunkImage", dynamicTexture), false);
                chunkImageMap.put(chunkPos, pair);
            }
        }
        return true;
    }

    public void reset() {
        chunkImageMap.clear();
        seed = -1;
    }

    public void invalidateChunk(ChunkPos chunk) {
        if(chunkImageMap.containsKey(chunk)) {
            chunkImageMap.get(chunk).setSecond(true);
        }
    }

    private void cleanUp() {
        for(ChunkPos chunkPos : chunkImageMap.keySet()) {
            ChunkPos playerChunkPos = new ChunkPos(mc.player.getPosition());
            int xDiff = chunkPos.x - playerChunkPos.x;
            int zDiff = chunkPos.z - playerChunkPos.z;
            int distance = (int) Math.sqrt(xDiff * xDiff + zDiff * zDiff);
            if(distance > (miniMap.getRenderDistance() + 4)) {
                chunkImageMap.remove(chunkPos);
            }
        }
    }

    //TODO: move to utils for version 4.5
    private <T> BufferedImage getImage(T source, ThrowingFunction<T, BufferedImage> readFunction) {//TODO move this to main client.
        try {
            return readFunction.apply(source);
        } catch (IOException ex) {
            LoggerUtils.logError("Reading! "+ex.toString());
            return null;
        }
    }
    @FunctionalInterface
    private interface ThrowingFunction<T, R> {//TODO move this to main client.
        R apply(T obj) throws IOException;
    }
}
