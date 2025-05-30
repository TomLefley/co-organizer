package dev.lefley.coorganizer;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import dev.lefley.coorganizer.handler.SharedItemDownloadResponseHandler;
import dev.lefley.coorganizer.ui.ShareContextMenuProvider;

/**
 * Co-Organizer Burp Suite Extension
 * 
 * This extension provides functionality to:
 * - Share HTTP request/response items via context menu
 * - Automatically import shared items from shared item download URLs
 * 
 * Architecture follows SOLID principles with separated concerns:
 * - Extension: Main coordinator class
 * - ShareService: Handles sharing functionality
 * - SharedItemDownloadResponseHandler: Processes incoming shared items
 * - HttpRequestResponseSerializer: Handles JSON serialization/deserialization
 * - NotificationService: Manages toast notifications
 * - SharedItemDownloadMatcher: URL matching logic
 */
public class Extension implements BurpExtension {
    private MontoyaApi api;
    
    @Override
    public void initialize(MontoyaApi montoyaApi) {
        this.api = montoyaApi;
        
        api.extension().setName("Co-Organizer");
        api.logging().logToOutput("Co-Organizer extension initializing...");

        registerContextMenuProvider();
        registerProxyResponseHandler();
        
        api.logging().logToOutput("Co-Organizer extension initialization complete");
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
}