package com.microfocus.octane.plugins.components.impl;

import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.microfocus.octane.plugins.configuration.OctaneConfigurationChangedListener;
import com.microfocus.octane.plugins.configuration.OctaneConfigurationManager;
import com.microfocus.octane.plugins.components.api.Constants;
import com.microfocus.octane.plugins.configuration.OctaneConfiguration;
import com.microfocus.octane.plugins.components.api.OctaneRestService;
import com.microfocus.octane.plugins.rest.OctaneEntityParser;
import com.microfocus.octane.plugins.rest.RestConnector;
import com.microfocus.octane.plugins.rest.entities.groups.GroupEntityCollection;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@ExportAsService({OctaneRestService.class})
@Named("octaneRestService")
public class OctaneRestServiceImpl implements OctaneRestService, OctaneConfigurationChangedListener {

	@ComponentImport
	private final ApplicationProperties applicationProperties;

	@ComponentImport
	private final PluginSettingsFactory pluginSettingsFactory;

	private OctaneConfiguration octaneConfiguration;

	private RestConnector restConnector = new RestConnector();

	@Inject
	public OctaneRestServiceImpl(final PluginSettingsFactory pluginSettingsFactory, final ApplicationProperties applicationProperties) {
		this.applicationProperties = applicationProperties;
		this.pluginSettingsFactory = pluginSettingsFactory;

		OctaneConfigurationManager.addListener(this);
		reloadConfiguration();
	}

	@Override
	public void reloadConfiguration() {
		restConnector.clearAll();
		octaneConfiguration = OctaneConfigurationManager.loadDetailsFromGlobalSettings(pluginSettingsFactory);
		if(octaneConfiguration.parseLocation()){
			restConnector.setBaseUrl(octaneConfiguration.getBaseUrl());
			restConnector.setCredentials(octaneConfiguration.getClientId(), octaneConfiguration.getClientSecret());
		}
	}

	@Override
	public GroupEntityCollection getCoverage(int applicationModuleId) {
		//http://localhost:8080/api/shared_spaces/1001/workspaces/1002/runs/groups?query="test_of_last_run={product_areas={(id IN '2001')}}"&group_by=status
		String url = String.format(Constants.PUBLIC_API_WORKSPACE_LEVEL_ENTITIES, octaneConfiguration.getSharespaceId(), octaneConfiguration.getWorkspaceId(), "runs/groups");
		Map<String, String> headers = new HashMap<>();
		headers.put(RestConnector.HEADER_ACCEPT, RestConnector.HEADER_APPLICATION_JSON);

		Set<String> queryParams = new HashSet<>();
		queryParams.add("group_by=status");
		queryParams.add(RestConnector.encodeParam(String.format("query=\"test_of_last_run={product_areas={(id IN '%s')}}\"", applicationModuleId)));
		//https://center.almoctane.com/api/shared_spaces/1001/workspaces/1002/runs/groups?query="test_of_last_run={product_areas={(id IN '89009')}}"&group_by=status

		try {
			String responseStr = restConnector.httpGet(url, queryParams, headers).getResponseData();
			GroupEntityCollection col = OctaneEntityParser.parseGroupCollection(new JSONObject(responseStr));
			return col;
		} catch (Exception e) {
			throw new RuntimeException("Failed to getCoverage : " + e.getMessage(), e);
		}
	}

	@Override
	public void onOctaneConfigurationChanged() {
		reloadConfiguration();
	}
}