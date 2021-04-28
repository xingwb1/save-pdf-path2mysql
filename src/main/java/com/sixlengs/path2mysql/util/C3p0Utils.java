package com.sixlengs.path2mysql.util;

import cn.hutool.core.util.StrUtil;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Slf4j
public class C3p0Utils {

    static ComboPooledDataSource dataSource=new ComboPooledDataSource("mysql");


    public static void initDataSource(String ip,String database,String username,String password){
        String url = StrUtil.format("jdbc:mysql://{}:3306/{}?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=GMT%2B8", ip, database);
        dataSource.setJdbcUrl(url);
        dataSource.setUser(username);
        dataSource.setPassword(password);
    }
    //从连接池中取用一个连接
    public static Connection getConnection(){
        try {

            return dataSource.getConnection();
        } catch (Exception e) {
            log.error("C3p0Utils 数据库连接出错", e);
        }
        return null;
    }

    //释放连接回连接池
     public static void release(Connection conn, PreparedStatement pst, ResultSet rs){
            if(rs!=null){  
                try {  
                    rs.close();  
                } catch (SQLException e) {
                    log.error("C3p0Utils:ResultSet关闭出错", e);
                }
            }  
            if(pst!=null){  
                try {  
                    pst.close();  
                } catch (SQLException e) {  
                    log.error("C3p0Utils:PreparedStatement", e);
                }
            }  
      
            if(conn!=null){  
                try {  
                    conn.close();  
                } catch (SQLException e) {
                    log.error("C3p0Utils 数据库连接关闭出错", e);
                }
            }  
        }

    public static void main(String[] args) {
        C3p0Utils.initDataSource("172.20.2.22","task","biao","Caip2018!");
        Connection connection = C3p0Utils.getConnection();
        System.out.println(connection);
    }
}