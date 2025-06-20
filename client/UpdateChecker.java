package org.base64CodePro.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class UpdateChecker {
    private final String serverHost;
    private final int serverPort;
    private final String currentVersion;
    private String latestVersion;
    private String updateUrl;
    private boolean updateAvailable;

    public UpdateChecker(String serverHost, int serverPort, String currentVersion) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.currentVersion = currentVersion;
    }

    public boolean isUpdateAvailable() throws IOException {
        try (Socket socket = new Socket(serverHost, serverPort);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             java.io.PrintWriter writer = new java.io.PrintWriter(socket.getOutputStream(), true)) {

            // 发送连接命令
            writer.println("CONNECT");

            // 等待服务器的OK响应
            String initialResponse = reader.readLine();
            if (!"OK".equals(initialResponse)) {
                System.err.println("连接服务器失败: 预期收到OK，但收到" + initialResponse);
                return false;
            }

            writer.println(currentVersion);

            // 读取响应
            String response = reader.readLine();
            if ("UPDATE_REQUIRED".equals(response)) {
                latestVersion = reader.readLine();
                updateUrl = reader.readLine();
                updateAvailable = true;
            } else {
                updateAvailable = false;
            }
        }
        return updateAvailable;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public String getUpdateUrl() {
        return updateUrl;
    }
}