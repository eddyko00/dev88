/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.dbstock;

import com.afweb.model.ConstantKey;
import com.afweb.model.stock.*;

import com.afweb.service.ServiceAFweb;

import com.afweb.service.ServiceRemoteDB;

import com.afweb.util.CKey;
import com.afweb.util.TimeConvertion;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.Connection;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import java.util.TimeZone;
import javax.sql.DataSource;
import java.util.logging.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/**
 *
 * @author eddy
 *
 * mysql -u sa -p show databases; use db_sample; show tables; drop table
 * lockObject;
 *
 *
 *
 */
public class StockDB {

    protected static Logger logger = Logger.getLogger("StockDB");

    static public int MaxMinuteAdminSignalTrading = 90;
    static public int Max2HAdmin = 120;
    private static JdbcTemplate jdbcTemplate;
    private static DataSource dataSource;
    private static String remoteURL = "";
    private ServiceRemoteDB remoteDB = new ServiceRemoteDB();

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

    public static String SQLupdateStockSignal(AFstockObj stock) {
        String sqlCMD = "update stock set longterm=" + stock.getLongterm() + ", shortterm=" + stock.getShortterm() + ", data='" + stock.getData() + "', direction=" + stock.getDirection() + " where id=" + stock.getId();
        return sqlCMD;
    }

    public static String SQLupdateStockName(AFstockObj stock) {
        String sqlCMD = "update stock set stockname='" + stock.getStockname() + "' where id=" + stock.getId();
        return sqlCMD;
    }

    public static String SQLupdateStockStatus(AFstockObj stock) {
        String sqlCMD = "update stock set status=" + stock.getStatus() + ", substatus=" + stock.getSubstatus() + ", failedupdate=" + stock.getFailedupdate() + " where symbol='" + stock.getSymbol() + "'";
        return sqlCMD;
    }

    public int updateStockStatusDB(AFstockObj stock) {
        try {
            String sqlCMD = SQLupdateStockStatus(stock);
            return processUpdateDB(sqlCMD);
        } catch (Exception ex) {
            logger.info("> updateStockStatus exception " + ex.getMessage());
        }
        return 0;
    }

//    public ArrayList getOpenStockNameArray() {
//        ArrayList stockNameArray = new ArrayList();
//        stockNameArray = getAllOpenStockName();
////        ArrayList stockArray = getAllOpenStock();
////        if (stockArray != null && stockArray.size() > 0) {
////            stockNameArray.clear();
////            for (int i = 0; i < stockArray.size(); i++) {
////                AFstockObj stock = (AFstockObj) stockArray.get(i);
////                stockNameArray.add(stock.getSymbol());
////            }
////        }
//        return stockNameArray;
//    }
    public static String insertStock(AFstockObj newS) {
        newS.setUpdatedatedisplay(new java.sql.Date(newS.getUpdatedatel()));
        String sqlCMD = "insert into stock (stockname, symbol, status, substatus, updatedatedisplay, updatedatel, failedupdate, longterm, shortterm, direction, data, id) "
                + "values ('" + newS.getStockname() + "'," + "'" + newS.getSymbol() + "',"
                + newS.getStatus() + "," + newS.getSubstatus() + ","
                + "'" + newS.getUpdatedatedisplay() + "'," + newS.getUpdatedatel()
                + "," + newS.getFailedupdate() + "," + newS.getLongterm() + "," + newS.getShortterm()
                + "," + newS.getDirection() + ",'" + newS.getData() + "'," + newS.getId() + ")";
        return sqlCMD;
    }

