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
package org.wso2.analytics.datasource.hbase;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.hbase.util.Pair;
import org.wso2.analytics.dataservice.AnalyticsRecordStore;
import org.wso2.analytics.recordstore.commons.AnalyticsIterator;
import org.wso2.analytics.recordstore.commons.Record;
import org.wso2.analytics.recordstore.commons.RecordGroup;
import org.wso2.analytics.recordstore.exception.AnalyticsException;
import org.wso2.analytics.recordstore.exception.AnalyticsTableNotAvailableException;
import org.wso2.analytics.dataservice.utils.AnalyticsUtils;
import org.wso2.analytics.datasource.hbase.rg.HBaseIDRecordGroup;
import org.wso2.analytics.datasource.hbase.rg.HBaseRegionSplitRecordGroup;
import org.wso2.analytics.datasource.hbase.rg.HBaseTimestampRecordGroup;
import org.wso2.analytics.datasource.hbase.util.HBaseAnalyticsDSConstants;
import org.wso2.analytics.datasource.hbase.util.HBaseUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Apache HBase implementation of {@link org.wso2.analytics.dataservice.AnalyticsRecordStore}
 */
public class HBaseAnalyticsRecordStore implements AnalyticsRecordStore {

    private Connection conn;

    private HBaseAnalyticsConfigurationEntry queryConfig;

    private static final Log log = LogFactory.getLog(HBaseAnalyticsRecordStore.class);

    public HBaseAnalyticsRecordStore(Connection conn, HBaseAnalyticsConfigurationEntry entry) throws IOException, AnalyticsException {
        this.conn = conn;
        this.queryConfig = entry;
    }

    public HBaseAnalyticsRecordStore() {
        this.conn = null;
        this.queryConfig = null;
    }

    @Override
    public void init(Map<String, String> properties) throws AnalyticsException {
        this.queryConfig = HBaseUtils.lookupConfiguration();
        String dsName = properties.get(HBaseAnalyticsDSConstants.DATASOURCE_NAME);
        if (dsName == null) {
            throw new AnalyticsException("The property '" + HBaseAnalyticsDSConstants.DATASOURCE_NAME +
                    "' is required");
        }
        try {
            /*Configuration config = (Configuration) AnalyticsUtils.loadGlobalDataSource(dsName);
            if (config == null) {
                throw new AnalyticsException("Failed to initialize HBase configuration based on data source" +
                        " definition");
            }
            this.conn = ConnectionFactory.createConnection(config);*/

            Configuration config = new Configuration();
            this.conn = ConnectionFactory.createConnection(config);
        } catch (IOException e) {
            throw new AnalyticsException("Error establishing connection to HBase instance based on data source" +
                    " definition: " + e.getMessage(), e);
        }
        if (this.conn == null) {
            throw new AnalyticsException("Error establishing connection to HBase instance : HBase Client initialization " +
                    "failed");
        }
        log.debug("Initialized connection to HBase instance successfully.");
    }

    @Override
    public void createTable(String tableName) throws AnalyticsException {
        /* If the table we're proposing to create already exists, return in silence */
        if (this.tableExists(tableName)) {
            log.debug("Creation of table [" + tableName + "] could not be carried out since said table already exists.");
            return;
        }

        HTableDescriptor dataDescriptor = new HTableDescriptor(TableName.valueOf(
                HBaseUtils.generateTableName(tableName, HBaseAnalyticsDSConstants.TableType.DATA)));
        /* creating table with standard column family "carbon-analytics-data" for storing the actual row data
         * in one column and the record timestamp in another */
        dataDescriptor.addFamily(new HColumnDescriptor(HBaseAnalyticsDSConstants.ANALYTICS_DATA_COLUMN_FAMILY_NAME)
                .setMaxVersions(1));

        HTableDescriptor indexDescriptor = new HTableDescriptor(TableName.valueOf(
                HBaseUtils.generateTableName(tableName, HBaseAnalyticsDSConstants.TableType.INDEX)));
        /* creating table with standard column family "carbon-analytics-index" for storing timestamp -> ID index*/
        indexDescriptor.addFamily(new HColumnDescriptor(HBaseAnalyticsDSConstants.ANALYTICS_INDEX_COLUMN_FAMILY_NAME)
                .setMaxVersions(1));

        /* Table creation should fail if index cannot be created, so attempting to create index table first. */
        Admin admin = null;
        try {
            admin = this.conn.getAdmin();
            admin.createTable(indexDescriptor);
            admin.createTable(dataDescriptor);
            log.debug("Table [" + tableName + "] created");
        } catch (IOException e) {
            throw new AnalyticsException("Error creating table [" + tableName + "] : " + e.getMessage(), e);
        } finally {
            AnalyticsUtils.closeQuietly(admin);
        }
    }

