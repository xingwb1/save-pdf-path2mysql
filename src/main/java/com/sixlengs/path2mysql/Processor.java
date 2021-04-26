package com.sixlengs.path2mysql;

import com.sixlengs.path2mysql.po.Full_img_unzip_2014_2015_fill_PDF_PATH;
import com.sixlengs.path2mysql.util.ConnectionFactory;
import com.sixlengs.path2mysql.util.ListSplitUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * BelongsProject: 中汽知投大数据
 *
 * @author wb, xing
 * CreateTime: 2021/4/19 10:13
 * Description:
 */
@Slf4j
public class Processor {
    static String directory;
    private static Connection conn = null;

//    private static PreparedStatement ps = null;
//    private static Logger logger = Logger.getLogger(Processor.class.getName());
//    private String sourceTable = "pdf_path_full_img_unzip_2014_2015_fill";

    public static void main(String[] args) {
        if (args != null && args.length >= 1) {
            directory = args[0];
            // 相对路径替换为绝对路径   Windows 路径转义   去掉最后的 / , 如果有
            directory = dealPath(directory);
            log.info("解析【{}】目录下 路径至mysql task表",directory);

            // 第一层遍历
            File[] files = new File(directory).listFiles();
            for (File fileSon : files) {
                List<String> pdfList = getAlldirectory(fileSon, new ArrayList<String>());
                List<Full_img_unzip_2014_2015_fill_PDF_PATH> list = pdfList.stream().map(s -> {
                    return new Full_img_unzip_2014_2015_fill_PDF_PATH(s);
                }).collect(Collectors.toList());
                save2mysql(list);

            }


        } else {
            System.out.println("参数错误");
            System.exit(0);
        }
    }

    // 相对路径替换为绝对路径   Windows 路径转义   去掉最后的 / , 如果有
    public static String dealPath(String path) {
        path = new File(path).getAbsolutePath().replace("\\", "/");
        return subs(path);
    }

    // 删除路径最后的斜杠
    public static String subs(String s) {
        if (s.lastIndexOf("/") == s.length()) {
            return s.substring(0, s.lastIndexOf("/"));
        }
        return s;
    }

    public static List<String> getAllFilePath(File file, List<String> filePathList) {
        if (file != null) {
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                for (File fileSon : files) {
                    getAllFilePath(fileSon, filePathList);
                }
            } else if (file.isFile() && file.getAbsolutePath().toUpperCase().endsWith("PDF")) {
                filePathList.add(file.getAbsolutePath().replace("\\", "/"));
            }
        }
        return filePathList;
    }

    public static void save2mysql(List<Full_img_unzip_2014_2015_fill_PDF_PATH> pdfList) {
        try {
            Connection conn = ConnectionFactory.getConnection();
            PreparedStatement ps;
            ps = conn.prepareStatement("insert into pdf_path_full_img_unzip_2014_2015_fill values (?,?)");
            List<List<Full_img_unzip_2014_2015_fill_PDF_PATH>> lists = ListSplitUtils.subListByNum(pdfList, 1000);
            for (List<Full_img_unzip_2014_2015_fill_PDF_PATH> list : lists) {
                // 批次写入 1000条
                log.info("PDF路径写入mysql,当前批次【{}】",list.size());
                for (Full_img_unzip_2014_2015_fill_PDF_PATH bean : list) {
                    ps.setString(1, bean.getFileName());
                    ps.setString(2, bean.getAbsolutePath());

                    ps.addBatch();
                    ps.executeBatch();
                    conn.setAutoCommit(false);
                    conn.commit();
                    ps.clearBatch();
                }
            }
            ps.close();

        }catch (Exception e){
            log.error("写入数据库错误",e);
        }
    }
}