    //http://www.byteslounge.com/tutorials/spring-jdbc-transactions-example
    public int addStock(String NormalizeSymbol) {

        try {
            Calendar dateNow = TimeConvertion.getDefaultCalendar(); //1970
            long dateNowLong = dateNow.getTimeInMillis();

            AFstockObj stock = getStock(NormalizeSymbol, dateNow);
            if (stock != null) {
                if (stock.getStatus() != ConstantKey.OPEN) {
                    stock.setStatus(ConstantKey.OPEN);
                    stock.setSubstatus(ConstantKey.INITIAL);
                    stock.setFailedupdate(0);
                    updateStockStatusDB(stock);
                    return ConstantKey.NEW;
                }

                return ConstantKey.EXISTED;
            }
            String sqlCMD = "insert into stock (stockname, symbol, status, substatus, updatedatedisplay, updatedatel, failedupdate, longterm, shortterm, direction, data) "
                    + "values ('" + NormalizeSymbol + "',"
                    + "'" + NormalizeSymbol + "',"
                    + ConstantKey.OPEN + "," + ConstantKey.INITIAL + ","
                    + "'" + new java.sql.Date(dateNowLong) + "',"
                    + dateNowLong + ",0,0,0,0,'')";

            int ret = processUpdateDB(sqlCMD);
            if (ret == 1) {
                return ConstantKey.NEW;
            }
        } catch (Exception e) {
            logger.info("> addStock exception " + e.getMessage());

        }
        return 0;
    }

    public int deleteStock(AFstockObj stock) {

        try {
            String deleteSQL = "delete from stock where id=" + stock.getId();
            return processUpdateDB(deleteSQL);
        } catch (Exception e) {
            logger.info("> deleteStock exception " + e.getMessage());
        }
        return 0;
    }

    public int disableStock(String NormalizeSymbol) {

        try {
            AFstockObj stock = getStock(NormalizeSymbol, null);
            if (stock == null) {
                return 0;
            }
            if (stock.getStatus() != ConstantKey.CLOSE) {
                stock.setStatus(ConstantKey.CLOSE);
                stock.setFailedupdate(0);
                updateStockStatusDB(stock);

                return ConstantKey.NEW;
            }
            return ConstantKey.EXISTED;
        } catch (Exception e) {
            logger.info("> removeStock exception " + e.getMessage());

        }
        return 0;
    }

    public ArrayList getAllOpenStock() {
        String sql = "select * from stock where status=" + ConstantKey.OPEN + " order by updatedatel asc";
        return getStockListSQL(sql);
    }

    public ArrayList getAllDisableStockName(int status) {
        String sql = "select symbol as symbol from stock where status=" + status + " order by updatedatel asc";
        return getAllSymbolSQL(sql);
    }

    public ArrayList getAllOpenStockName() {
        String sql = "select symbol as symbol from stock where status=" + ConstantKey.OPEN + " order by updatedatel asc";
        return getAllSymbolSQL(sql);
    }

    public String getAllStockDBSQL(String sql) {
        try {
            ArrayList<AFstockObj> entries = getStockListSQL(sql);
            String nameST = new ObjectMapper().writeValueAsString(entries);
            return nameST;
        } catch (JsonProcessingException ex) {
        }
        return null;

    }

    private ArrayList getStockListSQL(String sql) {
        if (ServiceAFweb.SysCheckCallRemoteMysql() == true) {
            try {
                ArrayList AFstockObjArry = remoteDB.getStockSqlRemoteDB_RemoteMysql(sql, remoteURL);
                return AFstockObjArry;
            } catch (Exception ex) {
            }
            return null;
        }

        List<AFstockObj> entries = new ArrayList<>();
        entries.clear();
        entries = this.jdbcTemplate.query(sql, new RowMapper() {
            public AFstockObj mapRow(ResultSet rs, int rowNum) throws SQLException {
                AFstockObj stock = new AFstockObj();
                stock.setId(rs.getInt("id"));
                stock.setStockname(rs.getString("stockname"));
                stock.setSymbol(rs.getString("symbol"));
                stock.setStatus(rs.getInt("status"));
                stock.setSubstatus(rs.getInt("substatus"));
                stock.setUpdatedatedisplay(new java.sql.Date(rs.getDate("updatedatedisplay").getTime()));
                stock.setUpdatedatel(rs.getLong("updatedatel"));
                stock.setFailedupdate(rs.getInt("failedupdate"));
                stock.setLongterm(rs.getFloat("longterm"));
                stock.setShortterm(rs.getFloat("shortterm"));
                stock.setDirection(rs.getFloat("direction"));
                stock.setData(rs.getString("data"));

                String tzid = "America/New_York"; //EDT
                TimeZone tz = TimeZone.getTimeZone(tzid);
                Date d = new Date(stock.getUpdatedatel());
                DateFormat format = new SimpleDateFormat("M/dd/yyyy hh:mm a z");
                format.setTimeZone(tz);
                String ESTdate = format.format(d);
                stock.setUpdateDateD(ESTdate);
                stock.setTRsignal(0);

                return stock;
            }
        });
        return (ArrayList) entries;
    }

