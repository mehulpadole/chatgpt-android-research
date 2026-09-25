package com.example.androidfeasibility;

public final class NavigationState {
    public enum Screen { HOME, CHAT, PROJECTS, SETTINGS, MEMORY }

    public final Screen screen;
    public final String projectId;
    public final String conversationId;

    private NavigationState(Screen screen, String projectId, String conversationId) {
        this.screen = screen;
        this.projectId = projectId == null ? "" : projectId;
        this.conversationId = conversationId == null ? "" : conversationId;
    }

    public static NavigationState home() { return new NavigationState(Screen.HOME, "", ""); }
    public NavigationState openProject(String projectId) {
        require(projectId, "projectId");
        return new NavigationState(Screen.PROJECTS, projectId, "");
    }
    public NavigationState openConversation(String conversationId) {
        require(conversationId, "conversationId");
        if (projectId.isEmpty()) throw new IllegalStateException("conversation requires project context");
        return new NavigationState(Screen.CHAT, projectId, conversationId);
    }
    public NavigationState toSettings() { return new NavigationState(Screen.SETTINGS, projectId, conversationId); }
    public NavigationState toProjects() { return new NavigationState(Screen.PROJECTS, projectId, ""); }
    public NavigationState toMemory() { return new NavigationState(Screen.MEMORY, projectId, conversationId); }
    public NavigationState toHome() { return home(); }

    private static void require(String value, String name) {
        if (value == null || value.isEmpty()) throw new IllegalArgumentException(name + " is empty");
    }
}
