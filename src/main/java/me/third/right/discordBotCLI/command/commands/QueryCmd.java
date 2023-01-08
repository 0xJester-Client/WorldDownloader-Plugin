package me.third.right.discordBotCLI.command.commands;

import me.third.right.discordBotCLI.command.Cmd;
import me.third.right.discordBotCLI.utils.enums.Authority;
import me.third.right.utils.client.objects.Pair;
import me.third.right.utils.client.utils.LoggerUtils;
import me.third.right.utils.client.utils.MathUtils;
import me.third.right.worldDownloader.Main;
import me.third.right.worldDownloader.hacks.WorldDownloader;
import me.third.right.worldDownloader.utils.ValueStore;
import org.apache.commons.lang3.time.StopWatch;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Cmd.CmdInfo(name = {"query", "search"},
        description = "Query the world database.\n" +
                "Syntax:\n" +
                "query <Entry type>\n" +
                "query <Entry type> <id>\n",
        authority = Authority.ADMIN
)
public class QueryCmd extends Cmd {
    protected final WorldDownloader worldDownloader = WorldDownloader.INSTANCE;

    @Override
    public void onMessage(MessageCreateEvent event, String[] strings) {
        if(worldDownloader.getDatabaseManager() == null) {
            event.getChannel().sendMessage("Database is not loaded.");
            return;
        }

        final StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        final WorldDownloader worldDownloader = WorldDownloader.INSTANCE;
        switch (strings.length) {
            case 3:
            case 2:
                if(mc.player == null) {
                    event.getChannel().sendMessage("You must be in game to use this command.");
                    return;
                }

                int id = -1;
                if(strings.length == 2) {
                    int chunkX = (int) mc.player.posX >> 4;
                    int chunkZ = (int) mc.player.posZ >> 4;
                    id = Objects.hashCode(chunkX) ^ Objects.hashCode(chunkZ);
                } else {
                    if(MathUtils.isInteger(strings[2])) {
                        id = Integer.parseInt(strings[2]);
                    } else {
                        event.getChannel().sendMessage("Invalid chunk id.");
                        return;
                    }
                }

                final EmbedBuilder embedBuilder = new EmbedBuilder();
                final ValueStore[] valueStores = worldDownloader.getDatabaseManager().getEntry(strings[1], id);
                embedBuilder.setTitle("Query Result" + (valueStores.length > 1 ? String.format("s (%s)", valueStores.length) : ""));
                if (valueStores.length == 0) {
                    event.getChannel().sendMessage("No results found.");
                    return;
                }

                ValueStore latest = null;
                String latestDate = null;
                for (ValueStore valueStore : valueStores) {//TODO add dimension check.
                    final String date = valueStore.getValue("date");
                    if (date.isEmpty()) {
                        LoggerUtils.logDebug("Date is empty?");//TODO add a system to move broken entries to a alt database. We really don't want to delete them as they may contain useful information.
                        continue;
                    }

                    if (latest == null) {
                        latest = valueStore;
                        latestDate = date;
                        continue;
                    }

                    if (isLatest(latestDate, date)) {
                        latest = valueStore;
                        latestDate = date;
                    }
                }

                if (latest == null) {
                    event.getChannel().sendMessage("No results found.");
                    return;
                }

                for (Pair<String, String> value : latest.getValues()) {
                    if(strings[1].equals("Block")) {
                        if(value.getFirst().equals("blocks")) {

                            final String[] blocksSplit = value.getSecond().split("#");
                            final StringBuilder finalString = new StringBuilder();
                            for (String blockSplit : blocksSplit) {
                                if (blockSplit.isEmpty()) continue;

                                final String[] blockSplitSplit = blockSplit.split(":");
                                final String blockName = blockSplitSplit[0];
                                final int blockCountInt = Integer.parseInt(blockSplitSplit[1].split("@")[0]);

                                finalString.append(String.format("%s: %s ", blockName, blockCountInt));
                            }
                            embedBuilder.addField("BLOCK COUNTS: ", finalString.toString());
                            continue;
                        }
                    }

                    embedBuilder.addField(value.getFirst().toUpperCase(Locale.ROOT)+": ", value.getSecond());
                }

                stopWatch.stop();
                embedBuilder.setFooter("World Downloader v"+ Main.INSTANCE.getVersion()+" | Took: "+stopWatch.getTime(TimeUnit.MILLISECONDS)+"ms");

                event.getChannel().sendMessage(embedBuilder);
                return;
            default:
                event.getChannel().sendMessage("Invalid arguments.");
                break;
        }
    }

    private boolean isLatest(String dateTime, String dateTime2) {
        if(dateTime.isEmpty() || dateTime2.isEmpty()) {
            return false;
        }

        final String dateTemp = dateTime.split(" ")[0];
        final String date2Temp = dateTime2.split(" ")[0];

        final String[] date = dateTemp.split("/");
        final String[] date2 = date2Temp.split("/");

        int year = Integer.parseInt(date[0]);
        int year2 = Integer.parseInt(date2[0]);
        if(year > year2) {
            int month = Integer.parseInt(date[0]);
            int month2 = Integer.parseInt(date2[0]);

            if (month > month2) {
                int day = Integer.parseInt(date[0]);
                int day2 = Integer.parseInt(date2[0]);

                return day > day2;
            } else{
                return false;
            }
        } else {
            return false;
        }
    }
}
