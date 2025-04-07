package com.omega.document.processor.controller;

import com.omega.document.processor.entity.DocumentEntity;
import com.omega.document.processor.service.DocxService;
import com.omega.document.processor.service.TranslateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.concurrent.ExecutionException;

/**
 * Class DocumentController
 *
 * @author KennySo
 * @date 2025/4/5
 */
@RestController
@RequestMapping("/document")
public class DocumentController {

    @Autowired
    private DocxService docxService;

    @Autowired
    private TranslateService translateService;

    @PostMapping("/upload")
    public ResponseEntity<FileSystemResource> handleFileUpload(@RequestParam("file") MultipartFile file) throws ExecutionException, InterruptedException {

        DocumentEntity documentEntity = docxService.extractContent(file);

        DocumentEntity translated = translateService.translate(documentEntity);
        File filledFile = docxService.fillTemplate(translated);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filledFile.getName() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new FileSystemResource(filledFile));
    }
}
