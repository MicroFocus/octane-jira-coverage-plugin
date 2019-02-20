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

package com.microfocus.octane.plugins.components.impl;

import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.microfocus.octane.plugins.components.api.Constants;
import com.microfocus.octane.plugins.components.api.OctaneRestService;
import com.microfocus.octane.plugins.configuration.OctaneConfigurationChangedListener;
import com.microfocus.octane.plugins.configuration.OctaneConfigurationManager;
import com.microfocus.octane.plugins.configuration.SpaceConfiguration;
import com.microfocus.octane.plugins.descriptors.OctaneEntityTypeDescriptor;
import com.microfocus.octane.plugins.rest.OctaneEntityParser;
import com.microfocus.octane.plugins.rest.RestConnector;
import com.microfocus.octane.plugins.rest.entities.OctaneEntity;
import com.microfocus.octane.plugins.rest.entities.OctaneEntityCollection;
import com.microfocus.octane.plugins.rest.entities.groups.GroupEntityCollection;
import com.microfocus.octane.plugins.rest.query.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.*;
import java.util.stream.Collectors;

@ExportAsService({OctaneRestService.class})
@Named("octaneRestService")
public class OctaneRestServiceImpl implements OctaneRestService, OctaneConfigurationChangedListener {

    private static final Logger log = LoggerFactory.getLogger(OctaneRestServiceImpl.class);

    @ComponentImport
    private final ApplicationProperties applicationProperties;

    @ComponentImport
    private final PluginSettingsFactory pluginSettingsFactory;

    private SpaceConfiguration octaneConfiguration;

    private RestConnector restConnector = new RestConnector();

    @Inject
    public OctaneRestServiceImpl(final PluginSettingsFactory pluginSettingsFactory, final ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
        this.pluginSettingsFactory = pluginSettingsFactory;

        OctaneConfigurationManager.getInstance().init(pluginSettingsFactory);
        OctaneConfigurationManager.getInstance().addListener(this);
        reloadConfiguration();
    }

    @Override
    public void reloadConfiguration() {
        log.debug("before reloadConfiguration");

        restConnector.clearAll();
        try {
            octaneConfiguration = OctaneConfigurationManager.getInstance().getConfiguration();
            restConnector.setBaseUrl(octaneConfiguration.getLocationParts().getBaseUrl());
            restConnector.setCredentials(octaneConfiguration.getClientId(), octaneConfiguration.getClientSecret());
            log.debug("after reloadConfiguration, url= " + octaneConfiguration.getLocationParts().getBaseUrl() + ", clientID=" + octaneConfiguration.getClientId());
        } catch (Exception e) {
            octaneConfiguration = null;
            log.error("Failed to reloadConfiguration : " + e.getClass().getName() + ", message =" + e.getMessage() + ", cause : " + e.getCause());
        }
    }

    @Override
    public GroupEntityCollection getCoverage(OctaneEntity octaneEntity, OctaneEntityTypeDescriptor typeDescriptor, long workspaceId) {
        //http://localhost:8080/api/shared_spaces/1001/workspaces/1002/runs/groups?query="test_of_last_run={product_areas={(id IN '2001')}}"&group_by=status

        String entityCondition = null;
        if (typeDescriptor.isHierarchicalEntity()) {
            String path = octaneEntity.getString("path");
            entityCondition = String.format("path='%s*'", path);
        } else {
            String id = octaneEntity.getString("id");
            entityCondition = String.format("id='%s'", id);
        }

        String url = String.format(Constants.PUBLIC_API_WORKSPACE_LEVEL_ENTITIES, octaneConfiguration.getLocationParts().getSpaceId(), workspaceId, "runs/groups");
        Map<String, String> headers = new HashMap<>();
        headers.put(RestConnector.HEADER_ACCEPT, RestConnector.HEADER_APPLICATION_JSON);

        String queryParam = OctaneQueryBuilder.create()
                .addGroupBy("status")
                .addQueryCondition(new RawTextQueryPhrase(String.format("(test_of_last_run={(%s={(%s)})})", typeDescriptor.getTestReferenceField(), entityCondition)))
                .addQueryCondition(new InQueryPhrase("subtype", Arrays.asList("run_automated", "gherkin_automated_run", "run_manual")))
                .addQueryCondition(new LogicalQueryPhrase("latest_pipeline_run", true))
                .addQueryCondition(new RawTextQueryPhrase("!test_of_last_run={null}")).build();

        //https://center.almoctane.com/api/shared_spaces/1001/workspaces/1002/runs/groups?query="test_of_last_run={product_areas={(id IN '89009')}}"&group_by=status

        String responseStr = restConnector.httpGet(url, Arrays.asList(queryParam), headers).getResponseData();
        GroupEntityCollection col = OctaneEntityParser.parseGroupCollection(responseStr);
        return col;
    }

