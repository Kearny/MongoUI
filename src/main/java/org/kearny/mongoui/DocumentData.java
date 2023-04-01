package org.kearny.mongoui;

import org.bson.Document;

import java.util.Map;

public class DocumentData {
    private final Map<String, Object> data;

    public DocumentData(Document document) {
        data = document;
    }

    public Object get(String key) {
        return data.get(key);
    }
}
