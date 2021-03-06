/**   
 * License Agreement for OpenSearchServer
 *
 * Copyright (C) 2012-2014 Emmanuel Keller / Jaeksoft
 * 
 * http://www.open-search-server.com
 * 
 * This file is part of OpenSearchServer.
 *
 * OpenSearchServer is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * OpenSearchServer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with OpenSearchServer. 
 *  If not, see <http://www.gnu.org/licenses/>.
 **/

package com.jaeksoft.searchlib.join;

import java.io.IOException;
import java.util.List;

import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.jaeksoft.searchlib.Client;
import com.jaeksoft.searchlib.ClientCatalog;
import com.jaeksoft.searchlib.SearchLibException;
import com.jaeksoft.searchlib.filter.FilterAbstract;
import com.jaeksoft.searchlib.filter.FilterList;
import com.jaeksoft.searchlib.function.expression.SyntaxError;
import com.jaeksoft.searchlib.index.FieldCacheIndex;
import com.jaeksoft.searchlib.index.ReaderAbstract;
import com.jaeksoft.searchlib.request.AbstractRequest;
import com.jaeksoft.searchlib.request.AbstractSearchRequest;
import com.jaeksoft.searchlib.request.SearchFieldRequest;
import com.jaeksoft.searchlib.result.ResultSearchSingle;
import com.jaeksoft.searchlib.result.collector.DocIdInterface;
import com.jaeksoft.searchlib.result.collector.join.JoinUtils;
import com.jaeksoft.searchlib.util.StringUtils;
import com.jaeksoft.searchlib.util.Timer;
import com.jaeksoft.searchlib.util.XPathParser;
import com.jaeksoft.searchlib.util.XmlWriter;
import com.jaeksoft.searchlib.web.ServletTransaction;

public class JoinItem implements Comparable<JoinItem> {

	public interface OuterCollector {
		public void collect(final int id, final String value);
	}

	public static enum JoinType {
		INNER, OUTER;

		private final String label = name().toLowerCase();

		final public static JoinType find(String name) {
			for (JoinType type : values())
				if (type.name().equalsIgnoreCase(name))
					return type;
			return INNER;
		}

		public String getLabel() {
			return label;
		}
	}

	private String indexName;

	private String queryTemplate;

	private FilterList filterList;

	private String queryString;

	private String localField;

	private String foreignField;

	private int position;

	private String paramPosition;

	private boolean returnFields;

	private boolean returnScores;

	private boolean returnFacets;

	private JoinType type;

	private transient OuterCollector outerCollector;

	private transient Client foreignClient;

	private transient AbstractSearchRequest foreignSearchRequest;

	public JoinItem() {
		indexName = null;
		queryTemplate = null;
		queryString = null;
		localField = null;
		foreignField = null;
		position = 0;
		paramPosition = null;
		returnFields = false;
		returnScores = false;
		returnFacets = false;
		type = JoinType.INNER;
		filterList = new FilterList();
		outerCollector = null;
		foreignSearchRequest = null;
		foreignClient = null;
	}

	public JoinItem(JoinItem source) {
		source.copyTo(this);
	}

	public void copyTo(JoinItem target) {
		target.indexName = indexName;
		target.queryTemplate = queryTemplate;
		target.queryString = queryString;
		target.localField = localField;
		target.foreignField = foreignField;
		target.position = position;
		target.paramPosition = paramPosition;
		target.returnFields = returnFields;
		target.returnScores = returnScores;
		target.returnFacets = returnFacets;
		target.type = type;
		target.filterList = new FilterList(filterList);
	}

	public JoinItem(XPathParser xpp, Node node) throws XPathExpressionException {
		indexName = XPathParser.getAttributeString(node, ATTR_NAME_INDEXNAME);
		queryTemplate = XPathParser.getAttributeString(node,
				ATTR_NAME_QUERYTEMPLATE);
		queryString = xpp.getNodeString(node, false);
		localField = XPathParser.getAttributeString(node, ATTR_NAME_LOCALFIELD);
		foreignField = XPathParser.getAttributeString(node,
				ATTR_NAME_FOREIGNFIELD);
		returnFields = Boolean.parseBoolean(XPathParser.getAttributeString(
				node, ATTR_NAME_RETURNFIELDS));
		returnScores = Boolean.parseBoolean(XPathParser.getAttributeString(
				node, ATTR_NAME_RETURNSCORES));
		returnFacets = Boolean.parseBoolean(XPathParser.getAttributeString(
				node, ATTR_NAME_RETURNFACETS));
		type = JoinType.find(XPathParser.getAttributeString(node,
				ATTR_NAME_TYPE));
		filterList = new FilterList();
	}

