package dev.lefley.coorganizer.ui;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;
import dev.lefley.coorganizer.service.ShareService;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ShareContextMenuProvider implements ContextMenuItemsProvider {
    private final MontoyaApi api;
    private final ShareService shareService;
    
    public ShareContextMenuProvider(MontoyaApi api) {
        this.api = api;
        this.shareService = new ShareService(api);
    }
    
    @Override
    public List<Component> provideMenuItems(ContextMenuEvent event) {
        api.logging().logToOutput("Context menu provider called");
        List<Component> menuItems = new ArrayList<>();
        
        api.logging().logToOutput("Selected request/responses count: " + event.selectedRequestResponses().size());
        
        if (!event.selectedRequestResponses().isEmpty()) {
            int itemCount = event.selectedRequestResponses().size();
            String menuText = itemCount == 1 ? "Share item" : "Share selected items";
            
            api.logging().logToOutput("Adding '" + menuText + "' menu item for " + itemCount + " items");
            JMenuItem shareItem = new JMenuItem(menuText);
            shareItem.addActionListener(e -> {
                api.logging().logToOutput("Share menu item clicked");
                // Run in background thread to avoid EDT blocking
                new Thread(() -> shareService.shareItems(event.selectedRequestResponses())).start();
            });
            menuItems.add(shareItem);
        } else {
            api.logging().logToOutput("No request/responses selected, not adding Share menu item");
        }
        
        api.logging().logToOutput("Returning " + menuItems.size() + " menu items");
        return menuItems;
    }
}