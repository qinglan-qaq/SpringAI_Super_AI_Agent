package com.lx.aisuperagent.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class LawAppDocumentLoader {
    private final ResourcePatternResolver resourcePatternResolver;

    //    构造器实现
    LawAppDocumentLoader(ResourcePatternResolver resourcePatternResolver) {
        this.resourcePatternResolver = resourcePatternResolver;
    }

    public List<Document> loadMarkdowns() {

        List<Document> allDocuments = new ArrayList<>();

        try {
//           获取资源 从Document路径下获取所有md文件
            Resource[] resources = resourcePatternResolver.getResources("classpath:document/*.md");
            log.info("发现{}个MarkDown文件", resources.length);

            for (Resource resource : resources) {
                String fileName = resource.getFilename();
                String title = resource.getDescription();
//                配置获取的参数
                MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
//                        新文档是否按照水平分割线划分
                        .withHorizontalRuleCreateDocument(true)
//                        是否包含代码块的内容
                        .withIncludeCodeBlock(false)
//                        是否包含引用块内容
                        .withIncludeBlockquote(false)
//                        将文件名作为metadata元数据
                        .withAdditionalMetadata("filename", fileName)
//                        将标题作为metadata元数据
                        .withAdditionalMetadata("title", title)
                        .build();


                MarkdownDocumentReader markdownDocumentReader = new MarkdownDocumentReader(resource, config);
                allDocuments.addAll(markdownDocumentReader.get());
                log.info("已读取所有MarkDown文件");
            }
        } catch (Exception e) {
            log.error("Markdown文档加载失败", e);
        }
        return allDocuments;
    }

}