    public AFstockObj getStockByStockID(int StockID, Calendar dateNow) {

        String sql = "select * from stock where id=" + StockID;
        return getStockSQL(sql, dateNow);
    }

    public AFstockObj getStock(String NormalizeSymbol, Calendar dateNow) {

        String sql = "select * from stock where symbol='" + NormalizeSymbol + "'";
        return getStockSQL(sql, dateNow);
    }

    private AFstockObj getStockSQL(String stockSQL, Calendar dateNow) {
        try {
            if (dateNow == null) {
                dateNow = dateNow = TimeConvertion.getCurrentCalendar();
            }
            AFstockObj stock = null;

            String sql = stockSQL;
            ArrayList<AFstockObj> entries = getStockListSQL(sql);
            if (entries == null) {
                return null;
            }
            if (entries.size() != 0) {
                stock = (AFstockObj) entries.get(0);
                return stock;
            }

        } catch (Exception e) {
            //com.afweb.model.stock.AFstockObj cannot be cast to java.util.List
            logger.info("> getStock exception " + stockSQL + " - " + e.getMessage());

        }
        return null;
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

    public int updateSQLArrayList(ArrayList SQLTran) {

        if (ServiceAFweb.SysCheckCallRemoteMysql() == true) {
            // just for testing
//            if (CKey.SQL_DATABASE == CKey.REMOTE_MYSQL) {
//                boolean result = ExecuteSQLArrayList(SQLTran);
//                if (result == true) {
//                    return 1;
//                }
//                return 0;
//            }

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

    ///////////
    public ArrayList getAllIdSQL(String sql) {
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

    public boolean restStockDB() {
        boolean status = true;
        try {
            processExecuteDB("drop table if exists dummy1");
        } catch (Exception e) {
            logger.info("> RestStockDB Table exception " + e.getMessage());
            status = false;
        }
        return status;
    }

//    public boolean cleanNNonlyStockDB() {
//        try {
//            processExecuteDB("drop table if exists neuralnet");
//            processExecuteDB("drop table if exists neuralnetdata");
//            processExecuteDB("drop table if exists neuralnet1");
//            ArrayList createTableList = new ArrayList();
//            if ((CKey.SQL_DATABASE == CKey.MSSQL) || (CKey.SQL_DATABASE == CKey.REMOTE_MS_SQL)) {
//                createTableList.add("create table neuralnet (id int identity not null, name varchar(255) not null unique, refname varchar(255) not null, status int not null, type int not null, weight text null, updatedatedisplay date null, updatedatel bigint not null, primary key (id))");
//                createTableList.add("create table neuralnet1 (id int identity not null, name varchar(255) not null unique, refname varchar(255) not null, status int not null, type int not null, weight text null, updatedatedisplay date null, updatedatel bigint not null, primary key (id))");
//                createTableList.add("create table neuralnetdata (id int identity not null, name varchar(255) not null, status int not null, type int not null, data text null, updatedatedisplay date null, updatedatel bigint not null, primary key (id))");
//
//            } else if ((CKey.SQL_DATABASE == CKey.DIRECT__MYSQL) || (CKey.SQL_DATABASE == CKey.REMOTE_PHP_MYSQL) || (CKey.SQL_DATABASE == CKey.LOCAL_MYSQL)) {
//                createTableList.add("create table neuralnet (id int(10) not null auto_increment, name varchar(255) not null unique, refname varchar(255) not null, status int(10) not null, type int(10) not null, weight text, updatedatedisplay date, updatedatel bigint(20) not null, primary key (id))");
//                createTableList.add("create table neuralnet1 (id int(10) not null auto_increment, name varchar(255) not null unique, refname varchar(255) not null, status int(10) not null, type int(10) not null, weight text, updatedatedisplay date, updatedatel bigint(20) not null, primary key (id))");
//                createTableList.add("create table neuralnetdata (id int(10) not null auto_increment, name varchar(255) not null, status int(10) not null, type int(10) not null, data text, updatedatedisplay date, updatedatel bigint(20) not null, primary key (id))");
//
//            }
//            //must use this ExecuteSQLArrayList to exec one by one for 2 db 
//            boolean resultCreate = ExecuteSQLArrayList(createTableList);
//
//            return true;
//        } catch (Exception e) {
//            logger.info("> cleanNNonlyStockDB Table exception " + e.getMessage());
//        }
//        return false;
//    }
    public boolean cleanStockDB() {
        try {
            processExecuteDB("drop table if exists dummy1");
            int result = initStockDB();
            if (result == -1) {
                return false;
            }
            processExecuteDB("drop table if exists dummy1");
            return true;
        } catch (Exception e) {
            logger.info("> RestStockDB Table exception " + e.getMessage());
        }
        return false;
    }

    public static String createDummytable() {
        String sqlCMD = "";
//        if ((CKey.SQL_DATABASE == CKey.MSSQL) || (CKey.SQL_DATABASE == CKey.REMOTE_MS_SQL)) {
//            sqlCMD = "create table dummy1 (id int identity not null, primary key (id))";
//        }
        if (ServiceAFweb.SysIsMySQLDB()) {
            sqlCMD = "create table dummy1 (id int(10) not null auto_increment, primary key (id))";
        }
        return sqlCMD;
    }

    public int testStockDB() {

        int total = 0;
        logger.info(">>>>> testStockDB");
        try {
            total = getCountRowsInTable(getJdbcTemplate(), "dummy1");
            if (total >= 0) {
                return 1;  // already exist
            }
        } catch (Exception e) {
            logger.info("> InitStockDB Table exception " + e.getMessage());
        }
        return -1;
    }

    public int initStockDB() {

        int total = 0;
        logger.info(">>>>> InitStockDB Table creation");
        try {

            boolean initDBflag = false;
            if (initDBflag == true) {
                processExecuteDB("drop table if exists dummy1");
            }
            total = getCountRowsInTable(getJdbcTemplate(), "dummy1");
        } catch (Exception e) {
            logger.info("> InitStockDB Table exception ");
            total = -1;
        }
        if (total >= 0) {
            return 1;  // already exist
        }

        try {

            processExecuteDB("drop table if exists eddy");
//            if ((CKey.SQL_DATABASE == CKey.MSSQL) || (CKey.SQL_DATABASE == CKey.REMOTE_MS_SQL)) {
//                processExecuteDB("create table eddy (id int identity not null, primary key (id))");
//            }
            if (ServiceAFweb.SysIsMySQLDB()) {
                processExecuteDB("create table eddy (id int(10) not null auto_increment, primary key (id))");
            }
            total = getCountRowsInTable(getJdbcTemplate(), "eddy");
            if (total == -1) {
                return -1;
            }
// sequency is important
            ArrayList dropTableList = new ArrayList();
            dropTableList.add("drop table if exists dummy1");
            dropTableList.add("drop table if exists lockobject");
            dropTableList.add("drop table if exists transationorder");
            dropTableList.add("drop table if exists tradingrule");
            dropTableList.add("drop table if exists performance");

//            dropTableList.add("drop table if exists stockinfo");
//            dropTableList.add("drop table if exists neuralnet");
//            dropTableList.add("drop table if exists neuralnet1");
//            dropTableList.add("drop table if exists neuralnetdata");
            dropTableList.add("drop table if exists comm");
            dropTableList.add("drop table if exists billing");
            dropTableList.add("drop table if exists stock");
            dropTableList.add("drop table if exists account");
            dropTableList.add("drop table if exists customer");
//            
            //must use this ExecuteSQLArrayList to exec one by one for 2 db 
            boolean resultDropList = ExecuteSQLArrayList(dropTableList);

            ArrayList createTableList = new ArrayList();
//            if ((CKey.SQL_DATABASE == CKey.MSSQL) || (CKey.SQL_DATABASE == CKey.REMOTE_MS_SQL)) {
//                createTableList.add("create table dummy1 (id int identity not null, primary key (id))");
//                createTableList.add("create table tradingrule (id int identity not null, trname varchar(255) not null, type int not null, trsignal int not null, updatedatedisplay date null, updatedatel bigint not null, status int not null, substatus int not null, investment float(10) not null, balance float(10) not null, longshare float(10) not null, longamount float(10) not null, shortshare float(10) not null, "
//                        + "shortamount float(10) not null,perf float(10) not null, linktradingruleid int not null, stockid int not null, accountid int not null, comment varchar(255) not null, primary key (id))");
//                createTableList.add("create table stock (id int identity not null, symbol varchar(255) not null unique, stockname varchar(255) not null, status int not null, substatus int not null, updatedatedisplay date null, updatedatel bigint not null, failedupdate int not null, longterm float(10) not null, shortterm float(10) not null, direction float(10) not null, data varchar(255) not null,primary key (id))");
////                
////                createTableList.add("create table stockinfo (id int identity not null, entrydatedisplay date not null, entrydatel bigint not null, fopen float(10) not null, fclose float(10) not null, high float(10) not null, low float(10) not null, volume float(10) not null, adjustclose float(10) not null, sym varchar(255) not null, stockid int not null, primary key (id))");
////                
//                createTableList.add("create table account (id int identity not null, accountname varchar(255) not null unique, type int not null, status int not null, substatus int not null, updatedatedisplay date null, updatedatel bigint not null, startdate date null, investment float(10) not null, balance float(10) not null, servicefee float(10) not null, portfolio varchar(255) not null, linkaccountid int not null, customerid int not null, primary key (id))");
//                createTableList.add("create table lockobject (id int identity not null, lockname varchar(255) not null unique, type int not null, lockdatedisplay date null, lockdatel bigint null, comment varchar(255) null, primary key (id))");
//                createTableList.add("create table customer (id int identity not null, username varchar(255) not null unique, password varchar(255) not null, type int not null, status int not null, substatus int not null, startdate date null, firstname varchar(255) null, lastname varchar(255) null, email varchar(255) null, payment float(10) not null, balance float(10) not null, "
//                        + "portfolio varchar(255) not null,data varchar(255) not null, updatedatedisplay date null, updatedatel bigint not null, primary key (id))");
//                createTableList.add("create table transationorder (id int identity not null, symbol varchar(255) null, status int not null, type int not null, entrydatedisplay date null, entrydatel bigint null, share float(10) not null, avgprice float(10) not null, trname varchar(255) null, trsignal int not null, accountid int not null, stockid int not null, tradingruleid int not null, comment varchar(255) not null, primary key (id))");
//
//                createTableList.add("create table performance (id int identity not null, name text null, type int not null, startdate date null, updatedatedisplay date null, updatedatel bigint not null, investment float(10) not null, balance float(10) not null, rating float(10) not null, netprofit float(10) not null, grossprofit float(10) not null, numtrade int not null"
//                        + ", accountid int not null, stockid int not null, tradingruleid int not null, primary key (id))");
//
////                createTableList.add("create table neuralnet (id int identity not null, name varchar(255) not null unique, refname varchar(255) not null, status int not null, type int not null, weight text null, updatedatedisplay date null, updatedatel bigint not null, primary key (id))");
////                createTableList.add("create table neuralnet1 (id int identity not null, name varchar(255) not null unique, refname varchar(255) not null, status int not null, type int not null, weight text null, updatedatedisplay date null, updatedatel bigint not null, primary key (id))");
////                createTableList.add("create table neuralnetdata (id int identity not null, name varchar(255) not null, status int not null, type int not null, data text null, updatedatedisplay date null, updatedatel bigint not null, primary key (id))");
//                createTableList.add("create table comm (id int identity not null, name varchar(255) not null unique, type int not null, status int not null, substatus int not null, updatedatedisplay date null, updatedatel bigint not null, data text null, accountid int not null, customerid int not null, primary key (id))");
//                createTableList.add("create table billing (id int identity not null, name varchar(255) not null unique, type int not null, status int not null, substatus int not null, updatedatedisplay date null, updatedatel bigint not null, payment float(10) not null, balance float(10) not null, data text null, accountid int not null, customerid int not null, primary key (id))");
//                createTableList.add("alter table tradingrule add constraint fktradingrul566192 foreign key (accountid) references account (id)");
//                createTableList.add("alter table transationorder add constraint fktransation900454 foreign key (tradingruleid) references tradingrule (id)");
//                createTableList.add("alter table account add constraint fkaccount643453 foreign key (customerid) references customer (id)");
//            }

            if (ServiceAFweb.SysIsMySQLDB()) {
                createTableList.add("create table dummy1 (id int(10) not null auto_increment, primary key (id))");
                createTableList.add("create table tradingrule (id int(10) not null auto_increment, trname varchar(255) not null, type int(10) not null, trsignal int(10) not null, updatedatedisplay date, updatedatel bigint(20) not null, status int(10) not null, substatus int(10) not null, investment float not null, balance float not null, longshare float not null, longamount float not null, shortshare float not null, "
                        + "shortamount float not null,perf float not null, linktradingruleid int(10) not null, stockid int(10) not null, accountid int(10) not null, comment varchar(255) not null, primary key (id))");
                createTableList.add("create table stock (id int(10) not null auto_increment, symbol varchar(255) not null unique, stockname varchar(255) not null, status int(10) not null, substatus int(10) not null, updatedatedisplay date, updatedatel bigint(20) not null, failedupdate int(10) not null, longterm float not null, shortterm float not null, direction float not null, data varchar(255) not null, primary key (id))");
//                
//                createTableList.add("create table stockinfo (id int(10) not null auto_increment, entrydatedisplay date not null, entrydatel bigint(20) not null, fopen float not null, fclose float not null, high float not null, low float not null, volume float not null, adjustclose float not null, sym varchar(255) not null, stockid int(10) not null, primary key (id))");
//
                createTableList.add("create table account (id int(10) not null auto_increment, accountname varchar(255) not null, type int(10) not null, status int(10) not null, substatus int(10) not null, updatedatedisplay date, updatedatel bigint(20) not null, startdate date, investment float not null, balance float not null, servicefee float not null, portfolio varchar(255) not null, linkaccountid int(10) not null, customerid int(10) not null, primary key (id))");
                createTableList.add("create table lockobject (id int(10) not null auto_increment, lockname varchar(255) not null unique, type int(10) not null, lockdatedisplay date, lockdatel bigint(20), comment varchar(255), primary key (id))");
                createTableList.add("create table customer (id int(10) not null auto_increment, username varchar(255) not null unique, password varchar(255) not null, type int(10) not null, status int(10) not null, substatus int(10) not null, startdate date, firstname varchar(255), lastname varchar(255), email varchar(255), payment float not null, balance float not null, "
                        + "portfolio varchar(255) not null,data varchar(255) not null, updatedatedisplay date, updatedatel bigint(20) not null, primary key (id))");
                createTableList.add("create table transationorder (id int(10) not null auto_increment, symbol varchar(255), status int(10) not null, type int(10) not null, entrydatedisplay date, entrydatel bigint(20), share float not null, avgprice float not null, trname varchar(255), trsignal int(10) not null, accountid int(10) not null, stockid int(10) not null, tradingruleid int(10) not null, comment varchar(255) not null, primary key (id))");
                createTableList.add("create table performance (id int(10) not null auto_increment, name text, type int(10) not null, startdate date, updatedatedisplay date, updatedatel bigint(20) not null, investment float not null, balance float not null, rating float not null, netprofit float not null, grossprofit float not null, numtrade int(10) not null"
                        + ", accountid int(10) not null, stockid int not null, tradingruleid int(10) not null, primary key (id))");

//                createTableList.add("create table neuralnet (id int(10) not null auto_increment, name varchar(255) not null unique, refname varchar(255) not null, status int(10) not null, type int(10) not null, weight text, updatedatedisplay date, updatedatel bigint(20) not null, primary key (id))");
//                createTableList.add("create table neuralnet1 (id int(10) not null auto_increment, name varchar(255) not null unique, refname varchar(255) not null, status int(10) not null, type int(10) not null, weight text, updatedatedisplay date, updatedatel bigint(20) not null, primary key (id))");
//                createTableList.add("create table neuralnetdata (id int(10) not null auto_increment, name varchar(255) not null, status int(10) not null, type int(10) not null, data text, updatedatedisplay date, updatedatel bigint(20) not null, primary key (id))");
                createTableList.add("create table comm (id int(10) not null auto_increment, name varchar(255) not null, type int(10) not null, status int(10) not null, substatus int(10) not null, updatedatedisplay date, updatedatel bigint(20) not null, data text, accountid int(10) not null, customerid int(10) not null, primary key (id))");
                createTableList.add("create table billing (id int(10) not null auto_increment, name varchar(255) not null, type int(10) not null, status int(10) not null, substatus int(10) not null, updatedatedisplay date, updatedatel bigint(20) not null, payment float not null, balance float not null, data text, accountid int(10) not null, customerid int(10) not null, primary key (id))");

//                createTableList.add("alter table stockinfo add constraint fkstockinfo189813 foreign key (stockid) references stock (id)");
                createTableList.add("alter table tradingrule add constraint fktradingrul566192 foreign key (accountid) references account (id)");
                createTableList.add("alter table transationorder add constraint fktransation900454 foreign key (tradingruleid) references tradingrule (id)");
                createTableList.add("alter table account add constraint fkaccount643453 foreign key (customerid) references customer (id)");
            }

            //must use this ExecuteSQLArrayList to exec one by one for 2 db 
            boolean resultCreate = ExecuteSQLArrayList(createTableList);

            logger.info("> InitStockDB Done - result " + resultCreate);
            total = getCountRowsInTable(getJdbcTemplate(), "stock");
            return 0;  // new database

        } catch (Exception e) {
            logger.info("> InitStockDB Table exception " + e.getMessage());
        }
        return -1;
    }

//    public int initNeuralNetBPObj() {
//
//        ///NeuralNetObj1 transition
//        ///NeuralNetObj0 release
//        if (CKey.NN1_WEIGHT_0.length() == 0) {
//            return 0;
//        }
//        String name = CKey.NN_version + "_" + ConstantKey.TR_NN1;
//        int ret = setCreateNeuralNetObj0(name, CKey.NN1_WEIGHT_0);
//
//        return ret;
//    }
    ////////////////////////
    public int deleteAllLock() {

        try {
            String deleteSQL = "delete from lockobject";
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
        } catch (JsonProcessingException ex) {
        }
        return null;
    }

    public ArrayList getAllLock() {
        String sql = "select * from lockobject";
        ArrayList entries = getAllLockObjSQL(sql);
        return entries;
    }

    public AFLockObject getLockName(String name, int type) {
        String sql = "select * from lockobject where lockname='" + name + "' and type=" + type;
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
            String sqlCMD = "update lockobject set lockdatedisplay='" + new java.sql.Date(lockDateValue) + "', lockdatel=" + lockDateValue + " where id=" + lock.getId();
            return processUpdateDB(sqlCMD);

        } catch (Exception ex) {
            logger.info("> setRenewLock exception " + ex.getMessage());
        }
        return 0;
    }

    private int setLockObject(String name, int type, long lockDateValue, String comment) {
        try {
            String sqlCMD = "insert into lockobject (lockname, type, lockdatedisplay, lockdatel, comment) VALUES "
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
            String sqlDelete = "delete from lockobject where lockname='" + name + "' and type=" + type;
            this.processExecuteDB(sqlDelete);
            return 1;
        } catch (Exception ex) {
            logger.info("> removeLock exception " + ex.getMessage());
        }
        return 0;
    }

//
    public ArrayList getAllSymbolSQL(String sql) {
        if (ServiceAFweb.SysCheckCallRemoteMysql() == true) {
            ArrayList nnList;
            try {
                nnList = remoteDB.getAllSymbolSqlRemoteDB_RemoteMysql(sql, remoteURL);
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
                    String name = rs.getString("symbol");
                    return name;
                }
            });
            return (ArrayList) entries;
        } catch (Exception e) {
            logger.info("> getAllNameSQL exception " + e.getMessage());
        }
        return null;
    }

    public String getAllSQLqueryDBSQL(String sql) {
        try {
            List retList = null;
            if (ServiceAFweb.SysCheckCallRemoteMysql() == true) {
                String retST = remoteDB.getAllSQLqueryRemoteDB_RemoteMysql(sql, remoteURL);
                return retST;
            }

            retList = this.jdbcTemplate.queryForList(sql);
            String retST = new ObjectMapper().writeValueAsString(retList);
            return retST;

        } catch (Exception e) {
            logger.info("> getAccountStockPerfromanceList " + e.getMessage());

        }
        return null;
    }

    public ArrayList getAllNameSQL(String sql) {
        if (ServiceAFweb.SysCheckCallRemoteMysql() == true) {
            ArrayList nnList;
            try {
                nnList = remoteDB.getAllNameSqlRemoteDB_RemoteMysql(sql, remoteURL);
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
                    String name = rs.getString("name");
                    return name;
                }
            });
            return (ArrayList) entries;
        } catch (Exception e) {
            logger.info("> getAllNameSQL exception " + e.getMessage());
        }
        return null;
    }

    public int updateRemoteMYSQL(String sqlCMD) throws Exception {
//        logger.info("> updateRemoteMYSQL " + sqlCMD);
        try {
            getJdbcTemplate().execute(sqlCMD);
            return 1;
        } catch (Exception e) {
            logger.info("> updateRemoteMYSQL exception " + e.getMessage());
        }
        return 0;
    }

    public String getRemoteMYSQL(String sql) throws SQLException {
        Statement stmt = null;
        Connection con = null;
        try {
            con = getDataSource().getConnection();
            stmt = con.createStatement();
            ResultSet resultSet = stmt.executeQuery(sql);
            // The ResultSetMetaData is where all metadata related information
            // for a result set is stored.
            ResultSetMetaData metadata = resultSet.getMetaData();
            int columnCount = metadata.getColumnCount();

            // To get the column names we do a loop for a number of column count
            // returned above. And please remember a JDBC operation is 1-indexed
            // so every index begin from 1 not 0 as in array.
            ArrayList<String> columns = new ArrayList<String>();
            for (int i = 1; i < columnCount + 1; i++) {
                String columnName = metadata.getColumnName(i);
                columns.add(columnName);
            }
            // Later we use the collected column names to get the value of the
            // column it self.
            StringBuilder retString = new StringBuilder();
            int firstList = 0;
            while (resultSet.next()) {
                if (firstList > 0) {
                    retString.append("~");
                }
                firstList++;
                int firstColumn = 0;
                for (String columnName : columns) {
                    if (firstColumn > 0) {
                        retString.append("~");
                    }
                    firstColumn++;
                    String value = resultSet.getString(columnName);
                    retString.append(value);
                }
            }
            String ret = retString.toString();
            return ret;
        } catch (SQLException e) {
            logger.info("> getRemoteMYSQL exception " + e.getMessage());
        } finally {
            if (con != null) {
                con.close();
            }
            if (stmt != null) {
                stmt.close();
            }
        }
        return "";
    }

