package com.microfocus.octane.plugins.components.impl;

import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.microfocus.octane.plugins.components.api.Constants;
import com.microfocus.octane.plugins.components.api.OctaneRestService;
import com.microfocus.octane.plugins.configuration.OctaneConfiguration;
import com.microfocus.octane.plugins.configuration.OctaneConfigurationChangedListener;
import com.microfocus.octane.plugins.configuration.OctaneConfigurationManager;
import com.microfocus.octane.plugins.rest.OctaneEntityParser;
import com.microfocus.octane.plugins.rest.RestConnector;
import com.microfocus.octane.plugins.rest.entities.OctaneEntity;
import com.microfocus.octane.plugins.rest.entities.OctaneEntityCollection;
import com.microfocus.octane.plugins.rest.entities.groups.GroupEntityCollection;
import com.microfocus.octane.plugins.rest.query.*;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

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
		octaneConfiguration = OctaneConfigurationManager.loadConfiguration(pluginSettingsFactory);
		if (octaneConfiguration.parseLocation()) {
			restConnector.setBaseUrl(octaneConfiguration.getBaseUrl());
			restConnector.setCredentials(octaneConfiguration.getClientId(), octaneConfiguration.getClientSecret());
		}
	}

	@Override
	public GroupEntityCollection getCoverage(String applicationModulePath) {
		//http://localhost:8080/api/shared_spaces/1001/workspaces/1002/runs/groups?query="test_of_last_run={product_areas={(id IN '2001')}}"&group_by=status
		String url = String.format(Constants.PUBLIC_API_WORKSPACE_LEVEL_ENTITIES, octaneConfiguration.getSharespaceId(), octaneConfiguration.getWorkspaceId(), "runs/groups");
		Map<String, String> headers = new HashMap<>();
		headers.put(RestConnector.HEADER_ACCEPT, RestConnector.HEADER_APPLICATION_JSON);

		String queryParam = OctaneQueryBuilder.create()
				.addGroupBy("status")
				.addQueryCondition(new RawTextQueryPhrase(String.format("(test_of_last_run={(product_areas={(path='%s*')})})", applicationModulePath)))
				.addQueryCondition(new InQueryPhrase("subtype", Arrays.asList("run_automated", "gherkin_automated_run", "run_manual")))
				.addQueryCondition(new LogicalQueryPhrase("latest_pipeline_run", true))
				.addQueryCondition(new RawTextQueryPhrase("!test_of_last_run={null}")).build();

		//https://center.almoctane.com/api/shared_spaces/1001/workspaces/1002/runs/groups?query="test_of_last_run={product_areas={(id IN '89009')}}"&group_by=status

		try {
			String responseStr = restConnector.httpGet(url, Arrays.asList(queryParam), headers).getResponseData();
			GroupEntityCollection col = OctaneEntityParser.parseGroupCollection(new JSONObject(responseStr));
			return col;
		} catch (Exception e) {
			throw new RuntimeException("Failed to getCoverage : " + e.getMessage(), e);
		}
	}

	@Override
	public OctaneEntityCollection getEntitiesByCondition(String collectionName, QueryPhrase phrase) {

		String queryCondition = OctaneQueryBuilder.create().addQueryCondition(phrase).build();
		String url = String.format(Constants.PUBLIC_API_WORKSPACE_LEVEL_ENTITIES,
				octaneConfiguration.getSharespaceId(), octaneConfiguration.getWorkspaceId(), collectionName);

		Map<String, String> headers = new HashMap<>();
		headers.put(RestConnector.HEADER_ACCEPT, RestConnector.HEADER_APPLICATION_JSON);
		headers.put("HPECLIENTTYPE", "HPE_CI_CLIENT");

		try {
			String responseStr = restConnector.httpGet(url, Arrays.asList(queryCondition), headers).getResponseData();
			OctaneEntityCollection col = OctaneEntityParser.parseCollection(new JSONObject(responseStr));
			return  col;
		} catch (Exception e) {
			throw new RuntimeException("Failed to getEntityById : " + e.getMessage(), e);
		}
	}

	@Override
	public void onOctaneConfigurationChanged() {
		reloadConfiguration();
	}
}