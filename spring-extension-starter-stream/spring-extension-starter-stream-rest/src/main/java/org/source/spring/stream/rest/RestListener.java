package org.source.spring.stream.rest;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.source.spring.common.io.Response;
import org.source.spring.stream.template.AbstractListener;
import org.source.utility.utils.Jsons;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.Objects;

@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Data
public class RestListener extends AbstractListener<String> {

    public ResponseEntity<Response<Void>> handleRequest(HttpServletRequest request) throws IOException {
        // 校验只支持json 格式
        // 从请求体读取 JSON
        JsonNode jsonNode = Jsons.getInstance().readTree(request.getInputStream());
        if (Objects.nonNull(jsonNode)) {
            this.processMessage(Jsons.str(jsonNode));
        }
        return ResponseEntity.ok().body(Response.success());
    }
}