    private boolean tableExists(String tableName) throws AnalyticsException {
        boolean isExist;
        Admin admin = null;
        try {
            admin = this.conn.getAdmin();
            isExist = admin.tableExists(TableName.valueOf(
                    HBaseUtils.generateTableName(tableName, HBaseAnalyticsDSConstants.TableType.DATA)));
        } catch (IOException e) {
            throw new AnalyticsException("Error checking existence of table [" + tableName + "] : " + e.getMessage(), e);
        } finally {
            AnalyticsUtils.closeQuietly(admin);
        }
        return isExist;
    }

    @Override
    public void deleteTable(String tableName) throws AnalyticsException {
        /* If the table we're trying to create does not exist, return in silence */
        if (!(this.tableExists(tableName))) {
            if (log.isDebugEnabled()) {
                log.debug("Deletion of table [" + tableName + "] could not be carried out since said table did not exist.");
            }
            return;
        }
        Admin admin = null;
        TableName dataTable = TableName.valueOf(HBaseUtils.generateTableName(tableName,
                HBaseAnalyticsDSConstants.TableType.DATA));
        TableName indexTable = TableName.valueOf(HBaseUtils.generateTableName(tableName,
                HBaseAnalyticsDSConstants.TableType.INDEX));
        try {
            admin = this.conn.getAdmin();
            /* delete the data table first */
            admin.disableTable(dataTable);
            admin.deleteTable(dataTable);
            /* finally, delete the index table */
            admin.disableTable(indexTable);
            admin.deleteTable(indexTable);
            log.debug("Table [" + tableName + "] deleted");
        } catch (IOException e) {
            throw new AnalyticsException("Error deleting table [" + tableName + "] : " + e.getMessage(), e);
        } finally {
            AnalyticsUtils.closeQuietly(admin);
        }
    }

    @Override
    public void put(List<Record> records) throws AnalyticsException {
        String tableName = null;
        if (records.isEmpty()) {
            return;
        }
        Table table, indexTable;
        Map<String, List<Record>> recordBatches = this.generateRecordBatches(records);
        try {
            /* iterating over record batches */
            for (Map.Entry<String, List<Record>> entry : recordBatches.entrySet()) {
                tableName = entry.getKey();
                table = this.conn.getTable(TableName.valueOf(HBaseUtils.generateTableName(tableName,
                        HBaseAnalyticsDSConstants.TableType.DATA)));
                indexTable = this.conn.getTable(TableName.valueOf(HBaseUtils.generateTableName(tableName,
                        HBaseAnalyticsDSConstants.TableType.INDEX)));
                /* Populating batched Put instances from records in a single batch */
                List<List<Put>> allPuts = this.populatePuts(recordBatches.get(entry.getKey()));
                /* Using Table.put(List<Put>) method to minimise network calls per table */
                try {
                    indexTable.put(allPuts.get(0));
                    table.put(allPuts.get(1));
                    log.debug("Processed " + records.size() + " PUT operations for [" + tableName + "]");
                } finally {
                    AnalyticsUtils.closeQuietly(table);
                    AnalyticsUtils.closeQuietly(indexTable);
                }
            }
        } catch (TableNotFoundException | RetriesExhaustedException e) {
            throw new AnalyticsTableNotAvailableException(tableName);
        } catch (IOException e) {
            throw new AnalyticsException("Error adding new records to table [" + tableName + "]: " + e.getMessage(), e);
        }
    }

