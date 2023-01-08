package me.third.right.discordBotCLI.command.commands;

import me.third.right.discordBotCLI.command.Cmd;
import me.third.right.discordBotCLI.utils.enums.Authority;
import me.third.right.worldDownloader.Main;
import me.third.right.worldDownloader.hacks.WorldDownloader;
import me.third.right.worldDownloader.managers.DatabaseManager;
import org.apache.commons.lang3.time.StopWatch;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

@Cmd.CmdInfo(name = {"worldDownloader", "wdl"}, description = "Return information about the currently loaded database.", authority = Authority.ADMIN)
public class WorldDownloaderCmd extends Cmd {
    @Override
    public void onMessage(MessageCreateEvent event, String[] strings) {
        final WorldDownloader worldDownloader = WorldDownloader.INSTANCE;
        final DatabaseManager databaseManager = worldDownloader.getDatabaseManager();

        final StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        final EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("World Downloader status:");
        embedBuilder.addField("Status:", worldDownloader.isEnabled() ? "Enabled" : "Disabled");
        if(databaseManager != null) {
            embedBuilder.addField("Database:", databaseManager.getDatabaseName());
            embedBuilder.addField("DB NewChunks Size:", databaseManager.getNewChunkCount()+"");
            embedBuilder.addField("DB Block Size:", databaseManager.getBlockCount()+"");

            final StringBuilder stringBuilder = new StringBuilder();
            HashMap<String , BigInteger> map = databaseManager.calcTotals();
            stringBuilder.append("Totals:");
            map.forEach((key, value) -> stringBuilder.append(" ").append(key).append(": ").append(value));
            embedBuilder.setDescription(stringBuilder.toString());
        } else {
            embedBuilder.addField("Database Manager:", "Not initialized");
        }
        stopWatch.stop();

        embedBuilder.setFooter("World Downloader v"+ Main.INSTANCE.getVersion()+" | Took: "+stopWatch.getTime(TimeUnit.MILLISECONDS)+"ms");

        event.getChannel().sendMessage(embedBuilder);
    }
}
