package dev.lefley.coorganizer.ui.dialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class CreateGroupDialog extends JDialog {
    private String groupName;
    private boolean confirmed = false;
    
    private JTextField nameField;
    private JButton createButton;
    private JButton cancelButton;
    
    public CreateGroupDialog(Window parent) {
        super(parent, "Create New Group", ModalityType.APPLICATION_MODAL);
        initializeComponents();
        layoutComponents();
        setupEventHandlers();
        setupDialog();
    }
    
    private void initializeComponents() {
        nameField = new JTextField(20);
        createButton = new JButton("Create Group");
        cancelButton = new JButton("Cancel");
        
        // Set default button
        getRootPane().setDefaultButton(createButton);
    }
    
    private void layoutComponents() {
        setLayout(new BorderLayout(10, 10));
        
        // Main content panel
        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
        
        GridBagConstraints gbc = new GridBagConstraints();
        
        // Title label
        JLabel titleLabel = new JLabel("Enter a name for your new group:");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(0, 0, 15, 0);
        gbc.anchor = GridBagConstraints.WEST;
        contentPanel.add(titleLabel, gbc);
        
        // Name field
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(0, 0, 15, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        contentPanel.add(nameField, gbc);
        
        add(contentPanel, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttonPanel.add(cancelButton);
        buttonPanel.add(createButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private void setupEventHandlers() {
        createButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleCreate();
            }
        });
        
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleCancel();
            }
        });
        
        // Handle Enter key in text field
        nameField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleCreate();
            }
        });
        
        // Enable/disable create button based on input
        nameField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updateCreateButton(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updateCreateButton(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updateCreateButton(); }
        });
    }
    
    private void setupDialog() {
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setResizable(false);
        pack();
        setLocationRelativeTo(getParent());
        
        // Focus on name field when dialog opens
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                nameField.requestFocusInWindow();
            }
        });
        
        updateCreateButton();
    }
    
    private void updateCreateButton() {
        String name = nameField.getText().trim();
        createButton.setEnabled(!name.isEmpty() && name.length() >= 2);
    }
    
    private void handleCreate() {
        String name = nameField.getText().trim();
        
        if (name.isEmpty()) {
            showError("Group name cannot be empty");
            return;
        }
        
        if (name.length() < 2) {
            showError("Group name must be at least 2 characters long");
            return;
        }
        
        if (name.length() > 50) {
            showError("Group name must be less than 50 characters long");
            return;
        }
        
        // Validate characters (alphanumeric, spaces, hyphens, underscores)
        if (!name.matches("^[a-zA-Z0-9\\s\\-_]+$")) {
            showError("Group name can only contain letters, numbers, spaces, hyphens, and underscores");
            return;
        }
        
        this.groupName = name;
        this.confirmed = true;
        dispose();
    }
    
    private void handleCancel() {
        this.confirmed = false;
        dispose();
    }
    
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Invalid Group Name", JOptionPane.ERROR_MESSAGE);
        nameField.requestFocusInWindow();
    }
    
    public String getGroupName() {
        return groupName;
    }
    
    public boolean isConfirmed() {
        return confirmed;
    }
    
    public static String showDialog(Window parent) {
        CreateGroupDialog dialog = new CreateGroupDialog(parent);
        dialog.setVisible(true);
        
        if (dialog.isConfirmed()) {
            return dialog.getGroupName();
        }
        return null;
    }
}