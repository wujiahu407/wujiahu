package org.base64CodePro.client;

import java.io.File;
import java.io.IOException;

public class ApplicationRestarter {
    public static void restartApplication() {
        try {
            // 获取当前Java进程的运行时
            final String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
            final File currentJar = new File(ApplicationRestarter.class.getProtectionDomain().getCodeSource().getLocation().toURI());

            // 检查当前运行的是否是JAR文件
            if (!currentJar.getName().endsWith(".jar")) {
                System.err.println("无法重启应用：当前不是运行在JAR文件中");
                return;
            }

            // 构建重启命令
            final StringBuilder cmd = new StringBuilder();
            cmd.append(javaBin).append(" -jar ").append(currentJar.getPath());

            // 执行重启命令
            Runtime.getRuntime().exec(cmd.toString());

            // 退出当前应用
            System.exit(0);
        } catch (Exception e) {
            System.err.println("重启应用失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}    