/*
 *  Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.analytics.datasource.rdbms.h2;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.wso2.analytics.data.commons.AnalyticsRecordStore;
import org.wso2.analytics.data.commons.exception.AnalyticsException;
import org.wso2.analytics.data.commons.test.AnalyticsRecordStoreTest;
import org.wso2.analytics.data.commons.utils.AnalyticsCommonUtils;
import org.wso2.analytics.datasource.rdbms.RDBMSAnalyticsRecordStore;

import javax.naming.NamingException;
import java.util.HashMap;
import java.util.Map;

/**
 * H2 implementation of analytics record store tests.
 */
public class H2FileDBAnalyticsRecordStoreTest extends AnalyticsRecordStoreTest {
                
    @BeforeClass
    public void setup() throws NamingException, AnalyticsException {
        //AnalyticsCommonUtils.clearGlobalCustomDataSourceRepo();
        System.setProperty(AnalyticsCommonUtils.WSO2_ANALYTICS_CONF_DIRECTORY_SYS_PROP, "src/test/resources/conf_h2a");
        AnalyticsRecordStore ars = new RDBMSAnalyticsRecordStore();
        Map<String, String> props = new HashMap<>();
        props.put("datasource", "WSO2_ANALYTICS_EVENT_STORE_DB");
        ars.init(props);
        this.init("H2FileDBAnalyticsDataSource", ars);
    }
    
    @AfterClass
    public void destroy() throws AnalyticsException {
        this.cleanup();
    }
    
}
