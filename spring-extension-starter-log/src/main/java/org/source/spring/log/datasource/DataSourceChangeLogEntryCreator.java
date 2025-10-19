package org.source.spring.log.datasource;

import net.ttddyy.dsproxy.listener.logging.DefaultQueryLogEntryCreator;
import net.ttddyy.dsproxy.proxy.ParameterSetOperation;

import java.util.List;
import java.util.SortedMap;

public class DataSourceChangeLogEntryCreator extends DefaultQueryLogEntryCreator {

    @Override
    public SortedMap<String, String> getParametersToDisplay(List<ParameterSetOperation> params) {
        return super.getParametersToDisplay(params);
    }
}