//    public String getRemoteMYSQL(String sql) throws SQLException {
//        Statement stmt = null;
//        Connection con = null;
//        try {
//            con = getDataSource().getConnection();
//            stmt = con.createStatement();
//            ResultSet resultSet = stmt.executeQuery(sql);
//            // The ResultSetMetaData is where all metadata related information
//            // for a result set is stored.
//            ResultSetMetaData metadata = resultSet.getMetaData();
//            int columnCount = metadata.getColumnCount();
//
//            // To get the column names we do a loop for a number of column count
//            // returned above. And please remember a JDBC operation is 1-indexed
//            // so every index begin from 1 not 0 as in array.
//            ArrayList<String> columns = new ArrayList<String>();
//            for (int i = 1; i < columnCount + 1; i++) {
//                String columnName = metadata.getColumnName(i);
//                columns.add(columnName);
//            }
//
//            // Later we use the collected column names to get the value of the
//            // column it self.
//            StringBuilder retString = new StringBuilder();
//            retString.append("[");
//
//            int firstList = 0;
//            while (resultSet.next()) {
//                if (firstList > 0) {
//                    retString.append(",");
//                }
//                firstList++;
//                retString.append("{");
//                int firstColumn = 0;
//                for (String columnName : columns) {
//                    if (firstColumn > 0) {
//                        retString.append(",");
//                    }
//                    firstColumn++;
//                    String value = resultSet.getString(columnName);
//                    retString.append("\"" + columnName + "\"" + ":" + "\"" + value + "\"");
//                }
//                retString.append("}");
//            }
//            retString.append("]");
//            String ret = retString.toString();
//            return ret;
//        } catch (SQLException e) {
//            logger.info("> getRemoteMYSQL exception " + e.getMessage());
//
//        } finally {
//            if (con != null) {
//                con.close();
//            }
//            if (stmt != null) {
//                stmt.close();
//            }
//        }
//        return "";
//    }
}
