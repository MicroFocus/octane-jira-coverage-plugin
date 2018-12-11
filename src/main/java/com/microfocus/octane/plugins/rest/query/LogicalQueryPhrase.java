package com.microfocus.octane.plugins.rest.query;

import java.util.ArrayList;
import java.util.Collection;

public class LogicalQueryPhrase implements QueryPhrase {

	private String fieldName;
	private Collection<QueryExpression> expressions = new ArrayList<>();

	public LogicalQueryPhrase(String fieldName) {
		this.fieldName = fieldName;
	}

	public LogicalQueryPhrase(String fieldName, Object queryValue) {
		this(fieldName);
		addExpression(new QueryExpression(queryValue));
	}

	public LogicalQueryPhrase(String fieldName, Object queryValue, ComparisonOperator op) {
		this(fieldName);
		addExpression(queryValue, op);
	}

	public void addExpression(Object queryValue, ComparisonOperator op) {
		addExpression(new QueryExpression(queryValue, op));
	}

	public void addExpression(QueryExpression expression) {
		this.getExpressions().add(expression);

	}

	public Collection<QueryExpression> getExpressions() {
		return expressions;
	}

	public String getFieldName() {
		return fieldName;
	}
}
