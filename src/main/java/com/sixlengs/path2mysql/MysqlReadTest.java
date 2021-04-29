package com.sixlengs.path2mysql;

import cn.hutool.core.util.StrUtil;
import com.sixlengs.path2mysql.util.C3p0Utils;
import com.sixlengs.path2mysql.util.TimeUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * BelongsProject: 中汽知投大数据
 *
 * @author wb, xing
 * CreateTime: 2021/4/19 10:13
 * Description:
 */
@Slf4j
public class MysqlReadTest {

    private static String table = "wubiao_mysqlwrite_test";

    /**
     * 缓存阈值，批次处理大小
     */
    private static final int BATCH_SIZE = 1000;

    public static void main(String[] args) {
        Connection conn = null;


        // pathList 数据需要拷贝出来,多出的线程单独用,主线程需要继续使用pathList清空,遍历
        PreparedStatement ps = null;
        C3p0Utils.initDataSource("172.20.2.22", "task", "biao", "Caip2018!");
        createTable();
        // 数据准备
        List<String> pathList = new ArrayList<>();
        for (int i = 0; i < BATCH_SIZE; i++) {
            pathList.add("/data/full_img_unzip/cipan5/full image for industrial design/20080102/20080102-2-001/2/CN302007000138385CN00003007281640DPIDZH20080102CN00P/CN302007000138385CN00003007281640DPDFZH20080102CN00O.PDF");
        }
        try {
            conn = C3p0Utils.getConnection();
            conn.setAutoCommit(false);
            ps = conn.prepareStatement(StrUtil.format("insert into {} values (null,?)", table));
            // 批次写入 1000条
            long start = System.currentTimeMillis();
            for (int i = 0; i < 1; i++) {
                for (String path : pathList) {
//                    ps.setString(1, bean.getFileName());
                    ps.setString(1, path);

                    ps.addBatch();
                    ps.executeBatch();
                    conn.commit();
                    ps.clearBatch();
                }
            }

            log.warn("批次大小【{}】 花费时间{}", pathList.size(),TimeUtils.longDiffFormat(start));
//                }
        } catch (Exception e) {
            log.error("写入数据库错误", e);
        } finally {
            C3p0Utils.release(conn, ps, null);
        }

    }


    public static void createTable() {
        Connection conn = null;
        PreparedStatement preparedStatement = null;
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd_HHmmss");
//            table = table + "_" + format.format(new Date());
            conn = C3p0Utils.getConnection();
            preparedStatement = conn.prepareStatement(StrUtil.format("CREATE TABLE IF NOT EXISTS {}  (" +
                    "  `num` int(8) NOT NULL AUTO_INCREMENT," +
                    "  `file_path` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL," +
                    "  PRIMARY KEY (`num`) USING BTREE" +
                    ") ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;", table));
            preparedStatement.execute();

        } catch (SQLException e) {
           e.printStackTrace();
        } finally {
            C3p0Utils.release(conn, preparedStatement, null);
        }
    }

}
