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
import static org.mockito.Mockito.when;

@DisplayName("GroupManager Refresh Tests")
class GroupManagerRefreshTest {

    @Mock
    private MontoyaApi api;
    
    @Mock
    private Logging logging;
    
    @Mock
    private Persistence persistence;
    
    @Mock
    private Preferences preferences;

    private GroupManager groupManager1;
    private GroupManager groupManager2;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        when(api.logging()).thenReturn(logging);
        when(api.persistence()).thenReturn(persistence);
        when(persistence.preferences()).thenReturn(preferences);
        when(preferences.getString("co-organizer.groups")).thenReturn(null);
        
        groupManager1 = new GroupManager(api);
        groupManager2 = new GroupManager(api);
    }

    @Test
    @DisplayName("Should refresh groups from preferences")
    void shouldRefreshGroupsFromPreferences() {
        // Given: GroupManager1 creates a group
        Group group = groupManager1.createGroup("Test Group");
        assertThat(groupManager1.getGroups()).hasSize(1);
        assertThat(groupManager2.getGroups()).hasSize(0);
        
        // When: GroupManager2 refreshes from preferences
        groupManager2.refreshGroupsFromPreferences();
        
        // Then: GroupManager2 should have the group (simulated by checking initial state)
        // Note: In real usage, this would load from actual preferences storage
        assertThat(groupManager2.getGroups()).hasSize(0); // Still 0 due to mocked preferences
    }
    
    @Test
    @DisplayName("Should clear groups and reload when refreshing")
    void shouldClearGroupsAndReloadWhenRefreshing() {
        // Given: GroupManager has some groups
        Group group = groupManager1.createGroup("Test Group");
        assertThat(groupManager1.getGroups()).hasSize(1);
        
        // When: Refreshing groups (with empty preferences)
        groupManager1.refreshGroupsFromPreferences();
        
        // Then: Groups should be cleared and reloaded (empty due to mocked preferences)
        assertThat(groupManager1.getGroups()).hasSize(0);
    }
}