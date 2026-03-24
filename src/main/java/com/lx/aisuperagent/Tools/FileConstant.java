package com.lx.aisuperagent.Tools;

import java.nio.file.Paths;

public interface FileConstant {
    /**
     * 文件保存目录
     */
    String FILE_SAVE_DIR = Paths.get(
            System.getProperty("user.dir"),
            "src", "main", "java", "com", "lx", "aisuperagent", "tmp"
    ).toString();
}
