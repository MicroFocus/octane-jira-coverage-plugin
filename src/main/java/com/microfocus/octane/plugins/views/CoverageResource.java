/*
 *     Copyright 2018 EntIT Software LLC, a Micro Focus company, L.P.
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package com.microfocus.octane.plugins.views;

import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import com.microfocus.octane.plugins.configuration.ConfigurationManager;
import com.microfocus.octane.plugins.configuration.WorkspaceConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/coverage")
@Scanned
public class CoverageResource {

    private static final Logger log = LoggerFactory.getLogger(CoverageResource.class);

    @ComponentImport
    private final UserManager userManager;

    @Inject
    public CoverageResource(UserManager userManager) {
        this.userManager = userManager;
    }

    @GET
    public Response getCoverage(@Context HttpServletRequest request, @QueryParam("project-key") String projectKey, @QueryParam("issue-key") String issueKey, @QueryParam("issue-id") String issueId,
                                @QueryParam("workspace-config-id") String workspaceConfigId, @QueryParam("workspace-id") String workspaceId) {
        UserProfile userProfile = userManager.getRemoteUser(request);
        if (userProfile == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        Map<String, Object> contextMap = CoverageUiHelper.buildCoverageContextMap(projectKey, issueKey, issueId, workspaceConfigId, workspaceId);
        return Response.ok(contextMap).build();
    }

    @GET
    @Path("/octane-workspaces")
    public Response getOctaneWorkspaces(@Context HttpServletRequest request, @QueryParam("project-key") String projectKey, @QueryParam("issue-type") String issueType) {
        UserProfile userProfile = userManager.getRemoteUser(request);
        if (userProfile == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        List<WorkspaceConfiguration> workspaceConfigsForProjectKey = ConfigurationManager.getInstance().getWorkspaceConfigurations().stream()
                .filter(wsc -> doesWorkspaceConfigContainsProjectAndIssueType(projectKey, issueType, wsc))
                .collect(Collectors.toList());

        List<Map<String, String>> wsConfigsMap = workspaceConfigsForProjectKey.stream()
                .map(this::toMap)
                .collect(Collectors.toList());

        return Response.ok(wsConfigsMap).build();
    }

    private static boolean doesWorkspaceConfigContainsProjectAndIssueType(String projectKey, String issueType, WorkspaceConfiguration workspaceConfiguration) {
        return workspaceConfiguration.getJiraProjects().stream().anyMatch(jiraProject -> jiraProject.equals(projectKey))
                && workspaceConfiguration.getJiraIssueTypes().stream().anyMatch(jiraIssueType -> jiraIssueType.equals(issueType));
    }

    private Map<String, String> toMap(WorkspaceConfiguration wsc) {
        Map<String, String> map = new HashMap<>();

        map.put("id", wsc.getId());
        map.put("spaceConfigId", wsc.getSpaceConfigurationId());
        map.put("spaceConfigName", ConfigurationManager.getInstance().getSpaceConfigurationById(wsc.getSpaceConfigurationId(), false).get().getName());
        map.put("workspaceId", String.valueOf(wsc.getWorkspaceId()));
        map.put("workspaceName", wsc.getWorkspaceName());

        return map;
    }
}
