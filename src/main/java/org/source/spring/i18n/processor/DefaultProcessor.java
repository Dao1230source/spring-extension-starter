package org.source.spring.i18n.processor;

import lombok.*;
import org.source.spring.i18n.facade.data.Dict;
import org.source.spring.i18n.facade.param.Dict1Param;
import org.source.spring.i18n.facade.param.Dict2Param;
import org.source.spring.i18n.facade.param.Dict3Param;
import org.source.spring.i18n.facade.param.Dict4Param;
import org.source.utility.tree.DefaultNode;
import org.source.utility.tree.DefaultTree;
import org.source.utility.tree.identity.StringElement;
import org.springframework.lang.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class DefaultProcessor implements Processor<Dict> {
    private static final DefaultTree<String, DictElement> DEFAULT_TREE = DefaultTree.build();

    @Override
    public Optional<Dict> findByKey(Dict3Param param) {
        return DEFAULT_TREE.get(e -> Dict3Param.uniqueKey3(param).equals(e.getId()))
                .map(DefaultNode::getElement).map(DictElement::getValue);
    }

    @Override
    public List<Dict> findByGroup(Dict2Param param) {
        return DEFAULT_TREE.find(e -> Dict2Param.uniqueKey2(param).equals(e.getId()))
                .stream().map(DefaultNode::getChildren).flatMap(Collection::stream)
                .map(DefaultNode::getElement).map(DictElement::getValue).filter(Objects::nonNull).toList();
    }

    @Override
    public List<String> findAllScopes() {
        return DEFAULT_TREE.getRoot().getChildren().stream().map(DefaultNode::getId).toList();
    }

    @Override
    public int save(Dict4Param param) {
        DEFAULT_TREE.add(List.of(DictElement.of2(param), DictElement.of3(param), DictElement.of4(param)));
        return 1;
    }

    @Override
    public int saveBatch(Collection<Dict4Param> params) {
        Set<DictElement> dictElements = params.stream().map(param ->
                        List.of(DictElement.of2(param), DictElement.of3(param), DictElement.of4(param)))
                .flatMap(Collection::stream).collect(Collectors.toSet());
        DEFAULT_TREE.add(dictElements);
        return params.size();
    }

    @Override
    public int removeByKey(Dict3Param param) {
        DEFAULT_TREE.remove(e -> Dict3Param.uniqueKey3(param).equals(e.getId()));
        return -1;
    }

    @Override
    public int removeByGroup(Dict2Param param) {
        DEFAULT_TREE.remove(e -> Dict2Param.uniqueKey2(param).equals(e.getId()));
        return -1;
    }

    @Builder
    @EqualsAndHashCode(callSuper = false)
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class DictElement extends StringElement {
        private String id;
        private @Nullable String parentId;
        private @Nullable Dict value;

        @Override
        public @NonNull String getId() {
            return id;
        }

        @Override
        public @Nullable String getParentId() {
            return parentId;
        }

        public static DictElement of2(Dict2Param param) {
            return DictElement.builder().id(Dict1Param.key(param)).build();
        }

        public static DictElement of3(Dict3Param param) {
            return DictElement.builder().id(Dict2Param.uniqueKey2(param))
                    .parentId(Dict1Param.key(param)).build();
        }

        public static DictElement of4(Dict4Param param) {
            return DictElement.builder().id(Dict3Param.uniqueKey3(param))
                    .parentId(Dict2Param.uniqueKey2(param))
                    .value(new Dict(param.getScope(), param.getGroup(), param.getKey(), param.getValue()))
                    .build();
        }
    }

}
