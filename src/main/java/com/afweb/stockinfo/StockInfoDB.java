/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.stockinfo;

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
 * @author eddy
 */
public class StockInfoDB {

    protected static Logger logger = Logger.getLogger("StockInfoDB");

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
//    public int deleteStockInfo(AFstockInfo stockInfo) {
//
//        try {
//            String deleteSQL = "delete from stockinfo where id=" + stockInfo.getId();
//            return processUpdateDB(deleteSQL);
//        } catch (Exception e) {
//            logger.info("> deleteStockInfo exception " + e.getMessage());
//        }
//        return 0;
//    }
    public int deleteStockInfoBySymDate(String sym, long datel) {

        try {
            String deleteSQL = "delete from stockinfo where sym='" + sym + "' and entrydatel < " + datel;
            return processUpdateDB(deleteSQL);
        } catch (Exception e) {
            logger.info("> deleteStockInfoByDate exception " + e.getMessage());
        }
        return 0;
    }

    public int deleteStockInfoBySym(String sym) {

        try {
            String deleteSQL = "delete from stockinfo where sym='" + sym + "'";
            return processUpdateDB(deleteSQL);
        } catch (Exception e) {
            logger.info("> deleteStockInfoByStockId exception " + e.getMessage());
        }
        return 0;
    }

    public ArrayList<AFstockInfo> getStockInfoBySymRange(String sym, long start, long end) {

        try {

            String sql = "select * from stockinfo where sym = '" + sym + "'";
            sql += " and entrydatel >= " + end + " and entrydatel <= " + start + " order by entrydatel desc";

            ArrayList<AFstockInfo> entries = getStockInfoListSQL(sql);
            return (ArrayList) entries;
        } catch (Exception e) {
            logger.info("> getStockInfo exception " + sym + " - " + e.getMessage());
        }
        return null;
    }

    // Heuoku cannot get the date of the first stockinfo????
    public ArrayList<AFstockInfo> getStockInfo_workaround(String sym, int length, Calendar dateNow) {

        try {
            String sql = "";
            sql = "select * from stockinfo where sym = '" + sym + "'";
            sql += " order by entrydatel desc";

            sql = ServiceAFweb.getSQLLengh(sql, length);

            ArrayList<AFstockInfo> entries = getStockInfoListSQL(sql);
            return (ArrayList) entries;
        } catch (Exception e) {
            logger.info("> getStockInfo exception " + sym + " - " + e.getMessage());
        }
        return null;
    }

    public ArrayList<AFstockInfo> getStockInfoBySym(String sym, int length, Calendar dateNow) {

        if (dateNow == null) {
            long date = TimeConvertion.getCurrentCalendar().getTimeInMillis();
            dateNow = TimeConvertion.workaround_nextday_endOfDate(date);
        }
        try {

            long stockInfoEndday = TimeConvertion.endOfDayInMillis(dateNow.getTimeInMillis());
            String sql = "select * from stockinfo where sym = '" + sym + "'";
            sql += " and entrydatel <= " + stockInfoEndday + " order by entrydatel desc";

            sql = ServiceAFweb.getSQLLengh(sql, length);

            ArrayList<AFstockInfo> entries = getStockInfoListSQL(sql);
            return (ArrayList) entries;
        } catch (Exception e) {
            logger.info("> getStockInfo exception " + sym + " - " + e.getMessage());
        }
        return null;
    }

    public ArrayList<AFstockInfo> getAllStockInfoDBSQLArray(String sql) {
        return getStockInfoListSQL(sql);
    }

//    public String getAllStockInfoDBSQL(String sql) {
//
//        try {
//            ArrayList<AFstockInfo> entries = getStockInfoListSQL(sql);
//            String nameST = new ObjectMapper().writeValueAsString(entries);
//            return nameST;
//        } catch (JsonProcessingException ex) {
//        }
//        return null;
//
//    }