	public final String NODE_NAME_JOIN = "join";
	public final String ATTR_NAME_INDEXNAME = "indexName";
	public final String ATTR_NAME_QUERYTEMPLATE = "queryTemplate";
	public final String ATTR_NAME_LOCALFIELD = "localField";
	public final String ATTR_NAME_FOREIGNFIELD = "foreignField";
	public final String ATTR_NAME_RETURNFIELDS = "returnFields";
	public final String ATTR_NAME_RETURNSCORES = "returnScores";
	public final String ATTR_NAME_RETURNFACETS = "returnFacets";
	public final String ATTR_NAME_TYPE = "type";

	public void writeXmlConfig(XmlWriter xmlWriter) throws SAXException {
		xmlWriter.startElement(NODE_NAME_JOIN, ATTR_NAME_INDEXNAME, indexName,
				ATTR_NAME_QUERYTEMPLATE, queryTemplate, ATTR_NAME_LOCALFIELD,
				localField, ATTR_NAME_FOREIGNFIELD, foreignField,
				ATTR_NAME_RETURNFIELDS, Boolean.toString(returnFields),
				ATTR_NAME_RETURNSCORES, Boolean.toString(returnScores),
				ATTR_NAME_RETURNFACETS, Boolean.toString(returnFacets),
				ATTR_NAME_TYPE, type.name());
		xmlWriter.textNode(queryString);
		xmlWriter.endElement();
	}

	/**
	 * @return the indexName
	 */
	public String getIndexName() {
		return indexName;
	}

	/**
	 * @param indexName
	 *            the indexName to set
	 */
	public void setIndexName(String indexName) {
		this.indexName = indexName;
	}

	/**
	 * @return the queryTemplate
	 */
	public String getQueryTemplate() {
		return queryTemplate;
	}

	/**
	 * @param queryTemplate
	 *            the queryTemplate to set
	 */
	public void setQueryTemplate(String queryTemplate) {
		this.queryTemplate = queryTemplate;
	}

	/**
	 * @return the queryString
	 */
	public String getQueryString() {
		return queryString;
	}

	/**
	 * @param queryString
	 *            the queryString to set
	 */
	public void setQueryString(String queryString) {
		this.queryString = queryString;
	}

	/**
	 * @return the localField
	 */
	public String getLocalField() {
		return localField;
	}

	/**
	 * @param localField
	 *            the localField to set
	 */
	public void setLocalField(String localField) {
		this.localField = localField;
	}

	/**
	 * @return the foreignField
	 */
	public String getForeignField() {
		return foreignField;
	}

	/**
	 * @param foreignField
	 *            the foreignField to set
	 */
	public void setForeignField(String foreignField) {
		this.foreignField = foreignField;
	}

	public void setOuterCollector(OuterCollector outerCollector) {
		this.outerCollector = outerCollector;
	}

	@Override
	public int compareTo(JoinItem o) {
		int c = 0;
		if ((c = indexName.compareTo(o.indexName)) != 0)
			return c;
		if ((c = queryTemplate.compareTo(o.queryTemplate)) != 0)
			return c;
		if ((c = queryString.compareTo(o.queryString)) != 0)
			return c;
		if ((c = localField.compareTo(o.localField)) != 0)
			return c;
		if ((c = foreignField.compareTo(o.foreignField)) != 0)
			return c;
		return 0;
	}

	public void setParamPosition(int position) {
		this.position = position;
		paramPosition = StringUtils
				.fastConcat("jq", Integer.toString(position));
	}

	public int getPosition() {
		return position;
	}

	public String getParamPosition() {
		return paramPosition;
	}

	/**
	 * @return the returnFields
	 */
	public boolean isReturnFields() {
		return returnFields;
	}

	/**
	 * @param returnFields
	 *            the returnFields to set
	 */
	public void setReturnFields(boolean returnFields) {
		this.returnFields = returnFields;
	}

	/**
	 * @return the returnScores
	 */
	public boolean isReturnScores() {
		return returnScores;
	}

	/**
	 * @param returnScores
	 *            the returnScores to set
	 */
	public void setReturnScores(boolean returnScores) {
		this.returnScores = returnScores;
	}

	/**
	 * @return the returnFacets
	 */
	public boolean isReturnFacets() {
		return returnFacets;
	}

