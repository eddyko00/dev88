/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.processsystem;

import com.afweb.model.*;
import com.afweb.model.stock.*;

import com.afweb.service.ServiceAFweb;
import com.afweb.dbsys.SysImp;
import com.afweb.stockinternet.StockInternetImpDao;

import com.afweb.util.*;

import java.util.ArrayList;

import java.util.Calendar;
import java.util.logging.Logger;
import javax.sql.DataSource;

/**
 *
 * @author eddyko
 */
public class SystemService {

    protected static Logger logger = Logger.getLogger("SystemService");
    private SysImp sysImp = new SysImp();

    public RequestObj SQLRequestSystem(ServiceAFweb serviceAFWeb, RequestObj sqlObj) {

        String st = "";
        String nameST = "";
        int ret;

        ArrayList<String> nameList = null;

        try {
            String typeCd = sqlObj.getCmd();
            int type = Integer.parseInt(typeCd);

            switch (type) {
//                case ServiceAFweb.AllId:
//                    nameList = getAllIdSQL(sqlObj.getReq());
//                    nameST = new ObjectMapper().writeValueAsString(nameList);
//                    sqlObj.setResp(nameST);
//                    return sqlObj;
//                case ServiceAFweb.AllName:
//                    nameList = getAllNameSQL(sqlObj.getReq());
//                    nameST = new ObjectMapper().writeValueAsString(nameList);
//                    sqlObj.setResp(nameST);
//                    return sqlObj;
//                case ServiceAFweb.AllLock:
//                    nameST = getAllLockDBSQL(sqlObj.getReq());
//                    sqlObj.setResp(nameST);
//                    return sqlObj;

//                case ServiceAFweb.AllSQLquery: //AllSQLreq = 14; //"14";  
//                    nameST = stockImp.getAllSQLquery(sqlObj.getReq());
//                    sqlObj.setResp(nameST);
//                    return sqlObj;
////////////////////////////////////////////////////////                    
//                case ServiceAFweb.RemoteGetMySQL:  //RemoteGetMySQL = 9; //"9"; 
//                    st = sqlObj.getReq();
//                    nameST = stockImp.getRemoteMYSQL(st);
//                    sqlObj.setResp("" + nameST);
//
//                    return sqlObj;
//                case ServiceAFweb.RemoteUpdateMySQL:  //RemoteUpdateMySQL = 10; //"10"; 
//                    st = sqlObj.getReq();
//                    ret = stockImp.updateRemoteMYSQL(st);
//                    sqlObj.setResp("" + ret);
//
//                    return sqlObj;
//                case ServiceAFweb.RemoteUpdateMySQLList:  //RemoteUpdateMySQLList = 11; //"11"; 
//                    st = sqlObj.getReq();
//                    String[] sqlList = st.split("~");
//                    for (int i = 0; i < sqlList.length; i++) {
//                        String sqlCmd = sqlList[i];
//                        ret = stockImp.updateRemoteMYSQL(sqlCmd);
//                    }
//                    sqlObj.setResp("" + sqlList.length);
//
//                    return sqlObj;
////////////////////////////                    
                default:
                    return null;

            }
        } catch (Exception ex) {
            logger.info("> StockSQLRequest exception " + sqlObj.getCmd() + " - " + ex.getMessage());
        }
        return null;
    }

    public void setDataSource(DataSource dataSource, String URL) {
        sysImp.setDataSource(dataSource, URL);
    }

    // 0 - new db, 1 - db already exist, -1 db error
    public int initStockDB() {
        try {

            int result = sysImp.initStockDB();

            if (result >= 0) {

                //dummy stock
                sysImp.addStock("T_T");

                if (result == 0) {
                    //clear lock                    
                    sysImp.deleteAllLock();
                    // add stocks
                    for (int i = 0; i < ServiceAFweb.primaryStock.length; i++) {
                        String stockN = ServiceAFweb.primaryStock[i];
                        sysImp.addStock(stockN);
                    }
                    sysImp.addStock("T.TO");
                    return 0; // new db
                }
                return 1; // DB already exist
            }
        } catch (Exception ex) {

        }
        return -1;  // DB error
    }

    public boolean cleanStockDB() {
        return sysImp.cleanStockDB();
    }

    public boolean restStockDB() {
        return sysImp.restStockDB();
    }

//    public boolean cleanNNonlyStockDB() {
//        return stockImp.cleanNNonlyStockDB();
//    }
    public int updateRemoteMYSQL(String sql) {
        return sysImp.updateRemoteMYSQL(sql);
    }

    public String getRemoteMYSQL(String sql) {
        return sysImp.getRemoteMYSQL(sql);
    }

    public ArrayList getAllNameSQL(String sql) {
        return sysImp.getAllNameSQL(sql);
    }

    public ArrayList<String> getAllIdSQL(String sql) {
        return sysImp.getAllIdSQL(sql);
    }

////////////////////////////////////////// 
    // System Lock
    public String getAllLockDBSQL(String sql) {
        return sysImp.getAllLockDBSQL(sql);
    }

    public int deleteAllLock() {
        return sysImp.deleteAllLock();
    }

    public ArrayList getAllLock() {
        return sysImp.getAllLock();
    }

    public int setRenewLock(String name, int type) {
        Calendar dateNow = TimeConvertion.getCurrentCalendar();
        long lockDateValue = dateNow.getTimeInMillis();

        if (type == ConstantKey.STOCK_LOCKTYPE) {
            SymbolNameObj symObj = new SymbolNameObj(name);
            name = symObj.getYahooSymbol();
        }
        return sysImp.setRenewLock(name, type, lockDateValue);
    }

    public AFLockObject getLockName(String name, int type) {

        if (type == ConstantKey.STOCK_LOCKTYPE) {
            SymbolNameObj symObj = new SymbolNameObj(name);
            name = symObj.getYahooSymbol();
        }
        name = name.toUpperCase();
        return sysImp.getLockName(name, type);
    }

    public int setLockName(String name, int type, long lockDateValue, String comment) {

        if (type == ConstantKey.STOCK_LOCKTYPE) {
            SymbolNameObj symObj = new SymbolNameObj(name);
            name = symObj.getYahooSymbol();
        }
        name = name.toUpperCase();
        return sysImp.setLockName(name, type, lockDateValue, comment);
    }

    public int removeLockName(String name, int type) {

        if (type == ConstantKey.STOCK_LOCKTYPE) {
            SymbolNameObj symObj = new SymbolNameObj(name);
            name = symObj.getYahooSymbol();
        }
        name = name.toUpperCase();
        return sysImp.removeLock(name, type);
    }
//////////////////////////////////////////    

    public StringBuffer getInternetScreenPage(String url) {
        StockInternetImpDao internet = new StockInternetImpDao();
        return internet.getInternetYahooScreenPage(url);
    }

    public AFstockObj getRealTimeStockInternet(String NormalizeSymbol) {
        StockInternetImpDao internet = new StockInternetImpDao();
        return internet.GetRealTimeStockInternet(NormalizeSymbol);
    }
//////////////////////////////////////////////    
}
