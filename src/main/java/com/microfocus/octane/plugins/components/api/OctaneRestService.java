package com.microfocus.octane.plugins.components.api;

import com.microfocus.octane.plugins.rest.entities.groups.GroupEntity;
import com.microfocus.octane.plugins.rest.entities.groups.GroupEntityCollection;

import java.util.List;
import java.util.Map;

public interface OctaneRestService {
	GroupEntityCollection getCoverage(int applicationModuleId);

	void getTests();
}