/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.stock;

import com.afweb.model.stock.*;

import com.afweb.service.ServiceAFweb;

import com.afweb.service.ServiceRemoteDB;

import com.afweb.util.CKey;
import com.afweb.util.TimeConvertion;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.sql.DataSource;
import java.util.logging.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/**
 *
 * @author eddyko
 */
public class NNetDB {

    protected static Logger logger = Logger.getLogger("NNetDB");

    private static JdbcTemplate jdbcTemplate;
    private static DataSource dataSource;
    private ServiceRemoteDB remoteDB = new ServiceRemoteDB();

//    private StockInfoDB stockinfodb = new StockInfoDB();
    /**
     * @return the dataSource
     */
    public DataSource getDataSource() {
        return dataSource;
    }

    /**
     * @return the jdbcTemplate
     */
    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    /**
     * @param jdbcTemplate the jdbcTemplate to set
     */
    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * @param dataSource the dataSource to set
     */
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

////////////////////////////  
/////////////////////////////////////////////////
    public boolean restStockInfoDB() {
        boolean status = true;
        try {
            processExecuteDB("drop table if exists dummynn1");
        } catch (Exception e) {
            logger.info("> RestStockInfoDB Table exception " + e.getMessage());
            status = false;
        }
        return status;
    }

    public boolean cleanStockInfoDB() {
        try {
            processExecuteDB("drop table if exists dummynn1");
            int result = initStockInfoDB();
            if (result == -1) {
                return false;
            }
            processExecuteDB("drop table if exists dummynn1");
            return true;
        } catch (Exception e) {
            logger.info("> RestStockDB Table exception " + e.getMessage());
        }
        return false;
    }

    public static String createDummyInfotable() {
        String sqlCMD = "";
        if ((CKey.SQL_DATABASE == CKey.DIRECT__MYSQL) || (CKey.SQL_DATABASE == CKey.REMOTE_PHP_MYSQL) || (CKey.SQL_DATABASE == CKey.LOCAL_MYSQL)) {
            sqlCMD = "create table dummyinfo1 (id int(10) not null auto_increment, primary key (id))";
        }
        return sqlCMD;
    }

    public int initStockInfoDB() {

        int total = 0;
        logger.info(">>>>> InitStockInfoDB Table creation");
        try {

            boolean initDBflag = false;
            if (initDBflag == true) {
                processExecuteDB("drop table if exists dummyinfo1");
            }
            total = getCountRowsInTable(getJdbcTemplate(), "dummyinfo1");
        } catch (Exception e) {
            logger.info("> InitStockInfoDB Table exception");
            total = -1;
        }
        if (total >= 0) {
            return 1;  // already exist
        }

        try {

            // sequency is important
            ArrayList dropTableList = new ArrayList();
            dropTableList.add("drop table if exists dummyinfo1");
            dropTableList.add("drop table if exists stockinfo");

            //must use this ExecuteSQLArrayList to exec one by one for 2 db 
            boolean resultDropList = ExecuteSQLArrayList(dropTableList);

            ArrayList createTableList = new ArrayList();
   

            if ((CKey.SQL_DATABASE == CKey.DIRECT__MYSQL) || (CKey.SQL_DATABASE == CKey.REMOTE_PHP_MYSQL) || (CKey.SQL_DATABASE == CKey.LOCAL_MYSQL)) {
                createTableList.add("create table dummyinfo1 (id int(10) not null auto_increment, primary key (id))");
                createTableList.add("create table stockinfo (id int(10) not null auto_increment, entrydatedisplay date not null, entrydatel bigint(20) not null, fopen float not null, fclose float not null, high float not null, low float not null, volume float not null, adjustclose float not null, sym varchar(255) not null, stockid int(10) not null, primary key (id))");
            }

            //must use this ExecuteSQLArrayList to exec one by one for 2 db 
            boolean resultCreate = ExecuteSQLArrayList(createTableList);

            logger.info("> InitStockInfoDB Done - result " + resultCreate);
            total = getCountRowsInTable(getJdbcTemplate(), "stockinfo");
            return 0;  // new database

        } catch (Exception e) {
            logger.info("> InitStockInfoDB Table exception " + e.getMessage());
        }
        return -1;
    }

    private boolean ExecuteSQLArrayList(ArrayList SQLTran) {
        String SQL = "";
        try {
            for (int i = 0; i < SQLTran.size(); i++) {
                SQL = (String) SQLTran.get(i);
                logger.info("> ExecuteSQLArrayList " + SQL);
                processExecuteDB(SQL);
            }
            return true;
        } catch (Exception e) {
            logger.info("> ExecuteSQLArrayList exception " + e.getMessage());
        }
        return false;

    }

    public int updateSQLInfoArrayList(ArrayList SQLTran) {

        if (ServiceAFweb.checkCallRemoteMysql() == true) {
            int ret = remoteDB.getExecuteRemoteListDB_Mysql(SQLTran);
            if (ret == 0) {
                return 0;
            }
            return 1;
        }

        try {
            for (int i = 0; i < SQLTran.size(); i++) {
                String SQL = (String) SQLTran.get(i);
                getJdbcTemplate().update(SQL);

                if ((i % 100) == 0) {
                    ServiceAFweb.AFSleep();
                }
            }
            return 1;
        } catch (Exception e) {
            logger.info("> UpdateSQLlList exception " + e.getMessage());
        }
        return 0;

    }

    ///////////
    public int getCountRowsInTable(JdbcTemplate jdbcTemplate, String tableName) throws Exception {
        if (ServiceAFweb.checkCallRemoteMysql() == true) {
            int count = remoteDB.getCountRowsRemoteDB_RemoteMysql(tableName);
            return count;
        }

        Integer result = jdbcTemplate.queryForObject("select count(0) from " + tableName, Integer.class);
        return (result != null ? result : 0);
    }

    public int processUpdateDB(String sqlCMD) throws Exception {
        if (ServiceAFweb.checkCallRemoteMysql() == true) {
            int ret = remoteDB.postExecuteRemoteDB_RemoteMysql(sqlCMD);
            return ret;
        }

//        logger.info("> processUpdateDB " + sqlCMD);
        getJdbcTemplate().update(sqlCMD);
        return 1;
    }

    public void processExecuteDB(String sqlCMD) throws Exception {
//        logger.info("> processExecuteDB " + sqlCMD);

        if (ServiceAFweb.checkCallRemoteMysql() == true) {
            int count = remoteDB.postExecuteRemoteDB_RemoteMysql(sqlCMD);
            return;
        }

        getJdbcTemplate().execute(sqlCMD);
    }

}