    @Override
    public OctaneEntityCollection getEntitiesByCondition(long workspaceId, String collectionName, Collection<QueryPhrase> conditions, Collection<String> fields) {

        String queryCondition = OctaneQueryBuilder.create().addQueryConditions(conditions).addSelectedFields(fields).build();
        String url;
        if (SPACE_CONTEXT == workspaceId) {
            url = String.format(Constants.PUBLIC_API_SHAREDSPACE_LEVEL_ENTITIES,
                    octaneConfiguration.getLocationParts().getSpaceId(), collectionName);
        } else {
            url = String.format(Constants.PUBLIC_API_WORKSPACE_LEVEL_ENTITIES,
                    octaneConfiguration.getLocationParts().getSpaceId(), workspaceId, collectionName);
        }

        Map<String, String> headers = new HashMap<>();
        headers.put(RestConnector.HEADER_ACCEPT, RestConnector.HEADER_APPLICATION_JSON);
        headers.put("HPECLIENTTYPE", "HPE_CI_CLIENT");

        String responseStr = restConnector.httpGet(url, Arrays.asList(queryCondition), headers).getResponseData();
        OctaneEntityCollection col = OctaneEntityParser.parseCollection(responseStr);
        return col;
    }

    public List<String> getSupportedOctaneTypes(long workspaceId, String udfName) {
        long spaceId = octaneConfiguration.getLocationParts().getSpaceId();
        String entityCollectionUrl = String.format(Constants.PUBLIC_API_WORKSPACE_LEVEL_ENTITIES, spaceId, workspaceId, "metadata/fields");
        Map<String, String> headers = new HashMap<>();
        headers.put(RestConnector.HEADER_ACCEPT, RestConnector.HEADER_APPLICATION_JSON);

        QueryPhrase fieldNameCondition = new LogicalQueryPhrase("name", udfName);
        Map<String, String> key2LabelType = new HashMap<>();
        key2LabelType.put("feature", OCTANE_ENTITY_FEATURE);
        key2LabelType.put("story", OCTANE_ENTITY_USER_STORY);
        key2LabelType.put("product_area", OCTANE_ENTITY_APPLICATION_MODULE);
        QueryPhrase typeCondition = new InQueryPhrase("entity_name", key2LabelType.keySet());

        String queryCondition = OctaneQueryBuilder.create().addQueryCondition(fieldNameCondition).addQueryCondition(typeCondition).build();
        String entitiesCollectionStr = restConnector.httpGet(entityCollectionUrl, Arrays.asList(queryCondition), headers).getResponseData();
        OctaneEntityCollection fields = OctaneEntityParser.parseCollection(entitiesCollectionStr);
        List<String> foundTypes = fields.getData().stream().map(e -> e.getString("entity_name")).map(key -> key2LabelType.get(key)).sorted().collect(Collectors.toList());

        return foundTypes;
    }

    @Override
    public void onOctaneConfigurationChanged() {
        reloadConfiguration();
    }
}