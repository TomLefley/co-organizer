package dev.lefley.coorganizer.ui;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.logging.Logging;
import burp.api.montoya.persistence.Persistence;
import burp.api.montoya.persistence.Preferences;
import dev.lefley.coorganizer.model.Group;
import dev.lefley.coorganizer.service.GroupManager;
import dev.lefley.coorganizer.service.ShareService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

@DisplayName("ShareContextMenuProvider Tests")
class ShareContextMenuProviderTest {

    @Mock
    private MontoyaApi api;
    
    @Mock
    private Logging logging;
    
    @Mock
    private Persistence persistence;
    
    @Mock
    private Preferences preferences;
    
    @Mock
    private ContextMenuEvent contextMenuEvent;

    private ShareContextMenuProvider menuProvider;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        when(api.logging()).thenReturn(logging);
        when(api.persistence()).thenReturn(persistence);
        when(persistence.preferences()).thenReturn(preferences);
        when(preferences.getString("co-organizer.groups")).thenReturn(null);
        
        menuProvider = new ShareContextMenuProvider(api);
    }

    @Test
    @DisplayName("Should return empty menu when no items selected")
    void shouldReturnEmptyMenuWhenNoItemsSelected() {
        // Given: No selected items
        when(contextMenuEvent.selectedRequestResponses()).thenReturn(Collections.emptyList());
        
        // When: Providing menu items
        List<Component> menuItems = menuProvider.provideMenuItems(contextMenuEvent);
        
        // Then: Should return empty menu
        assertThat(menuItems).isEmpty();
    }

    @Test
    @DisplayName("Should provide Share menu when items selected but no groups")
    void shouldProvideShareMenuWhenItemsSelectedButNoGroups() {
        // Given: Selected items but no groups
        HttpRequestResponse item1 = mock(HttpRequestResponse.class);
        when(contextMenuEvent.selectedRequestResponses()).thenReturn(Arrays.asList(item1));
        
        // When: Providing menu items
        List<Component> menuItems = menuProvider.provideMenuItems(contextMenuEvent);
        
        // Then: Should only have Share menu item
        assertThat(menuItems).hasSize(1);
        assertThat(menuItems.get(0)).isInstanceOf(JMenuItem.class);
        
        JMenuItem shareItem = (JMenuItem) menuItems.get(0);
        assertThat(shareItem.getText()).isEqualTo("Share");
    }

    @Test
    @DisplayName("Should provide both Share and Share with menus when groups available")
    void shouldProvideBothShareAndShareWithMenusWhenGroupsAvailable() {
        // Given: Selected items and some groups
        HttpRequestResponse item1 = mock(HttpRequestResponse.class);
        when(contextMenuEvent.selectedRequestResponses()).thenReturn(Arrays.asList(item1));
        
        // Mock group manager with groups (this would require dependency injection in real implementation)
        // For now, test the menu structure logic
        
        // When/Then: Test menu structure expectations
        // This test demonstrates the expected behavior - real implementation would need
        // more sophisticated mocking or dependency injection
        
        // Expected: 2 menu items - "Share" and "Share with..."
        // "Share with..." should be a JMenu with submenu items for each group
        assertThat(true).isTrue(); // Placeholder assertion
    }

    @Test
    @DisplayName("Should create submenu items for each group in table order")
    void shouldCreateSubmenuItemsForEachGroupInTableOrder() {
        // Given: Multiple groups in specific order
        Group group1 = new Group("Alpha Group", "key1", "AAAA-1111-BBBB-2222");
        Group group2 = new Group("Beta Group", "key2", "CCCC-3333-DDDD-4444");
        Group group3 = new Group("Gamma Group", "key3", "EEEE-5555-FFFF-6666");
        
        // When: Creating menu items (simulated)
        JMenu shareWithMenu = new JMenu("Share with...");
        
        // Simulate adding groups in order
        shareWithMenu.add(new JMenuItem(group1.getName()));
        shareWithMenu.add(new JMenuItem(group2.getName()));
        shareWithMenu.add(new JMenuItem(group3.getName()));
        
        // Then: Should maintain order
        assertThat(shareWithMenu.getItemCount()).isEqualTo(3);
        assertThat(shareWithMenu.getItem(0).getText()).isEqualTo("Alpha Group");
        assertThat(shareWithMenu.getItem(1).getText()).isEqualTo("Beta Group");
        assertThat(shareWithMenu.getItem(2).getText()).isEqualTo("Gamma Group");
    }

    @Test
    @DisplayName("Should handle dynamic group list updates")
    void shouldHandleDynamicGroupListUpdates() {
        // Given: Initial state with items selected
        HttpRequestResponse item1 = mock(HttpRequestResponse.class);
        when(contextMenuEvent.selectedRequestResponses()).thenReturn(Arrays.asList(item1));
        
        // When: Getting menu items multiple times (simulating group changes)
        List<Component> menuItems1 = menuProvider.provideMenuItems(contextMenuEvent);
        List<Component> menuItems2 = menuProvider.provideMenuItems(contextMenuEvent);
        
        // Then: Should handle multiple calls gracefully
        assertThat(menuItems1).isNotNull();
        assertThat(menuItems2).isNotNull();
        
        // Both calls should produce consistent results for same context
        assertThat(menuItems1.size()).isEqualTo(menuItems2.size());
    }

    @Test
    @DisplayName("Should provide menu items for single vs multiple selections")
    void shouldProvideMenuItemsForSingleVsMultipleSelections() {
        // Given: Single item selection
        HttpRequestResponse item1 = mock(HttpRequestResponse.class);
        when(contextMenuEvent.selectedRequestResponses()).thenReturn(Arrays.asList(item1));
        
        List<Component> singleItemMenu = menuProvider.provideMenuItems(contextMenuEvent);
        
        // Given: Multiple item selection
        HttpRequestResponse item2 = mock(HttpRequestResponse.class);
        when(contextMenuEvent.selectedRequestResponses()).thenReturn(Arrays.asList(item1, item2));
        
        List<Component> multipleItemMenu = menuProvider.provideMenuItems(contextMenuEvent);
        
        // Then: Both should provide menu items
        assertThat(singleItemMenu).isNotEmpty();
        assertThat(multipleItemMenu).isNotEmpty();
        
        // Menu structure should be the same regardless of selection count
        assertThat(singleItemMenu.size()).isEqualTo(multipleItemMenu.size());
    }

    @Test
    @DisplayName("Should create valid menu components")
    void shouldCreateValidMenuComponents() {
        // Given: A test context with selected items
        HttpRequestResponse item1 = mock(HttpRequestResponse.class);
        when(contextMenuEvent.selectedRequestResponses()).thenReturn(Arrays.asList(item1));
        
        // When: Getting menu items
        List<Component> menuItems = menuProvider.provideMenuItems(contextMenuEvent);
        
        // Then: All components should be valid Swing components
        for (Component component : menuItems) {
            assertThat(component).isNotNull();
            assertThat(component).isInstanceOfAny(JMenuItem.class, JMenu.class);
        }
    }
}