package dev.lefley.coorganizer;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import dev.lefley.coorganizer.handler.SharedItemDownloadResponseHandler;
import dev.lefley.coorganizer.ui.ShareContextMenuProvider;
import dev.lefley.coorganizer.ui.GroupTab;

/**
 * Co-Organizer Burp Suite Extension
 * 
 * This extension provides functionality to:
 * - Share HTTP request/response items via context menu
 * - Automatically import shared items from shared item download URLs
 * - Manage collaboration groups with invite codes
 * 
 * Architecture follows SOLID principles with separated concerns:
 * - Extension: Main coordinator class
 * - ShareService: Handles sharing functionality
 * - SharedItemDownloadResponseHandler: Processes incoming shared items
 * - HttpRequestResponseSerializer: Handles JSON serialization/deserialization
 * - NotificationService: Manages toast notifications
 * - SharedItemDownloadMatcher: URL matching logic
 * - GroupManager: Manages collaboration groups
 * - GroupTab: UI for group management
 */
public class Extension implements BurpExtension {
    private static final String EXTENSION_NAME = "Co-Organizer";
    private static final String TAB_NAME = "Co-Organizer Groups";
    
    private MontoyaApi api;
    
    @Override
    public void initialize(MontoyaApi montoyaApi) {
        this.api = montoyaApi;
        
        api.extension().setName(EXTENSION_NAME);
        api.logging().logToOutput(EXTENSION_NAME + " extension initializing...");

        registerContextMenuProvider();
        registerProxyResponseHandler();
        registerGroupTab();
        
        api.logging().logToOutput(EXTENSION_NAME + " extension initialization complete");
    }
    
    private void registerContextMenuProvider() {
        ShareContextMenuProvider contextMenuProvider = new ShareContextMenuProvider(api);
        api.userInterface().registerContextMenuItemsProvider(contextMenuProvider);
        api.logging().logToOutput("Context menu provider registered successfully");
    }
    
    private void registerProxyResponseHandler() {
        SharedItemDownloadResponseHandler responseHandler = new SharedItemDownloadResponseHandler(api);
        api.proxy().registerResponseHandler(responseHandler);
        api.logging().logToOutput("Proxy response handler registered successfully");
    }
    
    private void registerGroupTab() {
        GroupTab groupTab = new GroupTab(api);
        api.userInterface().registerSuiteTab(TAB_NAME, groupTab);
        api.logging().logToOutput("Group management tab registered successfully");
    }
}