package com.microfocus.octane.plugins.rest.entities.groups;


import com.microfocus.octane.plugins.rest.entities.OctaneEntity;

public class GroupEntity  {

	private int count;

	private OctaneEntity value;

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public OctaneEntity getValue() {
		return value;
	}

	public void setValue(OctaneEntity value) {
		this.value = value;
	}
}
