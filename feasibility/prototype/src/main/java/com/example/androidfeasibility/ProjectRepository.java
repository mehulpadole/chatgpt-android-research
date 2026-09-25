package com.example.androidfeasibility;

import java.util.List;

public interface ProjectRepository {
    void save(Project project) throws Exception;
    Project load(String projectId) throws Exception;
    List<Project> projects() throws Exception;
    void delete(String projectId) throws Exception;
}
