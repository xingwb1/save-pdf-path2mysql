package com.sixlengs.path2mysql;

import com.sixlengs.path2mysql.util.TimeUtils;
import lombok.SneakyThrows;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * BelongsProject: 中汽知投大数据
 *
 * @author wb, xing
 * CreateTime: 2021/4/28 17:10
 * Description:
 */
public class Test {
    //17659
    static final ExecutorService threadPool = Executors.newFixedThreadPool(10);
    static BufferedWriter writer = null;

    static {
        try {
//            writer = new BufferedWriter(new FileWriter("递归.txt"));
            writer = new BufferedWriter(new FileWriter("线程池递归.txt"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            String path = "C:\\Program Files";
            threadPool.execute(new ScanFileTask(new File(path)));
//        threadPool.awaitTermination();

            System.out.println("最后buffer大小" + buffer.size());
            System.out.println("最后计数器" + counter.get());
//        threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

//        scanFile(new File(path));
        } catch (Exception e) {
            e.printStackTrace();

//            threadPool.shutdown();
        }
    }

    // 单线程递归,  文件:858270 耗时:2分钟 31秒 463毫秒
//    public static void main(String[] args) throws Exception {
//        String path = "C:";
//        long start = System.currentTimeMillis();
//        scanFile(new File(path));
//        System.out.println(TimeUtils.longDiffFormat(start, System.currentTimeMillis()));
//    }

    private static List<String> buffer = new ArrayList<>();
    private static AtomicInteger counter = new AtomicInteger(0);

    private static class ScanFileTask implements Runnable {
        private File file;
//        private List<String> buffer;

        public ScanFileTask(File file) {
            this.file = file;
        }

        public ScanFileTask(File file, List<String> buffer) {
            this.file = file;
//            this.buffer = buffer;
        }

        @SneakyThrows
        @Override
        public synchronized void run() {

            if (file != null) {
                if (file.isDirectory()) {
                    System.out.println(Thread.currentThread().getName() + "====>目录" + file.getAbsolutePath());
                    File[] files = file.listFiles();
                    if (files != null) {
                        for (File sonFile : files) {
//                                ExecutorService threadPool = Executors.newFixedThreadPool(4);
                            threadPool.execute(new ScanFileTask(sonFile));
//                                threadPool.shutdown();
//                                threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                        }
                    }
                } else {
                    synchronized ("aaa") {
                        buffer.add(file.getAbsolutePath());
                        counter.getAndIncrement();
                        writer.write(TimeUtils.format(new Date()) + file.getAbsolutePath() + counter.get() + "==>" + buffer.size());
                        writer.newLine();
                        writer.flush();
                        System.out.println(Thread.currentThread().getName() + "====>文件" + file.getAbsolutePath() + "  " + counter.get() + "==>" + buffer.size());
//                        System.out.println(System.currentTimeMillis());
                    }
                }
            }

        }
    }

    private static void scanFile(File file) throws Exception {
        if (file != null) {
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                if (Objects.nonNull(files)) {
                    for (File sonFile : files) {
                        scanFile(sonFile);
                    }
                }
            } else {
                buffer.add(file.getAbsolutePath());
                counter.getAndIncrement();
                writer.write(file.getAbsolutePath());
                writer.newLine();
                writer.flush();
                System.out.println("====>文件" + file.getAbsolutePath() + "  " + counter.get() + "==>" + buffer.size());

            }
        }
    }

}
