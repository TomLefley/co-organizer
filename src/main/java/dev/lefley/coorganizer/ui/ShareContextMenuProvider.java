package dev.lefley.coorganizer.ui;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;
import dev.lefley.coorganizer.model.Group;
import dev.lefley.coorganizer.service.GroupManager;
import dev.lefley.coorganizer.service.ShareService;
import dev.lefley.coorganizer.util.Logger;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ShareContextMenuProvider implements ContextMenuItemsProvider {
    private final MontoyaApi api;
    private final ShareService shareService;
    private final GroupManager groupManager;
    private final Logger logger;
    
    public ShareContextMenuProvider(MontoyaApi api) {
        this.api = api;
        this.shareService = new ShareService(api);
        this.groupManager = new GroupManager(api);
        this.logger = new Logger(api, ShareContextMenuProvider.class);
    }
    
    @Override
    public List<Component> provideMenuItems(ContextMenuEvent event) {
        logger.debug("Context menu provider called");
        List<Component> menuItems = new ArrayList<>();
        
        logger.debug("Selected request/responses count: " + event.selectedRequestResponses().size());
        
        if (!event.selectedRequestResponses().isEmpty()) {
            // Add "Share" menu item (no encryption)
            JMenuItem shareItem = new JMenuItem("Share");
            shareItem.addActionListener(e -> {
                logger.debug("Share menu item clicked");
                // Run in background thread to avoid EDT blocking
                new Thread(() -> shareService.shareItems(event.selectedRequestResponses())).start();
            });
            menuItems.add(shareItem);
            
            // Add "Share with..." menu item with group submenu
            // Refresh groups to ensure we have the latest data
            groupManager.refreshGroupsFromPreferences();
            List<Group> groups = groupManager.getGroups();
            if (!groups.isEmpty()) {
                JMenu shareWithMenu = new JMenu("Share with...");
                
                for (Group group : groups) {
                    JMenuItem groupItem = new JMenuItem(group.getName());
                    groupItem.addActionListener(e -> {
                        logger.debug("Share with group '" + group.getName() + "' menu item clicked");
                        // Run in background thread to avoid EDT blocking
                        new Thread(() -> shareService.shareItems(event.selectedRequestResponses(), group)).start();
                    });
                    shareWithMenu.add(groupItem);
                }
                
                menuItems.add(shareWithMenu);
                logger.debug("Added 'Share with...' menu with " + groups.size() + " group options");
            } else {
                logger.debug("No groups available, not adding 'Share with...' menu");
            }
        } else {
            logger.debug("No request/responses selected, not adding Share menu items");
        }
        
        logger.debug("Returning " + menuItems.size() + " menu items");
        return menuItems;
    }
}