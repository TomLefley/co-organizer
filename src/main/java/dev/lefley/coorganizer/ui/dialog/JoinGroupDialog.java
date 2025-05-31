package dev.lefley.coorganizer.ui.dialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class JoinGroupDialog extends JDialog {
    private String inviteCode;
    private boolean confirmed = false;
    
    private JTextArea codeArea;
    private JButton joinButton;
    private JButton cancelButton;
    
    public JoinGroupDialog(Window parent) {
        super(parent, "Join Group", ModalityType.APPLICATION_MODAL);
        initializeComponents();
        layoutComponents();
        setupEventHandlers();
        setupDialog();
    }
    
    private void initializeComponents() {
        codeArea = new JTextArea(4, 30);
        codeArea.setLineWrap(true);
        codeArea.setWrapStyleWord(true);
        codeArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        
        joinButton = new JButton("Join Group");
        cancelButton = new JButton("Cancel");
        
        // Set default button
        getRootPane().setDefaultButton(joinButton);
    }
    
    private void layoutComponents() {
        setLayout(new BorderLayout(10, 10));
        
        // Main content panel
        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
        
        GridBagConstraints gbc = new GridBagConstraints();
        
        // Title label
        JLabel titleLabel = new JLabel("Paste the group invite below:");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(0, 0, 15, 0);
        gbc.anchor = GridBagConstraints.WEST;
        contentPanel.add(titleLabel, gbc);
        
        // Code area
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(0, 0, 15, 0);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        contentPanel.add(codeArea, gbc);
        
        add(contentPanel, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttonPanel.add(cancelButton);
        buttonPanel.add(joinButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private void setupEventHandlers() {
        joinButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleJoin();
            }
        });
        
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleCancel();
            }
        });
        
        // Enable/disable join button based on input
        codeArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updateJoinButton(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updateJoinButton(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updateJoinButton(); }
        });
    }
    
    private void setupDialog() {
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setResizable(true);
        setSize(450, 300);
        setMinimumSize(new Dimension(400, 250));
        setLocationRelativeTo(getParent());
        
        // Focus on code area when dialog opens
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                codeArea.requestFocusInWindow();
            }
        });
        
        updateJoinButton();
    }
    
    private void updateJoinButton() {
        String code = codeArea.getText().trim();
        joinButton.setEnabled(!code.isEmpty());
    }
    
    private void handleJoin() {
        String code = codeArea.getText().trim();
        
        if (code.isEmpty()) {
            showError("Invite code cannot be empty");
            return;
        }
        
        // Pass raw input to GroupManager - it will handle both formatted messages and raw codes
        this.inviteCode = code;
        this.confirmed = true;
        dispose();
    }
    
    private void handleCancel() {
        this.confirmed = false;
        dispose();
    }
    
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Invalid Invite Code", JOptionPane.ERROR_MESSAGE);
        codeArea.requestFocusInWindow();
    }
    
    public String getInviteCode() {
        return inviteCode;
    }
    
    public boolean isConfirmed() {
        return confirmed;
    }
    
    public static String showDialog(Window parent) {
        JoinGroupDialog dialog = new JoinGroupDialog(parent);
        dialog.setVisible(true);
        
        if (dialog.isConfirmed()) {
            return dialog.getInviteCode();
        }
        return null;
    }
}