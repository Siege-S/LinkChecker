package com.example.smslinkchecker;

public class ScanResult {
    private String engineName;
    private String method;
    private String category;
    private String result;

    public ScanResult(String engineName, String method, String category, String result) {
        this.engineName = engineName;
        this.method = method;
        this.category = category;
        this.result = result;
    }

    public String getEngineName() {
        return engineName;
    }

    public String getMethod() {
        return method;
    }

    public String getCategory() {
        return category;
    }

    public String getResult() {
        return result;
    }
}