    private ArrayList<AFstockInfo> getStockInfoListSQL(String sql) {
        if (ServiceAFweb.checkCallRemoteMysql() == true) {
            try {
                ArrayList AFstockObjArry = remoteDB.getStockInfoSqlRemoteDB_RemoteMysql(sql);
                return AFstockObjArry;
            } catch (Exception ex) {
            }
            return null;
        }

        List<AFstockInfo> entries = new ArrayList<>();
        entries.clear();
        entries = this.jdbcTemplate.query(sql, new RowMapper() {
            public AFstockInfo mapRow(ResultSet rs, int rowNum) throws SQLException {

                AFstockInfo stocktmp = new AFstockInfo();
                stocktmp.setEntrydatel(rs.getLong("entrydatel"));
                stocktmp.setEntrydatedisplay(new java.sql.Date(stocktmp.getEntrydatel()));
                stocktmp.setFopen(rs.getFloat("fopen"));
                stocktmp.setFclose(rs.getFloat("fclose"));
                stocktmp.setHigh(rs.getFloat("high"));
                stocktmp.setLow(rs.getFloat("low"));
                stocktmp.setVolume(rs.getFloat("volume"));
                stocktmp.setAdjustclose(rs.getFloat("adjustclose"));
                stocktmp.setSym(rs.getString("sym"));
                stocktmp.setStockid(rs.getInt("stockid"));
                stocktmp.setId(rs.getInt("id"));

                return stocktmp;
            }
        });
        return (ArrayList) entries;
    }

    public static String insertStockInfo(AFstockInfo newS) {
        newS.setEntrydatedisplay(new java.sql.Date(newS.getEntrydatel()));
        String sqlCMD
                = "insert into stockinfo (entrydatedisplay, entrydatel, fopen, fclose, high, low ,volume, adjustclose, sym, stockid, id) VALUES "
                + "('" + newS.getEntrydatedisplay() + "'," + newS.getEntrydatel() + ","
                + newS.getFopen() + "," + newS.getFclose() + "," + newS.getHigh() + "," + newS.getLow() + "," + newS.getVolume() + "," + newS.getAdjustclose()
                + ",'" + newS.getSym() + "'," + newS.getStockid() + "," + newS.getId() + ")";
        return sqlCMD;
    }

/////////////////////////////////////////////////
    public boolean restStockInfoDB() {
        boolean status = true;
        try {
            processExecuteDB("drop table if exists dummyinfo1");
        } catch (Exception e) {
            logger.info("> RestStockInfoDB Table exception " + e.getMessage());
            status = false;
        }
        return status;
    }

    public boolean cleanStockInfoDB() {
        try {
            processExecuteDB("drop table if exists dummyinfo1");
            int result = initStockInfoDB();
            if (result == -1) {
                return false;
            }
            processExecuteDB("drop table if exists dummyinfo1");
            return true;
        } catch (Exception e) {
            logger.info("> RestStockDB Table exception " + e.getMessage());
        }
        return false;
    }

    public static String createDummyInfotable() {
        String sqlCMD = "";
        if ((CKey.SQL_DATABASE == CKey.MSSQL) || (CKey.SQL_DATABASE == CKey.REMOTE_MS_SQL)) {
            sqlCMD = "create table dummyinfo1 (id int identity not null, primary key (id))";
        }
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
            if ((CKey.SQL_DATABASE == CKey.MSSQL) || (CKey.SQL_DATABASE == CKey.REMOTE_MS_SQL)) {
                createTableList.add("create table dummyinfo1 (id int identity not null, primary key (id))");
                createTableList.add("create table stockinfo (id int identity not null, entrydatedisplay date not null, entrydatel bigint not null, fopen float(10) not null, fclose float(10) not null, high float(10) not null, low float(10) not null, volume float(10) not null, adjustclose float(10) not null, sym varchar(255) not null, stockid int not null, primary key (id))");
            }

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
    public ArrayList getAllIdSQL(String sql) {
        if (ServiceAFweb.checkCallRemoteMysql() == true) {
            ArrayList nnList;
            try {
                nnList = remoteDB.getAllIdSqlRemoteDB_RemoteMysql(sql);
                return nnList;
            } catch (Exception ex) {
            }
            return null;
        }

        try {
            List<String> entries = new ArrayList<>();
            entries.clear();
            entries = this.jdbcTemplate.query(sql, new RowMapper() {
                public String mapRow(ResultSet rs, int rowNum) throws SQLException {
                    String name = rs.getString("id");
                    return name;
                }
            });
            return (ArrayList) entries;
        } catch (Exception e) {
            logger.info("> getAllIdSQL exception " + e.getMessage());
        }
        return null;
    }

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
