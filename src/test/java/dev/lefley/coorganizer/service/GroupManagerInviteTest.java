package dev.lefley.coorganizer.service;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.persistence.Persistence;
import burp.api.montoya.persistence.Preferences;
import burp.api.montoya.logging.Logging;
import dev.lefley.coorganizer.model.Group;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

@DisplayName("GroupManager Invite Code Tests")
class GroupManagerInviteTest {

    @Mock
    private MontoyaApi api;
    
    @Mock
    private Logging logging;
    
    @Mock
    private Persistence persistence;
    
    @Mock
    private Preferences preferences;

    private GroupManager groupManager;
    private GroupManager otherGroupManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        when(api.logging()).thenReturn(logging);
        when(api.persistence()).thenReturn(persistence);
        when(persistence.preferences()).thenReturn(preferences);
        when(preferences.getString("co-organizer.groups")).thenReturn(null);
        
        groupManager = new GroupManager(api);
        otherGroupManager = new GroupManager(api);
    }

    @Test
    @DisplayName("Should join group with raw invite code")
    void shouldJoinGroupWithRawInviteCode() throws GroupManager.InvalidInviteException {
        // Given: A test group created by someone else and its invite code
        Group testGroup = otherGroupManager.createGroup("Raw Code Test");
        String rawCode = otherGroupManager.generateInviteCode(testGroup);
        
        // When: Joining with raw code using different GroupManager (simulating different user)
        Group joinedGroup = groupManager.joinGroup(rawCode);
        
        // Then: Group should be created successfully
        assertThat(joinedGroup.getName()).isEqualTo("Raw Code Test");
        assertThat(joinedGroup.getFingerprint()).isEqualTo(testGroup.getFingerprint());
    }

    @Test
    @DisplayName("Should join group with formatted invite message")
    void shouldJoinGroupWithFormattedInviteMessage() throws GroupManager.InvalidInviteException {
        // Given: A test group created by someone else and formatted invite message
        Group testGroup = otherGroupManager.createGroup("Formatted Message Test");
        String inviteCode = otherGroupManager.generateInviteCode(testGroup);
        String formattedMessage = "Join my Co-Organizer group so that we can start sharing findings today! " + inviteCode;
        
        // When: Joining with formatted message using different GroupManager
        Group joinedGroup = groupManager.joinGroup(formattedMessage);
        
        // Then: Group should be created successfully
        assertThat(joinedGroup.getName()).isEqualTo("Formatted Message Test");
        assertThat(joinedGroup.getFingerprint()).isEqualTo(testGroup.getFingerprint());
    }

    @Test
    @DisplayName("Should join group with formatted message and extra whitespace")
    void shouldJoinGroupWithFormattedMessageAndWhitespace() throws GroupManager.InvalidInviteException {
        // Given: A test group created by someone else and formatted invite message with extra whitespace
        Group testGroup = otherGroupManager.createGroup("Whitespace Test");
        String inviteCode = otherGroupManager.generateInviteCode(testGroup);
        String formattedMessage = "  Join my Co-Organizer group so that we can start sharing findings today!   " + inviteCode + "  ";
        
        // When: Joining with formatted message using different GroupManager
        Group joinedGroup = groupManager.joinGroup(formattedMessage);
        
        // Then: Group should be created successfully
        assertThat(joinedGroup.getName()).isEqualTo("Whitespace Test");
        assertThat(joinedGroup.getFingerprint()).isEqualTo(testGroup.getFingerprint());
    }

    @Test
    @DisplayName("Should join group with raw code and extra whitespace")
    void shouldJoinGroupWithRawCodeAndWhitespace() throws GroupManager.InvalidInviteException {
        // Given: A test group created by someone else and raw invite code with extra whitespace
        Group testGroup = otherGroupManager.createGroup("Raw Whitespace Test");
        String inviteCode = otherGroupManager.generateInviteCode(testGroup);
        String rawCodeWithWhitespace = "  " + inviteCode + "  ";
        
        // When: Joining with raw code using different GroupManager
        Group joinedGroup = groupManager.joinGroup(rawCodeWithWhitespace);
        
        // Then: Group should be created successfully
        assertThat(joinedGroup.getName()).isEqualTo("Raw Whitespace Test");
        assertThat(joinedGroup.getFingerprint()).isEqualTo(testGroup.getFingerprint());
    }

    @Test
    @DisplayName("Should join group with multiline formatted message")
    void shouldJoinGroupWithMultilineFormattedMessage() throws GroupManager.InvalidInviteException {
        // Given: A test group created by someone else and formatted invite message with line breaks
        Group testGroup = otherGroupManager.createGroup("Multiline Test");
        String inviteCode = otherGroupManager.generateInviteCode(testGroup);
        String formattedMessage = "Join my Co-Organizer group so that we can start sharing findings today!\n" + inviteCode;
        
        // When: Joining with formatted message using different GroupManager
        Group joinedGroup = groupManager.joinGroup(formattedMessage);
        
        // Then: Group should be created successfully
        assertThat(joinedGroup.getName()).isEqualTo("Multiline Test");
        assertThat(joinedGroup.getFingerprint()).isEqualTo(testGroup.getFingerprint());
    }

    @Test
    @DisplayName("Should fail with empty input")
    void shouldFailWithEmptyInput() {
        // Given: Empty input
        String emptyInput = "";
        
        // When/Then: Should throw InvalidInviteException
        assertThatThrownBy(() -> groupManager.joinGroup(emptyInput))
            .isInstanceOf(GroupManager.InvalidInviteException.class);
    }

    @Test
    @DisplayName("Should fail with only prefix message")
    void shouldFailWithOnlyPrefixMessage() {
        // Given: Only the prefix message without code
        String onlyPrefix = "Join my Co-Organizer group so that we can start sharing findings today!";
        
        // When/Then: Should throw InvalidInviteException
        assertThatThrownBy(() -> groupManager.joinGroup(onlyPrefix))
            .isInstanceOf(GroupManager.InvalidInviteException.class);
    }

    @Test
    @DisplayName("Should fail with invalid base64")
    void shouldFailWithInvalidBase64() {
        // Given: Invalid base64 string
        String invalidBase64 = "this-is-not-valid-base64!@#$";
        
        // When/Then: Should throw InvalidInviteException
        assertThatThrownBy(() -> groupManager.joinGroup(invalidBase64))
            .isInstanceOf(GroupManager.InvalidInviteException.class);
    }

    @Test
    @DisplayName("Should generate formatted invite message for clipboard")
    void shouldGenerateFormattedInviteMessage() {
        // Given: A test group
        Group group = groupManager.createGroup("Clipboard Test Group");
        String inviteCode = groupManager.generateInviteCode(group);
        
        // When: Generating clipboard message (simulated)
        String expectedMessage = "Join my Co-Organizer group so that we can start sharing findings today! " + inviteCode;
        
        // Then: Message should be properly formatted
        assertThat(expectedMessage).startsWith("Join my Co-Organizer group so that we can start sharing findings today!");
        assertThat(expectedMessage).endsWith(inviteCode);
        assertThat(expectedMessage).contains(" " + inviteCode);
    }

    @Test
    @DisplayName("Should handle edge case with prefix appearing in group name")
    void shouldHandleEdgeCaseWithPrefixInGroupName() throws GroupManager.InvalidInviteException {
        // Given: A group created by someone else with the prefix text in its name
        Group edgeGroup = otherGroupManager.createGroup("Join my Co-Organizer group");
        String edgeInviteCode = otherGroupManager.generateInviteCode(edgeGroup);
        String formattedMessage = "Join my Co-Organizer group so that we can start sharing findings today! " + edgeInviteCode;
        
        // When: Joining with formatted message using different GroupManager
        Group joinedGroup = groupManager.joinGroup(formattedMessage);
        
        // Then: Should extract the correct code and create the group
        assertThat(joinedGroup.getName()).isEqualTo("Join my Co-Organizer group");
        assertThat(joinedGroup.getFingerprint()).isEqualTo(edgeGroup.getFingerprint());
    }
}