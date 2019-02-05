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

package com.microfocus.octane.plugins.admin;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import com.microfocus.octane.plugins.components.api.Constants;
import com.microfocus.octane.plugins.configuration.OctaneConfiguration;
import com.microfocus.octane.plugins.configuration.OctaneConfigurationManager;
import com.microfocus.octane.plugins.configuration.OctaneConfigurationOutgoing;
import com.microfocus.octane.plugins.rest.OctaneEntityParser;
import com.microfocus.octane.plugins.rest.RestConnector;
import com.microfocus.octane.plugins.rest.entities.OctaneEntity;
import com.microfocus.octane.plugins.rest.entities.OctaneEntityCollection;
import com.microfocus.octane.plugins.rest.query.InQueryPhrase;
import com.microfocus.octane.plugins.rest.query.LogicalQueryPhrase;
import com.microfocus.octane.plugins.rest.query.OctaneQueryBuilder;
import com.microfocus.octane.plugins.rest.query.QueryPhrase;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.*;
import java.util.stream.Collectors;

@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/")
@Scanned
public class ConfigResource {

    private static final Logger log = LoggerFactory.getLogger(ConfigResource.class);

    @ComponentImport
    private final UserManager userManager;


    @ComponentImport
    private final TransactionTemplate transactionTemplate;

    private List<WorkspaceConfigurationModel> models = new ArrayList<>();

    @Inject
    public ConfigResource(UserManager userManager, TransactionTemplate transactionTemplate) {
        this.userManager = userManager;
        this.transactionTemplate = transactionTemplate;

        models.add(new WorkspaceConfigurationModel("1001.1001", "ws1", "key1"));
        models.add(new WorkspaceConfigurationModel("1001.1002", "ws2", "key2"));
    }

    @GET
    @Path("/all")
    public Response getAllWorkspaceConfigurations(@Context HttpServletRequest request) {
        return Response.ok(models).build();
    }

    @GET
    @Path("/self/{id}")
    public Response getWorkspaceConfigurationById(@PathParam("id") String id) {
        return Response.ok(models.stream().filter(m -> id.equals("" + m.getId())).findFirst()).build();
    }

    @PUT
    @Path("/self/{id}")
    public Response updateWorkspaceConfigurationById(@Context HttpServletRequest request, @PathParam("id") String id, WorkspaceConfigurationModel modelForUpdate) {
        Optional<WorkspaceConfigurationModel> optional = models.stream().filter(m -> id.equals("" + m.getId())).findFirst();

        return Response.ok(modelForUpdate).build();
    }

    int counter = 3;

    @POST
    @Path("/self")
    public Response createWorkspaceConfiguration(@Context HttpServletRequest request, WorkspaceConfigurationModel model) {
        model.setId("" + counter++);
        models.add(model);
        return Response.ok(model).build();
    }

    @DELETE
    @Path("/self/{id}")
    public Response deleteWorkspaceConfigurationById(@Context HttpServletRequest request, @PathParam("id") String id) {

        Optional<WorkspaceConfigurationModel> optional = models.stream().filter(m -> id.equals("" + m.getId())).findFirst();
        if (optional.isPresent()) {
            models.remove(optional.get());
        }

        return Response.ok().build();
    }

    private boolean hasPermissions(HttpServletRequest request) {
        UserProfile username = userManager.getRemoteUser(request);
        return (username != null && userManager.isSystemAdmin(username.getUserKey()));
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@Context HttpServletRequest request) {

        if (!hasPermissions(request)) {
            return Response.status(Status.UNAUTHORIZED).build();
        }

        OctaneConfigurationOutgoing config = OctaneConfigurationManager.getInstance().loadConfiguration();
        config.setClientSecret(OctaneConfigurationManager.PASSWORD_REPLACE);

        if (StringUtils.isEmpty(config.getOctaneUdf())) {
            config.setOctaneUdf(OctaneConfigurationManager.DEFAULT_OCTANE_FIELD_UDF);
        }
        return Response.ok(config).build();
    }

