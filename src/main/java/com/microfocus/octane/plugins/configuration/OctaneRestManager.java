/*******************************************************************************
 * Copyright 2017-2023 Open Text.
 *
 * The only warranties for products and services of Open Text and
 * its affiliates and licensors (“Open Text”) are as may be set forth
 * in the express warranty statements accompanying such products and services.
 * Nothing herein should be construed as constituting an additional warranty.
 * Open Text shall not be liable for technical or editorial errors or
 * omissions contained herein. The information contained herein is subject
 * to change without notice.
 *
 * Except as specifically indicated otherwise, this document contains
 * confidential information and a valid license is required for possession,
 * use or copying. If this work is provided to the U.S. Government,
 * consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 * Computer Software Documentation, and Technical Data for Commercial Items are
 * licensed to the U.S. Government under vendor's standard commercial license.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *   http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.microfocus.octane.plugins.configuration;

import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.microfocus.octane.plugins.configuration.v3.SpaceConfiguration;
import com.microfocus.octane.plugins.descriptors.OctaneEntityTypeDescriptor;
import com.microfocus.octane.plugins.descriptors.OctaneEntityTypeManager;
import com.microfocus.octane.plugins.rest.OctaneEntityParser;
import com.microfocus.octane.plugins.rest.RestConnector;
import com.microfocus.octane.plugins.rest.entities.OctaneEntity;
import com.microfocus.octane.plugins.rest.entities.OctaneEntityCollection;
import com.microfocus.octane.plugins.rest.entities.groups.GroupEntityCollection;
import com.microfocus.octane.plugins.rest.query.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLHandshakeException;
import java.net.UnknownHostException;
import java.util.*;
import java.util.stream.Collectors;

import static com.microfocus.octane.plugins.configuration.PluginConstants.LAST_RUNS_FIELD;
import static com.microfocus.octane.plugins.configuration.PluginConstants.WORK_ITEMS;
import static com.microfocus.octane.plugins.rest.OctaneEntityParser.parseTestCoverageJson;

public class OctaneRestManager {

    private static final Logger log = LoggerFactory.getLogger(OctaneRestManager.class);

    public static GroupEntityCollection getCoverage(SpaceConfiguration sc, OctaneEntity octaneEntity, OctaneEntityTypeDescriptor typeDescriptor, long workspaceId) {
        //http://localhost:8080/api/shared_spaces/1001/workspaces/1002/runs/groups?query="test_of_last_run={product_areas={(id IN '2001')}}"&group_by=status

        String url = String.format(PluginConstants.PUBLIC_API_WORKSPACE_LEVEL_ENTITIES, sc.getLocationParts().getSpaceId(), workspaceId, "runs/groups");
        Map<String, String> headers = createHeaderMapWithOctaneClientType();
        headers.put(RestConnector.HEADER_ACCEPT, RestConnector.HEADER_APPLICATION_JSON);

        String queryParam = getQueryParam(octaneEntity, typeDescriptor);
        String responseStr = sc.getRestConnector().httpGet(url, Collections.singletonList(queryParam), headers).getResponseData();

        return OctaneEntityParser.parseGroupCollection(responseStr);
    }

    public static Map<String, Integer> getCoverageByTestCoverageField(SpaceConfiguration sc, OctaneEntity octaneEntity, long workspaceId) {
        //http://localhost:9090/dev/api/shared_spaces/1001/workspaces/1002/work_items/2007?fields=last_runs

        String url = String.format(PluginConstants.PUBLIC_API_WORKSPACE_LEVEL_SPECIFIC_ENTITY, sc.getLocationParts().getSpaceId(), workspaceId, WORK_ITEMS, octaneEntity.getId());
        Map<String, String> headers = createHeaderMapWithOctaneClientType();
        headers.put(RestConnector.HEADER_ACCEPT, RestConnector.HEADER_APPLICATION_JSON);

        String queryParam = OctaneQueryBuilder.create().addSelectedFields(LAST_RUNS_FIELD).build();
        String responseStr = sc.getRestConnector().httpGet(url, Collections.singletonList(queryParam), headers).getResponseData();

        try {
            OctaneEntity responseEntity = OctaneEntityParser.parseEntity(new JSONObject(responseStr));
            String testCoverageField = responseEntity.getString(LAST_RUNS_FIELD);

            JSONObject testCoverageJson = new JSONObject(testCoverageField);

            return parseTestCoverageJson(testCoverageJson);
        } catch (JSONException e) {
            throw new RuntimeException("Failed to convert response to entity object:" + e.getMessage());
        }
    }

    public static VersionEntity getOctaneServerVersion(SpaceConfiguration sc) {
        String url = "/admin/server/version";
        Map<String, String> headers = createHeaderMapWithOctaneClientType();
        headers.put(RestConnector.HEADER_ACCEPT, RestConnector.HEADER_APPLICATION_JSON);

        String response = sc.getRestConnector().httpGet(url, null, headers).getResponseData();

        return OctaneEntityParser.parseServerVersion(response);
    }

    private static String getQueryParam(OctaneEntity octaneEntity, OctaneEntityTypeDescriptor typeDescriptor) {
        OctaneQueryBuilder queryBuilder = OctaneQueryBuilder.create()
                .addGroupBy("status")
                .addQueryCondition(new CrossQueryPhrase("test_of_last_run", new CrossQueryPhrase(typeDescriptor.getTestReferenceField(), createGetEntityCondition(octaneEntity))))
                .addQueryCondition(new RawTextQueryPhrase("!test_of_last_run={null}"));

        return queryBuilder.build();
    }

    public static GroupEntityCollection getNativeStatusCoverageForRunsWithoutStatus(SpaceConfiguration sc, OctaneEntity octaneEntity, OctaneEntityTypeDescriptor typeDescriptor, long workspaceId) {
        //https://localhost:8080/api/shared_spaces/1001/workspaces/1002/runs/groups?&query=%22test_of_last_run={covered_requirement={path=%270000000001OT0002K9*%27}};!test_of_last_run={null};status={null}%22&group_by=native_status

        String url = String.format(PluginConstants.PUBLIC_API_WORKSPACE_LEVEL_ENTITIES, sc.getLocationParts().getSpaceId(), workspaceId, "runs/groups");
        Map<String, String> headers = createHeaderMapWithOctaneClientType();
        headers.put(RestConnector.HEADER_ACCEPT, RestConnector.HEADER_APPLICATION_JSON);

        OctaneQueryBuilder queryBuilder = OctaneQueryBuilder.create()
                .addGroupBy("native_status")
                .addQueryCondition(new CrossQueryPhrase("test_of_last_run", new CrossQueryPhrase(typeDescriptor.getTestReferenceField(), createGetEntityCondition(octaneEntity))))
                .addQueryCondition(new RawTextQueryPhrase("!test_of_last_run={null}"))
                .addQueryCondition(new RawTextQueryPhrase("status={null}"));

        String queryParam = queryBuilder.build();

        String responseStr = sc.getRestConnector().httpGet(url, Arrays.asList(queryParam), headers).getResponseData();
        GroupEntityCollection col = OctaneEntityParser.parseGroupCollection(responseStr);
        return col;
    }

    public static int getTotalTestsCount(SpaceConfiguration sc, OctaneEntity octaneEntity, OctaneEntityTypeDescriptor typeDescriptor, long workspaceId) {
        //http://localhost:8080/api/shared_spaces/1001/workspaces/1002/tests?fields=id&limit=1&query="((covered_content={(path='0000000000XC*')});((!(subtype='test_suite'))))"

        String url = String.format(PluginConstants.PUBLIC_API_WORKSPACE_LEVEL_ENTITIES, sc.getLocationParts().getSpaceId(), workspaceId, "tests");
        Map<String, String> headers = createHeaderMapWithOctaneClientType();
        headers.put(RestConnector.HEADER_ACCEPT, RestConnector.HEADER_APPLICATION_JSON);

        String queryParam = OctaneQueryBuilder.create()
                .addQueryCondition(new CrossQueryPhrase(typeDescriptor.getTestReferenceField(), createGetEntityCondition(octaneEntity)))
                //.addQueryCondition(new NegativeQueryPhrase(new LogicalQueryPhrase("subtype", "test_suite")))
                .addPageSize(1)
                .addSelectedFields("id")
                .build();

        String responseStr = sc.getRestConnector().httpGet(url, Arrays.asList(queryParam), headers).getResponseData();
        OctaneEntityCollection col = OctaneEntityParser.parseCollection(responseStr);
        return col.getTotalCount();
    }

    private static QueryPhrase createGetEntityCondition(OctaneEntity octaneEntity) {
        String path = octaneEntity.getString(PluginConstants.PATH);
        return new LogicalQueryPhrase(PluginConstants.PATH, path + "*");
    }

    public static OctaneEntityCollection getEntitiesByCondition(SpaceConfiguration sc, long workspaceId, String collectionName, Collection<QueryPhrase> conditions, Collection<String> fields, Integer limit, Integer offset) {

        String queryCondition = OctaneQueryBuilder.create().addQueryConditions(conditions).addSelectedFields(fields).addPageSize(limit).addStartIndex(offset).build();
        String url;
        if (PluginConstants.SPACE_CONTEXT == workspaceId) {
            url = String.format(PluginConstants.PUBLIC_API_SHAREDSPACE_LEVEL_ENTITIES,
                    sc.getLocationParts().getSpaceId(), collectionName);
        } else {
            url = String.format(PluginConstants.PUBLIC_API_WORKSPACE_LEVEL_ENTITIES,
                    sc.getLocationParts().getSpaceId(), workspaceId, collectionName);
        }

        Map<String, String> headers = createHeaderMapWithOctaneClientType();
        headers.put(RestConnector.HEADER_ACCEPT, RestConnector.HEADER_APPLICATION_JSON);

        String responseStr = sc.getRestConnector().httpGet(url, Arrays.asList(queryCondition), headers).getResponseData();
        OctaneEntityCollection col = OctaneEntityParser.parseCollection(responseStr);
        return col;
    }

    public static Set<String> getSupportedOctaneTypes(SpaceConfiguration sc, long workspaceId, String udfName) {
        long spaceId = sc.getLocationParts().getSpaceId();
        String entityCollectionUrl = String.format(PluginConstants.PUBLIC_API_WORKSPACE_LEVEL_ENTITIES, spaceId, workspaceId, "metadata/fields");
        Map<String, String> headers = createHeaderMapWithOctaneClientType();
        headers.put(RestConnector.HEADER_ACCEPT, RestConnector.HEADER_APPLICATION_JSON);

        Collection<QueryPhrase> conditions = new ArrayList<>();
        conditions.add(new LogicalQueryPhrase("name", udfName));
        conditions.add(new InQueryPhrase("entity_name", OctaneEntityTypeManager.getSupportedTypes()));
        conditions.add(new LogicalQueryPhrase("field_type", "string"));
        conditions.add(new LogicalQueryPhrase("max_length", 255));

        String queryCondition = OctaneQueryBuilder.create().addQueryConditions(conditions).build();

        String entitiesCollectionStr = sc.getRestConnector().httpGet(entityCollectionUrl, Arrays.asList(queryCondition), headers).getResponseData();
        OctaneEntityCollection fields = OctaneEntityParser.parseCollection(entitiesCollectionStr);
        Set<String> foundTypes = fields.getData().stream().map(e -> e.getString("entity_name")).collect(Collectors.toSet());

        return foundTypes;
    }

    private static Map<String, String> createHeaderMapWithOctaneClientType() {
        Map<String, String> headers = new HashMap<>();
        headers.put("HPECLIENTTYPE", "HPE_CI_CLIENT");
        return headers;
    }

    public static Set<String> getPossibleJiraFields(SpaceConfiguration sc, long workspaceId) {
        //https://mqalb011sngx.saas.hpe.com/api/shared_spaces/3004/workspaces/2002/metadata/fields?&query=%22field_type=%27string%27;is_user_defined=true;(entity_name+IN+%27feature%27,%27application_module%27,%27requirement_document%27,%27story%27)%22
        long spaceId = sc.getLocationParts().getSpaceId();
        String entityCollectionUrl = String.format(PluginConstants.PUBLIC_API_WORKSPACE_LEVEL_ENTITIES, spaceId, workspaceId, "metadata/fields");
        Map<String, String> headers = createHeaderMapWithOctaneClientType();
        headers.put(RestConnector.HEADER_ACCEPT, RestConnector.HEADER_APPLICATION_JSON);

        String queryCondition = OctaneQueryBuilder.create()
                .addQueryCondition(new LogicalQueryPhrase("field_type", "string"))
                .addQueryCondition(new LogicalQueryPhrase("is_user_defined", true))
                .addQueryCondition(new InQueryPhrase("entity_name", OctaneEntityTypeManager.getSupportedTypes()))
                .build();

        String collectionStr = sc.getRestConnector().httpGet(entityCollectionUrl, Arrays.asList(queryCondition), headers).getResponseData();
        OctaneEntityCollection fields = OctaneEntityParser.parseCollection(collectionStr);
        Set<String> foundJiraNames = fields.getData().stream().map(e -> e.getString("name")).filter(n -> n.toLowerCase().contains("jira")).collect(Collectors.toSet());

        return foundJiraNames;
    }

    public static OctaneEntityCollection getWorkspaces(SpaceConfiguration spaceConfiguration) {
        String getWorspacesUrl = String.format(PluginConstants.PUBLIC_API_SHAREDSPACE_LEVEL_ENTITIES, spaceConfiguration.getLocationParts().getSpaceId(), "workspaces");
        String queryString = OctaneQueryBuilder.create().addSelectedFields("id", "name").build();
        Map<String, String> headers = new HashMap<>();
        headers.put(RestConnector.HEADER_ACCEPT, RestConnector.HEADER_APPLICATION_JSON);
        String entitiesCollectionStr = spaceConfiguration.getRestConnector().httpGet(getWorspacesUrl, Arrays.asList(queryString), headers).getResponseData();

        OctaneEntityCollection workspaces = OctaneEntityParser.parseCollection(entitiesCollectionStr);
        if (workspaces.getData().isEmpty()) {
            throw new IllegalArgumentException("Incorrect space ID.");
        }
        return workspaces;
    }

    public static RestConnector getRestConnector(String baseUrl, String clientId, String clientSecret) {
        try {
            RestConnector restConnector = new RestConnector();
            restConnector.setBaseUrl(baseUrl);
            restConnector.setCredentials(clientId, clientSecret);
            boolean isConnected = restConnector.login();
            if (!isConnected) {
                throw new IllegalArgumentException("Failed to authenticate.");
            } else {
                return restConnector;
            }
        } catch (Exception exc) {
            String myErrorMessage;
            if (exc.getMessage().contains("platform.not_authorized")) {
                myErrorMessage = "Ensure your credentials are correct.";
            } else if (exc.getCause() != null && exc.getCause() instanceof SSLHandshakeException && exc.getCause().getMessage().contains("Received fatal alert")) {
                myErrorMessage = "Network exception, proxy settings may be missing or misconfigured.";
            } else if (exc.getMessage().startsWith("Connection timed out")) {
                myErrorMessage = "Timed out exception, proxy settings may be missing or misconfigured.";
            } else if (exc.getMessage().contains("Network Error")) {
                myErrorMessage = "Network error, proxy settings may be missing or misconfigured.";
            } else if (exc.getMessage().contains("unable to find valid certification")) {
                myErrorMessage = "The store does not contain the appropriate certificate.";
            } else if (exc.getMessage().contains("Request Error (invalid_request)")) {
                myErrorMessage = "Invalid request, proxy settings may be missing or misconfigured.";
            } else if (exc.getCause() != null && exc.getCause() instanceof UnknownHostException) {
                myErrorMessage = "Location is not available.";
            } else {
                myErrorMessage = exc.getMessage();
            }
            throw new IllegalArgumentException(myErrorMessage);
        }
    }
}