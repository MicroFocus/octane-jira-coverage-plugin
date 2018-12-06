package com.microfocus.octane.plugins.rest;

import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.microfocus.octane.plugins.rest.entities.OctaneEntity;
import com.microfocus.octane.plugins.rest.entities.OctaneEntityCollection;
import com.microfocus.octane.plugins.rest.entities.groups.GroupEntity;
import com.microfocus.octane.plugins.rest.entities.groups.GroupEntityCollection;

import java.util.Iterator;

public class OctaneEntityParser {

	public static GroupEntityCollection parseGroupCollection(JSONObject jsonObj) throws JSONException {
		GroupEntityCollection coll = new GroupEntityCollection();

		int total = jsonObj.getInt("groupsTotalCount");
		coll.setGroupsTotalCount(total);


		JSONArray entitiesJArr = jsonObj.getJSONArray("groups");
		for (int i = 0; i < entitiesJArr.length(); i++) {

			JSONObject entObj = entitiesJArr.getJSONObject(i);
			GroupEntity entity = parseGroupEntity(entObj);
			coll.getGroups().add(entity);
		}

		return coll;
	}

	public static OctaneEntityCollection parseCollection(JSONObject jsonObj) throws JSONException {
		OctaneEntityCollection coll = new OctaneEntityCollection();

		int total = jsonObj.getInt("total_count");
		coll.setTotalCount(total);

		if (jsonObj.has("exceeds_total_count")) {
			boolean exceedsTotalCount = jsonObj.getBoolean("exceeds_total_count");
			coll.setExceedsTotalCount(exceedsTotalCount);
		}

		JSONArray entitiesJArr = jsonObj.getJSONArray("data");
		for (int i = 0; i < entitiesJArr.length(); i++) {

			JSONObject entObj = entitiesJArr.getJSONObject(i);
			OctaneEntity entity = parseEntity(entObj);

			coll.getData().add(entity);
		}

		return coll;
	}

	public static OctaneEntity parseEntity(JSONObject entObj) throws JSONException {

		String type = entObj.getString("type");

		OctaneEntity entity = new OctaneEntity(type);
		for (Iterator<String> it = entObj.keys(); it.hasNext(); ) {
			String key = it.next();
			Object value = entObj.get(key);
			if (value instanceof JSONObject) {
				JSONObject jObj = (JSONObject) value;
				if (jObj.has("type")) {
					OctaneEntity valueEntity = parseEntity(jObj);
					value = valueEntity;
				} else if (jObj.has("total_count")) {
					OctaneEntityCollection coll = parseCollection(jObj);
					value = coll;
				} else {
					value = jObj.toString();
				}
			}
			entity.put(key, value);
		}
		return entity;
	}

	public static GroupEntity parseGroupEntity(JSONObject entObj) throws JSONException {

		GroupEntity groupEntity = new GroupEntity();
		int count = entObj.getInt("count");
		groupEntity.setCount(count);
		if(!entObj.isNull("value")){
			JSONObject jsonObject = entObj.getJSONObject("value");
			OctaneEntity entity = parseEntity(jsonObject);
			groupEntity.setValue(entity);
		}

		return groupEntity;
	}
}
