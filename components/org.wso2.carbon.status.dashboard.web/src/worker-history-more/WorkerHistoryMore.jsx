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
import {Link} from "react-router-dom";
//Material UI
import {FlatButton, GridList, GridTile, RaisedButton} from "material-ui";
import HomeButton from "material-ui/svg-icons/action/home";
//App Components
import StatusDashboardAPIS from "../utils/apis/StatusDashboardAPIs";
import JVMLoading from "./JVMClassLoading";
import JVMOsCpu from "./JVMOsCpu";
import JVMOsPhysicalMemory from "./JVMOsPhysicalMemory";
import JVMThread from "./JVMThread";
import HeapMemory from "./HeapMemory";
import NonHeapMemory from "./NonHeapMemory";
import FileDescriptor from "./FileDescriptor";
import Header from "../common/Header";

const cardStyle = {padding: 30, width: '90%'};
/**
 * class to manage worker history details.
 */
export default class WorkerHistoryMore extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            workerID: this.props.match.params.id.split("_")[0] + ":" + this.props.match.params.id.split("_")[1],
            jvmClassLoadingLoadedCurrent: [],
            jvmClassLoadingLoadedTotal: [],
            jvmClassLoadingUnloadedTotal: [],
            jvmOsCpuLoadProcess: [],
            jvmOsCpuLoadSystem: [],
            jvmOsPhysicalMemoryFreeSize: [],
            jvmOsPhysicalMemoryTotalSize: [],
            jvmOsSwapSpaceFreeSize: [],
            jvmOsSwapSpaceTotalSize: [],
            jvmOsVirtualMemoryCommittedSize: [],
            jvmThreadsCount: [],
            jvmThreadsDaemonCount: [],
            jvmMemoryHeapInit: [],
            jvmMemoryHeapUsed: [],
            jvmMemoryHeapCommitted: [],
            jvmMemoryHeapMax: [],
            jvmMemoryNonHeapInit: [],
            jvmMemoryNonHeapUsed: [],
            jvmMemoryNonHeapCommitted: [],
            jvmMemoryNonHeapMax: [],
            jvmOsFileDescriptorOpenCount: [],
            jvmOsFileDescriptorMaxCount:[],
            isApiWaiting: true
        };
    }

    componentWillMount() {
        let queryParams = {
            params: {
                more: true
            }
        };
        let that = this;
        StatusDashboardAPIS.getWorkerHistoryByID(this.props.match.params.id, queryParams)
            .then(function (response) {
                that.setState({
                    jvmClassLoadingLoadedCurrent: response.data.jvmClassLoadingLoadedCurrent.data,
                    jvmClassLoadingLoadedTotal: response.data.jvmClassLoadingLoadedTotal.data,
                    jvmClassLoadingUnloadedTotal: response.data.jvmClassLoadingUnloadedTotal.data,
                    jvmOsCpuLoadProcess: response.data.jvmOsCpuLoadProcess.data,
                    jvmOsCpuLoadSystem: response.data.jvmOsCpuLoadSystem.data,
                    jvmOsPhysicalMemoryFreeSize: response.data.jvmOsPhysicalMemoryFreeSize.data,
                    jvmOsPhysicalMemoryTotalSize: response.data.jvmOsPhysicalMemoryTotalSize.data,
                    jvmOsVirtualMemoryCommittedSize: response.data.jvmOsVirtualMemoryCommittedSize.data,
                    jvmOsSwapSpaceFreeSize: response.data.jvmOsSwapSpaceFreeSize.data,
                    jvmOsSwapSpaceTotalSize: response.data.jvmOsSwapSpaceTotalSize.data,
                    jvmThreadsCount: response.data.jvmThreadsCount.data,
                    jvmThreadsDaemonCount: response.data.jvmThreadsDaemonCount.data,
                    jvmMemoryHeapInit: response.data.jvmMemoryHeapInit.data,
                    jvmMemoryHeapUsed: response.data.jvmMemoryHeapUsed.data,
                    jvmMemoryHeapCommitted: response.data.jvmMemoryHeapCommitted.data,
                    jvmMemoryHeapMax: response.data.jvmMemoryHeapMax.data,
                    jvmMemoryNonHeapInit: response.data.jvmMemoryNonHeapInit.data,
                    jvmMemoryNonHeapUsed: response.data.jvmMemoryNonHeapUsed.data,
                    jvmMemoryNonHeapCommitted: response.data.jvmMemoryNonHeapCommitted.data,
                    jvmMemoryNonHeapMax: response.data.jvmMemoryNonHeapMax.data,
                    jvmOsFileDescriptorOpenCount: response.data.jvmOsFileDescriptorOpenCount.data,
                    jvmOsFileDescriptorMaxCount:response.data.jvmOsFileDescriptorMaxCount.data,
                    isApiWaiting: false
                });
            })
    }

    renderCharts() {
        if (this.state.isApiWaiting) {
            return (
                <div style={{backgroundColor: '#222222', width: '100%', height: '100%'}} data-toggle="loading"
                     data-loading-inverse="true">
                    <div id="wrapper" style={{
                        backgroundColor: '#222222',
                        textAlign: 'center',
                        paddingTop: '200px',
                        paddingBottom: '200px'
                    }}>
                        <i className="fw fw-loader5 fw-spin fw-inverse fw-5x"></i>
                    </div>
                </div>
            );
        } else {
            return (
                <div>
                    <div style={cardStyle}>
                        <JVMLoading
                            data={[this.state.jvmClassLoadingLoadedTotal, this.state.jvmClassLoadingLoadedCurrent,
                                this.state.jvmClassLoadingUnloadedTotal]}/>
                    </div>
                    <div style={cardStyle}>
                        <JVMOsCpu data={[this.state.jvmOsCpuLoadProcess, this.state.jvmOsCpuLoadSystem]}/>
                    </div>
                    <div style={cardStyle}>
                        <JVMOsPhysicalMemory
                            data={[this.state.jvmOsPhysicalMemoryFreeSize, this.state.jvmOsPhysicalMemoryTotalSize,
                                this.state.jvmOsSwapSpaceFreeSize, this.state.jvmOsSwapSpaceTotalSize,
                                this.state.jvmOsVirtualMemoryCommittedSize]}/>
                    </div>
                    <div style={cardStyle}>
                        <JVMThread data={[this.state.jvmThreadsCount, this.state.jvmThreadsDaemonCount]}/>
                    </div>
                    <div style={cardStyle}>
                        <HeapMemory data={[this.state.jvmMemoryHeapInit, this.state.jvmMemoryHeapUsed,
                            this.state.jvmMemoryHeapCommitted, this.state.jvmMemoryHeapMax]}/>
                    </div>
                    <div style={cardStyle}>
                        <NonHeapMemory data={[this.state.jvmMemoryNonHeapInit, this.state.jvmMemoryNonHeapUsed,
                            this.state.jvmMemoryNonHeapCommitted, this.state.jvmMemoryNonHeapMax]}/>
                    </div>
                    <div style={cardStyle}>
                        <FileDescriptor data={[this.state.jvmOsFileDescriptorOpenCount,
                            this.state.jvmOsFileDescriptorMaxCount]}/>
                    </div>
                </div>
            );
        }
    }
    render() {
        return (
            <div>
                <div className="navigation-bar">
                    <Header/>
                    <Link to={window.contextPath}><FlatButton label="Overview >"
                                                                         icon={<HomeButton color="black"/>}/></Link>
                    <Link to={window.contextPath + '/worker/' + this.props.match.params.id }>
                        <FlatButton label={this.state.workerID + " >"}/></Link>
                    <Link to={window.contextPath + '/worker/history/' + this.props.match.params.id }><FlatButton
                        label="Metrics >"/></Link>
                    <RaisedButton label= "More" disabled disabledLabelColor='white'
                                  disabledBackgroundColor='#f17b31'/>
                </div>
                <div className="worker-h1">
                    <h2 style={{marginLeft: 40}}> {this.state.workerID} Metrics </h2>
                </div>

                {this.renderCharts()}
            </div>
        );
    }
}

