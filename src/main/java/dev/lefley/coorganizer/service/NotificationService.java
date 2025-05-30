package dev.lefley.coorganizer.service;

import burp.api.montoya.MontoyaApi;

import javax.swing.*;
import java.awt.*;

public class NotificationService {
    private final MontoyaApi api;
    
    public NotificationService(MontoyaApi api) {
        this.api = api;
    }
    
    public void showToast(String message) {
        api.logging().logToOutput("Showing toast notification: " + message);
        
        SwingUtilities.invokeLater(() -> {
            try {
                Window parentWindow = findBurpWindow();
                createToastWindow(message, parentWindow);
            } catch (Exception e) {
                api.logging().logToError("Failed to show toast notification: " + e.getMessage());
            }
        });
    }
    
    private Window findBurpWindow() {
        Window[] windows = Window.getWindows();
        
        for (Window window : windows) {
            if (window.isVisible() && window instanceof JFrame) {
                JFrame frame = (JFrame) window;
                if (frame.getTitle().contains("Burp Suite")) {
                    return window;
                }
            }
        }
        
        return windows.length > 0 ? windows[0] : null;
    }
    
    private void createToastWindow(String message, Window parentWindow) {
        JWindow toastWindow = new JWindow();
        toastWindow.setAlwaysOnTop(true);
        
        JPanel toastPanel = createToastPanel(message);
        toastWindow.add(toastPanel);
        toastWindow.pack();
        
        positionToast(toastWindow, parentWindow);
        
        toastWindow.setVisible(true);
        
        Timer timer = new Timer(4000, e -> {
            toastWindow.setVisible(false);
            toastWindow.dispose();
        });
        timer.setRepeats(false);
        timer.start();
        
        api.logging().logToOutput("Toast notification displayed successfully");
    }
    
    private JPanel createToastPanel(String message) {
        JPanel toastPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                int arc = 12;
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
                
                g2.setColor(Color.GRAY);
                g2.setStroke(new BasicStroke(1));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
                
                g2.dispose();
            }
        };
        
        toastPanel.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        toastPanel.setLayout(new BorderLayout());
        toastPanel.setOpaque(false);
        
        JLabel messageLabel = new JLabel(message);
        messageLabel.setFont(messageLabel.getFont().deriveFont(Font.PLAIN, 12f));
        toastPanel.add(messageLabel, BorderLayout.CENTER);
        
        String iconText = message.toLowerCase().contains("failed") || message.toLowerCase().contains("error") ? "❌" : "✅";
        JLabel iconLabel = new JLabel(iconText);
        iconLabel.setFont(iconLabel.getFont().deriveFont(14f));
        iconLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));
        toastPanel.add(iconLabel, BorderLayout.WEST);
        
        return toastPanel;
    }
    
    private void positionToast(JWindow toastWindow, Window parentWindow) {
        if (parentWindow != null) {
            Rectangle parentBounds = parentWindow.getBounds();
            int x = parentBounds.x + parentBounds.width - toastWindow.getWidth() - 20;
            int y = parentBounds.y + 60;
            toastWindow.setLocation(x, y);
        } else {
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            Dimension screenSize = toolkit.getScreenSize();
            int x = screenSize.width - toastWindow.getWidth() - 20;
            int y = 60;
            toastWindow.setLocation(x, y);
        }
    }
}