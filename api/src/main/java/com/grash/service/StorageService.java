package com.grash.service;

import com.grash.model.File;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    /**
     * Uploads a file to the storage and returns the public URL.
     *
     * @param file   The file to be uploaded.
     * @param folder The folder where the file should be uploaded.
     * @return The file Path of the uploaded file.
     */
    String upload(MultipartFile file, String folder);

    /**
     * Downloads a file from the storage using its file path.
     *
     * @param filePath The path of the file to be downloaded.
     * @return A byte array of the file content.
     */
    byte[] download(String filePath);

    /**
     * Downloads a file from the storage using a File object.
     *
     * @param file The File object containing the URL and metadata.
     * @return A byte array of the file content.
     */
    byte[] download(File file);

    String generateSignedUrl(File file, long expirationMinutes);

    String generateSignedUrl(String filePath, long expirationMinutes);

    default String uploadAndSign(MultipartFile file, String folder) {
        return generateSignedUrl(upload(file, folder), 10);
    }

    String upload(byte[] data, String fileName, String folder);

    /**
     * Uploads a file to a specific, caller-controlled path (no key generation).
     * Overwrites any existing object at that path.
     *
     * @param data        The file content.
     * @param filePath    The exact path/key to upload to.
     * @param contentType The content type of the file.
     */
    void uploadAt(byte[] data, String filePath, String contentType);

    boolean exists(String filePath);
}
