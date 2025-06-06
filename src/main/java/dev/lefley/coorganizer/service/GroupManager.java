package dev.lefley.coorganizer.service;

import burp.api.montoya.MontoyaApi;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import dev.lefley.coorganizer.crypto.CryptoUtils;
import dev.lefley.coorganizer.model.Group;
import dev.lefley.coorganizer.model.GroupInvite;
import dev.lefley.coorganizer.util.Logger;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class GroupManager {
    private static final String PREFERENCES_KEY_GROUPS = "co-organizer.groups";
    private static final String INVITE_MESSAGE_PREFIX = "Join my Co-Organizer group so that we can start sharing findings today! ";
    
    private final MontoyaApi api;
    private final Logger logger;
    private final Gson gson;
    private final List<Group> groups;
    private final List<GroupManagerListener> listeners;
    
    public GroupManager(MontoyaApi api) {
        this.api = api;
        this.logger = new Logger(api, GroupManager.class);
        this.gson = new Gson();
        this.groups = new CopyOnWriteArrayList<>();
        this.listeners = new CopyOnWriteArrayList<>();
        
        loadGroupsFromPreferences();
    }
    
    public interface GroupManagerListener {
        void onGroupAdded(Group group);
        void onGroupRemoved(Group group);
    }
    
    public void addListener(GroupManagerListener listener) {
        listeners.add(listener);
    }
    
    public void removeListener(GroupManagerListener listener) {
        listeners.remove(listener);
    }
    
    public Group createGroup(String name) {
        logger.debug("Creating new group: " + name);
        
        // Validate group name
        String validationError = validateGroupName(name);
        if (validationError != null) {
            throw new IllegalArgumentException(validationError);
        }
        
        String symmetricKey = CryptoUtils.generateSymmetricKey();
        String fingerprint = CryptoUtils.generateFingerprint(name, symmetricKey);
        
        Group group = new Group(name, symmetricKey, fingerprint);
        groups.add(group);
        saveGroupsToPreferences();
        
        logger.info("Created group '" + name + "' with fingerprint: " + fingerprint);
        
        notifyGroupAdded(group);
        return group;
    }
    
    public Group joinGroup(String inviteInput) throws InvalidInviteException {
        logger.debug("Attempting to join group with invite input");
        
        // Validate invite input
        String validationError = validateInviteInput(inviteInput);
        if (validationError != null) {
            throw new InvalidInviteException(validationError);
        }
        
        try {
            // Extract invite code from input (handle both raw code and formatted message)
            String inviteCode = extractInviteCode(inviteInput);
            
            // Decode base64 invite code
            byte[] decodedBytes = Base64.getDecoder().decode(inviteCode);
            String jsonString = new String(decodedBytes);
            
            // Parse JSON to GroupInvite
            GroupInvite invite = gson.fromJson(jsonString, GroupInvite.class);
            
            if (invite.getName() == null || invite.getKey() == null || invite.getFingerprint() == null) {
                throw new InvalidInviteException("The group invite was malformed.");
            }
            
            // Create group from invite
            Group group = invite.toGroup();
            
            // Check if group already exists
            if (groups.contains(group)) {
                throw new InvalidInviteException("You are already a member of this group.");
            }
            
            groups.add(group);
            saveGroupsToPreferences();
            logger.info("Joined group '" + group.getName() + "' with fingerprint: " + group.getFingerprint());
            
            notifyGroupAdded(group);
            return group;
            
        } catch (IllegalArgumentException e) {
            // This typically happens when Base64 decoding fails
            logger.debug("Base64 decode failed for invite code: " + e.getMessage());
            throw new InvalidInviteException("The invite code contains invalid characters and cannot be decoded.", e);
        } catch (JsonSyntaxException e) {
            // This happens when the decoded content is not valid JSON
            logger.debug("JSON parsing failed for invite content: " + e.getMessage());
            throw new InvalidInviteException("The invite code does not contain valid group information.", e);
        } catch (Exception e) {
            // Catch any other unexpected exceptions
            logger.error("Unexpected error processing invite", e);
            throw new InvalidInviteException("An unexpected error occurred while processing the invite code.", e);
        }
    }
    
    public void leaveGroup(Group group) {
        logger.debug("Leaving group: " + group.getName());
        
        if (groups.remove(group)) {
            saveGroupsToPreferences();
            logger.info("Left group '" + group.getName() + "'");
            notifyGroupRemoved(group);
        } else {
            logger.error("Group not found: " + group.getName());
        }
    }
    
    public String generateInviteCode(Group group) {
        logger.debug("Generating invite code for group: " + group.getName());
        
        GroupInvite invite = new GroupInvite(group.getName(), group.getSymmetricKey(), group.getFingerprint());
        String jsonString = gson.toJson(invite);
        String base64Code = Base64.getEncoder().encodeToString(jsonString.getBytes());
        
        logger.debug("Generated invite code for group '" + group.getName() + "'");
        return base64Code;
    }
    
    public void copyInviteToClipboard(Group group) {
        try {
            String inviteCode = generateInviteCode(group);
            String inviteMessage = INVITE_MESSAGE_PREFIX + inviteCode;
            
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            StringSelection selection = new StringSelection(inviteMessage);
            clipboard.setContents(selection, null);
            
            logger.info("Copied invite message to clipboard for group: " + group.getName());
        } catch (SecurityException e) {
            logger.error("Permission denied accessing system clipboard", e);
        } catch (IllegalStateException e) {
            logger.error("System clipboard unavailable", e);
        } catch (Exception e) {
            logger.error("Unexpected error copying to clipboard", e);
        }
    }
    
    public List<Group> getGroups() {
        return new ArrayList<>(groups);
    }
    
    public boolean hasGroup(String fingerprint) {
        return groups.stream().anyMatch(group -> group.getFingerprint().equals(fingerprint));
    }
    
    public void refreshGroupsFromPreferences() {
        groups.clear();
        loadGroupsFromPreferences();
        logger.debug("Refreshed groups from preferences. Current group count: " + groups.size());
    }
    
    public synchronized void moveGroup(int fromIndex, int toIndex) {
        if (fromIndex >= 0 && fromIndex < groups.size() && toIndex >= 0 && toIndex < groups.size() && fromIndex != toIndex) {
            Group group = groups.remove(fromIndex);
            groups.add(toIndex, group);
            saveGroupsToPreferences();
            logger.debug("Moved group '" + group.getName() + "' from position " + fromIndex + " to " + toIndex);
        }
    }
    
    private String extractInviteCode(String inviteInput) throws InvalidInviteException {
        if (inviteInput == null || inviteInput.trim().isEmpty()) {
            throw new InvalidInviteException("The group invite was malformed.");
        }
        
        String trimmedInput = inviteInput.trim();
        
        // Check if input contains our invite message format
        String prefixToMatch = INVITE_MESSAGE_PREFIX.trim();
        if (trimmedInput.contains(prefixToMatch)) {
            // Find the position after the prefix
            int prefixIndex = trimmedInput.indexOf(prefixToMatch);
            int codeStartIndex = prefixIndex + prefixToMatch.length();
            
            if (codeStartIndex < trimmedInput.length()) {
                return trimmedInput.substring(codeStartIndex).trim();
            } else {
                throw new InvalidInviteException("The group invite was malformed.");
            }
        }
        
        // If no message format detected, treat entire input as invite code
        return trimmedInput;
    }
    
    private void notifyGroupAdded(Group group) {
        for (GroupManagerListener listener : listeners) {
            try {
                listener.onGroupAdded(group);
            } catch (Exception e) {
                logger.error("Error notifying listener of group addition: " + e.getMessage());
            }
        }
    }
    
    private void notifyGroupRemoved(Group group) {
        for (GroupManagerListener listener : listeners) {
            try {
                listener.onGroupRemoved(group);
            } catch (Exception e) {
                logger.error("Error notifying listener of group removal: " + e.getMessage());
            }
        }
    }
    
    private void loadGroupsFromPreferences() {
        try {
            String groupsJson = api.persistence().preferences().getString(PREFERENCES_KEY_GROUPS);
            if (groupsJson != null && !groupsJson.trim().isEmpty()) {
                Type listType = new TypeToken<List<Group>>(){}.getType();
                List<Group> savedGroups = gson.fromJson(groupsJson, listType);
                if (savedGroups != null) {
                    groups.addAll(savedGroups);
                    logger.debug("Loaded " + savedGroups.size() + " groups from preferences");
                }
            } else {
                logger.debug("No saved groups found in preferences");
            }
        } catch (JsonSyntaxException e) {
            logger.error("Corrupted group data in preferences, starting fresh", e);
            // Clear corrupted data and start fresh
            api.persistence().preferences().setString(PREFERENCES_KEY_GROUPS, null);
        } catch (Exception e) {
            logger.error("Unexpected error loading groups from preferences", e);
        }
    }
    
    private void saveGroupsToPreferences() {
        try {
            String groupsJson = gson.toJson(groups);
            api.persistence().preferences().setString(PREFERENCES_KEY_GROUPS, groupsJson);
            logger.debug("Saved " + groups.size() + " groups to preferences");
        } catch (Exception e) {
            logger.error("Failed to save groups to preferences - group changes may be lost on restart", e);
        }
    }
    
    /**
     * Validate group name input.
     * @param name the group name to validate
     * @return null if valid, error message if invalid
     */
    private String validateGroupName(String name) {
        if (name == null) {
            return "Group name cannot be null";
        }
        
        String trimmed = name.trim();
        
        if (trimmed.isEmpty()) {
            return "Group name cannot be empty";
        }
        
        if (trimmed.length() < 2) {
            return "Group name must be at least 2 characters long";
        }
        
        if (trimmed.length() > 50) {
            return "Group name must be less than 50 characters long";
        }
        
        // Validate characters (alphanumeric, spaces, hyphens, underscores)
        if (!trimmed.matches("^[a-zA-Z0-9\\s\\-_]+$")) {
            return "Group name can only contain letters, numbers, spaces, hyphens, and underscores";
        }
        
        // Check for excessive whitespace
        if (trimmed.contains("  ")) {
            return "Group name cannot contain consecutive spaces";
        }
        
        // Check if group with same name already exists
        if (groups.stream().anyMatch(group -> group.getName().equalsIgnoreCase(trimmed))) {
            return "A group with this name already exists";
        }
        
        return null; // Valid
    }
    
    /**
     * Validate invite input.
     * @param inviteInput the invite input to validate
     * @return null if valid, error message if invalid
     */
    private String validateInviteInput(String inviteInput) {
        if (inviteInput == null) {
            return "Invite code cannot be null";
        }
        
        String trimmed = inviteInput.trim();
        
        if (trimmed.isEmpty()) {
            return "Invite code cannot be empty";
        }
        
        // Basic length check to prevent excessive input
        if (trimmed.length() > 10000) {
            return "Invite code is too long";
        }
        
        return null; // Valid - let the parsing logic handle the rest
    }
    
    public static class InvalidInviteException extends Exception {
        public InvalidInviteException(String message) {
            super(message);
        }
        
        public InvalidInviteException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}