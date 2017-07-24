/*
* Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
* WSO2 Inc. licenses this file to you under the Apache License,
* Version 2.0 (the "License"); you may not use this file except
* in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.wso2.carbon.analytics.stream.persistence.internal;

import org.wso2.carbon.analytics.dataservice.core.AnalyticsDataService;
import org.wso2.carbon.analytics.eventsink.AnalyticsEventSinkService;
import org.wso2.carbon.event.stream.core.EventStreamService;

/**
 * This represents a service holder class for analytics stream persistence service.
 */
public class ServiceHolder {

    private static AnalyticsDataService analyticsDataService;
    private static EventStreamService eventStreamService;
    private static AnalyticsEventSinkService analyticsEventSinkService;

    private ServiceHolder() {
    }

    public static void setAnalyticsDataService(AnalyticsDataService analyticsDataService) {
        ServiceHolder.analyticsDataService = analyticsDataService;
    }

    public static AnalyticsDataService getAnalyticsDataService() {
        return analyticsDataService;
    }

    public static EventStreamService getEventStreamService() {
        return eventStreamService;
    }

    public static void setEventStreamService(EventStreamService eventStreamService) {
        ServiceHolder.eventStreamService = eventStreamService;
    }

    public static AnalyticsEventSinkService getAnalyticsEventSinkService() {
        return analyticsEventSinkService;
    }

    public static void setAnalyticsEventSinkService(
            AnalyticsEventSinkService analyticsEventSinkService) {
        ServiceHolder.analyticsEventSinkService = analyticsEventSinkService;
    }
}
