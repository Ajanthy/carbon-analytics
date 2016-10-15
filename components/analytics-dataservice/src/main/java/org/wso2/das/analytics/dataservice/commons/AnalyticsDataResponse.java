/*
 *  Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.wso2.das.analytics.dataservice.commons;

import java.io.Serializable;

/**
 * This class represents an analytics data service response.
 */
public class AnalyticsDataResponse implements Serializable {

    private static final long serialVersionUID = 7912542783175151940L;

    private String recordStoreName;

    private RecordGroup[] recordGroups;

    public AnalyticsDataResponse() {
    }

    public AnalyticsDataResponse(String recordStoreName, RecordGroup[] recordGroups) {
        this.recordStoreName = recordStoreName;
        this.recordGroups = recordGroups;
    }

    public String getRecordStoreName() {
        return recordStoreName;
    }

    public RecordGroup[] getRecordGroups() {
        return recordGroups;
    }

}
