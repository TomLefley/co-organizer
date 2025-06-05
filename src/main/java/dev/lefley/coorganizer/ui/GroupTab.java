package dev.lefley.coorganizer.ui;

import burp.api.montoya.MontoyaApi;
import dev.lefley.coorganizer.model.Group;
import dev.lefley.coorganizer.service.GroupManager;
import dev.lefley.coorganizer.service.NotificationService;
import dev.lefley.coorganizer.ui.components.BurpIcon;
import dev.lefley.coorganizer.ui.components.BurpIconFile;
import dev.lefley.coorganizer.ui.components.SimpleIconButton;
import dev.lefley.coorganizer.ui.dialog.CreateGroupDialog;
import dev.lefley.coorganizer.ui.dialog.JoinGroupDialog;
import dev.lefley.coorganizer.util.Logger;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class GroupTab extends JPanel implements GroupManager.GroupManagerListener {
    private final MontoyaApi api;
    private final GroupManager groupManager;
    private final NotificationService notificationService;
    private final Logger logger;
    
    private JTable groupTable;
    private GroupTableModel tableModel;
    private SimpleIconButton createButton;
    private SimpleIconButton joinButton;
    private SimpleIconButton moveUpButton;
    private SimpleIconButton moveDownButton;
    private SimpleIconButton copyInviteButton;
    private SimpleIconButton leaveButton;
    
    public GroupTab(MontoyaApi api) {
        this.api = api;
        this.groupManager = new GroupManager(api);
        this.notificationService = new NotificationService(api);
        this.logger = new Logger(api, GroupTab.class);
        
        initializeComponents();
        layoutComponents();
        setupEventHandlers();
        
        // Register as listener for group changes
        groupManager.addListener(this);
        
        logger.debug("Group tab initialized");
    }
    
    private void initializeComponents() {
        // Create table model and table
        tableModel = new GroupTableModel();
        groupTable = new JTable(tableModel);
        
        // Configure table
        groupTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        groupTable.setRowHeight(25);
        groupTable.getTableHeader().setReorderingAllowed(false);
        
        // Set column widths
        TableColumnModel columnModel = groupTable.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(300); // Name
        columnModel.getColumn(1).setPreferredWidth(150); // Created
        
        // Create buttons
        createButton = new SimpleIconButton(new BurpIcon.Builder(BurpIconFile.ADD).fontSized().build());
        createButton.setToolTipText("Create a new collaboration group");
        
        joinButton = new SimpleIconButton(new BurpIcon.Builder(BurpIconFile.LOGIN).fontSized().build());
        joinButton.setToolTipText("Join an existing group using an invite code");
        
        moveUpButton = new SimpleIconButton(new BurpIcon.Builder(BurpIconFile.UP).fontSized().build());
        moveUpButton.setToolTipText("Move selected group up in the list");
        moveUpButton.setEnabled(false);
        
        moveDownButton = new SimpleIconButton(new BurpIcon.Builder(BurpIconFile.DOWN).fontSized().build());
        moveDownButton.setToolTipText("Move selected group down in the list");
        moveDownButton.setEnabled(false);
        
        copyInviteButton = new SimpleIconButton(new BurpIcon.Builder(BurpIconFile.COPY).fontSized().build());
        copyInviteButton.setToolTipText("Copy invite code for selected group to clipboard");
        copyInviteButton.setEnabled(false);
        
        leaveButton = new SimpleIconButton(new BurpIcon.Builder(BurpIconFile.CLOSE).fontSized().build());
        leaveButton.setToolTipText("Leave selected group");
        leaveButton.setEnabled(false);
    }
    
    private void layoutComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Title panel with toolbar
        JPanel titlePanel = new JPanel(new BorderLayout());
        
        // Left side - title and subtitle
        JPanel titleTextPanel = new JPanel();
        titleTextPanel.setLayout(new BoxLayout(titleTextPanel, BoxLayout.Y_AXIS));
        
        JLabel titleLabel = new JLabel("Co-Organizer Groups");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        titleTextPanel.add(titleLabel);
        
        JLabel subtitleLabel = new JLabel("Groups can be used to control who can open your sharing links. Manage them here.");
        subtitleLabel.setFont(subtitleLabel.getFont().deriveFont(Font.PLAIN, 12f));
        subtitleLabel.setForeground(Color.GRAY);
        titleTextPanel.add(subtitleLabel);
        
        titlePanel.add(titleTextPanel, BorderLayout.WEST);
        
        // Right side - toolbar buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        buttonPanel.add(createButton);
        buttonPanel.add(joinButton);
        buttonPanel.add(new JSeparator(SwingConstants.VERTICAL));
        buttonPanel.add(copyInviteButton);
        buttonPanel.add(leaveButton);
        buttonPanel.add(new JSeparator(SwingConstants.VERTICAL));
        buttonPanel.add(moveUpButton);
        buttonPanel.add(moveDownButton);
        
        titlePanel.add(buttonPanel, BorderLayout.EAST);
        
        add(titlePanel, BorderLayout.NORTH);
        
        // Table panel with scroll pane
        JScrollPane scrollPane = new JScrollPane(groupTable);
        scrollPane.setPreferredSize(new Dimension(600, 300));
        add(scrollPane, BorderLayout.CENTER);
    }
    
    private void setupEventHandlers() {
        createButton.addClickListener(this::handleCreateGroup);
        joinButton.addClickListener(this::handleJoinGroup);
        moveUpButton.addClickListener(this::handleMoveUpSelected);
        moveDownButton.addClickListener(this::handleMoveDownSelected);
        copyInviteButton.addClickListener(this::handleCopyInviteSelected);
        leaveButton.addClickListener(this::handleLeaveGroupSelected);
        
        // Table selection listener
        groupTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateButtonStates();
            }
        });
    }
    
    private void handleCreateGroup() {
        String groupName = CreateGroupDialog.showDialog(SwingUtilities.getWindowAncestor(this));
        
        if (groupName != null) {
            // Run group creation in background thread to avoid blocking EDT
            new Thread(() -> {
                try {
                    Group group = groupManager.createGroup(groupName);
                    SwingUtilities.invokeLater(() -> {
                        notificationService.showSuccessToast("Created group '" + groupName + "'");
                        logger.info("Successfully created group: " + groupName);
                    });
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> {
                        String errorMsg = "Failed to create group: " + e.getMessage();
                        notificationService.showErrorToast(errorMsg);
                        logger.error(errorMsg);
                    });
                }
            }).start();
        }
    }
    
    private void handleJoinGroup() {
        String inviteCode = JoinGroupDialog.showDialog(SwingUtilities.getWindowAncestor(this));
        
        if (inviteCode != null) {
            // Run group joining in background thread to avoid blocking EDT
            new Thread(() -> {
                try {
                    Group group = groupManager.joinGroup(inviteCode);
                    SwingUtilities.invokeLater(() -> {
                        notificationService.showSuccessToast("Joined group '" + group.getName() + "'");
                        logger.info("Successfully joined group: " + group.getName());
                    });
                } catch (GroupManager.InvalidInviteException e) {
                    SwingUtilities.invokeLater(() -> {
                        notificationService.showErrorToast(e.getMessage());
                        logger.error("Failed to join group: " + e.getMessage());
                        
                        JOptionPane.showMessageDialog(this, e.getMessage(), "Failed to join group", JOptionPane.ERROR_MESSAGE);
                    });
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> {
                        String errorMsg = "Failed to join group: " + e.getMessage();
                        notificationService.showErrorToast(errorMsg);
                        logger.error(errorMsg);
                    });
                }
            }).start();
        }
    }
    
    private void handleCopyInvite(Group group) {
        // Run clipboard operation in background thread to avoid blocking EDT
        new Thread(() -> {
            try {
                groupManager.copyInviteToClipboard(group);
                SwingUtilities.invokeLater(() -> {
                    notificationService.showSuccessToast("Copied invite code for '" + group.getName() + "'");
                    logger.info("Copied invite code for group: " + group.getName());
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    String errorMsg = "Failed to copy invite code: " + e.getMessage();
                    notificationService.showErrorToast(errorMsg);
                    logger.error(errorMsg);
                });
            }
        }).start();
    }
    
    private void handleLeaveGroup(Group group) {
        int result = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to leave the group '" + group.getName() + "'?\n\nThis action cannot be undone.",
            "Confirm Leave Group",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        
        if (result == JOptionPane.YES_OPTION) {
            try {
                groupManager.leaveGroup(group);
                notificationService.showSuccessToast("Left group '" + group.getName() + "'");
                logger.info("Successfully left group: " + group.getName());
            } catch (Exception e) {
                String errorMsg = "Failed to leave group: " + e.getMessage();
                notificationService.showErrorToast(errorMsg);
                logger.error(errorMsg);
            }
        }
    }
    
    private void handleMoveUpSelected() {
        int selectedRow = groupTable.getSelectedRow();
        if (selectedRow > 0) {
            groupManager.moveGroup(selectedRow, selectedRow - 1);
            groupTable.getSelectionModel().setSelectionInterval(selectedRow - 1, selectedRow - 1);
        }
    }
    
    private void handleMoveDownSelected() {
        int selectedRow = groupTable.getSelectedRow();
        if (selectedRow >= 0 && selectedRow < groupManager.getGroups().size() - 1) {
            groupManager.moveGroup(selectedRow, selectedRow + 1);
            groupTable.getSelectionModel().setSelectionInterval(selectedRow + 1, selectedRow + 1);
        }
    }
    
    private void handleCopyInviteSelected() {
        int selectedRow = groupTable.getSelectedRow();
        if (selectedRow >= 0) {
            Group group = tableModel.getGroupAt(selectedRow);
            handleCopyInvite(group);
        }
    }
    
    private void handleLeaveGroupSelected() {
        int selectedRow = groupTable.getSelectedRow();
        if (selectedRow >= 0) {
            Group group = tableModel.getGroupAt(selectedRow);
            handleLeaveGroup(group);
        }
    }
    
    private void updateButtonStates() {
        int selectedRow = groupTable.getSelectedRow();
        boolean hasSelection = selectedRow >= 0;
        int groupCount = groupManager.getGroups().size();
        
        moveUpButton.setEnabled(hasSelection && selectedRow > 0);
        moveDownButton.setEnabled(hasSelection && selectedRow < groupCount - 1);
        copyInviteButton.setEnabled(hasSelection);
        leaveButton.setEnabled(hasSelection);
    }
    
    @Override
    public void onGroupAdded(Group group) {
        SwingUtilities.invokeLater(() -> {
            logger.trace("Updating UI after group added: " + group.getName());
            tableModel.fireTableDataChanged();
            updateButtonStates();
        });
    }
    
    @Override
    public void onGroupRemoved(Group group) {
        SwingUtilities.invokeLater(() -> {
            logger.trace("Updating UI after group removed: " + group.getName());
            tableModel.fireTableDataChanged();
            updateButtonStates();
        });
    }
    
    // Table model for groups
    private class GroupTableModel extends AbstractTableModel {
        private final String[] columnNames = {"Group name", "Added at"};
        
        @Override
        public int getRowCount() {
            return groupManager.getGroups().size();
        }
        
        @Override
        public int getColumnCount() {
            return columnNames.length;
        }
        
        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }
        
        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Group group = groupManager.getGroups().get(rowIndex);
            
            switch (columnIndex) {
                case 0: return group.getName();
                case 1: 
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm");
                    return sdf.format(new Date(group.getCreatedAt()));
                default: return "";
            }
        }
        
        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false; // No columns are editable
        }
        
        public Group getGroupAt(int rowIndex) {
            return groupManager.getGroups().get(rowIndex);
        }
    }
}