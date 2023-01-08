package me.third.right.worldDownloader.managers;

import java.util.Arrays;

//All created by Github Copilot :D
public class PerformanceTracker {
    private static boolean firstRun = true;
    private static int index = 0;
    private static final long[] times = new long[20];
    private static long highest = 0;
    private static long newest = 0;
    public static void addTime(long time) {
        if(time > highest) {
            highest = time;
        }

        newest = time;

        times[index] = time;
        index++;
        if(index >= times.length) index = 0;
    }

    public static long getAverage() {
        if(firstRun) {
            Arrays.fill(times, 0L);
            firstRun = false;
        }

        long total = 0;
        for(long time : times) {
            total += time;
        }
        return total / times.length;
    }

    public static long getHighest() {
        return highest;
    }

    public static long getNewest() {
        return newest;
    }
}
