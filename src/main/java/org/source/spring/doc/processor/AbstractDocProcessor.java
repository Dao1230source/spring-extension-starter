package org.source.spring.doc.processor;

import lombok.Getter;
import org.source.spring.doc.data.DocData;
import org.source.spring.object.entity.ObjectBodyEntityIdentity;
import org.source.spring.object.processor.AbstractObjectBodyProcessor;
import org.source.spring.object.processor.AbstractObjectProcessor;

@Getter
public abstract class AbstractDocProcessor<B extends ObjectBodyEntityIdentity>
        extends AbstractObjectBodyProcessor<B, DocData> {

    protected AbstractDocProcessor(AbstractObjectProcessor<?, ?> objectProcessor) {
        super(objectProcessor);
    }
}
