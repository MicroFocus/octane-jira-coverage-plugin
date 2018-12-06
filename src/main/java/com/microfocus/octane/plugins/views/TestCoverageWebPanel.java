package com.microfocus.octane.plugins.views;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugin.webfragment.contextproviders.AbstractJiraContextProvider;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.microfocus.octane.plugins.components.api.OctaneRestService;
import com.microfocus.octane.plugins.rest.entities.OctaneEntity;
import com.microfocus.octane.plugins.rest.entities.groups.GroupEntity;
import com.microfocus.octane.plugins.rest.entities.groups.GroupEntityCollection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Scanned
public class TestCoverageWebPanel extends AbstractJiraContextProvider {

	private OctaneRestService octaneRestService;

	public TestCoverageWebPanel(OctaneRestService octaneRestService) {

		this.octaneRestService = octaneRestService;
	}

	@Override
	public Map<String, Object> getContextMap(ApplicationUser applicationUser, JiraHelper jiraHelper) {
		Map<String, Object> contextMap = new HashMap<>();
		Issue currentIssue = (Issue) jiraHelper.getContextParams().get("issue");
		//Long issueId = currentIssue.getId(); //TODO USE id for retrieving information from octane

		List<OctaneEntity> groups = new ArrayList<>();
		GroupEntityCollection coverage = octaneRestService.getCoverage(2001);
		Map<String, GroupEntity> id2entity = coverage.getGroups().stream().filter(gr -> gr.getValue() != null).collect(Collectors.toMap(gr -> gr.getValue().getId(), Function.identity()));

		extractAndEnrichEntity(groups, id2entity, "green", "list_node.run_status.passed");
		extractAndEnrichEntity(groups, id2entity, "red", "list_node.run_status.failed");
		extractAndEnrichEntity(groups, id2entity, "blue", "list_node.run_status.planned");
		extractAndEnrichEntity(groups, id2entity, "gray", "list_node.run_status.skipped");

		contextMap.put("groups", groups);

		return contextMap;

	}

	private void extractAndEnrichEntity(List<OctaneEntity> result, Map<String, GroupEntity> id2entity, String color, String id) {
		GroupEntity groupEntity = id2entity.get(id);

		if (groupEntity != null) {
			OctaneEntity entity = groupEntity.getValue();
			entity.put("color", color);
			entity.put("count", groupEntity.getCount());
			result.add(entity);
		}
	}
}
