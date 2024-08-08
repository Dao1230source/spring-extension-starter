package org.source.spring.io;

import jakarta.validation.valueextraction.ExtractedValue;
import jakarta.validation.valueextraction.ValueExtractor;

public class RequestDataExtractor implements ValueExtractor<Request<@ExtractedValue ?>> {

    @Override
    public void extractValues(Request<?> originalValue, ValueReceiver receiver) {
        receiver.value("Request", originalValue);
    }
}
