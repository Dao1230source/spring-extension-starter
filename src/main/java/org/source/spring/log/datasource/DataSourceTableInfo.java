package org.source.spring.log.datasource;

import lombok.Data;
import org.source.spring.log.enums.PersistTypeEnum;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class DataSourceTableInfo {
    private String tableName;
    private PersistTypeEnum persistTypeEnum;
    private List<String> columns;
    private List<String> valueColumns;
    private Set<String> possibleIdColumns;

    public DataSourceTableInfo() {
        columns = new ArrayList<>();
        valueColumns = new ArrayList<>();
        possibleIdColumns = new HashSet<>();
    }

}
