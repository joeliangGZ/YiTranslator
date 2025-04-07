package com.omega.document.processor.service;

import com.omega.document.processor.entity.DocumentEntity;

import java.util.concurrent.ExecutionException;

/**
 * Interface DocumentService
 *
 * @author KennySo
 * @date 2025/4/6
 */
public interface TranslateService {

    DocumentEntity translate(DocumentEntity document) throws ExecutionException, InterruptedException;
}
