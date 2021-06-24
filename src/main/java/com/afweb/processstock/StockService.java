/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.processstock;

import com.afweb.processstockinfo.StockInfoProcess;
import com.afweb.model.*;
import com.afweb.model.stock.*;

import com.afweb.service.ServiceAFweb;
import com.afweb.stock.StockImp;
import com.afweb.util.TimeConvertion;

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
 * @author koed
 */
public class StockService {

    protected static Logger logger = Logger.getLogger("StockService");
    StockInfoProcess stockProcess = new StockInfoProcess();
    private StockImp stockImp = new StockImp();

    public String getAllStockDBSQL(String sql) {
        return stockImp.getAllStockDBSQL(sql);
    }

    public ArrayList getStockObjArray(ServiceAFweb serviceAFWeb, int length) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return null;
        }

        ArrayList stockList = serviceAFWeb.getStockImp().getStockObjArray(length);
        return stockList;
    }

    public AFstockObj getStockByName(ServiceAFweb serviceAFWeb, String symbol) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return null;
        }

        SymbolNameObj symObj = new SymbolNameObj(symbol);
        String NormalizeSymbol = symObj.getYahooSymbol();

        AFstockObj stock = stockImp.getStockByName(NormalizeSymbol, null);

        if (stock == null) {
            return null;
        }
        if (stock.getStatus() == ConstantKey.OPEN) {
            if (stock.getSubstatus() != ConstantKey.INITIAL) {
                ArrayList StockArray = serviceAFWeb.getStockInfo_workaroundServ(stock, 2, null);

                if (StockArray != null) {
                    if (StockArray.size() >= 2) {
                        AFstockInfo stocktmp = (AFstockInfo) StockArray.get(0);
                        stock.setAfstockInfo(stocktmp);
                        AFstockInfo prevStocktmp = (AFstockInfo) StockArray.get(1);
                        stock.setPrevClose(prevStocktmp.getFclose());
                    }
                }
            }
        }
        if (serviceAFWeb.mydebugSim == true) {
            Calendar cDate = null;
            cDate = Calendar.getInstance();
            cDate.setTimeInMillis(ServiceAFweb.SimDateL);
            ArrayList<AFstockInfo> stockInfolist = serviceAFWeb.getStockHistoricalServ(NormalizeSymbol, 80);
            if (stockInfolist != null) {
                if (stockInfolist.size() > 0) {
                    AFstockInfo stockinfo = stockInfolist.get(0);

                    stock.setAfstockInfo(stockinfo);
                    stock.setUpdatedatel(serviceAFWeb.SimDateL);
                    stock.setUpdatedatedisplay(new java.sql.Date(serviceAFWeb.SimDateL));
                    stock.setPrevClose(stockinfo.getFopen());

                    String tzid = "America/New_York"; //EDT
                    TimeZone tz = TimeZone.getTimeZone(tzid);
                    Date d = new Date(stock.getUpdatedatel());
                    DateFormat format = new SimpleDateFormat("M/dd/yyyy hh:mm a z");
                    format.setTimeZone(tz);
                    String ESTdate = format.format(d);
                    stock.setUpdateDateD(ESTdate);

                }
            }
        }
        return stock;
    }

    public AFstockObj getStockBySockID(ServiceAFweb serviceAFWeb, int stockID) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return null;
        }
        AFstockObj stock = stockImp.getStockByStockID(stockID, null);

        if (stock == null) {
            return null;
        }
        if (stock.getStatus() == ConstantKey.OPEN) {
            if (stock.getSubstatus() != ConstantKey.INITIAL) {
                ArrayList StockArray = serviceAFWeb.getStockInfo_workaroundServ(stock, 2, null);

                if (StockArray != null) {
                    if (StockArray.size() >= 2) {
                        AFstockInfo stocktmp = (AFstockInfo) StockArray.get(0);
                        stock.setAfstockInfo(stocktmp);
                        AFstockInfo prevStocktmp = (AFstockInfo) StockArray.get(1);
                        stock.setPrevClose(prevStocktmp.getFclose());
                    }
                }
            }
        }
        return stock;
    }

    public int addStock(ServiceAFweb serviceAFWeb, String symbol) {
        StockInfoProcess stockProcess = new StockInfoProcess();
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        SymbolNameObj symObj = new SymbolNameObj(symbol);
        String NormalizeSymbol = symObj.getYahooSymbol();

        AFstockObj stockRT = serviceAFWeb.getRealTimeStockInternetServ(NormalizeSymbol);
        if (stockRT == null) {
            return 0;
        }

        int result = stockImp.addStock(NormalizeSymbol);
        if (result == ConstantKey.NEW) {
//            stockProcess.ResetStockUpdateNameArray(serviceAFWeb);
        }
        return result;
    }

    public int deleteStock(ServiceAFweb serviceAFWeb, AFstockObj stock) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return 0;
        }
        return stockImp.deleteStock(stock);
    }

    public int disableStock(ServiceAFweb serviceAFWeb, String symbol) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        SymbolNameObj symObj = new SymbolNameObj(symbol);
        String NormalizeSymbol = symObj.getYahooSymbol();
        return stockImp.disableStock(NormalizeSymbol);
    }

    public boolean checkStock(ServiceAFweb serviceAFWeb, String NormalizeSymbol) {
        AFstockObj stock = getStockByName(serviceAFWeb, NormalizeSymbol);
        if (stock == null) {
            return false;
        }
        if (stock.getStatus() != ConstantKey.OPEN) {
            return false;
        }
        if (stock.getAfstockInfo() == null) {
            return false;
        }
        return true;
    }

    public int updateStockStatusDB(AFstockObj stock) {
        return stockImp.updateStockStatusDB(stock);
    }

    public ArrayList<String> getAllOpenStockNameArray(ServiceAFweb serviceAFWeb) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return null;
        }

        ArrayList stockNameList = stockImp.getOpenStockNameArray();
        return stockNameList;
    }

    public int updateSQLArrayList(ServiceAFweb serviceAFWeb, ArrayList SQLTran) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return 0;
        }
        return stockImp.updateSQLArrayList(SQLTran);
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

///////////////////////////////////////////
    // System
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
///////////////////////////////////////////        
}
