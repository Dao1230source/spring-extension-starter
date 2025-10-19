package org.source.spring.log;


import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.source.utility.constant.Constants;
import org.source.utility.enums.BaseExceptionEnum;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public interface LogDataProcessor {

    default void save(List<LogData> logDataList) {
        if (CollectionUtils.isEmpty(logDataList)) {
            return;
        }
        try (ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = validatorFactory.getValidator();
            String errorMessage = logDataList.stream().map(k -> {
                Set<ConstraintViolation<LogData>> validate = validator.validate(k);
                if (!CollectionUtils.isEmpty(validate)) {
                    return validate.stream().map(ConstraintViolation::getMessage).collect(Collectors.joining(Constants.COMMA));
                }
                return null;
            }).filter(Objects::nonNull).collect(Collectors.joining(Constants.SEMICOLON));
            if (StringUtils.hasText(errorMessage)) {
                throw BaseExceptionEnum.VALIDATE_ERROR.except("字段必填：{}", errorMessage);
            }
        }
        this.doSave(logDataList);
    }

    void doSave(List<LogData> logDataList);
}
