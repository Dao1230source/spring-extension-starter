package org.source.spring.log;

import lombok.extern.slf4j.Slf4j;
import org.source.utility.utils.Jsons;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Slf4j
@Configuration
public class DefaultLogDataProcessor implements LogDataProcessor {

    @Override
    public void doSave(List<LogData> logDataList) {
        log.info("save logs:{}", Jsons.str(logDataList));
    }
}
