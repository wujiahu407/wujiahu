package org.base64CodePro.server;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerMain extends JFrame {
    private static final int PORT = 8888;
    private static final int THREAD_POOL_SIZE = 10;
    private final ExecutorService threadPool;
    private ServerSocket serverSocket;
    private boolean isRunning;

    private JButton startButton;
    private JButton stopButton;
    private JTextArea logArea;

    public ServerMain() {
        threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        SQLiteManager.initialize();
        initComponents();
    }

    private void initComponents() {
        setTitle("文件传输服务器");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel buttonPanel = new JPanel();
        startButton = new JButton("启动服务器");
        stopButton = new JButton("停止服务器");
        stopButton.setEnabled(false);

        buttonPanel.add(startButton);
        buttonPanel.add(stopButton);

        logArea = new JTextArea(20, 60);
        logArea.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(logArea);

        getContentPane().add(buttonPanel, BorderLayout.NORTH);
        getContentPane().add(logScrollPane, BorderLayout.CENTER);

        startButton.addActionListener(e -> {
            startButton.setEnabled(false);
            // 使用SwingWorker在后台线程启动服务器
            new ServerStarter().execute();
        });

        stopButton.addActionListener(e -> stopServer());
    }

    private void startServer() {
        try {
            serverSocket = new ServerSocket(PORT);
            isRunning = true;
            log("服务器已启动，监听端口: " + PORT);

            // 更新按钮状态（在EDT中执行）
            SwingUtilities.invokeLater(() -> stopButton.setEnabled(true));

            while (isRunning) {
                Socket clientSocket = serverSocket.accept();
                log("客户端连接成功: " + clientSocket.getInetAddress());
                threadPool.submit(new ClientHandler(clientSocket));
            }
        } catch (IOException ex) {
            if (isRunning) {
                log("服务器异常: " + ex.getMessage());
            }
        } finally {
            stopServer();
        }
    }

    private void stopServer() {
        isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                log("服务器已停止");
            }
        } catch (IOException e) {
            log("关闭服务器时出错: " + e.getMessage());
        } finally {
            // 确保按钮状态正确（在EDT中执行）
            SwingUtilities.invokeLater(() -> {
                startButton.setEnabled(true);
                stopButton.setEnabled(false);
            });
        }
    }

    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append("[" + LocalDateTime.now() + "] " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    // 使用SwingWorker在后台线程启动服务器
    private class ServerStarter extends SwingWorker<Void, Void> {
        @Override
        protected Void doInBackground() {
            startServer();
            return null;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ServerMain server = new ServerMain();
            server.setVisible(true);
        });
    }
}