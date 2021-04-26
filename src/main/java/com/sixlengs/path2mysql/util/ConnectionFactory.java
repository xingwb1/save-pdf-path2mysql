package com.sixlengs.path2mysql.util;


import cn.hutool.core.util.StrUtil;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.Properties;

public class ConnectionFactory {

    //    private static String driver;
//    private static String url;
//    private static String username;
//    private static String password;
    private static String tableName;
//    // 静态代码块，加载时只被执行一次
//    static {
//// Properties存储的是键值对的map
//        Properties props = new Properties();
//        props.put("user", "biao");
//        props.put("driver", "com.mysql.cj.jdbc.Driver");
//        props.put("password", "Caip2018!");
//        props.put("url", "jdbc:mysql://172.20.2.22:3306/task?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=GMT%2B8");
//
//        try {
////读取流的信息，加载到类里面
//            DRIVER = props.getProperty("driver");
//            URL = props.getProperty("url");
//            USER = props.getProperty("user");
//            PASSWORD = props.getProperty("password");
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    public static Connection getConnection(String ip, String database, String table, String username, String password) {
        String url = StrUtil.format("jdbc:mysql://{}:3306/{}?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=GMT%2B8", ip, database);
        Connection conn = null;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(url, username, password);
        } catch (Exception e) {
        }
        return conn;
    }

    public static void createTable(String tableName) {
        tableName = tableName;
        String createTable = StrUtil.format("create table {} ()", tableName);

    }

    public static void savePath(List<String> data, String tableName) {
        String sql = "insert table ";
    }


//        public static org.apache.hadoop.hbase.client.Connection createConnection(Configuration configuration, ExecutorService threads) {
//            Connection conn=null;
//            try {
//                Class.forName(DRIVER);
//                conn= DriverManager.getConnection(URL, USER, PASSWORD);
//            } catch (Exception e) {
//            }
//            return (org.apache.hadoop.hbase.client.Connection) conn;
//        }

    public static void main(String[] args) {
        System.out.println(ConnectionFactory.getConnection());
    }

}
