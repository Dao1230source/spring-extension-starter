package org.source.spring.log.datasource;

import lombok.Builder;
import lombok.Data;
import org.source.spring.log.enums.PersistTypeEnum;

import java.time.LocalDateTime;

/**
 * 数据库表变动日志数据，bizId是业务数据唯一值，按字段变化计转为多个变更对象。
 * <pre>
 * 比如新增用户：
 * bizId=00001，tableName=user，columnName=userId，columnValue=00001
 * bizId=00001，tableName=user，columnName=username，columnValue=Tom
 * ...
 * </pre>
 */
@Builder
@Data
public class DataSourceLogData {
    private String bizId;
    private String tableName;
    private String columnName;
    private String columnValue;
    private PersistTypeEnum persistTypeEnum;
    private int effectRecordNum;
    private String createUser;
    private LocalDateTime createTime;
}