    private List<List<Put>> populatePuts(List<Record> records) throws AnalyticsException {
        byte[] data;
        List<Put> puts = new ArrayList<>();
        List<Put> indexPuts = new ArrayList<>();
        for (Record record : records) {
            String recordId = record.getId();
            long timestamp = record.getTimestamp();
            if (timestamp < 0L) {
                throw new AnalyticsException("HBase Analytics Record store does not support negative UNIX timestamps");
            }
            Map<String, Object> columns = record.getValues();
            if ((columns == null) || columns.isEmpty()) {
                data = new byte[]{};
            } else {
                data = AnalyticsUtils.encodeRecordValues(columns);
            }
            Put put = new Put(Bytes.toBytes(recordId));
            put.addColumn(HBaseAnalyticsDSConstants.ANALYTICS_DATA_COLUMN_FAMILY_NAME,
                    HBaseAnalyticsDSConstants.ANALYTICS_ROWDATA_QUALIFIER_NAME, data);
            put.addColumn(HBaseAnalyticsDSConstants.ANALYTICS_DATA_COLUMN_FAMILY_NAME,
                    HBaseAnalyticsDSConstants.ANALYTICS_TS_QUALIFIER_NAME, Bytes.toBytes(timestamp));
            indexPuts.add(this.putIndexData(record));
            puts.add(put);
        }
        List<List<Put>> output = new ArrayList<>();
        output.add(indexPuts);
        output.add(puts);
        return output;
    }

    private Put putIndexData(Record record) {
        Put indexPut = new Put(HBaseUtils.encodeLong(record.getTimestamp()));
        /* Setting the column qualifier the same as the column value to enable multiple columns per row with
        * unique qualifiers, since we will anyway not use the qualifier during index read */
        indexPut.addColumn(HBaseAnalyticsDSConstants.ANALYTICS_INDEX_COLUMN_FAMILY_NAME, Bytes.toBytes(record.getId()),
                Bytes.toBytes(record.getId()));
        return indexPut;
    }

    private Map<String, List<Record>> generateRecordBatches(List<Record> records) {
        Map<String, List<Record>> recordBatches = new HashMap<>();
        List<Record> recordBatch;
        for (Record record : records) {
            recordBatch = recordBatches.get(record.getTableName());
            if (recordBatch == null) {
                recordBatch = new ArrayList<>();
                recordBatches.put(record.getTableName(), recordBatch);
            }
            recordBatch.add(record);
        }
        return recordBatches;
    }

    @Override
    public RecordGroup[] get(String tableName, int numPartitionsHint, List<String> columns, long timeFrom,
                             long timeTo, int recordsFrom, int recordsCount) throws AnalyticsException {
        if (recordsFrom > 0) {
            throw new HBaseUnsupportedOperationException("Pagination is not supported for HBase Analytics Record Store Implementation");
        }
        if (!this.tableExists(tableName)) {
            throw new AnalyticsTableNotAvailableException(tableName);
        }
        if ((timeFrom < 0) && (timeTo >= Long.MAX_VALUE - 1)) {
            log.debug("Performing GET on region split contours for table [" + tableName + "]");
            return this.computeRegionSplits(tableName, columns, recordsCount);
        } else {
            log.debug("Performing GET through timestamp slices for table [" + tableName + "]");
            return new HBaseTimestampRecordGroup[]{
                    new HBaseTimestampRecordGroup(tableName, columns, timeFrom, timeTo, recordsCount)
            };
        }
    }

    @Override
    public RecordGroup[] get(String tableName, int numPartitionsHint, List<String> columns,
                             List<String> ids) throws AnalyticsException {
        if (!this.tableExists(tableName)) {
            throw new AnalyticsTableNotAvailableException(tableName);
        }
        log.debug("Performing GET by direct Record ID lookup for table [" + tableName + "]");
        return new HBaseIDRecordGroup[]{
                new HBaseIDRecordGroup(tableName, columns, ids)
        };
    }

    @Override
    public AnalyticsIterator<Record> readRecords(RecordGroup recordGroup) throws AnalyticsException {
        if (recordGroup instanceof HBaseIDRecordGroup) {
            HBaseIDRecordGroup idRecordGroup = (HBaseIDRecordGroup) recordGroup;
            return this.getRecords(idRecordGroup.getTableName(),
                    idRecordGroup.getColumns(), idRecordGroup.getIds());

        } else if (recordGroup instanceof HBaseTimestampRecordGroup) {
            HBaseTimestampRecordGroup tsRecordGroup = (HBaseTimestampRecordGroup) recordGroup;
            return this.getRecords(tsRecordGroup.getTableName(),
                    tsRecordGroup.getColumns(), tsRecordGroup.getStartTime(), tsRecordGroup.getEndTime(),
                    tsRecordGroup.getRecordsCount());

        } else if (recordGroup instanceof HBaseRegionSplitRecordGroup) {
            HBaseRegionSplitRecordGroup rsRecordGroup = (HBaseRegionSplitRecordGroup) recordGroup;
            return this.getRecords(rsRecordGroup.getTableName(),
                    rsRecordGroup.getColumns(), rsRecordGroup.getRecordsCount(), rsRecordGroup.getStartRow(), rsRecordGroup.getEndRow());
        } else {
            throw new AnalyticsException("Invalid HBase RecordGroup implementation: " + recordGroup.getClass());
        }
    }

