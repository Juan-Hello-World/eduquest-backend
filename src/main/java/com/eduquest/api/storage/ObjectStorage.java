package com.eduquest.api.storage;

public interface ObjectStorage {

    void put(String key, byte[] data, String contentType);

    byte[] get(String key);

    void delete(String key);

    String temporaryUrl(String key, String fileName, boolean download);
}
