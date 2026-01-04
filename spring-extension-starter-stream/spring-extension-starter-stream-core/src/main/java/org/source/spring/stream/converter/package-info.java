@NonNullApi
package org.source.spring.stream.converter;

/*
 * 发送消息（Object/List<Object>）-> stream (转换为 json 字符串)-> 真正的生产者
 * 消费者消费消息（String/List<String>）-> stream (转换为 json 字符串) - > 业务消费逻辑（Object/List<Object>）
 */

import org.springframework.lang.NonNullApi;