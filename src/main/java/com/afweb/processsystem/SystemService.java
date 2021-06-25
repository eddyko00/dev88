/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.processsystem;

import com.afweb.processstockinfo.StockInfoProcess;
import com.afweb.model.*;
import com.afweb.model.stock.*;

import com.afweb.service.ServiceAFweb;
import com.afweb.stock.StockImp;

import com.afweb.util.CKey;
import com.afweb.util.TimeConvertion;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 *
 * @author eddyko
 */
public class SystemService {

    protected static Logger logger = Logger.getLogger("SystemService");
    private StockImp stockImp = new StockImp();

    public RequestObj StockSQLRequest(ServiceAFweb serviceAFWeb, RequestObj sqlObj) {

        String st = "";
        String nameST = "";
        int ret;
        int accountId = 0;
        ArrayList<String> nameList = null;

        try {
            String typeCd = sqlObj.getCmd();
            int type = Integer.parseInt(typeCd);

            switch (type) {
                case ServiceAFweb.AllId:
                    nameList = getAllIdSQL(sqlObj.getReq());
                    nameST = new ObjectMapper().writeValueAsString(nameList);
                    sqlObj.setResp(nameST);
                    return sqlObj;
                case ServiceAFweb.AllName:
                    nameList = getAllNameSQL(sqlObj.getReq());
                    nameST = new ObjectMapper().writeValueAsString(nameList);
                    sqlObj.setResp(nameST);
                    return sqlObj;
                case ServiceAFweb.AllLock:
                    nameST = getAllLockDBSQL(sqlObj.getReq());
                    sqlObj.setResp(nameST);
                    return sqlObj;
////////////////////////////                    

            }
        } catch (Exception ex) {
            logger.info("> StockSQLRequest exception " + sqlObj.getCmd() + " - " + ex.getMessage());
        }
        return null;
    }

    
    public void setDataSource(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        stockImp.setDataSource(jdbcTemplate, dataSource);
    }

    // 0 - new db, 1 - db already exist, -1 db error
    public int initStockDB() {
        try {

            int result = stockImp.initStockDB();

            if (result >= 0) {

                //dummy stock
                stockImp.addStock("T_T");

                if (result == 0) {
                    //clear lock                    
                    stockImp.deleteAllLock();
                    // add stocks
                    for (int i = 0; i < ServiceAFweb.primaryStock.length; i++) {
                        String stockN = ServiceAFweb.primaryStock[i];
                        stockImp.addStock(stockN);
                    }
                    stockImp.addStock("T.TO");
                    return 0; // new db
                }
                return 1; // DB already exist
            }
        } catch (Exception ex) {

        }
        return -1;  // DB error
    }

    
    public ArrayList getAllNameSQL(String sql) {
        return stockImp.getAllNameSQL(sql);
    }

    public ArrayList<String> getAllIdSQL(String sql) {
        return stockImp.getAllIdSQL(sql);
    }

////////////////////////////////////////// 
    // System Lock
    public String getAllLockDBSQL(String sql) {
        return stockImp.getAllLockDBSQL(sql);
    }

    public ArrayList getAllLock() {
        return stockImp.getAllLock();
    }

    public int setRenewLock(String name, int type) {
        Calendar dateNow = TimeConvertion.getCurrentCalendar();
        long lockDateValue = dateNow.getTimeInMillis();

        if (type == ConstantKey.STOCK_LOCKTYPE) {
            SymbolNameObj symObj = new SymbolNameObj(name);
            name = symObj.getYahooSymbol();
        }
        return stockImp.setRenewLock(name, type, lockDateValue);
    }

    public AFLockObject getLockName(String name, int type) {

        if (type == ConstantKey.STOCK_LOCKTYPE) {
            SymbolNameObj symObj = new SymbolNameObj(name);
            name = symObj.getYahooSymbol();
        }
        name = name.toUpperCase();
        return stockImp.getLockName(name, type);
    }

    public int setLockName(String name, int type, long lockDateValue, String comment) {

        if (type == ConstantKey.STOCK_LOCKTYPE) {
            SymbolNameObj symObj = new SymbolNameObj(name);
            name = symObj.getYahooSymbol();
        }
        name = name.toUpperCase();
        return stockImp.setLockName(name, type, lockDateValue, comment);
    }

    public int removeLock(String name, int type) {

        if (type == ConstantKey.STOCK_LOCKTYPE) {
            SymbolNameObj symObj = new SymbolNameObj(name);
            name = symObj.getYahooSymbol();
        }
        name = name.toUpperCase();
        return stockImp.removeLock(name, type);
    }
//////////////////////////////////////////    
}
