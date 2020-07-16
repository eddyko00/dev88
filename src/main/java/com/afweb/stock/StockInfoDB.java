/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.stock;

import com.afweb.model.ConstantKey;
import com.afweb.model.stock.*;

import com.afweb.service.ServiceAFweb;
import com.afweb.service.*;

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
import java.util.List;

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
public class StockInfoDB {

    protected static Logger logger = Logger.getLogger("StockInfoDB");

    static public int MaxMinuteAdminSignalTrading = 90;
    static public int Max2HAdmin = 120;
    private static JdbcTemplate jdbcTemplate;
    private static DataSource dataSource;
    private ServiceRemoteInfoDB remoteDB = new ServiceRemoteInfoDB();

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

    public static String SQLupdateStockSignal(AFstockObj stock) {
        String sqlCMD = "update stock set longterm=" + stock.getLongterm() + ", shortterm=" + stock.getShortterm() + ", direction=" + stock.getDirection() + " where id=" + stock.getId();
        return sqlCMD;
    }


    public int deleteStockInfoByStockId(AFstockObj stockObj) {
        try {
            String deleteSQL = "delete from stockinfo where stockid=" + stockObj.getId();
            return processUpdateDB(deleteSQL);
        } catch (Exception e) {
            logger.info("> deleteStockInfoByStockId exception " + e.getMessage());
        }
        return 0;
    }


    public static boolean checkCallRemoteSQL_Mysql() {
        boolean ret = false;
        if (CKey.SQL_DATABASE == CKey.REMOTE_MYSQL) {
            ret = true;
        }
        return ret;
    }

    public ArrayList<AFstockInfo> getStockInfo(AFstockObj stock, long start, long end) {
        try {
            if (stock == null) {
                return null;
            }
            if (stock.getSubstatus() == ConstantKey.INITIAL) {
                return null;
            }

            String sql = "select * from stockinfo where stockid = " + stock.getId();
            sql += " and entrydatel >= " + end + " and entrydatel <= " + start + " order by entrydatel desc";

            ArrayList<AFstockInfo> entries = getStockInfoListSQL(sql);
            return (ArrayList) entries;
        } catch (Exception e) {
            logger.info("> getStockInfo exception " + stock.getSymbol() + " - " + e.getMessage());
        }
        return null;
    }

    public ArrayList<AFstockInfo> getStockInfo(AFstockObj stock, int length, Calendar dateNow) {

        if (dateNow == null) {
            dateNow = TimeConvertion.getCurrentCalendar();
        }
        try {
            if (stock == null) {
                return null;
            }
            if (stock.getSubstatus() == ConstantKey.INITIAL) {
                return null;
            }

            long stockInfoEndday = TimeConvertion.endOfDayInMillis(dateNow.getTimeInMillis());
            String sql = "select * from stockinfo where stockid = " + stock.getId();
            sql += " and entrydatel <= " + stockInfoEndday + " order by entrydatel desc";

            sql = ServiceAFweb.getSQLLengh(sql, length);

            ArrayList<AFstockInfo> entries = getStockInfoListSQL(sql);
            return (ArrayList) entries;
        } catch (Exception e) {
            logger.info("> getStockInfo exception " + stock.getSymbol() + " - " + e.getMessage());
        }
        return null;
    }

    public String getAllStockInfoDBSQL(String sql) {
        try {
            ArrayList<AFstockInfo> entries = getStockInfoListSQL(sql);
            String nameST = new ObjectMapper().writeValueAsString(entries);
            return nameST;
        } catch (JsonProcessingException ex) {
        }
        return null;

    }

    private ArrayList getStockInfoListSQL(String sql) {
        if (checkCallRemoteSQL_Mysql() == true) {
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
                stocktmp.setStockid(rs.getInt("stockid"));
                stocktmp.setId(rs.getInt("id"));

                return stocktmp;
            }
        });
        return (ArrayList) entries;
    }

    public int deleteStockInfo(AFstockInfo stockInfo) {
        try {
            String deleteSQL = "delete from stockinfo where id=" + stockInfo.getId();
            return processUpdateDB(deleteSQL);
        } catch (Exception e) {
            logger.info("> deleteStockInfo exception " + e.getMessage());
        }
        return 0;
    }

//    public static String insertStockInfo(AFstockInfo newS) {
//        newS.setEntrydatedisplay(new java.sql.Date(newS.getEntrydatel()));
//        String sqlCMD
//                = "insert into stockinfo (entrydatedisplay, entrydatel, fopen, fclose, high, low ,volume, adjustclose, stockid, id) VALUES "
//                + "('" + newS.getEntrydatedisplay() + "'," + newS.getEntrydatel() + ","
//                + newS.getFopen() + "," + newS.getFclose() + "," + newS.getHigh() + "," + newS.getLow() + "," + newS.getVolume() + "," + newS.getAdjustclose()
//                + "," + newS.getStockid() + "," + newS.getId() + ")";
//        return sqlCMD;
//    }

