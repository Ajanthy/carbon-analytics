/*
*  Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.analytics.datasource.hbase.rg;

import org.wso2.analytics.data.commons.exception.AnalyticsException;
import org.wso2.analytics.data.commons.sources.RecordGroup;

import java.util.List;

public class HBaseTimestampRecordGroup implements RecordGroup {

    private static final long serialVersionUID = -5892873497625619175L;
    private int recordsCount;
    private String tableName;
    private List<String> columns;
    private long startTime, endTime;

    public HBaseTimestampRecordGroup() {
    }

    public HBaseTimestampRecordGroup(String tableName, List<String> columns, long timeFrom, long timeTo, int recordsCount) {
        this.tableName = tableName;
        this.columns = columns;
        this.startTime = timeFrom;
        this.endTime = timeTo;
        this.recordsCount = recordsCount;
    }

    @Override
    public String[] getLocations() throws AnalyticsException {
        return new String[]{"localhost"};
    }

    public String getTableName() {
        return tableName;
    }

    public List<String> getColumns() {
        return columns;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public int getRecordsCount() {
        return recordsCount;
    }

}
