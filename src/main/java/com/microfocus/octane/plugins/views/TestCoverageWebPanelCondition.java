package com.microfocus.octane.plugins.views;


import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.Condition;

import java.util.Map;

public class TestCoverageWebPanelCondition implements Condition {

	@Override
	public void init(Map<String, String> map) throws PluginParseException {
		int t=5;
	}

	@Override
	public boolean shouldDisplay(Map<String, Object> map) {
		return true;
	}
}
