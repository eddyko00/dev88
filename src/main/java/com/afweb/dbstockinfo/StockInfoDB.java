/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.dbstockinfo;

import static com.afweb.dbsys.SysDB.Max2HAdmin;
import static com.afweb.dbsys.SysDB.MaxMinuteAdminSignalTrading;
import com.afweb.model.ConstantKey;
import com.afweb.model.stock.*;

import com.afweb.service.ServiceAFweb;

import com.afweb.service.*;

import com.afweb.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Date;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

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
    public static String remoteURL = "";
    private ServiceRemoteDBInfo remoteDB = new ServiceRemoteDBInfo();

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
    public void setDataSource(DataSource dataSource, String URL) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.remoteURL = URL;
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
        if (ServiceAFweb.SysCheckCallRemoteMysql() == true) {
            try {
                ArrayList AFstockObjArry = remoteDB.getStockInfoSqlRemoteDB_RemoteMysql(sql, remoteURL);
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
//        if ((CKey.SQL_DATABASE == CKey.MSSQL) || (CKey.SQL_DATABASE == CKey.REMOTE_MS_SQL)) {
//            sqlCMD = "create table dummyinfo1 (id int identity not null, primary key (id))";
//        }
        if (ServiceAFweb.SysIsMySQLDB()) {
            sqlCMD = "create table dummyinfo1 (id int(10) not null auto_increment, primary key (id))";
        }
        return sqlCMD;
    }

    public int initStockInfoDB() {

        int total = 0;
        logger.info(">>>>> InitStockInfoDB Table creation URL:" + remoteURL);
        try {

            boolean initDBflag = false;
            if (initDBflag == true) {
                processExecuteDB("drop table if exists dummyinfo1");
            }
            total = getCountRowsInTable(getJdbcTemplate(), "dummyinfo1");
        } catch (Exception e) {
            logger.info("> InitStockInfoDB Table exception ");
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
            dropTableList.add("drop table if exists lockinfoobject");            

            //must use this ExecuteSQLArrayList to exec one by one for 2 db 
            boolean resultDropList = ExecuteSQLArrayList(dropTableList);

            ArrayList createTableList = new ArrayList();
//            if ((CKey.SQL_DATABASE == CKey.MSSQL) || (CKey.SQL_DATABASE == CKey.REMOTE_MS_SQL)) {
//                createTableList.add("create table dummyinfo1 (id int identity not null, primary key (id))");
//                createTableList.add("create table stockinfo (id int identity not null, entrydatedisplay date not null, entrydatel bigint not null, fopen float(10) not null, fclose float(10) not null, high float(10) not null, low float(10) not null, volume float(10) not null, adjustclose float(10) not null, sym varchar(255) not null, stockid int not null, primary key (id))");
//            }

            if (ServiceAFweb.SysIsMySQLDB()) {
                createTableList.add("create table dummyinfo1 (id int(10) not null auto_increment, primary key (id))");
                createTableList.add("create table stockinfo (id int(10) not null auto_increment, entrydatedisplay date not null, entrydatel bigint(20) not null, fopen float not null, fclose float not null, high float not null, low float not null, volume float not null, adjustclose float not null, sym varchar(255) not null, stockid int(10) not null, primary key (id))");
                createTableList.add("create table lockinfoobject (id int(10) not null auto_increment, lockname varchar(255) not null unique, type int(10) not null, lockdatedisplay date, lockdatel bigint(20), comment varchar(255), primary key (id))");
                
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

        if (ServiceAFweb.SysCheckCallRemoteMysql() == true) {
            int ret = remoteDB.getExecuteRemoteListDB_Mysql(SQLTran, remoteURL);
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
    ////////////////////////
    public int deleteAllLock() {

        try {
            String deleteSQL = "delete from lockinfoobject";
            processExecuteDB(deleteSQL);
            return 1;
        } catch (Exception e) {
            logger.info("> DeleteAllLock exception " + e.getMessage());
        }
        return 0;
    }

    public String getAllLockDBSQL(String sql) {
        try {
            ArrayList<AFLockObject> entries = getAllLockObjSQL(sql);
            String nameST = new ObjectMapper().writeValueAsString(entries);
            return nameST;
        } catch (Exception ex) {
        }
        return null;
    }

    public ArrayList getAllLock() {
        String sql = "select * from lockinfoobject";
        ArrayList entries = getAllLockObjSQL(sql);
        return entries;
    }

    public AFLockObject getLockName(String name, int type) {
        String sql = "select * from lockinfoobject where lockname='" + name + "' and type=" + type;
        ArrayList entries = getAllLockObjSQL(sql);
        if (entries != null) {
            if (entries.size() == 1) {
                AFLockObject lock = (AFLockObject) entries.get(0);
                return lock;
            }
        }
        return null;
    }

    private ArrayList getAllLockObjSQL(String sql) {
        if (ServiceAFweb.SysCheckCallRemoteMysql() == true) {
            ArrayList lockList;
            try {
                lockList = remoteDB.getAllLockSqlRemoteDB_RemoteMysql(sql, remoteURL);
                return lockList;
            } catch (Exception ex) {

            }
            return null;
        }

        try {
            List<AFLockObject> entries = new ArrayList<>();
            entries.clear();
            entries = this.jdbcTemplate.query(sql, new RowMapper() {
                public AFLockObject mapRow(ResultSet rs, int rowNum) throws SQLException {
                    AFLockObject lock = new AFLockObject();
                    lock.setLockname(rs.getString("lockname"));
                    lock.setType(rs.getInt("type"));
                    lock.setLockdatedisplay(new java.sql.Date(rs.getDate("lockdatedisplay").getTime()));
                    lock.setLockdatel(Long.parseLong(rs.getString("lockdatel")));
                    lock.setId(rs.getInt("id"));
                    lock.setComment(rs.getString("comment"));

                    String tzid = "America/New_York"; //EDT
                    TimeZone tz = TimeZone.getTimeZone(tzid);
                    Date d = new Date(lock.getLockdatel());
                    DateFormat format = new SimpleDateFormat("M/dd/yyyy hh:mm a z");
                    format.setTimeZone(tz);
                    String ESTdate = format.format(d);
                    lock.setUpdateDateD(ESTdate);

                    return lock;
                }
            });
            return (ArrayList) entries;
        } catch (Exception e) {
            logger.info("> getAllLockObjSQL exception " + e.getMessage());
        }
        return null;
    }

    public int setRenewLock(String name, int type, long lockDateValue) {

        try {
            AFLockObject lock = getLockName(name, type);

            if (lock == null) {
                return 0;
            }
            String sqlCMD = "update lockinfoobject set lockdatedisplay='" + new java.sql.Date(lockDateValue) + "', lockdatel=" + lockDateValue + " where id=" + lock.getId();
            return processUpdateDB(sqlCMD);

        } catch (Exception ex) {
            logger.info("> setRenewLock exception " + ex.getMessage());
        }
        return 0;
    }

    private int setLockObject(String name, int type, long lockDateValue, String comment) {
        try {
            String sqlCMD = "insert into lockinfoobject (lockname, type, lockdatedisplay, lockdatel, comment) VALUES "
                    + "('" + name + "'," + type + ",'" + new java.sql.Date(lockDateValue) + "'," + lockDateValue + ",'" + comment + "')";
            return processUpdateDB(sqlCMD);

        } catch (Exception e) {
            logger.info("> setLockObject exception " + name + " - " + e.getMessage());
        }
        return 0;
    }

    public int setLockName(String name, int type, long lockDateValue, String comment) {

        try {
            AFLockObject lock = getLockName(name, type);
            if (lock == null) {
                return setLockObject(name, type, lockDateValue, comment);
            }

            int allowTime = 6; // default 6 minutes

            if (type == ConstantKey.STOCK_LOCKTYPE) {
                allowTime = 3; // 3 minutes for stock timeout
            } else if (type == ConstantKey.STOCK_UPDATE_LOCKTYPE) {
                allowTime = 3; // 3 minutes for stock timeout
            } else if (type == ConstantKey.SRV_LOCKTYPE) {
                allowTime = 10; // 10 minutes for stock timeout
            } else if (type == ConstantKey.NN_TR_LOCKTYPE) {
                allowTime = 30; // 30 minutes for NN trrain timeout                
            } else if (type == ConstantKey.ADMIN_SIGNAL_LOCKTYPE) {
                allowTime = MaxMinuteAdminSignalTrading; // 90 minutes for stock timeout                
            } else if (type == ConstantKey.NN_LOCKTYPE) {
                allowTime = MaxMinuteAdminSignalTrading; // 90 minutes for stock timeout                
            } else if (type == ConstantKey.H2_LOCKTYPE) {
                allowTime = Max2HAdmin; // 100 minutes for stock timeout

            }
            long lockDate = lock.getLockdatel();
            long lockDate10Min = TimeConvertion.addMinutes(lockDate, allowTime);

            if (lockDate10Min > lockDateValue) {
                return 0;
            }
            removeLock(name, type);

        } catch (Exception ex) {
            logger.info("> SetLockName exception " + ex.getMessage());
        }
        return 0;
    }

    public int removeLock(String name, int type) {

        try {
            String sqlDelete = "delete from lockinfoobject where lockname='" + name + "' and type=" + type;
            this.processExecuteDB(sqlDelete);
            return 1;
        } catch (Exception ex) {
            logger.info("> removeLock exception " + ex.getMessage());
        }
        return 0;
    }
    
    /////////////////////////
    ///////////
    public ArrayList getAllIdInfoSQL(String sql) {
        if (ServiceAFweb.SysCheckCallRemoteMysql() == true) {
            ArrayList nnList;
            try {
                nnList = remoteDB.getAllIdSqlRemoteDB_RemoteMysql(sql, remoteURL);
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
        if (ServiceAFweb.SysCheckCallRemoteMysql() == true) {
            int count = remoteDB.getCountRowsRemoteDB_RemoteMysql(tableName, remoteURL);
            return count;
        }

        Integer result = jdbcTemplate.queryForObject("select count(0) from " + tableName, Integer.class);
        return (result != null ? result : 0);
    }

    public int processUpdateDB(String sqlCMD) throws Exception {
        if (ServiceAFweb.SysCheckCallRemoteMysql() == true) {
            int ret = remoteDB.postExecuteRemoteDB_RemoteMysql(sqlCMD, remoteURL);
            return ret;
        }

//        logger.info("> processUpdateDB " + sqlCMD);
        getJdbcTemplate().update(sqlCMD);
        return 1;
    }

    public void processExecuteDB(String sqlCMD) throws Exception {
//        logger.info("> processExecuteDB " + sqlCMD);

        if (ServiceAFweb.SysCheckCallRemoteMysql() == true) {
            int count = remoteDB.postExecuteRemoteDB_RemoteMysql(sqlCMD, remoteURL);
            return;
        }

        getJdbcTemplate().execute(sqlCMD);
    }

}
