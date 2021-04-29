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
public class ProcessorForkJoin {
    private static String directory;
    private static String ip;
    private static String database;
    private static String table;
    private static String username;
    private static String password;
    private static String suffix;
    private static final Integer THREAD_NUM = 10;
    /**
     * main参数，所有数据类型 all
     */
    private static final String ALL = "ALL";
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(THREAD_NUM);
    private static final ForkJoinPool forkJoinPool = new ForkJoinPool(THREAD_NUM);

    /**
     * 递归获取文件路径, 组数计数器 多线程
     */
    private static AtomicInteger readPathGroupNum = new AtomicInteger(0);
    /**
     * 路径写入mysql 组数计数器,多线程
     */
    private static AtomicInteger mysqlGroupNum = new AtomicInteger(0);
    /**
     * ForkJoin 递归便利目录，路径计数器，用于和写入mysql数据进行比对，校验核对，保证多线程遍历中数据没有丢失
     */
    private static AtomicInteger scanSum = new AtomicInteger(0);
    /**
     * 写入mysql 总数
     */
    private static AtomicInteger writeSum = new AtomicInteger(0);
    /**
     * ForkJoin , 多线程递归，将路径统一写入缓存
     */
    private static List<String> buffer = new ArrayList<>();
    /**
     * 缓存阈值，批次处理大小
     */
    private static final int BATCH_SIZE = 1000;

