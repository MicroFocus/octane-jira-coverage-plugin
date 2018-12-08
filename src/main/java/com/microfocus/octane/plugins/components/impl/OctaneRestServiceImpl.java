package com.microfocus.octane.plugins.components.impl;

import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.net.RequestFactory;
import com.microfocus.octane.plugins.components.api.Constants;
import com.microfocus.octane.plugins.components.api.OctaneConfiguration;
import com.microfocus.octane.plugins.components.api.OctaneRestService;
import com.microfocus.octane.plugins.rest.OctaneEntityParser;
import com.microfocus.octane.plugins.rest.RestConnector;
import com.microfocus.octane.plugins.rest.entities.OctaneEntityCollection;
import com.microfocus.octane.plugins.rest.entities.groups.GroupEntity;
import com.microfocus.octane.plugins.rest.entities.groups.GroupEntityCollection;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.*;

@ExportAsService({OctaneRestService.class})
@Named("octaneRestService")
public class OctaneRestServiceImpl implements OctaneRestService {

	Map<String, String> cookies = new HashMap<>();

	@ComponentImport
	private final ApplicationProperties applicationProperties;

	private OctaneConfiguration octaneConfiguration;
	private RestConnector restConnector = new RestConnector();

	@Inject
	public OctaneRestServiceImpl(final ApplicationProperties applicationProperties) {
		this.applicationProperties = applicationProperties;

		//login();
	}

	private void login() {
		octaneConfiguration = new OctaneConfiguration().setBaseUrl("http://localhost:8080").setUserName("sa@nga").setPassword("Welcome1").setSharedspaceId(1001).setWorkspaceId(1002);
		restConnector.setBaseUrl(octaneConfiguration.getBaseUrl());
		restConnector.setCredentials(octaneConfiguration.getUserName(), octaneConfiguration.getPassword());
		restConnector.login();
	}

	@Override
	public GroupEntityCollection getCoverage(int applicationModuleId) {
		//http://localhost:8080/api/shared_spaces/1001/workspaces/1002/runs/groups?query="test_of_last_run={product_areas={(id IN '2001')}}"&group_by=status
		String url = String.format(Constants.PUBLIC_API_WORKSPACE_LEVEL_ENTITIES, octaneConfiguration.getSharedspaceId(), octaneConfiguration.getWorkspaceId(), "runs/groups");
		Map<String, String> headers = new HashMap<>();
		headers.put(RestConnector.HEADER_ACCEPT, RestConnector.HEADER_APPLICATION_JSON);

		Set<String> queryParams = new HashSet<>();
		queryParams.add("group_by=status");
		queryParams.add(RestConnector.encodeParam(String.format("test_of_last_run={product_areas={(id IN '%s')}}", applicationModuleId)));

		String responseStr = restConnector.httpGet(url, queryParams, headers).getResponseData();
		try {
			GroupEntityCollection col = OctaneEntityParser.parseGroupCollection(new JSONObject(responseStr));
			return col;

		} catch (Exception e) {
			throw new RuntimeException("Failed to getCoverage : " + e.getMessage(), e);
		}
	}

	@Override
	public void getTests() {
		String entityCollectionUrl = String.format(Constants.PUBLIC_API_WORKSPACE_LEVEL_ENTITIES, octaneConfiguration.getSharedspaceId(), octaneConfiguration.getWorkspaceId(), "tests");

		Map<String, String> headers = new HashMap<>();
		headers.put(RestConnector.HEADER_ACCEPT, RestConnector.HEADER_APPLICATION_JSON);

		String entitiesCollectionStr = restConnector.httpGet(entityCollectionUrl, null, headers).getResponseData();

		try {
			JSONObject jsonObj = new JSONObject(entitiesCollectionStr);
			OctaneEntityCollection col = OctaneEntityParser.parseCollection(jsonObj);
			int t2 = 5;
		} catch (JSONException e) {
			int t3 = 5;
		}

		int t = 5;
	}


}