	/**
	 * @param returnFacets
	 *            the returnFacets to set
	 */
	public void setReturnFacets(boolean returnFacets) {
		this.returnFacets = returnFacets;
	}

	/**
	 * @return the type
	 */
	public JoinType getType() {
		return type;
	}

	/**
	 * @param type
	 *            the type to set
	 */
	public void setType(JoinType type) {
		this.type = type;
	}

	protected final Client getFogeignClient() throws SearchLibException {
		Client foreignClient = ClientCatalog.getClient(indexName);
		if (foreignClient == null)
			throw new SearchLibException("No client found: " + indexName);
		return foreignClient;
	}

	protected AbstractSearchRequest getForeignSearchRequest(
			final Client foreignClient) throws SearchLibException {
		AbstractRequest foreignRequest = StringUtils.isEmpty(queryTemplate) ? new SearchFieldRequest(
				foreignClient) : foreignClient.getNewRequest(queryTemplate);
		if (foreignRequest == null)
			throw new SearchLibException("The request template was not found: "
					+ queryTemplate);
		if (!(foreignRequest instanceof AbstractSearchRequest))
			throw new SearchLibException(
					"The request template is not a Search request: "
							+ queryTemplate);
		return (AbstractSearchRequest) foreignRequest;
	}

	private final void lazyLoadForeignSearchRequest() throws SearchLibException {
		if (foreignClient == null)
			foreignClient = getFogeignClient();
		if (foreignSearchRequest == null)
			foreignSearchRequest = getForeignSearchRequest(foreignClient);
	}

	public DocIdInterface apply(AbstractSearchRequest searchRequest,
			ReaderAbstract reader, DocIdInterface docs, int joinResultSize,
			JoinResult joinResult, List<JoinFacet> joinFacets, Timer timer)
			throws SearchLibException {
		try {
			FieldCacheIndex localStringIndex = reader
					.getStringIndex(localField);
			if (localStringIndex == null)
				throw new SearchLibException(
						"No string index found for the local field: "
								+ localField);
			lazyLoadForeignSearchRequest();
			foreignSearchRequest.setStart(0);
			foreignSearchRequest.setRows(0);
			foreignSearchRequest.setUsers(searchRequest.getUsers());
			foreignSearchRequest.setGroups(searchRequest.getGroups());
			foreignSearchRequest.setQueryString(queryString);
			for (FilterAbstract<?> filter : filterList)
				foreignSearchRequest.getFilterList().add(filter);
			String joinResultName = "join " + joinResult.joinPosition;
			Timer t = new Timer(timer, joinResultName + " foreign search");
			ResultSearchSingle resultSearch = (ResultSearchSingle) foreignClient
					.request(foreignSearchRequest);
			t.getDuration();
			joinResult.setForeignResult(resultSearch);
			if (foreignSearchRequest.isFacet()) {
				if (returnFacets)
					joinFacets.add(new JoinFacet(joinResult,
							foreignSearchRequest.getFacetFieldList(),
							resultSearch));
				foreignSearchRequest.getFacetFieldList().clear();
			}

			ReaderAbstract foreignReader = resultSearch.getReader();
			FieldCacheIndex foreignFieldIndex = foreignReader
					.getStringIndex(foreignField);
			if (foreignFieldIndex == null)
				throw new SearchLibException(StringUtils.fastConcat(
						"No string index found for the foreign field: ",
						foreignField));
			t = new Timer(timer, joinResultName + " join");
			DocIdInterface joinDocs = JoinUtils.join(docs, localStringIndex,
					resultSearch.getDocs(), foreignFieldIndex, joinResultSize,
					joinResult.joinPosition, t, type, outerCollector,
					foreignReader);
			t.getDuration();
			return joinDocs;
		} catch (IOException e) {
			throw new SearchLibException(e);
		}
	}

	public void setFromServlet(ServletTransaction transaction, String prefix)
			throws SearchLibException, SyntaxError {
		String myPrefix = StringUtils.fastConcat(prefix, paramPosition);
		String q = transaction.getParameterString(myPrefix);
		if (q != null)
			setQueryString(q);
		myPrefix = StringUtils.fastConcat(myPrefix, ".");
		filterList.addFromServlet(transaction, myPrefix);
		lazyLoadForeignSearchRequest();
		foreignSearchRequest.setFromServlet(transaction, myPrefix);
	}

	public void setParam(String param) {
		if (param != null)
			setQueryString(param);
	}
}
