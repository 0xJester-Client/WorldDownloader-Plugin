package me.third.right.worldDownloader.utils;

import me.third.right.utils.client.objects.Pair;

public class ValueStore {
    private final String name;
    private final Pair<String, String>[] values;

    @SafeVarargs
    public ValueStore(String name, Pair<String, String>... values) {
        this.name = name;
        this.values = values;
    }

    public String getValue(String key) {
        for(Pair<String, String> value : values) {
            if(value.getFirst().equals(key)) {
                return value.getSecond();
            }
        }
        return null;
    }

    public Pair<String, String>[] getValues() {
        return values;
    }

    public String getName() {
        return name;
    }
}
