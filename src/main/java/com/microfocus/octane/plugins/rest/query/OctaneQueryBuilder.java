/*
 *     Copyright 2017 EntIT Software LLC, a Micro Focus company, L.P.
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */


package com.microfocus.octane.plugins.rest.query;

import org.apache.commons.lang.SerializationUtils;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by berkovir on 06/12/2016.
 */
public class OctaneQueryBuilder implements Serializable {


	private StringBuilder sb;
	private Integer pageSize;
	private Integer startIndex;
	private List<String> selectedFields;
	private List<String> orderBy;
	private List<String> groupBy;
	private Collection<QueryPhrase> queryConditions;

	public static OctaneQueryBuilder create() {
		return new OctaneQueryBuilder();
	}

	public OctaneQueryBuilder addQueryCondition(QueryPhrase condition) {
		if (queryConditions == null) {
			queryConditions = new ArrayList<>();
		}
		queryConditions.add(condition);
		return this;
	}

	public OctaneQueryBuilder addQueryConditions(Collection<QueryPhrase> conditions) {
		if (queryConditions == null) {
			queryConditions = new ArrayList<>();
		}
		queryConditions.addAll(conditions);
		return this;
	}

	public OctaneQueryBuilder addStartIndex(Integer startIndex) {
		this.startIndex = startIndex;
		return this;
	}

	public OctaneQueryBuilder addPageSize(int pageSize) {
		this.pageSize = pageSize;
		return this;
	}

	public OctaneQueryBuilder addSelectedFields(String... fieldNames) {
		return addSelectedFields(Arrays.asList(fieldNames));
	}

	public OctaneQueryBuilder addSelectedFields(Collection<String> fieldNames) {
		if (fieldNames != null && !fieldNames.isEmpty()) {
			if (selectedFields == null) {
				selectedFields = new ArrayList<>();
			}
			selectedFields.addAll(fieldNames);
		}
		return this;
	}

	public OctaneQueryBuilder addOrderBy(String... fieldNames) {
		return addOrderBy(Arrays.asList(fieldNames));
	}

	public OctaneQueryBuilder addOrderBy(Collection<String> fieldNames) {
		if (fieldNames != null && !fieldNames.isEmpty()) {
			if (orderBy == null) {
				orderBy = new ArrayList<>();
			}
			orderBy.addAll(fieldNames);
		}
		return this;
	}

	public OctaneQueryBuilder addGroupBy(String fieldName) {

		if (groupBy == null) {
			groupBy = new ArrayList<>();
		}
		groupBy.add(fieldName);

		return this;
	}

	public String build() {
		sb = new StringBuilder();
		buildQuery();

		buildPageSize();
		buildStartIndex();

		buildSelectedFields();
		buildOrderBy();
		buildGroupBy();
		return sb.toString();
	}


	private void buildSelectedFields() {
		if (selectedFields != null && !selectedFields.isEmpty()) {
			sb.append("&").append("fields=").append(StringUtils.join(selectedFields, ","));
		}
	}

	private void buildQuery() {
		// query="id='100'"
		if (queryConditions != null && !queryConditions.isEmpty()) {
			String splitter = "";
			sb.append("&").append("query=\"");

			//query="id>100;status='open';(rank>10||rank<20)"

			String join = queryConditions.stream().map(q -> buildPhraseString(q)).collect(Collectors.joining(";"));
			sb.append(join);

			sb.append("\"");
		}
	}

	private static String buildPhraseString(QueryPhrase phrase) {
		String output = null;
		if (phrase instanceof LogicalQueryPhrase) {
			LogicalQueryPhrase logicalPhrase = (LogicalQueryPhrase) phrase;

			List<String> expStrings = new ArrayList<>();
			for (QueryExpression exp : logicalPhrase.getExpressions()) {
				String comparisonOperator = exp.getOperator().getValue();
				String valueStr = getExpressionValueString(exp.getValue());
				String expStr = String.format("%s%s%s", logicalPhrase.getFieldName(), comparisonOperator, valueStr);
				expStrings.add(expStr);
			}
			output = StringUtils.join(expStrings, "||");
			if (expStrings.size() > 1) {
				output = "(" + output + ")";
			}
		} else if (phrase instanceof CrossQueryPhrase) {
			//release={id=5002}
			CrossQueryPhrase crossPhrase = (CrossQueryPhrase) phrase;
			String expStr = String.format("%s={%s}", crossPhrase.getFieldName(), buildPhraseString(crossPhrase.getQueryPhrase()));
			output = expStr;
		} else if (phrase instanceof NegativeQueryPhrase) {
			NegativeQueryPhrase negativePhrase = (NegativeQueryPhrase) phrase;
			String expStr = String.format("!%s", buildPhraseString(negativePhrase.getQueryPhrase()));
			output = expStr;
		} else if (phrase instanceof InQueryPhrase) {
			InQueryPhrase inQueryPhrase = (InQueryPhrase) phrase;
			StringBuilder sb = new StringBuilder("(");
			sb.append(inQueryPhrase.getFieldName());
			sb.append("+IN+");

			String values = inQueryPhrase.getValues().stream().map(v -> getExpressionValueString(v)).collect(Collectors.joining(","));
			sb.append(values);

			sb.append(")");
			output = sb.toString();
		} else if (phrase instanceof NullQueryPhrase) {
			output = "null";
		} else if (phrase instanceof RawTextQueryPhrase) {
			RawTextQueryPhrase rawTextQueryPhrase = (RawTextQueryPhrase) phrase;
			output = rawTextQueryPhrase.getRawText();
		} else {
			throw new UnsupportedOperationException();
		}
		return output;
	}

	private static String getExpressionValueString(Object value) {
		if (value instanceof Number || value instanceof Boolean) {
			return "" + value;
		} else {
			String str = value == null ? "null" : value.toString();
			str = str.replace("\'", "*").replace("\"", "\\\"");
			str = "'" + str + "'";
			return str;
		}
	}


	private void buildStartIndex() {
		if (startIndex != null) {
			sb.append("&").append("offset=").append(startIndex);
		}
	}

	private void buildPageSize() {
		if (pageSize != null) {
			sb.append("&").append("limit=").append(pageSize);
		}
	}

	private void buildOrderBy() {
		if (orderBy != null && !orderBy.isEmpty()) {
			sb.append("&").append("order_by=").append("{").append(StringUtils.join(orderBy, ",")).append("}");
		}
	}

	private void buildGroupBy() {
		if (groupBy != null && !groupBy.isEmpty()) {
			sb.append("&").append("group_by=").append(StringUtils.join(groupBy, ","));
		}
	}

	public OctaneQueryBuilder clone() {
		OctaneQueryBuilder qb = (OctaneQueryBuilder) SerializationUtils.clone(this);
		return qb;
	}

	public static String encodeParam(String param) {
		String ret;

		try {
			ret = URLEncoder.encode(param, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			ret = "";
		}

		return ret;
	}
}