    private AnalyticsIterator<Record> getRecords(String tableName, List<String> columns, List<String> ids)
            throws AnalyticsException {
        int batchSize = this.queryConfig.getBatchSize();
        return new HBaseRecordIterator(tableName, columns, ids, this.conn, batchSize);
    }

    private AnalyticsIterator<Record> getRecords(String tableName, List<String> columns, long startTime,
                                                 long endTime, int recordsCount)
            throws AnalyticsException {
        int batchSize = this.queryConfig.getBatchSize();
        return new HBaseTimestampIterator(tableName, columns, startTime, endTime, recordsCount, this.conn, batchSize);
    }

    private AnalyticsIterator<Record> getRecords(String tableName, List<String> columns, int recordsCount, byte[] startRow, byte[] endRow)
            throws AnalyticsException {
        return new HBaseRegionSplitIterator(tableName, columns, recordsCount, this.conn, startRow, endRow);
    }

    private RecordGroup[] computeRegionSplits(String tableName, List<String> columns, int recordsCount) throws AnalyticsException {
        List<RecordGroup> regionalGroups = new ArrayList<>();
        String formattedTableName = HBaseUtils.generateTableName(tableName, HBaseAnalyticsDSConstants.TableType.DATA);
        try {
            RegionLocator locator = this.conn.getRegionLocator(TableName.valueOf(formattedTableName));
            final Pair<byte[][], byte[][]> startEndKeys = locator.getStartEndKeys();
            byte[][] startKeys = startEndKeys.getFirst();
            byte[][] endKeys = startEndKeys.getSecond();
            for (int i = 0; i < startKeys.length && i < endKeys.length; i++) {
                RecordGroup regionalGroup = new HBaseRegionSplitRecordGroup(tableName, columns, recordsCount,
                        startKeys[i], endKeys[i], locator.getRegionLocation(startKeys[i]).getHostname());
                regionalGroups.add(regionalGroup);
            }
        } catch (IOException e) {
            throw new AnalyticsException("Error computing region splits for table [" + tableName + "] : " + e.getMessage(), e);
        }
        return regionalGroups.toArray(new RecordGroup[regionalGroups.size()]);
    }

    @Override
    public void delete(String tableName, long timeFrom, long timeTo) throws AnalyticsException {
        int batchSize = this.queryConfig.getBatchSize();
        int batchCounter = 0;
        ListMultimap<String, Long> recordsWithRef = ArrayListMultimap.create();
        List<byte[]> timestamps = new ArrayList<>();
        String formattedTableName = HBaseUtils.generateTableName(tableName, HBaseAnalyticsDSConstants.TableType.INDEX);
        Table indexTable = null;
        Scan indexScan = new Scan();
        if (timeFrom >= 0L) {
            indexScan.setStartRow(HBaseUtils.encodeLong(timeFrom));
        }
        if (!(timeTo >= Long.MAX_VALUE - 1)) {
            indexScan.setStopRow(HBaseUtils.encodeLong(timeTo));
        }
        indexScan.addFamily(HBaseAnalyticsDSConstants.ANALYTICS_INDEX_COLUMN_FAMILY_NAME);
        ResultScanner resultScanner = null;
        try {
            indexTable = this.conn.getTable(TableName.valueOf(formattedTableName));
            resultScanner = indexTable.getScanner(indexScan);
            for (Result rowResult : resultScanner) {
                /* Using Result.rawCells() because the descriptors in the secondary index are never known in advance */
                Cell[] cells = rowResult.rawCells();
                for (Cell cell : cells) {
                    if (cell != null) {
                        /* recordId -> the record ID which corresponds to the index lookup */
                        String recordId = Bytes.toString(CellUtil.cloneValue(cell));
                        /* timeStampRef -> what actual index entry was used to retrieve this particular record ID */
                        Long timeStampRef = Bytes.toLong(rowResult.getRow());
                        recordsWithRef.put(recordId, timeStampRef);
                        batchCounter++;
                    }
                }
                timestamps.add(rowResult.getRow());
                if (batchCounter >= batchSize) {
                    batchCounter = 0;
                    /* Delete only the records which actually have their timestamp matching the retrieved
                    secondary index entries */
                    this.deleteDataRows(tableName, recordsWithRef);
                    /* Delete ALL retrieved secondary index entries, whether backed by actual records or not */
                    this.deleteRows(tableName, HBaseAnalyticsDSConstants.TableType.INDEX, timestamps);
                    recordsWithRef.clear();
                    timestamps.clear();
                }
            }
            this.deleteDataRows(tableName, recordsWithRef);
            this.deleteRows(tableName, HBaseAnalyticsDSConstants.TableType.INDEX, timestamps);
        } catch (IOException e) {
            throw new AnalyticsException("Index for table [" + tableName + "] could not be read for deletion: " + e.getMessage(), e);
        } finally {
            AnalyticsUtils.closeQuietly(resultScanner);
            AnalyticsUtils.closeQuietly(indexTable);
        }
    }

