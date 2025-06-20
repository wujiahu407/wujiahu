package org.base64CodePro.server;

import java.io.*;
import java.net.Socket;
import java.util.Base64;
import java.util.zip.CRC32;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private static final String SAVE_DIR = "D:/received_files/";

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
        new File(SAVE_DIR).mkdirs();

    }

    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)) {

            // 握手协议
            String command = reader.readLine();

            if ("CONNECT".equals(command)) {
                writer.println("OK");

                // 接收客户端版本
                String clientVersion = reader.readLine();
                String serverVersion = VersionManager.getVersion();

                if (!serverVersion.equals(clientVersion)) {
                    writer.println("VERSION_DIFFERENT");  // 修改响应类型
                    writer.println(serverVersion);
                    writer.println(VersionManager.getUpdateUrl());

                    // 继续处理文件传输，不再终止
                    handleFileTransfer(reader, writer);
                } else {
                    writer.println("UPDATE_NOT_REQUIRED");

                    // 处理文件传输
                    handleFileTransfer(reader, writer);
                }
            }
        } catch (IOException e) {
            System.err.println("处理客户端连接时出错: " + e.getMessage());
            e.printStackTrace(); // 添加这行打印完整堆栈

            // 添加更详细的错误信息
            System.err.println("错误类型: " + e.getClass().getName());
            System.err.println("客户端地址: " + clientSocket.getInetAddress());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleFileTransfer(BufferedReader reader, PrintWriter writer) throws IOException {
        String fileName = reader.readLine();
        if (fileName == null) {
            return;
        }

        StringBuilder base64Builder = new StringBuilder();
        String line;
        while (!(line = reader.readLine()).isEmpty()) {
            base64Builder.append(line);
        }
        String base64Data = base64Builder.toString();

        String receivedCrc32 = reader.readLine();

        // 解码并保存文件
        byte[] fileData = Base64.getDecoder().decode(base64Data);
        String savePath = SAVE_DIR + fileName;

        try (FileOutputStream fos = new FileOutputStream(savePath)) {
            fos.write(fileData);
        }

        // 验证CRC32
        String calculatedCrc32 = calculateCRC32(fileData);
        boolean crcValid = calculatedCrc32.equals(receivedCrc32);

        // 保存到数据库
        SQLiteManager.saveFileRecord(fileName, savePath, crcValid ? receivedCrc32 : null, crcValid);

        // 响应客户端
        writer.println(crcValid ? "TRANSFER_SUCCESS" : "TRANSFER_FAILED");
    }

    private String calculateCRC32(byte[] data) {
        CRC32 crc32 = new CRC32();
        crc32.update(data);
        return Long.toHexString(crc32.getValue());
    }
}