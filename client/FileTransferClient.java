package org.base64CodePro.client;

import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Base64;

import java.util.Properties;
import java.util.zip.CRC32;

public class FileTransferClient {
    private static final int BUFFER_SIZE = 8192;
    private final ClientMain clientMain;
    private final String serverHost;
    private final int serverPort;

    public FileTransferClient(ClientMain clientMain) {
        this.clientMain = clientMain;
        // 从配置中读取服务器地址和端口
        Properties config = clientMain.getConfig();
        this.serverHost = config.getProperty("server.host", "localhost");
        this.serverPort = Integer.parseInt(config.getProperty("server.port", "8888"));
    }

    public boolean sendFile(String filePath) {
        try (Socket socket = new Socket(serverHost, serverPort);
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            clientMain.log("尝试连接服务器: " + serverHost + ":" + serverPort);

            // 握手协议
            writer.println("CONNECT");

            // 等待服务器的OK响应
            String initialResponse = reader.readLine();
            clientMain.log("服务器初始响应: " + initialResponse);
            if (initialResponse == null) {
                clientMain.log("错误: 服务器没有响应");
                return false;
            }
            if (!"OK".equals(initialResponse)) {
                clientMain.log("连接服务器失败: 预期收到OK，但收到" + initialResponse);
                return false;
            }

            clientMain.log("发送客户端版本: " + ClientMain.VERSION);
            writer.println(ClientMain.VERSION);

            String response = reader.readLine();
            clientMain.log("服务器版本检查响应: " + response);
            if (response == null) {
                clientMain.log("错误: 服务器在版本检查时没有响应");
                return false;
            }

            if ("UPDATE_REQUIRED".equals(response)) {
                // 旧的响应类型，为了兼容旧版本服务器
                clientMain.log("服务器要求更新客户端");
                String latestVersion = reader.readLine();
                String updateUrl = reader.readLine();
                clientMain.log("最新版本: " + latestVersion);
                clientMain.log("更新地址: " + updateUrl);

                // 显示提示对话框但不阻止上传
                JOptionPane.showMessageDialog(
                        null,
                        "服务器版本(" + latestVersion + ")与客户端版本不一致，建议更新。\n更新地址: " + updateUrl,
                        "版本提示",
                        JOptionPane.INFORMATION_MESSAGE
                );
                return false;  // 仍然返回 false，因为旧版本服务器不支持继续上传
            } else if ("VERSION_DIFFERENT".equals(response)) {
                // 新的响应类型
                clientMain.log("服务器版本与客户端不一致");
                String latestVersion = reader.readLine();
                String updateUrl = reader.readLine();
                clientMain.log("服务器版本: " + latestVersion);
                clientMain.log("更新地址: " + updateUrl);

                // 显示提示对话框但不阻止上传
                JOptionPane.showMessageDialog(
                        null,
                        "服务器版本(" + latestVersion + ")与客户端版本不一致，建议更新。\n更新地址: " + updateUrl,
                        "版本提示",
                        JOptionPane.INFORMATION_MESSAGE
                );

                // 继续上传文件的逻辑
            } else {
                clientMain.log("连接服务器失败: " + response);
                return false;
            }

            // 发送文件
            File file = new File(filePath);
            clientMain.log("准备发送文件: " + file.getName() + "，大小: " + file.length() + " 字节");
            byte[] fileData;
            try {
                fileData = Files.readAllBytes(file.toPath());
                clientMain.log("文件读取成功");
            } catch (IOException e) {
                clientMain.log("读取文件失败: " + e.getMessage());
                return false;
            }

            // 计算CRC32校验值
            String crc32 = calculateCRC32(fileData);
            clientMain.log("文件CRC32校验值: " + crc32);

            // Base64编码
            String base64Encoded = Base64.getEncoder().encodeToString(fileData);
            clientMain.log("Base64编码完成，编码后大小: " + base64Encoded.length() + " 字符");

            // 发送文件名
            clientMain.log("发送文件名: " + file.getName());
            writer.println(file.getName());

            // 分块发送Base64数据
            int chunkSize = 1024;
            int totalChunks = (int) Math.ceil(base64Encoded.length() / (double) chunkSize);
            clientMain.log("开始分块发送数据，共 " + totalChunks + " 块");

            for (int i = 0; i < base64Encoded.length(); i += chunkSize) {
                int endIndex = Math.min(i + chunkSize, base64Encoded.length());
                writer.println(base64Encoded.substring(i, endIndex));

                // 更新进度条
                int progress = (int) ((i / (double) base64Encoded.length()) * 100);
                SwingUtilities.invokeLater(() ->
                        clientMain.getProgressBar().setValue(progress)
                );

                if (i % (chunkSize * 10) == 0) {
                    clientMain.log("已发送 " + (i / chunkSize) + " 块，进度: " + progress + "%");
                }
            }

            clientMain.log("数据块发送完成");

            // 发送空行表示数据结束
            writer.println();
            clientMain.log("发送数据结束标记");

            // 发送CRC32校验值
            writer.println(crc32);
            clientMain.log("发送CRC32校验值: " + crc32);

            // 接收服务器响应
            String serverResponse = reader.readLine();
            clientMain.log("接收到服务器响应: " + serverResponse);

            if (serverResponse == null) {
                clientMain.log("错误: 服务器没有返回传输结果");
                clientMain.log("服务器连接信息: " + serverHost + ":" + serverPort);
                return false;
            }

            boolean success = "TRANSFER_SUCCESS".equals(serverResponse);
            clientMain.log("传输" + (success ? "成功" : "失败"));
            return success;
        } catch (Exception e) {
            clientMain.log("发送文件时出错: " + e.getMessage());
            clientMain.log("错误类型: " + e.getClass().getName());

            // 打印详细堆栈信息到日志
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            clientMain.log("详细错误信息:\n" + sw.toString());

            e.printStackTrace();
            return false;
        }
    }

    private String calculateCRC32(byte[] data) {
        CRC32 crc32 = new CRC32();
        crc32.update(data);
        return Long.toHexString(crc32.getValue());
    }

    // 添加这两个方法到类的末尾
    public String getServerHost() {
        return serverHost;
    }

    public int getServerPort() {
        return serverPort;
    }
}