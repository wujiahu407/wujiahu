package org.base64CodePro.client;

import javax.swing.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateDownloader {
    private final String downloadUrl;
    private final String savePath;
    private final JProgressBar progressBar;
    private final JLabel statusLabel;

    public UpdateDownloader(String downloadUrl, String savePath, JProgressBar progressBar, JLabel statusLabel) {
        this.downloadUrl = downloadUrl;
        this.savePath = savePath;
        this.progressBar = progressBar;
        this.statusLabel = statusLabel;
    }

    public boolean download() {
        try {
            URL url = new URL(downloadUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                statusLabel.setText("下载失败: HTTP错误 " + responseCode);
                return false;
            }
            
            int fileSize = connection.getContentLength();
            if (fileSize <= 0) {
                statusLabel.setText("下载失败: 无法获取文件大小");
                return false;
            }
            
            try (InputStream inputStream = connection.getInputStream();
                 FileOutputStream outputStream = new FileOutputStream(savePath)) {
                
                byte[] buffer = new byte[4096];
                int bytesRead;
                long totalBytesRead = 0;
                
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                    
                    final int progress = (int) ((totalBytesRead * 100) / fileSize);
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setValue(progress);
                        statusLabel.setText("下载中: " + progress + "%");
                    });
                }
            }
            
            statusLabel.setText("下载完成");
            return true;
        } catch (Exception e) {
            statusLabel.setText("下载失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}    