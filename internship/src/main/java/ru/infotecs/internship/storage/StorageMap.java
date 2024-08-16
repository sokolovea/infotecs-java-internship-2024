package ru.infotecs.internship.storage;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component()
public class StorageMap implements Externalizable {
    public static final Long DEFAULT_TTL_MS = 60_000L;
    private ConcurrentHashMap<String, RecordValue> storage = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public StorageMap() {
        startTrim();
    }

    public void putValue(String key, String value) throws NullPointerException {
        RecordValue newRecordValue = new RecordValue(value, DEFAULT_TTL_MS);
        storage.put(key, newRecordValue);
    }

    public void putValue(String key, String value, Long tllMS) {
        if (!isTtlCorrect(tllMS)) {
            tllMS = DEFAULT_TTL_MS;
        }
        RecordValue recordValue = new RecordValue(value, tllMS);
        storage.put(key, recordValue);
    }

    public RecordValue getValue(String key) {
        if (!isKeyValid(key)) {
            return null;
        }
        return storage.get(key);
    }

    public RecordValue removeValue(String key) {
        return storage.remove(key);
    }

    private boolean isKeyExist(String key) {
        try {
            return storage.containsKey(key);
        } catch (NullPointerException e) {
            return false;
        }
    }

    public boolean isKeyValid(String key) {
        return isKeyExist(key) && (storage.get(key).getExpirationTime() > System.currentTimeMillis());
    }

    public static Boolean isTtlCorrect(Long ttl) {
        return (ttl != null); //It was decided not to fix ttl <= 0 on DEFAULT_TTL_MS automatically
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(storage);
        out.writeLong(System.currentTimeMillis());
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        storage = (ConcurrentHashMap<String, RecordValue>) in.readObject();
//        System.out.println("Init: " + storage.size());
        long oldReferencePointTime = in.readLong();
        long referencePointTime = System.currentTimeMillis();
        long deltaTime = referencePointTime - oldReferencePointTime;
        for (RecordValue recordValue : storage.values()) {
            recordValue.setExpirationTime(recordValue.getExpirationTime() + deltaTime);
        }
    }

    public void startTrim() {
        scheduler.scheduleAtFixedRate(this::trim, 2, 2, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void stopTrim() {
//        System.out.println("Trim stopped");
        scheduler.shutdownNow();
    }

    private void trim() {
//        System.out.println("Before trimming " + storage.size());
        long currentTime = System.currentTimeMillis();
        for (Map.Entry<String, RecordValue> entry : storage.entrySet()) {
            storage.computeIfPresent(entry.getKey(), (keyInternal, valueInternal) -> {
//                if (keyInternal.equals("1"))
//                    System.out.println("Осталось:" + (valueInternal.getExpirationTime() - System.currentTimeMillis()) / 1000);
                if (valueInternal.getExpirationTime() == null || valueInternal.getExpirationTime() < currentTime) {
                    return null;
                }
                return valueInternal;
            });
        }
        System.out.println("Trimmed " + storage.size());
    }
}
