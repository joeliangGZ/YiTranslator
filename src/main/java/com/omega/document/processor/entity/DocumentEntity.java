package com.omega.document.processor.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Class DocumentContent
 *
 * @author KennySo
 * @date 2025/4/6
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentEntity {

    private String id;
    private String originalFilename;
    private String templateFilename;
    private List<DocumentItem> items;
}
