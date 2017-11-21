/*
 *  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import React from "react";
//App Components
import DashboardUtils from "../utils/DashboardUtils";
import ChartCard from "../common/ChartCard";
// Material UI
import {Card, CardHeader, CardMedia, Divider} from "material-ui";

const metadata = {names: ['Time', 'Open Count', 'Max Count'], types: ['time', 'linear', 'linear']};
const chartConfig = {
    x: 'Time',
    charts: [{type: 'area', y: 'Open Count', fill: '#f17b31', markRadius: 2},
        {type: 'area', y: 'Max Count', markRadius: 2}],
    width: 700,
    height: 200,
    tickLabelColor: '#9c9898',
    axisLabelColor: '#9c9898',
    legendTitleColor: '#9c9898',
    legendTextColor: '#9c9898',
    interactiveLegend: true,
    disableVerticalGrid: true,
    disableHorizontalGrid: true
};

/**
 * JVM File Descriptor chart component.
 */
export default class FileDescriptor extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            jvmOsFileDescriptorOpenCount: this.props.data[0],
            jvmOsFileDescriptorMaxCount: this.props.data[1]
        };
    }

    componentWillReceiveProps(nextprops) {
        this.setState({
            jvmOsFileDescriptorOpenCount: nextprops.data[0],
            jvmOsFileDescriptorMaxCount: nextprops.data[1]
        });
    }

    render() {
        if(this.state.jvmOsFileDescriptorOpenCount.length === 0 && this.state.jvmOsFileDescriptorMaxCount.length === 0){
            return(
                <div style={{paddingLeft: 10}}>
                    <Card>
                        <CardHeader
                            title="File Descriptor"
                        />
                        <Divider/>
                        <CardMedia>
                            <div style={{backgroundColor: '#131313'}}>
                                <h4 style={{marginTop: 0}}>No Data Available</h4>
                            </div>
                        </CardMedia>
                    </Card>
                </div>
            );
        }
        return (
            <div style={{paddingLeft: 10}}>
                <ChartCard data={DashboardUtils.getCombinedChartList(this.state.jvmOsFileDescriptorOpenCount,
                    this.state.jvmOsFileDescriptorMaxCount)}
                           metadata={metadata} config={chartConfig} title="File Descriptor"/>
            </div>
        );
    }
}