    public static void main(String[] args) {
        try {
            // 目录 后缀: PDF/* ip 库 表名 用户名 密码    >=7原因: 可能有jvm 参数
            if (args != null && args.length >= 7) {
                directory = args[0];
                // 相对路径替换为绝对路径   Windows 路径转义   去掉最后的 / , 如果有
                directory = dealPath(directory);
                suffix = args[1].toUpperCase();
                ip = args[2];
                database = args[3];
                table = args[4];
                username = args[5];
                password = args[6];
                // 初始化连接
                C3p0Utils.initDataSource(ip, database, username, password);
                // 开始时间计时
                long nowStart = System.currentTimeMillis();
                // 建表
                createTable();
                log.info("任务分析: 解析【{}】目录下, 后缀为:【{}】 (All/all代表所有文件) 的文件绝对路径写入mysql: ip：【{}】  库名：【{}】 表名：【{}】  用户名:【{}】 密码:【{}】", directory, suffix, ip, database, table, username, password);

                // ForkJoin 递归遍历目录
                File file = new File(directory);
                if (!file.exists()) {
                    throw new FileNotFoundException(StrUtil.format("指定目录不存在：【{}】", file.getAbsolutePath()));
                }
                ForkJoinScanDirTask scanTask = new ForkJoinScanDirTask(new File(directory));
                forkJoinPool.execute(scanTask);
                scanTask.join();
                // 处理缓存区剩余 ， 不足1000数据
                log.info("最后清理缓冲区数据 条目数【{}】", buffer.size());
//                //  写入mysql ,使用多线程,不能阻塞住
                threadPool.execute(new MysqlTask(buffer));

                // 停止接收新任务
                threadPool.shutdown();
                // 等待子线程执行完毕
                threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                log.info("------任务结束------- 当前时间{}  花费时间{} \n\n", TimeUtils.format(new Date()), TimeUtils.longDiffFormat(nowStart, System.currentTimeMillis()));
                System.exit(0);
            } else {
                log.error("错误参数个数{}:<7,使用格式:  目录 后缀: PDF/* ip  库名 表名 用户名 密码  +其他jvm参数(可选)", args.length);
                System.exit(0);
            }
        } catch (Exception e) {
            log.error("错误{}", e.getMessage(), e);
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


    private static class ForkJoinScanDirTask extends RecursiveAction {
        //查询的目录
        File file;

        public ForkJoinScanDirTask(File path) {
            this.file = path;
        }

        @Override
        protected void compute() {
            try {
                List<ForkJoinScanDirTask> subTaskLists = new ArrayList<>();
                //目录
                if (file != null) {
                    if (file.isDirectory()) {
                        //所有任务的列表
                        //展示目录下面的文件
                        File[] files = file.listFiles();
                        //有可能外表是一个目录，但是里面却没有一个文件
                        if (files != null) {
                            for (File file : files) {
                                ForkJoinScanDirTask subTask = new ForkJoinScanDirTask(file);
                                subTaskLists.add(subTask);
                            }
                            if (!subTaskLists.isEmpty()) {
                                for (ForkJoinScanDirTask subTaskList : invokeAll(subTaskLists)) {
                                    subTaskList.join();
                                }
                            }
                        }
                        // 文件
                    } else {
                        synchronized ("scanFile") {
                            //筛选
                            //全部文件，没指定后缀
                            if (ALL.equals(suffix.toUpperCase())) {
                                buffer.add(file.getAbsolutePath().replace("\\", "/"));
                                scanSum.incrementAndGet();
                                if (buffer.size() >= BATCH_SIZE) {
                                    log.info("文件(All)已读 组数【{}】 批次大小【{}】 读取文件总数校验：【{}】", readPathGroupNum.incrementAndGet(), BATCH_SIZE, scanSum.get());
                                    // 等待,内存最多存 一万条
                                    while (true) {
                                        if (readPathGroupNum.get() - mysqlGroupNum.get() >= THREAD_NUM) {
                                            Thread.sleep(10);
                                        } else {
                                            break;
                                        }
                                    }
                                    threadPool.execute(new MysqlTask(buffer));
                                    buffer.clear();
                                }
                                //指定后缀
                            } else {
//
                                if (file.getAbsolutePath().toUpperCase().endsWith(suffix)) {
                                    buffer.add(file.getAbsolutePath().replace("\\", "/"));
                                    scanSum.incrementAndGet();
                                    if (buffer.size() >= BATCH_SIZE) {
                                        log.info("文件{}已读 组数【{}】 批次大小【{}】 文件总数校验：【{}】", suffix, readPathGroupNum.incrementAndGet(), BATCH_SIZE, scanSum.get());
//                                        log.info("{}文件已读 组数【{}】 批次大小【{}】 ForkJoin多线程递归遍历文件数校验：【{}】", suffix, readPathGroupNum.incrementAndGet(), BATCH_SIZE, scanSum.get());
                                        while (true) {
                                            if (readPathGroupNum.get() - mysqlGroupNum.get() >= THREAD_NUM) {
                                                Thread.sleep(10);
                                            } else {
                                                break;
                                            }
                                        }
                                        threadPool.execute(new MysqlTask(buffer));
                                        buffer.clear();
                                    }
                                }
                            }

                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public static void createTable() {
        Connection conn = null;
        PreparedStatement preparedStatement = null;
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd_HHmmss");
            table = table + "_" + format.format(new Date());
            conn = C3p0Utils.getConnection();
            preparedStatement = conn.prepareStatement(StrUtil.format("create table {}  (" +
                    "  `num` int(8) NOT NULL AUTO_INCREMENT," +
                    "  `file_path` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL," +
                    "  PRIMARY KEY (`num`) USING BTREE" +
                    ") ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;", table));
            preparedStatement.execute();

        } catch (SQLException e) {
            log.error("建表失败 {}.{}", database, table, e);
        } finally {
            C3p0Utils.release(conn, preparedStatement, null);
        }
    }

    private static class MysqlTask implements Runnable {
        private Connection conn = null;
        List<String> pathList;

        // pathList 数据需要拷贝出来,多出的线程单独用,主线程需要继续使用pathList清空,遍历
        public MysqlTask(List<String> pathList) {
            this.pathList = new ArrayList<>(pathList);
        }

        @Override
        public void run() {
            synchronized ("mysql") {
                PreparedStatement ps = null;
                try {
                    conn = C3p0Utils.getConnection();
                    conn.setAutoCommit(false);
                    ps = conn.prepareStatement(StrUtil.format("insert into {} values (null,?)", table));
//                List<List<String>> lists = ListSplitUtils.subListByNum(pathList, 1000);
//                for (List<String> list : lists) {
                    // 批次写入 1000条
                    for (String path : pathList) {
                        writeSum.incrementAndGet();
//                    ps.setString(1, bean.getFileName());
                        ps.setString(1, path);

                        ps.addBatch();
                        ps.executeBatch();
                        conn.commit();
                        ps.clearBatch();
                    }
                    log.warn("已写mysql, 组数【{}】 批次大小【{}】 写入文件总数校验:【{}】", mysqlGroupNum.incrementAndGet(), pathList.size(), writeSum.get());
//                }
                } catch (Exception e) {
                    log.error("写入数据库错误", e);
                } finally {
                    C3p0Utils.release(conn, ps, null);
                }
            }
        }
    }
}
