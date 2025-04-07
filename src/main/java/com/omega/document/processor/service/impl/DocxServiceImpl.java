package com.omega.document.processor.service.impl;

import com.omega.document.processor.entity.DocumentEntity;
import com.omega.document.processor.entity.DocumentItem;
import com.omega.document.processor.service.DocxService;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTText;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class WordProcessingService
 *
 * @author KennySo
 * @date 2025/4/6
 */
@Service("docxService")
public class DocxServiceImpl implements DocxService {

    private final String templateDir = "templates/";

    private final String productDir = "products/";

    public DocumentEntity extractContent(MultipartFile file) {
        try {
            // 生成唯一ID
            String uuid = UUID.randomUUID().toString();
            String originalFilename = file.getOriginalFilename();
            String baseName = Objects.requireNonNull(originalFilename).replace(".docx", "");
            String templateFilename = baseName + "_template_" + System.currentTimeMillis() + ".docx";

            // 创建存储目录
            File dir = new File(templateDir);
            if (!dir.exists()) dir.mkdirs();

            List<DocumentItem> items = new ArrayList<>();
            int num = 1;

            XWPFDocument doc = new XWPFDocument(file.getInputStream());
            FileOutputStream out = new FileOutputStream(templateDir + templateFilename);

            // 处理段落
            for (XWPFParagraph paragraph : doc.getParagraphs()) {
                num = processParagraph(paragraph, num, items);
            }

            // 处理表格
            for (XWPFTable table : doc.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        for (XWPFParagraph paragraph : cell.getParagraphs()) {
                            num = processParagraph(paragraph, num, items);
                        }
                    }
                }
            }

            doc.write(out);

            return new DocumentEntity(uuid, originalFilename, templateFilename, items);

        } catch (Exception e) {
            throw new RuntimeException("文档解析异常.");
        }
    }

    private int processParagraph(XWPFParagraph paragraph, int currentNum, List<DocumentItem> items) {
        String originalText = paragraph.getText();
        if (originalText == null || originalText.trim().isEmpty()) {
            return currentNum;
        }

        Pattern pattern = Pattern.compile("^(\\d+(?:\\.\\d+)*\\s+)(.*)$");
        Matcher matcher = pattern.matcher(originalText);
        String newText;
        String contentToStore;

        if (matcher.find()) {
            String numberPart = matcher.group(1);
            contentToStore = matcher.group(2);
            newText = numberPart + "{{" + currentNum + "}}";
        } else {
            contentToStore = originalText;
            newText = "{{" + currentNum + "}}";
        }

        items.add(new DocumentItem(currentNum, contentToStore, null));
        processPlaceholder(paragraph, newText);
        return currentNum + 1;
    }

    private void processPlaceholder(XWPFParagraph paragraph, String newText) {
        CTP ctp = paragraph.getCTP();
        List<CTR> runs = ctp.getRList();
        CTRPr copyStyle = null;

        if (!runs.isEmpty()) {
            CTR firstRun = runs.get(0);
            if (firstRun.isSetRPr()) {
                copyStyle = (CTRPr) firstRun.getRPr().copy();
            }
        }

        ctp.getRList().clear();
        CTR newRun = ctp.addNewR();
        if (copyStyle != null) {
            newRun.setRPr(copyStyle);
        }
        CTText text = newRun.addNewT();
        text.setStringValue(newText);
    }

    public File fillTemplate(DocumentEntity documentEntity) {
        File templateFile = new File(templateDir + documentEntity.getTemplateFilename());
        if (!templateFile.exists()) {
            throw new RuntimeException("Template file not found.");
        }

        Map<Integer, String> contentMap = new ConcurrentHashMap<>();
        for (DocumentItem item : documentEntity.getItems()) {
            contentMap.put(item.getPlaceholderNumber(), item.getTranslateContent());
        }

        try {
            XWPFDocument doc = new XWPFDocument(Files.newInputStream(templateFile.toPath()));

            // 处理段落
            for (XWPFParagraph paragraph : doc.getParagraphs()) {
                processParagraph(paragraph, contentMap);
            }

            // 处理表格
            for (XWPFTable table : doc.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        for (XWPFParagraph paragraph : cell.getParagraphs()) {
                            processParagraph(paragraph, contentMap);
                        }
                    }
                }
            }

            // 保存临时文件
            File dir = new File(productDir);
            if (!dir.exists()) dir.mkdirs();

            String outputFilename = documentEntity.getOriginalFilename();
            File outputFile = new File(productDir + outputFilename);
            try (FileOutputStream out = new FileOutputStream(outputFile)) {
                doc.write(out);
            }
            return outputFile;
        } catch (Exception e) {
            throw new RuntimeException("文档填充异常.");
        }
    }

    private void processParagraph(XWPFParagraph paragraph, Map<Integer, String> contentMap) {
        String paragraphText = paragraph.getText();
        if (paragraphText == null || paragraphText.trim().isEmpty()) return;

        // 匹配所有 {{数字}} 占位符
        Pattern pattern = Pattern.compile("\\{\\{(\\d+)\\}\\}");
        Matcher matcher = pattern.matcher(paragraphText);
        StringBuffer replacedText = new StringBuffer();

        // 逐个处理匹配项
        while (matcher.find()) {
            int placeholderId = Integer.parseInt(matcher.group(1));
            String replacement;
            // 关键改进：只在 map 中存在时替换，否则保留原占位符
            if (contentMap.containsKey(placeholderId)) {
                replacement = contentMap.get(placeholderId);
            } else {
                replacement = matcher.group(0); // 保留原始占位符文本
            }

            // 安全替换（处理特殊字符）
            matcher.appendReplacement(replacedText, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(replacedText);

        // 保留第一个 Run 的样式
        CTP ctp = paragraph.getCTP();
        List<CTR> runs = ctp.getRList();
        CTRPr copyStyle = !runs.isEmpty() && runs.get(0).isSetRPr() ? (CTRPr) runs.get(0).getRPr().copy() : null;

        // 清空原有内容并写入新文本
        ctp.getRList().clear();
        CTR newRun = ctp.addNewR();
        if (copyStyle != null) newRun.setRPr(copyStyle);
        newRun.addNewT().setStringValue(replacedText.toString());
    }

}
