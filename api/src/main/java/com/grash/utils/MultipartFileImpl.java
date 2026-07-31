package com.grash.utils;

import org.springframework.web.multipart.MultipartFile;

import java.io.*;


public class MultipartFileImpl implements MultipartFile {
    private final byte[] imgContent;
    private final String name;

    public MultipartFileImpl(byte[] imgContent, String name) {
        this.imgContent = imgContent;
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getOriginalFilename() {
        return this.name;

    }

    @Override
    public String getContentType() {
        String nameLowerCase = name.toLowerCase();
        if (nameLowerCase.endsWith(".csv")) {
            return "text/csv";
        } else if (nameLowerCase.endsWith(".pdf")) {
            return "application/pdf";
        }
        return null;
    }

    @Override
    public boolean isEmpty() {
        return imgContent == null || imgContent.length == 0;
    }

    @Override
    public long getSize() {
        return imgContent.length;
    }

    @Override
    public byte[] getBytes() throws IOException {
        return imgContent;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(imgContent);
    }

    @Override
    public void transferTo(File dest) throws IOException, IllegalStateException {
        try (FileOutputStream fos = new FileOutputStream(dest)) {
            fos.write(imgContent);
        }
    }
}