//    public int updateStockInfoTransaction(AFstockObj stock, ArrayList<AFstockInfo> StockArray) {
////        logger.info("> addStockInfoTransaction " + stock.getSymbol() + " - " + StockArray.size());
//        try {
//            if (stock == null) {
//                return 0;
//            }
//            if (StockArray == null) {
//                return 0;
//            }
//            if (StockArray.size() == 0) {
//                return 1;
//            }
//            Calendar dateNow = TimeConvertion.getCurrentCalendar();
//            long stockinfoDBEndDay = 0;
//            ArrayList stockinfoDBArray = getStockInfo(stock, 1, dateNow);
//            if (stockinfoDBArray != null && stockinfoDBArray.size() == 1) {
//                AFstockInfo stockinfoDB = (AFstockInfo) stockinfoDBArray.get(0);
//                stockinfoDBEndDay = stockinfoDB.getEntrydatel();
//            }
//
//            // jdbc transaction
//            ArrayList sqlTranList = new ArrayList();
//
//            if (stock.getSubstatus() == ConstantKey.INITIAL) {
//                String sqlDelete = "DELETE From stockinfo where stockid=" + stock.getId();
//                sqlTranList.add(sqlDelete);
//                stock.setSubstatus(ConstantKey.OPEN);
//
//            }
//            int resultAdd = 0;
//            for (int i = 0; i < StockArray.size(); i++) {
//                AFstockInfo stockinfoTemp = StockArray.get(i);
//                long stockinfoRTEndDay = stockinfoTemp.getEntrydatel();
//
//                stockinfoDBEndDay = TimeConvertion.endOfDayInMillis(stockinfoDBEndDay);
//                stockinfoRTEndDay = TimeConvertion.endOfDayInMillis(stockinfoRTEndDay);
//
//                if (stockinfoRTEndDay < stockinfoDBEndDay) {
//                    continue;
//                } else if (stockinfoRTEndDay == stockinfoDBEndDay) {
//                    resultAdd++;
//                    String updateSQL
//                            = "update stockinfo set entrydatedisplay='" + stockinfoTemp.getEntrydatedisplay() + "', entrydatel=" + stockinfoTemp.getEntrydatel() + ", "
//                            + "fopen=" + stockinfoTemp.getFopen() + ", fclose=" + stockinfoTemp.getFclose() + ", high=" + stockinfoTemp.getHigh() + ", "
//                            + "low=" + stockinfoTemp.getLow() + ", volume=" + stockinfoTemp.getVolume() + ", adjustclose=" + stockinfoTemp.getAdjustclose()
//                            + " where entrydatel=" + stockinfoTemp.getEntrydatel() + " and stockid='" + stock.getId() + "'";
//
//                    //update current stockinfo
//                    sqlTranList.add(updateSQL);
//                    continue;
//                }
//                resultAdd++;
//                // Add stockinfo
//                String insertSQL
//                        = "insert into stockinfo (entrydatedisplay, entrydatel, fopen, fclose, high, low ,volume, adjustclose, stockid) VALUES "
//                        + "('" + new java.sql.Date(stockinfoTemp.getEntrydatel()) + "'," + stockinfoTemp.getEntrydatel() + ","
//                        + stockinfoTemp.getFopen() + "," + stockinfoTemp.getFclose() + "," + stockinfoTemp.getHigh() + "," + stockinfoTemp.getLow() + "," + stockinfoTemp.getVolume() + "," + stockinfoTemp.getAdjustclose() + "," + stock.getId() + ")";
//                sqlTranList.add(insertSQL);
//
//            }
//            //must sepalate stock and stockinfo to exec one by one for 2 db 
//            int sqlResult = updateSQLArrayList(sqlTranList);
//            sqlTranList.clear();
//
////            if (getEnv.checkLocalPC() == true) {
////                logger.info("> addStockInfoTransaction " + stock.getSymbol() + " add " + resultAdd);
////            }
//            //clear Fail update count
//            long dateNowLong = dateNow.getTimeInMillis();
//            stock.setUpdatedatedisplay(new java.sql.Date(dateNowLong));
//            stock.setUpdatedatel(dateNowLong);
//            stock.setFailedupdate(0);
//            // part of transaction
//            String sqlUpdateStockSQL
//                    = "update stock set substatus=" + stock.getSubstatus() + ", stockname='" + stock.getStockname()
//                    + "', updatedatedisplay='" + stock.getUpdatedatedisplay() + "',updatedatel=" + stock.getUpdatedatel() + ", failedupdate="
//                    + stock.getFailedupdate() + " where symbol='" + stock.getSymbol() + "'";
//            sqlTranList.add(sqlUpdateStockSQL);
//            // process all transaction
//            sqlResult = updateSQLArrayList(sqlTranList);
//            if (sqlResult == 1) {
//                return 1; //resultAdd; 
//            }
//        } catch (Exception e) {
//            logger.info("> addStockInfoTransaction exception " + stock.getSymbol() + " - " + e.getMessage());
//        }
//        return 0;
//    }

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

        if (checkCallRemoteSQL_Mysql() == true) {
            // just for testing
//            if (CKey.SQL_DATABASE == CKey.REMOTE_MYSQL) {
//                boolean result = ExecuteSQLArrayList(SQLTran);
//                if (result == true) {
//                    return 1;
//                }
//                return 0;
//            }

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
        if (checkCallRemoteSQL_Mysql() == true) {
            int count = remoteDB.getCountRowsRemoteDB_RemoteMysql(tableName);
            return count;
        }

        Integer result = jdbcTemplate.queryForObject("select count(0) from " + tableName, Integer.class);
        return (result != null ? result : 0);
    }

    public int processUpdateDB(String sqlCMD) throws Exception {
        if (checkCallRemoteSQL_Mysql() == true) {
            int ret = remoteDB.postExecuteRemoteDB_RemoteMysql(sqlCMD);
            return ret;
        }

//        logger.info("> processUpdateDB " + sqlCMD);
        getJdbcTemplate().update(sqlCMD);
        return 1;
    }

    public void processExecuteDB(String sqlCMD) throws Exception {
//        logger.info("> processExecuteDB " + sqlCMD);

        if (checkCallRemoteSQL_Mysql() == true) {
            int count = remoteDB.postExecuteRemoteDB_RemoteMysql(sqlCMD);
            return;
        }
        getJdbcTemplate().execute(sqlCMD);
    }


    public int initStockDB() {

        int total = 0;
        try {

            processExecuteDB("drop table if exists eddy");
            

            if ((CKey.SQL_DATABASE == CKey.MYSQL) || (CKey.SQL_DATABASE == CKey.REMOTE_MYSQL) || (CKey.SQL_DATABASE == CKey.LOCAL_MYSQL)) {
                processExecuteDB("create table eddy (id int(10) not null auto_increment, primary key (id))");
            }
            total = getCountRowsInTable(getJdbcTemplate(), "eddy");
            if (total == -1) {
                logger.info("> InitStockInfoDB failed");
                return -1;
            }

            ArrayList dropTableList = new ArrayList();

            dropTableList.add("drop table if exists stockinfo");
            boolean resultDropList = ExecuteSQLArrayList(dropTableList);
            
            
//            dropTableList = new ArrayList();
//            dropTableList.add("drop table if exists dummy1");
//            dropTableList.add("drop table if exists lockobject");
//            dropTableList.add("drop table if exists transationorder");
//            dropTableList.add("drop table if exists tradingrule");
//            dropTableList.add("drop table if exists stockinfo");
//            dropTableList.add("drop table if exists performance");
//            dropTableList.add("drop table if exists neuralnet");
//            dropTableList.add("drop table if exists neuralnet1");
//            dropTableList.add("drop table if exists neuralnetdata");
//            dropTableList.add("drop table if exists comm");
//            dropTableList.add("drop table if exists billing");
//            dropTableList.add("drop table if exists stock");
//            dropTableList.add("drop table if exists account");
//            dropTableList.add("drop table if exists customer");
//            resultDropList = ExecuteSQLArrayList(dropTableList);            

            ArrayList createTableList = new ArrayList();

            if ((CKey.SQL_DATABASE == CKey.MYSQL) || (CKey.SQL_DATABASE == CKey.REMOTE_MYSQL) || (CKey.SQL_DATABASE == CKey.LOCAL_MYSQL)) {
                createTableList.add("create table stockinfo (id int(10) not null auto_increment, entrydatedisplay date not null, entrydatel bigint(20) not null, fopen float not null, fclose float not null, high float not null, low float not null, volume float not null, adjustclose float not null, stockid int(10) not null, primary key (id))");
            }

            //must use this ExecuteSQLArrayList to exec one by one for 2 db 
            boolean resultCreate = ExecuteSQLArrayList(createTableList);

            return 0;  // new database

        } catch (Exception e) {
            logger.info("> InitStockInfoDB Table exception " + e.getMessage());
        }
        return -1;
    }

    public ArrayList getAllIdSQL(String sql) {
        
        if (checkCallRemoteSQL_Mysql() == true) {
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

}
