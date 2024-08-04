package org.source.spring.log.datasource;

import lombok.Builder;
import lombok.Data;
import org.source.spring.log.enums.PersistTypeEnum;

import java.time.LocalDateTime;

@Builder
@Data
public class DataSourceRecordLog {
    private String logId;
    private String tableName;
    private String columnName;
    private String columnValue;
    private PersistTypeEnum persistTypeEnum;
    private int effectRecordNum;
    private String createUser;
    private LocalDateTime createTime;
}
