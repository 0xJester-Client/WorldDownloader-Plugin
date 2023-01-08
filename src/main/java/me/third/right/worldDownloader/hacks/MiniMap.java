package me.third.right.worldDownloader.hacks;

import me.bush.eventbus.annotation.EventListener;
import me.third.right.ThirdMod;
import me.third.right.events.client.DisconnectEvent;
import me.third.right.events.client.PacketEvent;
import me.third.right.events.client.TickEvent;
import me.third.right.events.player.DimensionChangeEvent;
import me.third.right.modules.Hack;
import me.third.right.settings.setting.CheckboxSetting;
import me.third.right.settings.setting.EnumSetting;
import me.third.right.settings.setting.SliderSetting;
import me.third.right.utils.client.enums.Category;
import me.third.right.utils.client.manage.ThreadManager;
import me.third.right.worldDownloader.events.CImageCompleteEvent;
import me.third.right.worldDownloader.huds.MiniMapElement;
import me.third.right.worldDownloader.managers.ChunkImagerManager;
import net.minecraft.network.play.server.SPacketBlockChange;
import net.minecraft.network.play.server.SPacketChunkData;
import net.minecraft.world.chunk.Chunk;

import javax.imageio.ImageIO;

import static me.third.right.utils.client.utils.ColourUtils.rgbToInt;
import static me.third.right.worldDownloader.utils.ChunkUtils.isChunkEmpty;

@Hack.HackInfo(name = "MiniMap", description = "Shows a minimap.", category = Category.HUD)
public class MiniMap extends Hack {
    //Vars
    public static MiniMap INSTANCE;
    private final ChunkImagerManager chunkImagerManager = new ChunkImagerManager();
    private enum Page { Render, Structures }
    //Settings
    private final EnumSetting<Page> page = setting(new EnumSetting<>("Page", Page.values(), Page.Render));
    // * Render
    private final SliderSetting scale = setting(new SliderSetting("Scale", 0.5,0.1,1,0.1, SliderSetting.ValueDisplay.DECIMAL, X -> !page.getSelected().equals(Page.Render)));
    private final SliderSetting renderDistance = setting(new SliderSetting("RenderDistance", 4,1,15,1, SliderSetting.ValueDisplay.INTEGER, X -> !page.getSelected().equals(Page.Render)));
    //private final CheckboxSetting rotate = setting(new CheckboxSetting("Rotate", false, X -> !page.getSelected().equals(Page.Render)));
    private final SliderSetting red = setting(new SliderSetting("Red", 0,0,255,1, SliderSetting.ValueDisplay.INTEGER, X -> !page.getSelected().equals(Page.Render)));
    private final SliderSetting green = setting(new SliderSetting("Green", 0,0,255,1, SliderSetting.ValueDisplay.INTEGER, X -> !page.getSelected().equals(Page.Render)));
    private final SliderSetting blue = setting(new SliderSetting("Blue", 0,0,255,1, SliderSetting.ValueDisplay.INTEGER, X -> !page.getSelected().equals(Page.Render)));
    private final SliderSetting alpha = setting(new SliderSetting("Alpha", 255,0,255,1, SliderSetting.ValueDisplay.INTEGER, X -> !page.getSelected().equals(Page.Render)));
    private final CheckboxSetting outline = setting(new CheckboxSetting("Outline", true, X -> !page.getSelected().equals(Page.Render)));
    private final SliderSetting redOutline = setting(new SliderSetting("RedOutline", 255,0,255,1, SliderSetting.ValueDisplay.INTEGER, X -> !outline.isChecked() || !page.getSelected().equals(Page.Render)));
    private final SliderSetting greenOutline = setting(new SliderSetting("GreenOutline", 0,0,255,1, SliderSetting.ValueDisplay.INTEGER, X -> !outline.isChecked() || !page.getSelected().equals(Page.Render)));
    private final SliderSetting blueOutline = setting(new SliderSetting("BlueOutline", 255,0,255,1, SliderSetting.ValueDisplay.INTEGER, X -> !outline.isChecked() || !page.getSelected().equals(Page.Render)));
    private final SliderSetting alphaOutline = setting(new SliderSetting("AlphaOutline", 255,0,255,1, SliderSetting.ValueDisplay.INTEGER, X -> !outline.isChecked() || !page.getSelected().equals(Page.Render)));
    // * Structures
    private final CheckboxSetting showSlimeChunks = setting(new CheckboxSetting("ShowSlimeChunks", false, X -> !page.getSelected().equals(Page.Structures)));
    private final CheckboxSetting showWaypoints = setting(new CheckboxSetting("ShowWaypoints", false, X -> !page.getSelected().equals(Page.Structures)));
    private final CheckboxSetting showUpdates = setting(new CheckboxSetting("ShowUpdates", false, X -> !page.getSelected().equals(Page.Structures)));

    public MiniMap() {
        INSTANCE = this;
        ImageIO.setUseCache(false);
    }

    @Override
    public void onEnable() {
        ThirdMod.EVENT_PROCESSOR.subscribe(this);
    }

    @Override
    public void onDisable() {
        ThirdMod.EVENT_PROCESSOR.subscribe(this);
    }

    //Events

    @EventListener
    public void onTick(TickEvent event) {
        if(nullCheckFull()) return;
        chunkImagerManager.onTick();
    }

    @EventListener
    public void onPacketReceive(PacketEvent.Receive event) {
        if(nullCheckFull() || event.getPacket() == null) return;

        if(ThreadManager.INSTANCE == null) return;

        if(!chunkImagerManager.isReady()) return;

        if(event.getPacket() instanceof SPacketChunkData) {
            final SPacketChunkData packet = (SPacketChunkData) event.getPacket();

            final Chunk chunk = mc.world.getChunk(packet.getChunkX(), packet.getChunkZ());

            if(isChunkEmpty(chunk)) return;
            chunkImagerManager.chunkToImageFiltered(chunk);

        } else if(event.getPacket() instanceof SPacketBlockChange) {
            final SPacketBlockChange packet = (SPacketBlockChange) event.getPacket();

            final Chunk chunk = mc.world.getChunk(packet.getBlockPosition());

            if(isChunkEmpty(chunk)) return;
            chunkImagerManager.chunkToImageFiltered(chunk);

        }
    }

    @EventListener
    public void onRenderComplete(CImageCompleteEvent event) {
        if(nullCheckFull()) return;
        MiniMapElement.INSTANCE.invalidateChunk(event.getChunkPos());
    }

    @EventListener
    public void onDimChange(DimensionChangeEvent event) {
        MiniMapElement.INSTANCE.reset();
    }

    @EventListener
    public void onDisconnect(DisconnectEvent event) {
        MiniMapElement.INSTANCE.reset();
    }

    public int getColour() {
        return rgbToInt(red.getValueI(), green.getValueI(), blue.getValueI(), alpha.getValueI());
    }

    public int getColourOutline() {
        return rgbToInt(redOutline.getValueI(), greenOutline.getValueI(), blueOutline.getValueI(), alphaOutline.getValueI());
    }

    public boolean isOutline() {
        return outline.isChecked();
    }

    public boolean isSlimeChunks() {
        return showSlimeChunks.isChecked();
    }

    public boolean isWaypoints() {
        return showWaypoints.isChecked();
    }

    public boolean isShowUpdates() {
        return showUpdates.isChecked();
    }

    public ChunkImagerManager getChunkImagerManager() {return chunkImagerManager;}

    public int getRenderDistance() {return renderDistance.getValueI();}

    public double getScale() {return scale.getValue();}
}
