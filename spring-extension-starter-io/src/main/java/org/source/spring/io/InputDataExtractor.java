package org.source.spring.io;

import jakarta.validation.valueextraction.ExtractedValue;
import jakarta.validation.valueextraction.ValueExtractor;

public class InputDataExtractor implements ValueExtractor<Input<@ExtractedValue ?>> {

    @Override
    public void extractValues(Input<?> originalValue, ValueReceiver receiver) {
        receiver.value("Request", originalValue);
    }
}
