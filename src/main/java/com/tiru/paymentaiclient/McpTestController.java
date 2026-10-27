package com.tiru.paymentaiclient;

import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class McpTestController {

    private final SyncMcpToolCallbackProvider mcpTools;

    public McpTestController(SyncMcpToolCallbackProvider mcpTools) {
        this.mcpTools = mcpTools;
    }

    @GetMapping("/mcp-tools")
    public String checkMcpTools() {
        int count = mcpTools.getToolCallbacks().length;
        return "MCP connected. Tools discovered: " + count;
    }
}