    @Path("/test-connection")
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response testConnection(final OctaneConfigurationOutgoing outgoingConfig, @Context HttpServletRequest request) {
        UserProfile username = userManager.getRemoteUser(request);
        if (username == null || !userManager.isSystemAdmin(username.getUserKey())) {
            return Response.status(Status.UNAUTHORIZED).build();
        }

        String errorMsg = checkConfiguration(outgoingConfig);

        if (errorMsg != null) {
            Map<String, String> status = new HashMap<>();
            status.put("failed", "Validation failed : " + errorMsg);
            return Response.status(Status.CONFLICT).entity(status).build();
        }

        OctaneConfiguration internalConfig = OctaneConfigurationManager.getInstance().convertToInternalConfiguration(outgoingConfig);
        List<String> warnings = new ArrayList<>();
        try {
            checkOctaneFieldExistance(warnings, internalConfig);
            checkJiraIssueTypeExistance(warnings, internalConfig);
            checkJiraProjectsExistance(warnings, internalConfig);
        } catch (Exception e) {
            log.error("Failed to check warnings : " + e.getMessage());
        }

        if (!warnings.isEmpty()) {
            Map<String, String> status = new HashMap<>();
            status.put("warning", "Attention : <ul><li>" + StringUtils.join(warnings, "</li><li>") + "</li></ul>");
            return Response.status(Status.CONFLICT).entity(status).build();
        }

        return Response.ok().build();
    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response put(final OctaneConfigurationOutgoing config, @Context HttpServletRequest request) {
        UserProfile username = userManager.getRemoteUser(request);
        if (username == null || !userManager.isSystemAdmin(username.getUserKey())) {
            return Response.status(Status.UNAUTHORIZED).build();
        }

        String errorMsg = checkConfiguration(config);

        if (errorMsg != null) {
            return Response.status(Status.CONFLICT).entity("Failed to save : " + errorMsg).build();
        } else {
            transactionTemplate.execute(new TransactionCallback() {
                public Object doInTransaction() {
                    OctaneConfigurationManager.getInstance().saveConfiguration(config);
                    return null;
                }
            });
        }

        return Response.ok().build();
    }

    private String checkConfiguration(OctaneConfigurationOutgoing outgoingConfig) {
        String errorMsg = null;
        if (StringUtils.isEmpty(outgoingConfig.getLocation())) {
            errorMsg = "Location URL is required";
        } else if (StringUtils.isEmpty(outgoingConfig.getClientId())) {
            errorMsg = "Client ID is required";
        } else if (StringUtils.isEmpty(outgoingConfig.getClientSecret())) {
            errorMsg = "Client secret is required";
        } else if (StringUtils.isEmpty(outgoingConfig.getOctaneUdf())) {
            errorMsg = "Octane field is required";
        } else {
            OctaneConfiguration internalConfig = OctaneConfigurationManager.getInstance().convertToInternalConfiguration(outgoingConfig);
            if (errorMsg == null) {
                try {
                    RestConnector restConnector = new RestConnector();
                    restConnector.setBaseUrl(internalConfig.getBaseUrl());
                    restConnector.setCredentials(internalConfig.getClientId(), internalConfig.getClientSecret());
                    boolean isConnected = restConnector.login();
                    if (!isConnected) {
                        errorMsg = "Failed to authenticate";
                    } else {
                        String entityCollectionUrl = String.format(Constants.PUBLIC_API_WORKSPACE_LEVEL_ENTITIES,
                                internalConfig.getSharedspaceId(), internalConfig.getWorkspaceId(), "");

                        Map<String, String> headers = new HashMap<>();
                        headers.put(RestConnector.HEADER_ACCEPT, RestConnector.HEADER_APPLICATION_JSON);

                        try {
                            String entitiesCollectionStr = restConnector.httpGet(entityCollectionUrl, null, headers).getResponseData();

                            JSONObject jsonObj = new JSONObject(entitiesCollectionStr);
                            OctaneEntity workspaceEntity = OctaneEntityParser.parseEntity(jsonObj);
                        } catch (JSONException e) {
                            errorMsg = "Incorrect sharedspace id or workspace id";
                        }
                    }
                } catch (Exception exc) {
                    if (exc.getMessage().contains("platform.not_authorized")) {
                        errorMsg = "Validate credentials";
                    } else if (exc.getMessage().contains("type workspace does not exist")) {
                        errorMsg = "Workspace '" + internalConfig.getWorkspaceId() + "' is not available";
                    } else if (exc.getMessage().contains("type shared_space does not exist")) {
                        errorMsg = "Sharedspace '" + internalConfig.getSharedspaceId() + "' is not available";
                    } else {
                        errorMsg = "Validate that location is correct.";
                    }
                }
            }
        }
        return errorMsg;
    }


    private void checkOctaneFieldExistance(List<String> warnings, OctaneConfiguration internalConfig) {
        String entityCollectionUrl = String.format(Constants.PUBLIC_API_WORKSPACE_LEVEL_ENTITIES,
                internalConfig.getSharedspaceId(), internalConfig.getWorkspaceId(), "metadata/fields");

        Map<String, String> headers = new HashMap<>();
        headers.put(RestConnector.HEADER_ACCEPT, RestConnector.HEADER_APPLICATION_JSON);

        try {
            RestConnector restConnector = new RestConnector();
            restConnector.setBaseUrl(internalConfig.getBaseUrl());
            restConnector.setCredentials(internalConfig.getClientId(), internalConfig.getClientSecret());
            boolean isConnected = restConnector.login();

            QueryPhrase fieldNameCondition = new LogicalQueryPhrase("name", internalConfig.getOctaneUdf());
            Map<String, String> key2LabelType = new HashMap<>();
            key2LabelType.put("feature", "Feature");
            key2LabelType.put("story", "User Story");
            key2LabelType.put("product_area", "Application module");
            QueryPhrase typeCondition = new InQueryPhrase("entity_name", key2LabelType.keySet());

            String queryCondition = OctaneQueryBuilder.create().addQueryCondition(fieldNameCondition).addQueryCondition(typeCondition).build();
            String entitiesCollectionStr = restConnector.httpGet(entityCollectionUrl, Arrays.asList(queryCondition), headers).getResponseData();
            OctaneEntityCollection fields = OctaneEntityParser.parseCollection(entitiesCollectionStr);
            Set<String> foundTypes = fields.getData().stream().map(e -> e.getString("entity_name")).collect(Collectors.toSet());
            Set<String> missingTypes = key2LabelType.keySet().stream().filter(key -> !foundTypes.contains(key)).map(key -> key2LabelType.get(key)).collect(Collectors.toSet());
            if (!missingTypes.isEmpty()) {
                String warn = String.format("The following Octane entity types have no field '%s' : %s",
                        internalConfig.getOctaneUdf(), StringUtils.join(missingTypes, ", "));
                warnings.add(warn);
            }
        } catch (Exception e) {
            log.error(String.format("Failed on checkOctaneFieldExistance : %s", e.getMessage()), e);
        }
    }

    private void checkJiraIssueTypeExistance(List<String> warnings, OctaneConfiguration internalConfig) {
        if (internalConfig.getJiraIssueTypes() != null) {
            Set<String> existingIssueTypes = ComponentAccessor.getConstantsManager().getAllIssueTypeObjects().stream()
                    .map(IssueType::getName).map(String::toLowerCase).collect(Collectors.toSet());
            Set<String> missingTypes = internalConfig.getJiraIssueTypes().stream().filter(name -> !existingIssueTypes.contains(name)).collect(Collectors.toSet());
            if (!missingTypes.isEmpty()) {
                String warn = String.format("The following issue types are not found in Jira : %s", StringUtils.join(missingTypes, ", "));
                warnings.add(warn);
            }
        }
    }

    private void checkJiraProjectsExistance(List<String> warnings, OctaneConfiguration internalConfig) {
        if (internalConfig.getJiraProjects() != null) {
            Set<String> existingProjects = ComponentAccessor.getProjectManager().getProjectObjects().stream().map(Project::getKey).map(String::toUpperCase).collect(Collectors.toSet());

            Set<String> missingKeys = internalConfig.getJiraProjects().stream().filter(key -> !existingProjects.contains(key)).collect(Collectors.toSet());
            if (!missingKeys.isEmpty()) {
                String warn = String.format("The following project keys are not found in Jira : %s", StringUtils.join(missingKeys, ", "));
                warnings.add(warn);
            }
        }
    }

}
