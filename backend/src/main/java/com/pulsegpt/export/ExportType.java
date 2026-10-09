package com.pulsegpt.export;

public enum ExportType {
    MARKDOWN("text/markdown", ".md"),
    PDF("application/pdf", ".pdf"),
    TELEPROMPTER("text/plain", ".txt"),
    CHECKLIST("text/markdown", "-checklist.md"),
    JSON("application/json", ".json"),
    TIMELINE("text/markdown", "-timeline.md"),
    PRODUCTION_PACKAGE("application/zip", "-package.zip");

    private final String mimeType;
    private final String defaultExtension;

    ExportType(String mimeType, String defaultExtension) {
        this.mimeType = mimeType;
        this.defaultExtension = defaultExtension;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getDefaultExtension() {
        return defaultExtension;
    }
}
