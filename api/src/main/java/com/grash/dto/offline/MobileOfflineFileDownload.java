package com.grash.dto.offline;

public record MobileOfflineFileDownload(long fileId, String name, String mimeType, long size, String url) {}
