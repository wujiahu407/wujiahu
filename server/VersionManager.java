package org.base64CodePro.server;

import java.sql.*;

public class VersionManager {
    public static String getVersion() {
        String sql = "SELECT config_value FROM " + SQLiteManager.CONFIG_TABLE + " WHERE config_key = 'server_version'";
        try (Connection conn = SQLiteManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                return rs.getString("config_value");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "1.0.0";
    }

    public static String getUpdateUrl() {
        String sql = "SELECT config_value FROM " + SQLiteManager.CONFIG_TABLE + " WHERE config_key = 'update_url'";
        try (Connection conn = SQLiteManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                return rs.getString("config_value");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "http://example.com/client_update.jar";
    }
}    