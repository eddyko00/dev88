/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.processstockinfo;
import com.afweb.model.*;
import com.afweb.model.account.*;
import com.afweb.model.stock.*;
import com.afweb.nn.*;
import com.afweb.processstock.StockProcess;

import com.afweb.service.ServiceAFweb;
import com.afweb.stockinternet.StockInternetImpDao;

import com.afweb.util.*;
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

/**
 *
 * @author eddy
 */
public class StockInfoService {
        protected static Logger logger = Logger.getLogger("StockService");
    StockProcess stockProcess = new StockProcess();
    
    
    public int removeStockInfo(ServiceAFweb serviceAFWeb, String symbol) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        SymbolNameObj symObj = new SymbolNameObj(symbol);
        String NormalizeSymbol = symObj.getYahooSymbol();
        AFstockObj stockObj = serviceAFWeb.getStockRealTimeServ(NormalizeSymbol);
        if (stockObj != null) {
            return serviceAFWeb.getStockInfoImp().deleteStockInfoByStockId(stockObj);
        }
        return 0;
    }

    public int cleanAllStockInfo(ServiceAFweb serviceAFWeb) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
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
            AFstockObj stockObj = serviceAFWeb.getStockRealTimeServ(symbol);
            if (stockObj == null) {
                continue;
            }
            if (CKey.CACHE_STOCKH == true) {

                long endStaticDay = 0;
                ArrayList<AFstockInfo> stockInfoArrayStatic = ServiceAFweb.getAllStaticStockHistoryServ(symbol);
                if (stockInfoArrayStatic == null) {
                    stockInfoArrayStatic = new ArrayList();
                }
                if (stockInfoArrayStatic.size() > 0) {
//                logger.info("> getStockHistorical" + NormalizeSymbol + " " + stockInfoArrayStatic.size());
                    AFstockInfo stockInfo = stockInfoArrayStatic.get(0);
                    endStaticDay = TimeConvertion.endOfDayInMillis(stockInfo.getEntrydatel());
                    endStaticDay = TimeConvertion.addDays(endStaticDay, -3);
                    serviceAFWeb.getStockInfoImp().deleteStockInfoByDate(stockObj, endStaticDay);
                }

            }
        }
        return 1;
    }
    

    /////recent day first and the old data last////////////
    // return stock history starting recent date to the old date
    public ArrayList<AFstockInfo> getStockHistorical(ServiceAFweb serviceAFWeb, String symbol, int length) {
        ServiceAFweb.lastfun = "getStockHistorical";

        if (length == 0) {
            return null;
        }
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return null;
        }
//        if (checkCallRemoveMysql() == true) {
//            return getServiceAFwebREST().getStockHistorical(symbol, length);
//        }
        SymbolNameObj symObj = new SymbolNameObj(symbol);
        String NormalizeSymbol = symObj.getYahooSymbol();

        List<AFstockInfo> mergedList = new ArrayList();

        Calendar dateNow = TimeConvertion.getCurrentCalendar();
        //////some bug in Heroku to get the current day actually missing the first date
        ///// may be the server time - 2hr when try to do end of day not working in this case.
        ///// so, need work around to move to next begining of day
        long endDay = TimeConvertion.workaround_nextday_endOfDayInMillis(dateNow.getTimeInMillis());
        long start = endDay;
        float len = (float) (1.5 * length);  // add sat sun in to the length
        length = (int) (len);
        long end = TimeConvertion.addDays(start, -length);

        if (CKey.CACHE_STOCKH == true) {
            start = TimeConvertion.endOfDayInMillis(dateNow.getTimeInMillis());
            end = TimeConvertion.addDays(start, -length);

            long endStaticDay = 0;
            ArrayList<AFstockInfo> stockInfoArrayStatic = ServiceAFweb.getAllStaticStockHistoryServ(NormalizeSymbol);
            if (stockInfoArrayStatic == null) {
                stockInfoArrayStatic = new ArrayList();
            }
            if (stockInfoArrayStatic.size() > 0) {
//                logger.info("> getStockHistorical" + NormalizeSymbol + " " + stockInfoArrayStatic.size());
                AFstockInfo stockInfo = stockInfoArrayStatic.get(0);
                endStaticDay = TimeConvertion.endOfDayInMillis(stockInfo.getEntrydatel());
                end = TimeConvertion.addDays(endStaticDay, 1);

            }

            long startLoop = start;
            long endLoop = 0;
            while (true) {
                long endDay100 = TimeConvertion.addDays(startLoop, -100);
                endLoop = TimeConvertion.endOfDayInMillis(endDay100);
                if (endLoop <= end) {
                    endLoop = end;
                }
                ArrayList<AFstockInfo> stockInfoArray = getStockHistoricalRange(serviceAFWeb, NormalizeSymbol, startLoop, endLoop);
                if (stockInfoArray == null) {
                    break;
                }
                if (stockInfoArray.size() == 0) {
                    break;
                }
                mergedList.addAll(stockInfoArray);
                startLoop = TimeConvertion.addMiniSeconds(endLoop, -10);
                if (endLoop == end) {
                    break;
                }
            }
            mergedList.addAll(stockInfoArrayStatic);

        } else {
            long startLoop = start;
            long endLoop = 0;
            while (true) {
                long endDay100 = TimeConvertion.addDays(startLoop, -100);
                endLoop = TimeConvertion.endOfDayInMillis(endDay100);
                if (endLoop <= end) {
                    endLoop = end;
                }
                ArrayList<AFstockInfo> stockInfoArray = getStockHistoricalRange(serviceAFWeb, NormalizeSymbol, startLoop, endLoop);
                if (stockInfoArray == null) {
                    break;
                }
                if (stockInfoArray.size() == 0) {
                    break;
                }
                mergedList.addAll(stockInfoArray);
                startLoop = TimeConvertion.addMiniSeconds(endLoop, -10);
                if (endLoop == end) {
                    break;
                }
            }
        }
        if (mergedList.size() == 0) {
            return (ArrayList) mergedList;
        }
