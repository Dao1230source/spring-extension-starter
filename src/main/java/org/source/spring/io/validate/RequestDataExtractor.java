package org.source.spring.io.validate;

import jakarta.validation.valueextraction.ExtractedValue;
import jakarta.validation.valueextraction.ValueExtractor;
import org.source.spring.io.Request;

public class RequestDataExtractor implements ValueExtractor<Request<@ExtractedValue ?>> {

    @Override
    public void extractValues(Request<?> originalValue, ValueReceiver receiver) {
        receiver.value(null, originalValue.getData());
    }
}
