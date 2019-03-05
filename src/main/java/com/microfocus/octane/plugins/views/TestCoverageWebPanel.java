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

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugin.webfragment.contextproviders.AbstractJiraContextProvider;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.microfocus.octane.plugins.components.api.OctaneRestService;
import com.microfocus.octane.plugins.configuration.OctaneConfigurationManager;
import com.microfocus.octane.plugins.configuration.WorkspaceConfiguration;
import com.microfocus.octane.plugins.descriptors.AggregateDescriptor;
import com.microfocus.octane.plugins.descriptors.OctaneEntityTypeDescriptor;
import com.microfocus.octane.plugins.descriptors.OctaneEntityTypeManager;
import com.microfocus.octane.plugins.rest.RestStatusException;
import com.microfocus.octane.plugins.rest.entities.OctaneEntity;
import com.microfocus.octane.plugins.rest.entities.OctaneEntityCollection;
import com.microfocus.octane.plugins.rest.query.InQueryPhrase;
import com.microfocus.octane.plugins.rest.query.QueryPhrase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Scanned
public class TestCoverageWebPanel extends AbstractJiraContextProvider {

    private static final Logger log = LoggerFactory.getLogger(TestCoverageWebPanel.class);
    private OctaneConfigurationManager configurationManager = OctaneConfigurationManager.getInstance();
    private OctaneRestService octaneRestService;
    private Map<String, Map<String, Object>> cache = new HashMap<>();

    private final static String UDF_NOT_DEFINED_IN_OCTANE = "platform.unknown_field";

    public TestCoverageWebPanel(OctaneRestService octaneRestService) {
        this.octaneRestService = octaneRestService;
    }


    @Override
    public Map<String, Object> getContextMap(ApplicationUser applicationUser, JiraHelper jiraHelper) {
        Issue currentIssue = (Issue) jiraHelper.getContextParams().get("issue");
        if (cache.containsKey(currentIssue.getKey())) {
            //return cache only if it was created upto 5 sec ago
            Map<String, Object> cachedContextMap = cache.get(currentIssue.getKey());
            long timestamp = (long) cachedContextMap.get("timestamp");
            if (timestamp + TimeUnit.SECONDS.toMillis(5) > System.currentTimeMillis()) {
                return cachedContextMap;
            }

        }
        Map<String, Object> contextMap = new HashMap<>();
        log.trace("configurationManager.isValidConfiguration() = " + configurationManager.isValidConfiguration());
        LinkedHashMap<String, Long> perf = new LinkedHashMap<>();
        long startTotal = System.currentTimeMillis();
        if (configurationManager.isValidConfiguration()) {
            try {
                WorkspaceConfiguration workspaceConfig = configurationManager.getConfiguration().getWorkspaces().stream()
                        .filter(w -> w.getJiraProjects().contains(jiraHelper.getProject().getKey())).findFirst().get();//we use get without validation because validation is done in condition

                QueryPhrase jiraKeyCondition = new InQueryPhrase(workspaceConfig.getOctaneUdf(), Arrays.asList(currentIssue.getKey(), currentIssue.getId().toString()));
                boolean found = false;
                for (AggregateDescriptor aggDescriptor : OctaneEntityTypeManager.getAggregators()) {
                    boolean isMatch = false;
                    for (OctaneEntityTypeDescriptor entityDesc : aggDescriptor.getDescriptors()) {
                        if (workspaceConfig.getOctaneEntityTypes().contains(entityDesc.getTypeName())) {
                            isMatch = true;
                            break;
                        }
                    }
                    if (isMatch) {
                        long start = System.currentTimeMillis();
                        OctaneEntity octaneEntity = tryFindMatchingOctaneEntity(workspaceConfig, contextMap, jiraKeyCondition, aggDescriptor);
                        long duration = System.currentTimeMillis() - start;
                        perf.put(aggDescriptor.getCollectionName(), duration);
                        if (octaneEntity != null) {
                            OctaneEntityTypeDescriptor typeDescriptor = (aggDescriptor.isSubtyped() ? OctaneEntityTypeManager.getByTypeName(octaneEntity.getString("subtype")) :
                                    OctaneEntityTypeManager.getByTypeName(octaneEntity.getType()));

                            //all tests
                            start = System.currentTimeMillis();
                            CoverageUiHelper.getAllTestsAndFillContextMap(octaneRestService, octaneEntity, typeDescriptor, workspaceConfig, contextMap);
                            perf.put("allTests", System.currentTimeMillis() - start);

                            //coverage
                            start = System.currentTimeMillis();
                            CoverageUiHelper.getCoverageAndFillContextMap(octaneRestService, octaneEntity, typeDescriptor, workspaceConfig, contextMap);
                            perf.put("coverage", System.currentTimeMillis() - start);
                            found = true;
                            break;
                        }
                    }
                }

                if (!found) {
                    contextMap.put("status", "noData");
                } else {
                    //context map is filled
                }
            } catch (RestStatusException e) {
                if (e.getResponse().getStatusCode() == 401) {
                    //credentials issue
                } else {
                    log.error("Failed to fill ContextMap (1) : " + e.getMessage());
                }
            } catch (Exception e) {
                log.error("Failed to fill ContextMap (2) : " + e.getMessage());
            }
        }

        if (!contextMap.containsKey("status")) {
            contextMap.put("status", "noValidConfiguration");
            String configUrl = ComponentAccessor.getApplicationProperties().getString(APKeys.JIRA_BASEURL) + "/plugins/servlet/admin/octane";
            contextMap.put("configUrl", configUrl);
        }

        boolean showPerf = (boolean) OctaneConfigurationManager.getInstance().getUserParameter(applicationUser.getUsername(), OctaneConfigurationManager.SHOW_PERF_PARAMETER, false);
        if (showPerf) {
            perf.put("total", System.currentTimeMillis() - startTotal);
            contextMap.put("perf", perf.entrySet().stream().map(entry -> entry.getKey() + " " + entry.getValue()).collect(Collectors.joining("; ")));
        }

        contextMap.put("timestamp", System.currentTimeMillis());
        cache.put(currentIssue.getKey(), contextMap);
        return contextMap;
    }

    private OctaneEntity tryFindMatchingOctaneEntity(WorkspaceConfiguration workspaceConfiguration, Map<String, Object> contextMap, QueryPhrase jiraKeyCondition, AggregateDescriptor aggrDescriptor) {
        try {
            List<QueryPhrase> conditions = new ArrayList<>();
            conditions.add(jiraKeyCondition);

            List<String> fields = new ArrayList<>();
            fields.add("name");
            if (aggrDescriptor.isSubtyped()) {
                fields.add("subtype");
                List<String> typeNames = aggrDescriptor.getDescriptors().stream().map(OctaneEntityTypeDescriptor::getTypeName).collect(Collectors.toList());
                QueryPhrase subTypeCondition = new InQueryPhrase("subtype", typeNames);
                conditions.add(subTypeCondition);
            } else {
                if (aggrDescriptor.getDescriptors().get(0).isHierarchicalEntity()) {
                    fields.add("path");
                }
            }
            OctaneEntityCollection entities = octaneRestService.getEntitiesByCondition(workspaceConfiguration.getWorkspaceId(), aggrDescriptor.getCollectionName(), conditions, fields);
            if (!entities.getData().isEmpty()) {
                OctaneEntity octaneEntity = entities.getData().get(0);
                return octaneEntity;
            }
        } catch (RestStatusException e) {
            if (UDF_NOT_DEFINED_IN_OCTANE.equals(e.getErrorCode())) {
                //field is not defined - skip
            } else {
                throw e;
            }
        }
        return null;
    }

}