//        if (length < 50) {
//            ArrayList<AFstockInfo> sockInfoArray = new ArrayList<AFstockInfo>(mergedList);
//            ArrayList<AFstockInfo> retArray = new ArrayList();
//            for (int i = 0; i < sockInfoArray.size(); i++) {
//                AFstockInfo sInfo = sockInfoArray.get(i);
//                retArray.add(sInfo);
//                if (i > length) {
//                    break;
//                }
//            }
//            return retArray;
//        }

        ////////////////error in HEROKU and Local not sure why?????? //////////////
        ////////////////error in HEROKU and Local not sure why?????? //////////////
        ////////////////error in HEROKU and Local not sure why?????? //////////////
        // TZ problem make sure it is set to TZ Canada/Eastern
        if (mergedList.size() > 1) {

//           AFstockInfo first = mergedList.get(0);
//           AFstockInfo first1 = mergedList.get(1);
//           logger.info(symbol + "getStockHistorical first " + first.getEntrydatel() + " first-1 " + first1.getEntrydatel());
            AFstockInfo last = mergedList.get(mergedList.size() - 1);
            AFstockInfo last1 = mergedList.get(mergedList.size() - 2);

            if (last.getEntrydatel() > last1.getEntrydatel()) {
//                logger.info(symbol + " getStockHistorical last " + last.getEntrydatel() + " last-1 " + last1.getEntrydatel());
                //drop the last become only the last one become the current day (not happen in local) 
                mergedList.remove(last);

            }
        }
//        return (ArrayList) mergedList;
        ArrayList<AFstockInfo> sockInfoArray = new ArrayList<AFstockInfo>(mergedList);
        ArrayList<AFstockInfo> retArray = new ArrayList();
        for (int i = 0; i < sockInfoArray.size(); i++) {
            AFstockInfo sInfo = sockInfoArray.get(i);
            retArray.add(sInfo);
            if (i > length) {
                break;
            }
        }

        if (serviceAFWeb.mydebugSim == true) {
            sockInfoArray = new ArrayList<AFstockInfo>(mergedList);
            retArray = new ArrayList();
            for (int i = 0; i < sockInfoArray.size(); i++) {
                AFstockInfo sInfo = sockInfoArray.get(i);
                if (sInfo.getEntrydatel() > serviceAFWeb.SimDateL) {
                    continue;
                }
                retArray.add(sInfo);
                if (i > length) {
                    break;
                }
            }
        }
        return retArray;
    }

    public ArrayList<AFstockInfo> getStockHistoricalRange(ServiceAFweb serviceAFWeb, String symbol, long start, long end) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return null;
        }

        SymbolNameObj symObj = new SymbolNameObj(symbol);
        String NormalizeSymbol = symObj.getYahooSymbol();
        ArrayList<AFstockInfo> stockInfoArray = serviceAFWeb.getStockInfoImp().getStockHistoricalRange(NormalizeSymbol, start, end);

        return stockInfoArray;
    }
    
    
    
}
