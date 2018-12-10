package com.microfocus.octane.plugins.rest.query;

/**
 * Represent expression value of query, for example "=5" or ">5"
 */
public class QueryExpression {

	private ComparisonOperator operator = ComparisonOperator.Equal;
	private Object value;

	public QueryExpression() {

	}

	public QueryExpression(Object value) {
		this.value = value;
	}

	public QueryExpression(Object value, ComparisonOperator op) {
		this.value = value;
		this.operator = op;
	}

	public Object getValue() {
		return value;
	}


	public ComparisonOperator getOperator() {
		return operator;
	}


}