    @Override
    public void delete(String tableName, List<String> ids) throws AnalyticsException {
        Table dataTable = null;
        List<Delete> dataDeletes = new ArrayList<>();
        String dataTableName = HBaseUtils.generateTableName(tableName, HBaseAnalyticsDSConstants.TableType.DATA);
        List<Delete> timestampDeletes = this.lookupIndexDeletes(dataTableName, ids, tableName);
        dataDeletes.addAll(ids.stream().map(recordId -> new Delete(Bytes.toBytes(recordId))).collect(Collectors.toList()));
        try {
            dataTable = this.conn.getTable(TableName.valueOf(dataTableName));
            dataTable.delete(dataDeletes);
            log.debug("Processed deletion of " + dataDeletes.size() + " records from table [" + tableName + "]");
            this.deleteColumns(tableName, HBaseAnalyticsDSConstants.TableType.INDEX, timestampDeletes);
        } catch (IOException e) {
            throw new AnalyticsException("Error deleting records from [" + tableName + "] : "
                    + e.getMessage(), e);
        } finally {
            AnalyticsUtils.closeQuietly(dataTable);
        }
    }

    private List<Delete> lookupIndexDeletes(String dataTableName, List<String> rowIds, String tableName)
            throws AnalyticsException {
        List<Delete> indexDeletes = new ArrayList<>();
        List<Get> gets = new ArrayList<>();
        Table dataTable = null;
        gets.addAll(rowIds.stream().filter(rowId -> !rowId.isEmpty()).map(rowId ->
                new Get(Bytes.toBytes(rowId)).addColumn(HBaseAnalyticsDSConstants.ANALYTICS_DATA_COLUMN_FAMILY_NAME,
                        HBaseAnalyticsDSConstants.ANALYTICS_TS_QUALIFIER_NAME)).collect(Collectors.toList()));
        try {
            dataTable = this.conn.getTable(TableName.valueOf(dataTableName));
            Result[] results = dataTable.get(gets);
            for (Result res : results) {
                if (res.containsColumn(HBaseAnalyticsDSConstants.ANALYTICS_DATA_COLUMN_FAMILY_NAME,
                        HBaseAnalyticsDSConstants.ANALYTICS_TS_QUALIFIER_NAME)) {
                    Cell dataCell = res.getColumnLatestCell(HBaseAnalyticsDSConstants.ANALYTICS_DATA_COLUMN_FAMILY_NAME,
                            HBaseAnalyticsDSConstants.ANALYTICS_TS_QUALIFIER_NAME);
                    byte[] data = CellUtil.cloneValue(dataCell);
                    if (data.length > 0) {
                        indexDeletes.add(new Delete(data).addColumn(
                                HBaseAnalyticsDSConstants.ANALYTICS_INDEX_COLUMN_FAMILY_NAME, res.getRow()));
                    }
                }
            }

        } catch (IOException e) {
            throw new AnalyticsException("The secondary index for table [" + tableName +
                    "] could not be initialized for deletion of rows: " + e.getMessage(), e);
        } finally {
            AnalyticsUtils.closeQuietly(dataTable);
        }
        return indexDeletes;
    }

