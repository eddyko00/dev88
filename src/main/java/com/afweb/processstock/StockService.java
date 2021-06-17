/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.processstock;

import com.afweb.model.*;
import com.afweb.model.account.*;
import com.afweb.model.stock.*;
import com.afweb.processnn.TradingNNprocess;

import com.afweb.service.ServiceAFweb;

import com.afweb.util.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Logger;

/**
 *
 * @author koed
 */
public class StockService {

    protected static Logger logger = Logger.getLogger("StockService");

    public ArrayList getStockArray(ServiceAFweb serviceAFWeb, int length) {
        if (serviceAFWeb.getServerObj().isSysMaintenance() == true) {
            return null;
        }

        ArrayList stockList = serviceAFWeb.getStockImp().getStockArray(length);
        return stockList;
    }

    public AFstockObj getStockRealTime(ServiceAFweb serviceAFWeb, String symbol) {
        if (serviceAFWeb.getServerObj().isSysMaintenance() == true) {
            return null;
        }

        SymbolNameObj symObj = new SymbolNameObj(symbol);
        String NormalizeSymbol = symObj.getYahooSymbol();

        AFstockObj stock = serviceAFWeb.getStockImp().getRealTimeStock(NormalizeSymbol, null);

        if (serviceAFWeb.mydebugSim == true) {
            Calendar cDate = null;
            cDate = Calendar.getInstance();
            cDate.setTimeInMillis(ServiceAFweb.SimDateL);
            ArrayList<AFstockInfo> stockInfolist = serviceAFWeb.getStockHistorical(NormalizeSymbol, 80);
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

                    return stock;
                }
            }
        }
        return stock;
    }

    public int addStock(ServiceAFweb serviceAFWeb, String symbol) {
        StockProcess stockProcess = new StockProcess();
        if (serviceAFWeb.getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        SymbolNameObj symObj = new SymbolNameObj(symbol);
        String NormalizeSymbol = symObj.getYahooSymbol();
        int result = serviceAFWeb.getStockImp().addStock(NormalizeSymbol);
        if (result == ConstantKey.NEW) {
            stockProcess.ResetStockUpdateNameArray(serviceAFWeb);
        }
        return result;
    }

    public int disableStock(ServiceAFweb serviceAFWeb, String symbol) {
        if (serviceAFWeb.getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        SymbolNameObj symObj = new SymbolNameObj(symbol);
        String NormalizeSymbol = symObj.getYahooSymbol();
        return serviceAFWeb.getStockImp().disableStock(NormalizeSymbol);
    }

    public int cleanAllStockInfo(ServiceAFweb serviceAFWeb) {
        if (serviceAFWeb.getServerObj().isSysMaintenance() == true) {
            return 0;
        }
        logger.info("> cleanAllStockInfo");
        AccountObj accountObj = serviceAFWeb.getAdminObjFromCache();
        ArrayList<String> stockNameArray = serviceAFWeb.SystemAccountStockNameList(accountObj.getId());
        for (int i = 0; i < stockNameArray.size(); i++) {
            String symbol = stockNameArray.get(i);
            if (symbol.equals("T.T")) {
                continue;
            }
            AFstockObj stockObj = getStockRealTime(serviceAFWeb, symbol);
            if (stockObj == null) {
                continue;
            }
            if (CKey.CACHE_STOCKH == true) {

                long endStaticDay = 0;
                ArrayList<AFstockInfo> stockInfoArrayStatic = TradingNNprocess.getAllStockHistory(symbol);
                if (stockInfoArrayStatic == null) {
                    stockInfoArrayStatic = new ArrayList();
                }
                if (stockInfoArrayStatic.size() > 0) {
//                logger.info("> getStockHistorical" + NormalizeSymbol + " " + stockInfoArrayStatic.size());
                    AFstockInfo stockInfo = stockInfoArrayStatic.get(0);
                    endStaticDay = TimeConvertion.endOfDayInMillis(stockInfo.getEntrydatel());
                    endStaticDay = TimeConvertion.addDays(endStaticDay, -3);
                    serviceAFWeb.getStockImp().deleteStockInfoByDate(stockObj, endStaticDay);
                }

            }
        }
        return 1;
    }

    public int removeStockInfo(ServiceAFweb serviceAFWeb, String symbol) {
        if (serviceAFWeb.getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        SymbolNameObj symObj = new SymbolNameObj(symbol);
        String NormalizeSymbol = symObj.getYahooSymbol();
        AFstockObj stockObj = getStockRealTime(serviceAFWeb,NormalizeSymbol);
        if (stockObj != null) {
            return serviceAFWeb.getStockImp().deleteStockInfoByStockId(stockObj);
        }
        return 0;
    }
    
    
}
