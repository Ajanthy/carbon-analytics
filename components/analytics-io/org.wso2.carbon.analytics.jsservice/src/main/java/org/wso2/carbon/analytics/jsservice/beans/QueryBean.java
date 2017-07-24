/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.carbon.analytics.jsservice.beans;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * The Class QueryBean.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "query")
public class QueryBean {
	
	/** The columns. */
	@XmlElement(required = false)
	private List<String> columns;
	
	/** The query. */
	@XmlElement(required = false)
	private String query;
	
	/** The start. */
	@XmlElement(required = false)
	private int start;
	
	/** The count. */
	@XmlElement(required = false)
	private int count;

    /** The sorting fields */
    @XmlElement(required = false)
    private List<SortByFieldBean> sortBy;

	/**
	 * Gets the columns.
	 * @return the columns
	 */
	public List<String> getColumns() {
		return columns;
	}

	/**
	 * Gets the query.
	 * @return the query
	 */
	public String getQuery() {
		return query;
	}

	/**
	 * Gets the start.
	 * @return the start
	 */
	public int getStart() {
		return start;
	}

	/**
	 * Gets the count.
	 * @return the count
	 */
	public int getCount() {
		return count;
	}

    /**
     * get the sortByField list
     * @return a List of Fields with their sorting types
     */
    public List<SortByFieldBean> getSortBy() {
        return sortBy;
    }

    /**
     * Set the sortBy list
     * @param sortBy List of sorting field with their sort types
     */
    public void setSortBy(List<SortByFieldBean> sortBy) {
        this.sortBy = sortBy;
    }
}
