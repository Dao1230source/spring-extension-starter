package org.source.spring.doc.data;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.lang.model.element.Element;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public interface Path {
    List<String> getPaths();

    void setPaths(List<String> path);

    List<String> getRequestMethods();

    void setRequestMethods(List<String> requestMethod);

    default void processRequestMapping(Element element) {
        RequestMapping requestMapping = element.getAnnotation(RequestMapping.class);
        if (Objects.nonNull(requestMapping)) {
            this.setPaths(List.of(requestMapping.value()));
            this.setRequestMethods(Arrays.stream(requestMapping.method()).map(RequestMethod::name).toList());
        }
    }

    default void processMapping(Element element) {
        processRequestMapping(element);
        PostMapping postMapping = element.getAnnotation(PostMapping.class);
        if (Objects.nonNull(postMapping)) {
            this.setPaths(List.of(postMapping.value()));
            this.setRequestMethods(List.of(RequestMethod.POST.name()));
        }
        GetMapping getMapping = element.getAnnotation(GetMapping.class);
        if (Objects.nonNull(getMapping)) {
            this.setPaths(List.of(getMapping.value()));
            this.setRequestMethods(List.of(RequestMethod.GET.name()));
        }
    }
}
