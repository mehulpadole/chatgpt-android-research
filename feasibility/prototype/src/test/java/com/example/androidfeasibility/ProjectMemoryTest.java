package com.example.androidfeasibility;

import java.util.List;

public final class ProjectMemoryTest {
    public static void main(String[] args) throws Exception {
        testProjectOwnsConversationMembership();
        testMemoryScopesDoNotLeak();
        testNavigationIsStableAndExplicit();
        System.out.println("PROJECT MEMORY TESTS PASSED");
    }

    private static void testProjectOwnsConversationMembership() throws Exception {
        InMemoryProjectRepository repository = new InMemoryProjectRepository();
        Project project = new Project("project-1", "Research", 1L);
        project.addConversation("conversation-1");
        repository.save(project);
        Project restored = repository.load("project-1");
        check(restored != null && restored.hasConversation("conversation-1"),
                "project must preserve local conversation membership");
        check(repository.projects().size() == 1, "project repository must list local projects");
    }

    private static void testMemoryScopesDoNotLeak() throws Exception {
        InMemoryMemoryRepository repository = new InMemoryMemoryRepository();
        repository.save(new MemoryEntry("global", MemoryEntry.Scope.GLOBAL, "",
                "global preference", true, 1L));
        repository.save(new MemoryEntry("project", MemoryEntry.Scope.PROJECT, "project-1",
                "project fact", true, 2L));
        List<MemoryEntry> project = repository.list(MemoryEntry.Scope.PROJECT, "project-1");
        check(project.size() == 1 && project.get(0).content.equals("project fact"),
                "project memory must be scoped to its project");
        check(repository.list(MemoryEntry.Scope.PROJECT, "project-2").isEmpty(),
                "project memory must not leak to another project");
        check(repository.list(MemoryEntry.Scope.GLOBAL, "project-1").size() == 1,
                "global memory must remain available independent of project ID");
    }

    private static void testNavigationIsStableAndExplicit() {
        NavigationState state = NavigationState.home();
        check(state.screen == NavigationState.Screen.HOME, "initial navigation must be home");
        state = state.openProject("project-1").openConversation("conversation-1");
        check(state.screen == NavigationState.Screen.CHAT
                        && state.projectId.equals("project-1")
                        && state.conversationId.equals("conversation-1"),
                "navigation must retain project and conversation context");
        check(state.toSettings().screen == NavigationState.Screen.SETTINGS,
                "settings must be an explicit stable destination");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
