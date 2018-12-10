package com.microfocus.octane.plugins.views;


import com.atlassian.jira.issue.Issue;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.Condition;

import java.util.Map;

public class TestCoverageWebPanelCondition implements Condition {

	@Override
	public void init(Map<String, String> map) throws PluginParseException {
		int t = 5;
	}

	@Override
	public boolean shouldDisplay(Map<String, Object> map) {
		Issue issue = (Issue) map.get("issue");
		String issueType = issue.getIssueType().getName();
		long id = issue.getId();
		boolean isEven = (id % 2) == 0;
		return isEven;
	}
}
