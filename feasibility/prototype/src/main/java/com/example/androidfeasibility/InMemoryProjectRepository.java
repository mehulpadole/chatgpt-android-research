package com.example.androidfeasibility;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class InMemoryProjectRepository implements ProjectRepository {
    private final Map<String, Project> projects = new LinkedHashMap<>();

    @Override public synchronized void save(Project project) {
        if (project == null) throw new IllegalArgumentException("project is null");
        projects.put(project.projectId, project.copy());
    }

    @Override public synchronized Project load(String projectId) {
        Project project = projects.get(projectId);
        return project == null ? null : project.copy();
    }

    @Override public synchronized List<Project> projects() {
        List<Project> result = new ArrayList<>();
        for (Project project : projects.values()) result.add(project.copy());
        return result;
    }

    @Override public synchronized void delete(String projectId) { projects.remove(projectId); }
}