    private void deleteColumns(String tableName, HBaseAnalyticsDSConstants.TableType type, List<Delete> deletes) throws AnalyticsException {
        Table table = null;
        String dataTableName = HBaseUtils.generateTableName(tableName, type);
        try {
            table = this.conn.getTable(TableName.valueOf(dataTableName));
            table.delete(deletes);
        } catch (IOException e) {
            throw new AnalyticsException("Error deleting columns from [" + tableName + "] of type " + type + " : "
                    + e.getMessage(), e);
        } finally {
            AnalyticsUtils.closeQuietly(table);
        }
    }

    private void deleteRows(String tableName, HBaseAnalyticsDSConstants.TableType type, List<byte[]> rows) throws AnalyticsException {
        Table table = null;
        List<Delete> deletes = new ArrayList<>();
        String dataTableName = HBaseUtils.generateTableName(tableName, type);
        deletes.addAll(rows.stream().map(Delete::new).collect(Collectors.toList()));
        try {
            table = this.conn.getTable(TableName.valueOf(dataTableName));
            table.delete(deletes);
        } catch (IOException e) {
            throw new AnalyticsException("Error deleting rows from [" + tableName + "] of type " + type + " : "
                    + e.getMessage(), e);
        } finally {
            AnalyticsUtils.closeQuietly(table);
        }
    }

    private void deleteDataRows(String tableName, ListMultimap<String, Long> recordsWithRef) throws AnalyticsException {
        Table dataTable = null;
        List<Get> gets = new ArrayList<>();
        List<Delete> deletes = new ArrayList<>();
        String dataTableName = HBaseUtils.generateTableName(tableName, HBaseAnalyticsDSConstants.TableType.DATA);
        try {
            dataTable = this.conn.getTable(TableName.valueOf(dataTableName));
            /* For all records set for termination, check if they really do have their timestamps within the given range
             through a GET operation (i.e. kick down doors and check if any of them are really Sarah Connor) */
            gets.addAll(recordsWithRef.keySet().stream().map(recordId ->
                    new Get(Bytes.toBytes(recordId)).addColumn(HBaseAnalyticsDSConstants.ANALYTICS_DATA_COLUMN_FAMILY_NAME,
                            HBaseAnalyticsDSConstants.ANALYTICS_TS_QUALIFIER_NAME)).collect(Collectors.toList()));

            Result[] results = dataTable.get(gets);
            for (Result res : results) {
                if (res.containsColumn(HBaseAnalyticsDSConstants.ANALYTICS_DATA_COLUMN_FAMILY_NAME,
                        HBaseAnalyticsDSConstants.ANALYTICS_TS_QUALIFIER_NAME)) {
                    Cell dataCell = res.getColumnLatestCell(HBaseAnalyticsDSConstants.ANALYTICS_DATA_COLUMN_FAMILY_NAME,
                            HBaseAnalyticsDSConstants.ANALYTICS_TS_QUALIFIER_NAME);
                    byte[] originalTimestamp = CellUtil.cloneValue(dataCell);
                    if (originalTimestamp.length > 0) {
                        List<Long> candidateIndexEntries = recordsWithRef.get(Bytes.toString(res.getRow()));
                        if (candidateIndexEntries.contains(Bytes.toLong(originalTimestamp))) {
                            deletes.add(new Delete(res.getRow()));
                        }
                    }
                }
            }
            /* Cyberdyne Systems Model 101 Series 800 is GO */
            dataTable.delete(deletes);
        } catch (IOException e) {
            throw new AnalyticsException("Error deleting records from [" + tableName + "] : "
                    + e.getMessage(), e);
        } finally {
            AnalyticsUtils.closeQuietly(dataTable);
        }
    }

    @Override
    public void destroy() throws AnalyticsException {
        try {
            this.conn.close();
            log.debug("Closed HBase connection transients successfully.");
        } catch (IOException ignore) {
                /* do nothing, the connection is dead anyway */
        }
    }

    private static class HBaseUnsupportedOperationException extends AnalyticsException {

        private static final long serialVersionUID = -380641886204128313L;

        HBaseUnsupportedOperationException(String s) {
            super(s);
        }
    }

}