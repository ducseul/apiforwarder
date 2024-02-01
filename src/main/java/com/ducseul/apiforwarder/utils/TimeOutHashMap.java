package com.ducseul.apiforwarder.utils;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TimeOutHashMap<K, V> {
    private final Map<K, V> dataMaps;
    private final Map<K, Long> timestamps;
    private final ScheduledExecutorService executorService;

    private final long pruneInterval;

    public TimeOutHashMap(long pruneInterval) {
        this(pruneInterval, TimeUnit.MILLISECONDS);
    }

    public TimeOutHashMap(long pruneInterval, TimeUnit pruneUnit) {
        this.dataMaps = new ConcurrentHashMap<>();
        this.timestamps = new ConcurrentHashMap<>();
        this.pruneInterval = pruneInterval;
        this.executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(this::pruneTimeOut, pruneInterval, pruneInterval, pruneUnit);
    }

    public void put(K key, V value) {
        dataMaps.put(key, value);
        timestamps.put(key, System.currentTimeMillis());
    }

    public V get(K key) {
        return dataMaps.get(key);
    }

    public boolean containsKey(K key){
        return dataMaps.containsKey(key);
    }

    public boolean containsValue(V value){
        return dataMaps.containsValue(value);
    }

    private synchronized void pruneTimeOut() {
        long currentTime = System.currentTimeMillis();
        timestamps.entrySet().removeIf(entry -> currentTime - entry.getValue() > pruneInterval);
        dataMaps.keySet().removeIf(key -> !timestamps.containsKey(key));
    }

    public void stopPruning() {
        executorService.shutdown();
    }
}
