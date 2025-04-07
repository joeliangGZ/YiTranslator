package com.omega.document.processor.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omega.document.processor.entity.DocumentEntity;
import com.omega.document.processor.entity.DocumentItem;
import com.omega.document.processor.service.TranslateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class TranslateServiceImpl implements TranslateService {

    private static final String API_URL = "http://localhost:5000/translate";
    private static final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public DocumentEntity translate(DocumentEntity document) throws ExecutionException, InterruptedException {
        if (document == null) return null;

        // 创建线程池（最大并发 20 个线程）
        ExecutorService executor = Executors.newFixedThreadPool(20);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        // 对每个条目并发调用外部翻译 API
        for (final DocumentItem item : document.getItems()) {
            // 更新当前条目的 translateContent
            CompletableFuture<Void> future = CompletableFuture
                    .supplyAsync(() -> requestTranslate(item), executor)
                    .thenAccept(item::setTranslateContent);
            futures.add(future);
        }

        // 等待所有任务完成
        CompletableFuture<Void> all = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        all.get();
        executor.shutdown();
        return document;
    }

    @SuppressWarnings("unchecked")
    private String requestTranslate(DocumentItem item) {
        // 构造请求体： {"text": "originalContent"}
        Map<String, String> reqMap = new HashMap<>();
        reqMap.put("text", item.getOriginalContent());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(reqMap, headers);

        // 使用 RestTemplate 发送 POST 请求
        Map<String, Object> response = restTemplate.postForObject(API_URL, entity, Map.class);
        // 从响应中获取 translated 字段（需确保外部 API 返回格式为 {"translated": "翻译结果"}）
        return response != null ? response.get("translated").toString() : "";
    }

    private static String getResp(HttpURLConnection conn) throws IOException {
        int responseCode = conn.getResponseCode();
        InputStream is = (responseCode >= 200 && responseCode < 300)
                ? conn.getInputStream() : conn.getErrorStream();
        StringBuilder responseBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                responseBuilder.append(line);
            }
        }
        return responseBuilder.toString();
    }
}
