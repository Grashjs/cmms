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
}
