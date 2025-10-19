package org.source.spring.log.datasource;

import lombok.extern.slf4j.Slf4j;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.logging.AbstractQueryLoggingListener;
import net.ttddyy.dsproxy.proxy.ParameterSetOperation;
import org.source.spring.log.LogData;
import org.source.spring.log.Logs;
import org.source.spring.log.enums.LogSystemTypeEnum;
import org.source.spring.log.enums.PersistTypeEnum;
import org.source.spring.trace.TraceContext;
import org.source.utility.constant.Constants;
import org.source.utility.utils.Streams;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
public class DataSourceChangeListener extends AbstractQueryLoggingListener {

    public static final String DELETE_COLUMN_REPLACER = "all_columns";

    private final DataSourceChangeLogEntryCreator logEntryCreator;

    public DataSourceChangeListener() {
        logEntryCreator = new DataSourceChangeLogEntryCreator();
    }

    @Override
    public void afterQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
        if (Objects.nonNull(execInfo.getThrowable())) {
            return;
        }
        if (!Logs.getDataSourceEnabled()) {
            return;
        }
        List<String> excludeTableNames = Logs.getDataSourceExcludeTableNames();
        queryInfoList.forEach(queryInfo -> {
            String sql = queryInfo.getQuery().toLowerCase();
            // remove hibernate sql comments
            sql = sql.replaceAll("/\\*.+\\*/", "").trim();
            DataSourceTableInfo tableInfo = new DataSourceTableInfo();
            try {
                if (sql.startsWith(PersistTypeEnum.SELECT.getKeyword())) {
                    return;
                } else if (sql.startsWith(PersistTypeEnum.INSERT.getKeyword())) {
                    parseInsert(tableInfo, sql);
                } else if (sql.startsWith(PersistTypeEnum.UPDATE.getKeyword())) {
                    parseUpdate(tableInfo, sql);
                } else if (sql.startsWith(PersistTypeEnum.DELETE.getKeyword())) {
                    parseDelete(tableInfo, sql);
                } else {
                    return;
                }
            } catch (Exception e) {
                log.error("datasource proxy parse sql:{}", sql, e);
            }
            if (!CollectionUtils.isEmpty(excludeTableNames) && excludeTableNames.contains(tableInfo.getTableName())) {
                return;
            }
            List<DataSourceRecordLog> recordLogs = queryInfoLog(execInfo, queryInfo, tableInfo);
            List<LogData> logDataList = recordLogs.stream().map(k -> {
                LogData logData = new LogData();
                logData.setLogId(k.getLogId());
                logData.setParentLogId(Logs.getDataSourceParentLogId());
                logData.setRefId(Logs.getDataSourceRefId());
                logData.setDesc(k.getPersistTypeEnum().getDesc());
                logData.setSystemType(LogSystemTypeEnum.DATABASE.getType());
                logData.setExtra(k);
                logData.setUserId(TraceContext.getUserIdOrDefault());
                return logData;
            }).toList();
            Logs.save(logDataList);
        });
    }

    protected List<DataSourceRecordLog> queryInfoLog(ExecutionInfo execInfo, QueryInfo queryInfo, DataSourceTableInfo tableInfo) {
        List<String> keyColumns = Objects.requireNonNullElse(Logs.getDataSourceKeyColumns(), List.of());
        List<String> excludeColumns = Objects.requireNonNullElse(Logs.getDataSourceExcludeColumns(), List.of());
        List<List<ParameterSetOperation>> parametersList = queryInfo.getParametersList();
        List<DataSourceRecordLog> logs = new ArrayList<>();
        for (int i = 0; i < parametersList.size(); i++) {
            List<ParameterSetOperation> ps = parametersList.get(i);
            SortedMap<String, String> parametersToDisplay = this.logEntryCreator.getParametersToDisplay(ps);
            Map<String, String> columnValueMap = HashMap.newHashMap(parametersToDisplay.size());
            for (int j = 0; j < tableInfo.getColumns().size(); j++) {
                columnValueMap.put(tableInfo.getColumns().get(j), parametersToDisplay.get(String.valueOf(j + 1)));
            }
            int effectRecords = effectRecords(execInfo, tableInfo, i);
            if (effectRecords <= 0) {
                continue;
            }
            String key = Streams.of(keyColumns).map(columnValueMap::get).filter(Objects::nonNull).reduce(Constants.COLON, String::concat);
            tableInfo.getValueColumns().stream().filter(k -> !excludeColumns.contains(k)).forEach(c -> {
                DataSourceRecordLog recordLog = DataSourceRecordLog.builder()
                        .logId(key)
                        .tableName(tableInfo.getTableName())
                        .effectRecordNum(effectRecords)
                        .columnName(c)
                        .columnValue(columnValueMap.get(c))
                        .persistTypeEnum(tableInfo.getPersistTypeEnum())
                        .createUser(TraceContext.getUserId())
                        .createTime(LocalDateTime.now())
                        .build();
                logs.add(recordLog);
            });
        }
        return logs;
    }

    private int effectRecords(ExecutionInfo execInfo, DataSourceTableInfo tableInfo, int paramIndex) {
        if (PersistTypeEnum.DELETE.equals(tableInfo.getPersistTypeEnum())) {
            int[] resultSizes = (int[]) execInfo.getResult();
            return resultSizes[paramIndex];
        }
        Object result = execInfo.getResult();
        if (Objects.nonNull(result)) {
            return (int) execInfo.getResult();
        }
        return 0;
    }

    @Override
    protected void writeLog(String message) {
        log.debug(message);
    }

    protected void parseInsert(DataSourceTableInfo tableInfo, String insertSql) {
        tableInfo.setPersistTypeEnum(PersistTypeEnum.INSERT);
        int start = insertSql.indexOf(Constants.LEFT_BRACKET);
        String tableName = insertSql.substring(PersistTypeEnum.INSERT.getKeyword().length(), start).trim();
        tableInfo.setTableName(tableName);
        int end = insertSql.indexOf(Constants.RIGHT_BRACKET);
        String params = insertSql.substring(start + 1, end);
        Arrays.stream(params.split(Constants.COMMA)).forEach(c -> {
            String columnName = c.trim();
            tableInfo.getColumns().add(columnName);
            tableInfo.getValueColumns().add(columnName);
        });
        tableInfo.setPossibleIdColumns(new HashSet<>(tableInfo.getValueColumns()));
    }

    protected void parseUpdate(DataSourceTableInfo tableInfo, String updateSql) {
        tableInfo.setPersistTypeEnum(PersistTypeEnum.UPDATE);
        int setIndex = updateSql.indexOf(Constants.SET);
        String tableName = updateSql.substring(PersistTypeEnum.UPDATE.getKeyword().length(), setIndex).trim();
        tableInfo.setTableName(tableName);
        int whereIndex = updateSql.indexOf(Constants.WHERE);
        String setValueString = updateSql.substring(setIndex + Constants.SET.length(), whereIndex);
        Arrays.stream(setValueString.split(Constants.COMMA)).forEach(k -> {
            String columnName = k.split(Constants.EQUAL)[0].trim();
            tableInfo.getColumns().add(columnName);
            tableInfo.getValueColumns().add(columnName);
        });
        parseConditionColumns(tableInfo, updateSql, whereIndex);
    }

    protected void parseDelete(DataSourceTableInfo tableInfo, String deleteSql) {
        tableInfo.setPersistTypeEnum(PersistTypeEnum.DELETE);
        int tableIndex = deleteSql.indexOf(PersistTypeEnum.DELETE.getKeyword());
        int whereIndex = deleteSql.indexOf(Constants.WHERE);
        String tableName = deleteSql.substring(tableIndex + PersistTypeEnum.DELETE.getKeyword().length(), whereIndex).trim();
        tableInfo.setTableName(tableName);
        tableInfo.getValueColumns().add(DELETE_COLUMN_REPLACER);
        parseConditionColumns(tableInfo, deleteSql, whereIndex);
    }

    protected void parseConditionColumns(DataSourceTableInfo tableInfo, String sql, int whereIndex) {
        String conditionString = sql.substring(whereIndex + Constants.WHERE.length());
        List<String> conditionColumns = Arrays.stream(conditionString.split(Constants.AND)).map(k -> k.split(Constants.EQUAL)[0].trim()).toList();
        tableInfo.getColumns().addAll(conditionColumns);
        tableInfo.getPossibleIdColumns().addAll(conditionColumns);
    }

}
