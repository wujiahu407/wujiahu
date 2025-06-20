package org.base64CodePro.server;

import java.io.File;
import java.sql.*;

public class SQLiteManager {
    private static final String DB_URL = "jdbc:sqlite:D:/soft/sqlite/data/t.db";

    // 文件记录表
    public static final String FILE_TABLE = "file_records";
    // 客户端信息表
    public static final String CLIENT_TABLE = "client_info";
    // 系统配置表
    public static final String CONFIG_TABLE = "system_config";

    public static void initialize() {

        createFileTable();

        createClientTable();

        createConfigTable();

        initializeConfig();
    }

    private static void createFileTable() {
        String sql = "CREATE TABLE IF NOT EXISTS " + FILE_TABLE + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "file_name TEXT NOT NULL, " +
                "save_path TEXT NOT NULL, " +
                "upload_time DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "crc32_hash TEXT, " +
                "transfer_status BOOLEAN, " +
                "file_size LONG, " +
                "client_ip TEXT, " +
                "client_id TEXT" +
                ")";
        executeUpdate(sql);
    }

    private static void createClientTable() {
        String sql = "CREATE TABLE IF NOT EXISTS " + CLIENT_TABLE + " (" +
                "client_id TEXT PRIMARY KEY, " +
                "first_connect_time DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "last_connect_time DATETIME, " +
                "total_transfers INTEGER DEFAULT 0, " +
                "client_version TEXT" +
                ")";
        executeUpdate(sql);
    }

    private static void createConfigTable() {
        String sql = "CREATE TABLE IF NOT EXISTS " + CONFIG_TABLE + " (" +
                "config_key TEXT PRIMARY KEY, " +
                "config_value TEXT, " +
                "description TEXT" +
                ")";
        executeUpdate(sql);
    }

    private static void initializeConfig() {
        // 初始化默认配置
        insertConfigIfNotExists("server_version", "1.0.0", "服务器版本号");
        insertConfigIfNotExists("update_url", "http://example.com/client_update.jar", "客户端更新URL");
        insertConfigIfNotExists("max_file_size", "1073741824", "最大文件大小(字节)");
    }

    private static void insertConfigIfNotExists(String key, String value, String description) {
        String selectSql = "SELECT config_value FROM " + CONFIG_TABLE + " WHERE config_key = ?";
        String insertSql = "INSERT INTO " + CONFIG_TABLE + " (config_key, config_value, description) VALUES (?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(selectSql)) {

            pstmt.setString(1, key);
            ResultSet rs = pstmt.executeQuery();

            if (!rs.next()) {
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    insertStmt.setString(1, key);
                    insertStmt.setString(2, value);
                    insertStmt.setString(3, description);
                    insertStmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void saveFileRecord(String fileName, String savePath, String crc32, boolean status) {
        String sql = "INSERT INTO " + FILE_TABLE + " (file_name, save_path, crc32_hash, transfer_status, file_size) " +
                "VALUES (?, ?, ?, ?, ?)";

        File file = new File(savePath);
        long fileSize = file.exists() ? file.length() : -1;

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, fileName);
            pstmt.setString(2, savePath);
            pstmt.setString(3, crc32);
            pstmt.setBoolean(4, status);
            pstmt.setLong(5, fileSize);

            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void executeUpdate(String sql) {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        try {
            // 确保SQLite JDBC驱动已加载
            Class.forName("org.sqlite.JDBC");
            return DriverManager.getConnection(DB_URL);
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite驱动加载失败: " + e.getMessage());
            throw new SQLException("无法加载SQLite驱动", e);
        } catch (SQLException e) {
            System.err.println("数据库连接失败: " + e.getMessage() + ", 路径: " + DB_URL);
            throw e;
        }
    }
}