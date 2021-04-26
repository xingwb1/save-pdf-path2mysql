package com.sixlengs.path2mysql.po;

import lombok.Data;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * BelongsProject: 中汽知投大数据
 *
 * @author wb, xing
 * CreateTime: 2021/4/19 10:14
 * Description:
 */
@Data
public class Full_img_unzip_2014_2015_fill_PDF_PATH {
    static Pattern PATTERN = Pattern.compile("(?<directory>.*)/(?<fileName>.*)\\.(?<suffix>.*)");
    /**
     * pdf 文件名 , 主键
     */
    private String fileName;
    /**
     * pdf 绝对路径
     */
    private String absolutePath;

    public Full_img_unzip_2014_2015_fill_PDF_PATH(String name, String fileName) {
        this.fileName = fileName;
        this.absolutePath = absolutePath;
    }


    public Full_img_unzip_2014_2015_fill_PDF_PATH(String absolutePath) {
        this.absolutePath = absolutePath;
        Matcher matcher = PATTERN.matcher(absolutePath);
        while (matcher.find()) {
            this.fileName = matcher.group("fileName");
        }
    }


}
