package com.sixlengs.path2mysql;

import cn.hutool.core.util.StrUtil;
import com.sixlengs.path2mysql.util.C3p0Utils;
import com.sixlengs.path2mysql.util.TimeUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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
    static String ip;
    static String database;
    static String table;
    static String username;
    static String password;
    static String suffix;
    static final ExecutorService threadPool = Executors.newFixedThreadPool(10);

    /**
     * 递归获取文件路径, 组数计数器 非多线程
     */
    static long readPathGroupNum = 1;
    /**
     * 路径写入mysql 组数计数器,多线程
     */
    private static AtomicInteger mysqlGroupNum = new AtomicInteger(1);

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
                log.info("任务分析: 解析【{}】目录下, 后缀为:【{}】 (*代表所有文件) 的文件绝对路径写入mysql: ip：【{}】  库名：【{}】 表名：【{}】  用户名:【{}】 密码:【{}】", directory, suffix, ip, database, table, username, password);

                List<String> buffer = getAllFilePath(new File(directory), new ArrayList<String>());
//                List<String> buffer = scanDirNoRecursion(directory);
                log.info("最后清理缓冲区数据 条目数【{}】", buffer.size());
                //  递归时,每满2000写一次,最后返回的是最后一批,不满2000的数据,写入mysql
                //  写入mysql ,使用多线程,不能阻塞住
                threadPool.execute(new MysqlTask(buffer));

                // 停止接收新任务
                threadPool.shutdown();
                // 等待子线程执行完毕
                threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                log.info("------任务结束------- 当前时间{}  花费时间{} \n\n", TimeUtils.format(new Date()), TimeUtils.longDiffFormat(nowStart, System.currentTimeMillis()));
                System.exit(0);
            } else {
              log.error("错误参数个数{},使用格式:  目录 后缀: PDF/* ip  库名 表名 用户名 密码",args.length);
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
    //非递归
    public static List<String> scanDirNoRecursion(String path){
        List<String> buffer = new ArrayList<>();
        LinkedList list = new LinkedList();
        File dir = new File(path);
        File file[] = dir.listFiles();
        for (int i = 0; i < file.length; i++) {
            if (file[i].isDirectory()) {
                list.add(file[i]);
            } else{
                dealFile(file[i],buffer);
//                System.out.println(file[i].getAbsolutePath());
//                num++;
            }
        }
        File tmp;
        while (!list.isEmpty()) {
            tmp = (File)list.removeFirst();//首个目录
            if (tmp.isDirectory()) {
                file = tmp.listFiles();
                if (file == null) {
                    continue;
                }
                for (int i = 0; i < file.length; i++) {
                    if (file[i].isDirectory()) {
                        list.add(file[i]);//目录则加入目录列表，关键
                    } else{
                        dealFile(file[i],buffer);
//                        System.out.println(file[i].getAbsolutePath());
//                        num++;
                    }
                }
            } else {
                dealFile(tmp,buffer);
//                System.out.println(tmp.getAbsolutePath());
//                num++;
            }
        }
        return buffer;
    }
    private static void dealFile(File file,List<String> buffer) {
        try {
            // 未指定后缀
            if ("*".equals(suffix)) {
                buffer.add(file.getAbsolutePath().replace("\\", "/"));
                if (buffer.size() >= batchSize) {
                    log.info("路径遍历获取所有文件 组数【{}】 批次大小【{}】", readPathGroupNum++, batchSize);
                    // 等待,内存最多存 一万条
                    while (true) {
                        if (readPathGroupNum - mysqlGroupNum.longValue() > 10) {
                            Thread.sleep(10);
                        } else {
                            break;
                        }
                    }
                    threadPool.execute(new MysqlTask(buffer));
                    buffer.clear();
                }
            }
            // 指定后缀 , 大写比较
            else {
                if (file.getAbsolutePath().toUpperCase().endsWith(suffix)) {
                    buffer.add(file.getAbsolutePath().replace("\\", "/"));
                    if (buffer.size() >= batchSize) {
                        log.info("路径遍历{}文件 组数【{}】 批次大小【{}】", suffix, readPathGroupNum++, batchSize);
                        while (true) {
                            if (readPathGroupNum - mysqlGroupNum.longValue() > 10) {
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
            // 最后清空不够2000条的缓存
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    static int batchSize = 200;
    public static List<String> getAllFilePath(File file, List<String> buffer) {
        try {
            if (file != null) {
                if (file.isDirectory()) {
                    File[] files = file.listFiles();
                    // FIXME 空文件夹 null 指针?
                    if (files != null && files.length > 0) {
                        for (File fileSon : files) {
                            getAllFilePath(fileSon, buffer);
                        }
                    }
                } else if (file.isFile()) {
                    // 未指定后缀
                    if ("*".equals(suffix)) {
                        buffer.add(file.getAbsolutePath().replace("\\", "/"));
                        if (buffer.size() >= batchSize) {
                            log.info("路径遍历获取所有文件 组数【{}】 批次大小【{}】", readPathGroupNum++, batchSize);
                            // 等待,内存最多存 一万条
                            while (true) {
                                if (readPathGroupNum - mysqlGroupNum.longValue() > 10) {
                                    Thread.sleep(50);
                                } else {
                                    break;
                                }
                            }
                            threadPool.execute(new MysqlTask(buffer));
                            buffer.clear();
                        }
                    }
                    // 指定后缀 , 大写比较
                    else {
                        if (file.getAbsolutePath().toUpperCase().endsWith(suffix)) {
                            buffer.add(file.getAbsolutePath().replace("\\", "/"));
                            if (buffer.size() >= batchSize) {
                                log.info("路径遍历{}文件 组数【{}】 批次大小【{}】",suffix, readPathGroupNum++, batchSize);
                                while (true) {
                                    if (readPathGroupNum - mysqlGroupNum.longValue() > 10) {
                                        Thread.sleep(50);
                                    } else {
                                        break;
                                    }
                                }
                                threadPool.execute(new MysqlTask(buffer));
                                buffer.clear();
                            }
                        }
                    }
                    // 最后清空不够2000条的缓存
                }
            }
        } catch (Exception e) {
            log.error("遍历异常 {}", e.getMessage(), e);
        }
        return buffer;
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

        private MysqlTask() {
        }

        // pathList 数据需要拷贝出来,多出的线程单独用,主线程需要继续使用pathList清空,遍历
        public MysqlTask(List<String> pathList) {
            this.pathList = new ArrayList<>(pathList);
        }

        @Override
        public void run() {
            PreparedStatement ps = null;
            try {
                conn = C3p0Utils.getConnection();
                conn.setAutoCommit(false);
                ps = conn.prepareStatement(StrUtil.format("insert into {} values (null,?)", table));
//                List<List<String>> lists = ListSplitUtils.subListByNum(pathList, 1000);
//                for (List<String> list : lists) {
                // 批次写入 1000条
                for (String path : pathList) {
//                    ps.setString(1, bean.getFileName());
                    ps.setString(1, path);

                    ps.addBatch();
                    ps.executeBatch();
                    conn.commit();
                    ps.clearBatch();
                }
                log.warn("文件路径写入mysql, 组数【{}】 批次大小【{}】", mysqlGroupNum.getAndIncrement(), pathList.size());
//                }
            } catch (Exception e) {
                log.error("写入数据库错误", e);
            } finally {
                C3p0Utils.release(conn, ps, null);
            }
        }
    }
}
