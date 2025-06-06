package dev.lefley.coorganizer.service;

import burp.api.montoya.MontoyaApi;
import dev.lefley.coorganizer.ui.components.BurpIcon;
import dev.lefley.coorganizer.ui.components.BurpIconFile;
import dev.lefley.coorganizer.util.Logger;

import javax.swing.*;
import java.awt.*;

public class NotificationService {
    // Toast timing constants
    private static final int TOAST_DISPLAY_DURATION_MS = 1000;
    private static final int FADE_STEPS = 20;
    private static final int FADE_DELAY_MS = 50;
    
    private final MontoyaApi api;
    private final Logger logger;
    
    public NotificationService(MontoyaApi api) {
        this.api = api;
        this.logger = new Logger(api, NotificationService.class);
    }
    
    public void showSuccessToast(String message) {
        showToast(message, ToastType.SUCCESS);
    }
    
    public void showErrorToast(String message) {
        showToast(message, ToastType.ERROR);
    }
    
    private void showToast(String message, ToastType type) {
        SwingUtilities.invokeLater(() -> {
            try {
                Window parentWindow = findBurpWindow();
                createToastWindow(message, type, parentWindow);
            } catch (Exception e) {
                // Silently fail - toast notifications are not critical
                logger.error("Failed to show toast notification", e);
            }
        });
    }
    
    private enum ToastType {
        SUCCESS, ERROR
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
    
    private void createToastWindow(String message, ToastType type, Window parentWindow) {
        JWindow toastWindow = new JWindow();
        toastWindow.setAlwaysOnTop(true);
        
        JPanel toastPanel = createToastPanel(message, type);
        toastWindow.add(toastPanel);
        toastWindow.pack();
        
        positionToast(toastWindow, parentWindow);
        
        toastWindow.setVisible(true);
        
        // Show toast for specified duration, then fade out
        Timer showTimer = new Timer(TOAST_DISPLAY_DURATION_MS, e -> {
            try {
                startFadeOut(toastWindow);
            } catch (Exception ex) {
                logger.error("Error during toast fade out", ex);
                // Ensure window is disposed even if fade fails
                if (toastWindow.isDisplayable()) {
                    toastWindow.setVisible(false);
                    toastWindow.dispose();
                }
            }
        });
        showTimer.setRepeats(false);
        showTimer.start();
        
    }
    
    private JPanel createToastPanel(String message, ToastType type) {
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
        
        // Create icon based on toast type
        BurpIconFile iconFile = type == ToastType.ERROR ? BurpIconFile.WARNING : BurpIconFile.TICK;
        Icon icon = new BurpIcon.Builder(iconFile).fontSized().build();
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));
        toastPanel.add(iconLabel, BorderLayout.WEST);
        
        return toastPanel;
    }
    
    private void startFadeOut(JWindow toastWindow) {
        final float[] opacity = {1.0f};
        
        Timer fadeTimer = new Timer(FADE_DELAY_MS, null);
        fadeTimer.addActionListener(e -> {
            try {
                opacity[0] -= 1.0f / FADE_STEPS;
                
                if (opacity[0] <= 0.0f) {
                    // Fade complete - cleanup resources
                    fadeTimer.stop();
                    if (toastWindow.isDisplayable()) {
                        toastWindow.setVisible(false);
                        toastWindow.dispose();
                    }
                } else {
                    // Continue fading
                    if (toastWindow.isDisplayable()) {
                        toastWindow.setOpacity(Math.max(0.0f, opacity[0]));
                    } else {
                        // Window was disposed externally, stop timer
                        fadeTimer.stop();
                    }
                }
            } catch (Exception ex) {
                logger.error("Error during fade animation", ex);
                // Ensure cleanup on error
                fadeTimer.stop();
                if (toastWindow.isDisplayable()) {
                    toastWindow.setVisible(false);
                    toastWindow.dispose();
                }
            }
        });
        
        fadeTimer.start();
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