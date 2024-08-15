package ru.infotecs.internship.storage;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.context.annotation.Scope;
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
//@Scope("prototype")
public class StorageMap implements Externalizable {
    public static final Long DEFAULT_TTL_MS = 60_000L;
    private ConcurrentHashMap<String, RecordValue> storage = new ConcurrentHashMap<>();
    private long referencePointTime = System.currentTimeMillis();

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public StorageMap() {
//        for (int i = 0; i < 50_000; i++) {
//            storage.put(String.valueOf(i), new RecordValue("ashdfi", (i % 173 + 5) * 1000));
//        }
        startTrim();
    }

    public ConcurrentHashMap<String, RecordValue> getStorage() {
        return storage;
    }

    private void setStorage(ConcurrentHashMap<String, RecordValue> storage) {
        this.storage = storage;
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

    public RecordValue getValue(String key) throws NullPointerException {
        if (!isKeyValid(key)) {
            return null;
        }
        return storage.get(key);
    }

    public RecordValue removeValue(String key) {
        return storage.remove(key);
    }

    private Boolean isKeyExist(String key) {
        try {
            return storage.containsKey(key);
        } catch (NullPointerException e) {
            return false;
        }
    }

    public boolean isKeyValid(String key) {
        if (isKeyExist(key)) {
        System.out.println(storage.get(key).getExpirationTime() - System.currentTimeMillis()); }
        return isKeyExist(key) && (storage.get(key).getExpirationTime() > System.currentTimeMillis());
    }

    public static Boolean isTtlCorrect(Long ttl) {
        return (ttl != null);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(storage);
        out.writeLong(System.currentTimeMillis());
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        storage = (ConcurrentHashMap<String, RecordValue>) in.readObject();
        System.out.println("Init: " + storage.size());
        long oldReferencePointTime = in.readLong();
        referencePointTime = System.currentTimeMillis();
        long deltaTime = referencePointTime - oldReferencePointTime;
        for (RecordValue recordValue : storage.values()) {
            recordValue.setExpirationTime(recordValue.getExpirationTime() + deltaTime);
        }
        System.out.println(isKeyValid("1"));
        System.out.println(isKeyValid("1923"));
        System.out.println(isKeyValid("170"));
        System.out.println("Post-init: " + storage.size());
    }

    private void startTrim() {
        scheduler.scheduleAtFixedRate(this::trim, 2, 2, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void stopTrim() {
        System.out.println("Trim stopped");
        scheduler.shutdownNow();
    }

    private void trim() {
        System.out.println("Before trimming " + storage.size());
        long currentTime = System.currentTimeMillis();
        for (Map.Entry<String, RecordValue> entry : storage.entrySet()) {
            storage.computeIfPresent(entry.getKey(), (keyInternal, valueInternal) -> {
                if (keyInternal.equals("1"))
                    System.out.println("Осталось:" + (valueInternal.getExpirationTime() - System.currentTimeMillis()) / 1000);
                if (valueInternal.getExpirationTime() == null || valueInternal.getExpirationTime() < currentTime) {
                    return null;
                }
                return valueInternal;
            });
        }
        System.out.println("Trimmed " + storage.size());
    }
}
