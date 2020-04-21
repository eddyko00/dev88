package com.afweb.service;

import com.afweb.util.CKey;
import com.afweb.util.getEnv;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

public class WebAppConfig {

    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        if ((CKey.SQL_DATABASE == CKey.LOCAL_MYSQL) || (CKey.SQL_DATABASE == CKey.REMOTE_MYSQL)) {
            dataSource.setDriverClassName("com.mysql.jdbc.Driver");
            dataSource.setUrl("jdbc:mysql://mysql:3306/sampledb");
            dataSource.setUsername("sa");
            dataSource.setPassword("admin");

            if (getEnv.checkLocalPC() == true) {
                String Local_mysql = "jdbc:mysql://localhost:3306/sampledb?useSSL=true";
                dataSource.setUrl(Local_mysql);

            }

        }
        if (CKey.SQL_DATABASE == CKey.MYSQL) {

//            dataSource.setDriverClassName("com.mysql.jdbc.Driver");
//            dataSource.setUrl("jdbc:mysql://www.db4free.net:3306/eddydb_sample?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC&useSSL=false");
//            dataSource.setUsername("eddysa");
//            dataSource.setPassword("eddyadmin");
            

            // create my MYSQL not PHP
//sh-4.2$ env | grep MYSQL
//MYSQL_PREFIX=/opt/rh/rh-mysql80/root/usr
//MYSQL_VERSION=8.0
//MYSQL_DATABASE=sampledb
//MYSQL_PASSWORD=admin
//MYSQL_PORT_3306_TCP_PORT=3306
//MYSQL_PORT_3306_TCP=tcp://172.30.253.152:3306
//MYSQL_SERVICE_PORT_MYSQL=3306
//MYSQL_PORT_3306_TCP_PROTO=tcp
//MYSQL_PORT_3306_TCP_ADDR=172.30.253.152
//MYSQL_SERVICE_PORT=3306
//MYSQL_USER=sa
//MYSQL_ROOT_PASSWORD=admin
//MYSQL_PORT=tcp://172.30.253.152:3306
//MYSQL_SERVICE_HOST=172.30.253

//Open Shfit error Loading class `com.mysql.jdbc.Driver'. This is deprecated. The new driver class is `com.mysql.cj.jdbc.Driver'. The driver is automatically registered via the SPI and manual loading of the driver class is generally unnecessary.
//            dataSource.setDriverClassName("com.mysql.jdbc.Driver");
            dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
            dataSource.setUrl("jdbc:mysql://172.30.253.152:3306/sampledb?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC&useSSL=false");
            dataSource.setUsername("sa");
            dataSource.setPassword("admin");            
            
//$host = "bmppikx9mn79axgjlhh4-mysql.services.clever-cloud.com";
//$username = "uphyltwqsqsipjri";
//$password = "5V7FaMjFWryhGXYcagw2";
//$DBName = "bmppikx9mn79axgjlhh4";        
//
//            dataSource.setDriverClassName("com.mysql.jdbc.Driver");
//            dataSource.setUrl("jdbc:mysql://bmppikx9mn79axgjlhh4-mysql.services.clever-cloud.com:3306/bmppikx9mn79axgjlhh4?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC&useSSL=false");
//            dataSource.setUsername("uphyltwqsqsipjri");
//            dataSource.setPassword("5V7FaMjFWryhGXYcagw2");    
            
        }

        if ((CKey.SQL_DATABASE == CKey.MSSQL) || (CKey.SQL_DATABASE == CKey.REMOTE_MS_SQL)) {           
            dataSource.setDriverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            dataSource.setUrl("jdbc:sqlserver://sql.freeasphost.net/MSSQL2016;databaseName=eddyko00_SampleDB");
            dataSource.setUsername("eddyko00_SampleDB");
            dataSource.setPassword("DBSamplePW");
        }

        CKey.dataSourceURL = dataSource.getUrl();
        return dataSource;
    }
}
