package org.base64CodePro.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Properties;

public class ClientMain extends JFrame {
    public static final String VERSION = "1.0.3";
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8888;

    private JTextField fileField;
    private JButton browseButton;
    private JButton sendButton;
    private JTextArea logArea;
    private JLabel statusLabel;
    private JProgressBar progressBar;

    private FileTransferClient fileClient;
    private Properties config;

    public JProgressBar getProgressBar() {
        return progressBar;
    }

    public ClientMain() {
        loadConfig();
        initComponents();
        checkUpdate();
    }
    // 提供公共访问方法


    private void initComponents() {
        // 设置窗口图标和外观
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            log("设置外观失败: " + e.getMessage());
        }

        setTitle("文件传输客户端 v" );
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // 创建主面板，使用更现代的布局
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        mainPanel.setBackground(new Color(240, 240, 240));

        // 创建标题面板
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(70, 130, 180));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel titleLabel = new JLabel("Base64 文件传输系统");
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 36));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        // 文件选择面板
        JPanel filePanel = new JPanel(new BorderLayout(8, 8));
        filePanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                "选择文件",
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                new Font("微软雅黑", Font.BOLD, 32)
        ));
        filePanel.setBackground(new Color(250, 250, 250));

        fileField = new JTextField();
        fileField.setFont(new Font("微软雅黑", Font.PLAIN, 32));
        fileField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 180, 180)),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        browseButton = new JButton("浏览...");
        browseButton.setFont(new Font("微软雅黑", Font.BOLD, 24));
        browseButton.setBackground(new Color(100, 150, 200));
        browseButton.setForeground(Color.black);
        browseButton.setFocusPainted(false);

        sendButton = new JButton("发送文件");
        sendButton.setFont(new Font("微软雅黑", Font.BOLD, 24));
        sendButton.setBackground(new Color(70, 130, 180));
        sendButton.setForeground(Color.black);
        sendButton.setFocusPainted(false);

        filePanel.add(fileField, BorderLayout.CENTER);
        filePanel.add(browseButton, BorderLayout.EAST);

        // 日志面板
        logArea = new JTextArea(15, 50);
        logArea.setEditable(false);
        logArea.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        logArea.setBackground(new Color(252, 252, 252));
        logArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                "日志信息",
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                new Font("微软雅黑", Font.BOLD, 12)
        ));

        // 状态栏
        JPanel statusPanel = new JPanel(new BorderLayout(5, 5));
        statusPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        statusPanel.setBackground(new Color(240, 240, 240));

        statusLabel = new JLabel("就绪");
        statusLabel.setFont(new Font("微软雅黑", Font.PLAIN, 24));

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);
        progressBar.setBackground(new Color(240, 240, 240));
        progressBar.setForeground(new Color(70, 130, 180));

        statusPanel.add(statusLabel, BorderLayout.WEST);
        statusPanel.add(progressBar, BorderLayout.CENTER);

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(new Color(240, 240, 240));
        buttonPanel.add(sendButton);

        // 添加组件到主面板
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(filePanel, BorderLayout.CENTER);
        mainPanel.add(logScrollPane, BorderLayout.SOUTH);

        // 底部面板
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(new Color(240, 240, 240));
        bottomPanel.add(statusPanel, BorderLayout.CENTER);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel);

        // 添加事件监听器
        browseButton.addActionListener(new BrowseButtonListener());
        sendButton.addActionListener(new SendButtonListener());

        fileClient = new FileTransferClient(this);
    }

    private void loadConfig() {
        config = new Properties();
        try {
            config.load(getClass().getResourceAsStream("/client.properties"));
        } catch (IOException e) {
            log("加载配置文件失败: " + e.getMessage());
        }
    }

    private void checkUpdate() {
        new Thread(() -> {
            try {
                log("正在检查更新...");
                UpdateChecker checker = new UpdateChecker(SERVER_HOST, SERVER_PORT, VERSION);
                if (checker.isUpdateAvailable()) {
                    log("发现新版本: " + checker.getLatestVersion());
                    log("更新地址: " + checker.getUpdateUrl());

                    int choice = JOptionPane.showConfirmDialog(
                            ClientMain.this,
                            "发现新版本 " + checker.getLatestVersion() + "，是否更新？",
                            "更新提示",
                            JOptionPane.YES_NO_OPTION
                    );

                    if (choice == JOptionPane.YES_OPTION) {
                        UpdateDownloader downloader = new UpdateDownloader(
                                checker.getUpdateUrl(),
                                "client_update.jar",
                                progressBar,
                                statusLabel
                        );

                        if (downloader.download()) {
                            log("更新下载完成，重启程序以应用更新。");
                            JOptionPane.showMessageDialog(
                                    ClientMain.this,
                                    "更新下载完成，程序将重启以应用更新。",
                                    "更新成功",
                                    JOptionPane.INFORMATION_MESSAGE
                            );

                            // 重启应用
                            ApplicationRestarter.restartApplication();
                        }
                    }
                } else {
                    log("当前已是最新版本");
                }
            } catch (Exception e) {
                log("检查更新失败: " + e.getMessage());
            }
        }).start();
    }

    public void log(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ClientMain client = new ClientMain();
            client.setVisible(true);
        });
    }

    private class BrowseButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            JFileChooser fileChooser = new JFileChooser();
            int result = fileChooser.showOpenDialog(ClientMain.this);

            if (result == JFileChooser.APPROVE_OPTION) {
                fileField.setText(fileChooser.getSelectedFile().getAbsolutePath());
            }
        }
    }

    private class SendButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String filePath = fileField.getText().trim();
            if (filePath.isEmpty()) {
                JOptionPane.showMessageDialog(
                        ClientMain.this,
                        "请选择要上传的文件",
                        "错误",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            File file = new File(filePath);
            if (!file.exists()) {
                JOptionPane.showMessageDialog(
                        ClientMain.this,
                        "文件不存在: " + filePath,
                        "错误",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            // 禁用按钮防止重复点击
            sendButton.setEnabled(false);
            progressBar.setValue(0);
            progressBar.setVisible(true);
            statusLabel.setText("正在上传...");

            // 在新线程中执行文件传输
            new Thread(() -> {
                try {
                    log("开始上传文件: " + filePath);
                    log("服务器地址: " + fileClient.getServerHost() + ":" + fileClient.getServerPort());

                    boolean success = fileClient.sendFile(filePath);
                    if (success) {
                        log("文件上传成功: " + filePath);
                        statusLabel.setText("上传成功");
                        JOptionPane.showMessageDialog(
                                ClientMain.this,
                                "文件上传成功",
                                "上传成功",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                    } else {
                        log("文件上传失败: " + filePath);
                        statusLabel.setText("上传失败");
                        JOptionPane.showMessageDialog(
                                ClientMain.this,
                                "文件上传失败，请查看日志了解详情",
                                "上传失败",
                                JOptionPane.ERROR_MESSAGE
                        );
                    }
                } catch (Exception ex) {
                    log("上传过程中发生错误: " + ex.getMessage());
                    log("错误类型: " + ex.getClass().getName());

                    // 打印详细堆栈信息到日志
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    ex.printStackTrace(pw);
                    log("详细错误信息:\n" + sw.toString());

                    statusLabel.setText("上传错误");
                    JOptionPane.showMessageDialog(
                            ClientMain.this,
                            "上传过程中发生错误: " + ex.getMessage(),
                            "上传错误",
                            JOptionPane.ERROR_MESSAGE
                    );
                } finally {
                    // 恢复UI状态
                    SwingUtilities.invokeLater(() -> {
                        sendButton.setEnabled(true);
                        progressBar.setVisible(false);
                    });
                }
            }).start();
        }
    }

    public Properties getConfig() {
        return config;
    }
}