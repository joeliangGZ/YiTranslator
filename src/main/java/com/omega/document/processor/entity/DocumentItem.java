package com.omega.document.processor.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Class TranslationItem
 *
 * @author KennySo
 * @date 2025/4/6
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DocumentItem {

    private int placeholderNumber;
    private String originalContent;
    private String translateContent;
}
