package com.lx.aisuperagent.Tools;

import cn.hutool.core.io.FileUtil;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class PDFGenerationTool {

    @Tool(name = "Generate a PDF with the given content")
    public String GeneratePDF(
//            给大模型理解的参数
            @ToolParam(description = "Name of the file to save th egenenrated PDF") String fileName,
            @ToolParam(description = "content to be included in the PDF") String content) {
        String fileDir = FileConstant.FILE_SAVE_DIR;
        String filePath = fileDir + "/" + fileName;
        try {
            FileUtil.mkdir(fileDir);
//           创建PdfWriter 和 pdfDocument对象
            try (
                    PdfWriter writer = new PdfWriter(filePath);
                    PdfDocument pdf = new PdfDocument(writer);
                    Document document = new Document(pdf);
            ) {
                // 使用内置中文字体
                PdfFont font = PdfFontFactory.createFont("STSongStd-Light", "UniGB-UCS2-H");
                document.setFont(font);
                // 创建段落
                Paragraph paragraph = new Paragraph(content);
                // 添加段落并关闭文档
                document.add(paragraph);
            }
            return "PDF generated successfully to: " + filePath;
        } catch (IOException e) {
            return "Error generating PDF: " + e.getMessage();
        }
    }
}
