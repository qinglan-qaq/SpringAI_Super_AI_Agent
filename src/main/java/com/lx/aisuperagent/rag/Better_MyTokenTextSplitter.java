package com.lx.aisuperagent.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentTransformer;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * 文档分割器：
 * - splitWithBuilder() 使用 Spring AI 原生的 TokenTextSplitter（Builder模式）
 * - apply() 使用自定义的递归字符分割（语义优先、多级分隔符）
 */
@Component
public class Better_MyTokenTextSplitter implements DocumentTransformer {

    // 多级分隔符（优先级从高到低）
    private static final String[] SEPARATORS = {
            "\n\n",   // 段落
            "\n",     // 换行
            "。", "！", "？",   // 中文句子边界
            ". ", "! ", "? ",  // 英文句子边界
            " ",      // 单词
            ""        // 字符
    };

    // Token 计数函数（默认使用字符数/3 估算，可替换为精确计数器）
    private final Function<String, Integer> tokenCounter;
    private final int chunkSize;        // 目标块大小（token 数）
    private final int minChunkSize;     // 最小块大小（token，低于此值不单独成块）

    /**
     * 默认构造器：使用估算计数器，chunkSize=512，minChunkSize=10
     */
    public Better_MyTokenTextSplitter() {
        this(text -> (int) Math.ceil(text.length() / 3.0), 512, 10);
    }

    /**
     * 全参数构造器（支持自定义 token 计数函数）
     */
    public Better_MyTokenTextSplitter(
            Function<String, Integer> tokenCounter,
            int chunkSize,
            int minChunkSize) {
        this.tokenCounter = tokenCounter;
        this.chunkSize = chunkSize;
        this.minChunkSize = minChunkSize;
    }


    public List<Document> splitWithBuilder(List<Document> documents) {
        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(512)
                .withMinChunkSizeChars(400)
                .withMinChunkLengthToEmbed(10)
                .withMaxNumChunks(5000)
                .withKeepSeparator(true)
                .build();
        return splitter.apply(documents);
    }

    /**
     * 对长文本递归字符分割
     * 优先按照段落和句子分割
     * 最终使用的方案
     *
     * @param documents the function argument
     * @return
     */
    @Override
    public List<Document> apply(List<Document> documents) {
        List<Document> result = new ArrayList<>();

        for (Document doc : documents) {

            List<String> chunks = splitText(doc.getText(), 0);

            for (String chunk : chunks) {

                Document newDoc = new Document(chunk);
                //      创建新文档并复制元数据
                newDoc.getMetadata().putAll(doc.getMetadata());

                result.add(newDoc);

            }
        }
        return result;
    }

    /**
     * 递归分割核心方法
     *
     * @param text   待分割文本
     * @param sepIdx 当前使用的分隔符索引
     * @return 分割后的文本块列表
     */
    private List<String> splitText(String text, int sepIdx) {
        int tokenLen = tokenCounter.apply(text);
        //      满足大小要求，直接返回
        if (tokenLen <= chunkSize) {
            return List.of(text);
        }
        //      对于给定的符号划分
        String separator = SEPARATORS[sepIdx];
        List<String> pieces;

        if (separator.isEmpty()) {
            // 最后一级：按字符分割，并尝试合并到接近 chunkSize
            pieces = splitByChar(text);
        } else {
            pieces = splitBySeparator(text, separator);
        }

        // 如果无法分割（只有一个部分），降级到下一级分隔符
        if (pieces.size() <= 1) {
            if (sepIdx + 1 < SEPARATORS.length) {
                return splitText(text, sepIdx + 1);
            } else {
                // 所有分隔符均无效，强制按 token 大小截断
                return forceSplitByToken(text);
            }
        }

        // 递归处理每个片段
        List<String> result = new ArrayList<>();
        for (String piece : pieces) {
            // 若片段 token 数仍超标，继续用同一级分隔符递归（不再降级，因为已经分割过）
            if (tokenCounter.apply(piece) > chunkSize) {
                result.addAll(splitText(piece, sepIdx));
            } else {
                // 过滤掉过小的片段
                if (tokenCounter.apply(piece) >= minChunkSize) {
                    result.add(piece);
                }
            }
        }
        return result;
    }


    /**
     * 按分隔符分割，保留分隔符在结果中
     *
     * @param text
     * @param separator
     * @return
     */
    private List<String> splitBySeparator(String text, String separator) {
        List<String> parts = new ArrayList<>();
        int start = 0;
        int sepLen = separator.length();
        while (true) {
            int idx = text.indexOf(separator, start);
            if (idx == -1) {
                parts.add(text.substring(start));
                break;
            }
            parts.add(text.substring(start, idx + sepLen));
            start = idx + sepLen;
            if (start >= text.length()) break;
        }
        parts.removeIf(s -> s.trim().isEmpty());
        return parts;
    }

    /**
     * 按字符分割（最后兜底）
     *
     * @param text
     */
    private List<String> splitByChar(String text) {
        List<String> chars = new ArrayList<>();
        for (char c : text.toCharArray()) {
            chars.add(String.valueOf(c));
        }
        return chars;
    }

    /**
     * 强制按 token 数量截断
     */
    private List<String> forceSplitByToken(String text) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        int totalLen = text.length();
        int approxCharsPerToken = 3;  // 粗略估算
        int step = chunkSize * approxCharsPerToken;
        while (start < totalLen) {
            int end = Math.min(start + step, totalLen);
            chunks.add(text.substring(start, end));
            start = end;
        }
        return chunks;
    }
}