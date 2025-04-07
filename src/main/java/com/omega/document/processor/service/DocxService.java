package com.omega.document.processor.service;

import com.omega.document.processor.entity.DocumentEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

/**
 * Interface DocumentService
 *
 * @author KennySo
 * @date 2025/4/6
 */
public interface DocxService {

    DocumentEntity extractContent(MultipartFile file);

    File fillTemplate(DocumentEntity entity);
}
