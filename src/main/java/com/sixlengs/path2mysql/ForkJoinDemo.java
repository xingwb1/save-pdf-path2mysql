package com.sixlengs.path2mysql;

import com.sixlengs.path2mysql.util.TimeUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicInteger;

/*
使用fork/join的异步用法演示不要求返回值
遍历指定的目录，包含了子目录，寻找指定类型的文件
 */
public class ForkJoinDemo {
   private static BufferedWriter writer;

    static {
        try {
            writer = new BufferedWriter(new FileWriter("aa1.txt", true));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
    不要求返回值的情况
     */
    private static class ForkJoinScanDirTask extends RecursiveAction {
        File file;//表示进行查询的目录

        public ForkJoinScanDirTask(File path) {
            this.file = path;
        }

        @Override
        protected void compute() {
            try {
                ArrayList<ForkJoinScanDirTask> subTaskLists = new ArrayList<>();
            /*
            如果是目录，继续进行拆分
             */
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
                        // 如果是目录
                    } else {
                        //进行筛选
//                    boolean matches = file.getAbsolutePath().endsWith("txt");//判断是不是txt文件
//                    if (matches) {
//                        System.out.println("文件："+file.getName());
//                    }
                        buffer.add(file.getAbsolutePath());

                        writer.write(file.getAbsolutePath() + "==>" + buffer.size());
                        writer.newLine();
                        writer.flush();
//                        System.out.println(file.getAbsolutePath()+counter.get()+"==>"+buffer.size());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static List<String> buffer = new ArrayList<>();
    private static AtomicInteger counter = new AtomicInteger(1);

    // forkjoin 10线程 44秒 622毫秒 , 不打印控制台 36秒,  不计数 34秒
    //  20线程 47秒 , 线程不是越多越好
    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        ForkJoinPool pool = new ForkJoinPool(10);
        String path = "C:/";
        File file = new File(path);
        ForkJoinScanDirTask myTask = new ForkJoinScanDirTask(file);
        pool.execute(myTask);
        myTask.join();
        System.out.println(TimeUtils.longDiffFormat(start));
    }

}