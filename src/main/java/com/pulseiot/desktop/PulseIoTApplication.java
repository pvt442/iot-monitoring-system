package com.pulseiot.desktop;

import javax.swing.*;
import java.awt.*;

/** Điểm khởi chạy ứng dụng Desktop PULSE IoT (Java Swing). */
public class PulseIoTApplication {
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) { }
        System.setProperty("awt.useSystemAAFontSettings", "on");
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Pulse IoT – Real-time Sensor Monitoring");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setIconImage(Shell.appIcon());
            f.setContentPane(new Shell());
            f.setMinimumSize(new Dimension(1080, 700));
            f.setSize(1400, 880);
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}
