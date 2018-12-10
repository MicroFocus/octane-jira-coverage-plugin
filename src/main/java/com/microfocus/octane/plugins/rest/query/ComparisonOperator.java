package com.microfocus.octane.plugins.rest.query;

public enum ComparisonOperator {

	Equal("="),

	Greater(">"),

	GreaterOrEqual(">="),

	Less("<"),

	LessOrEqual("<=");

	private final String value;

	ComparisonOperator(String value) {
		this.value = value;
	}

	public String getValue(){
		return value;
	}
}
