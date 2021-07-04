/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.processstockinfo;

import com.afweb.model.*;
import com.afweb.model.account.*;
import com.afweb.model.stock.*;

import com.afweb.service.ServiceAFweb;
import com.afweb.dbstockinfo.StockInfoImp;
import com.afweb.processcache.ECacheService;

import com.afweb.util.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Calendar;

import java.util.List;
import java.util.logging.Logger;
import javax.sql.DataSource;

/**
 *
 * @author eddy
 */
public class StockInfoService {

    protected static Logger logger = Logger.getLogger("StockService");
    StockInfoProcess stockProcess = new StockInfoProcess();
    StockInfoImp stockInfoImp = new StockInfoImp();

    public RequestObj SQLRequestStockInfo(ServiceAFweb serviceAFWeb, RequestObj sqlObj) {

        String st = "";
        String nameST = "";

        ArrayList<String> nameList = null;

        try {
            String typeCd = sqlObj.getCmd();
            int type = Integer.parseInt(typeCd);

            switch (type) {
//                case ServiceAFweb.AllIdInfo:
//                    nameList = getAllIdStockInfoSQL(sqlObj.getReq());
//                    nameST = new ObjectMapper().writeValueAsString(nameList);
//                    sqlObj.setResp(nameST);
//                    return sqlObj;
//                case ServiceAFweb.AllStockInfo:
//                    nameST = getAllStockInfoDBSQL(serviceAFWeb, sqlObj.getReq());
//                    sqlObj.setResp(nameST);
//                    return sqlObj;

//                case ServiceAFweb.updateStockInfoTransaction:  //updateStockInfoTransaction = "103";
//                    try {
//                        st = sqlObj.getReq();
//                        StockInfoTranObj stockInfoTran = new ObjectMapper().readValue(st, StockInfoTranObj.class);
//
//                        int result = updateStockInfoTransaction(serviceAFWeb, stockInfoTran);
//                        sqlObj.setResp("" + result);
//
//                    } catch (Exception ex) {
//                    }
//                    return sqlObj;

//                case ServiceAFweb.StockHistoricalRange: //StockHistoricalRange = 114; //"114";  
//                    try {
//                        String symbol = sqlObj.getReq();
//                        String startSt = sqlObj.getReq1();
//                        long start = Long.parseLong(startSt);
//                        String endSt = sqlObj.getReq2();
//                        long end = Long.parseLong(endSt);
//                        ArrayList<AFstockInfo> StockArray = serviceAFWeb.getStockHistoricalRangeServ(symbol, start, end);
//                        nameST = new ObjectMapper().writeValueAsString(StockArray);
//                        sqlObj.setResp("" + nameST);
//                    } catch (Exception ex) {
//                    }
//                    return sqlObj;
                default:
                    return null;
            }
        } catch (Exception ex) {
            logger.info("> StockInfoSQLRequest exception " + sqlObj.getCmd() + " - " + ex.getMessage());
        }
        return null;
    }

    public int updateAllStockInfo(ServiceAFweb serviceAFWeb) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return 0;
        }
        return stockProcess.updateAllStockInfo(serviceAFWeb);
    }

    public ArrayList<AFstockInfo> getStockInfo(String sym, int length, Calendar dateNow) {
        return stockInfoImp.getStockInfo(sym, length, dateNow);
    }

    // Heuoku cannot get the date of the first stockinfo????
    public ArrayList<AFstockInfo> getStockInfo_workaround(String sym, int length, Calendar dateNow) {
        return stockInfoImp.getStockInfo_workaround(sym, length, dateNow);
    }

    /////recent day first and the old data last////////////
    // return stock history starting recent date to the old date    
    public ArrayList<AFstockInfo> getStockHistorical(ServiceAFweb serviceAFWeb, String symbol, int length) {
        if (ECacheService.cacheFlag == true) {
            if (length > 100) {
                String name = symbol + length;
                ArrayList<AFstockInfo> infoList = ECacheService.getStockHistorical(name);
                if (infoList == null) {
                    infoList = getStockHistoricalImp(serviceAFWeb, symbol, length);
                    if (infoList != null) {
                        ECacheService.putStockHistorical(name, infoList);
                    }
                }
                return infoList;
            }
        }
        return getStockHistoricalImp(serviceAFWeb, symbol, length);
    }

    /////recent day first and the old data last////////////
    // return stock history starting recent date to the old date
    public ArrayList<AFstockInfo> getStockHistoricalImp(ServiceAFweb serviceAFWeb, String symbol, int length) {
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

        if (CKey.STATIC_STOCKH == true) {
            start = TimeConvertion.endOfDayInMillis(dateNow.getTimeInMillis());
            end = TimeConvertion.addDays(start, -length);

            long endStaticDay = 0;
            ArrayList<AFstockInfo> stockInfoArrayStatic = ServiceAFweb.SysGetAllStaticStockHistoryServ(NormalizeSymbol);
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
        AFstockObj stock = serviceAFWeb.StoGetStockObjBySym(NormalizeSymbol);
        if (stock == null) {
            return null;
        }
        if (stock.getSubstatus() == ConstantKey.INITIAL) {
            return null;
        }
        ArrayList<AFstockInfo> stockInfoArray = stockInfoImp.getStockHistoricalRange(NormalizeSymbol, start, end);

        return stockInfoArray;
    }

    public int removeStockInfo(ServiceAFweb serviceAFWeb, String symbol) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        SymbolNameObj symObj = new SymbolNameObj(symbol);
        String NormalizeSymbol = symObj.getYahooSymbol();
        AFstockObj stockObj = serviceAFWeb.StoGetStockObjBySym(NormalizeSymbol);
        if (stockObj != null) {
            return stockInfoImp.deleteStockInfoBySym(stockObj.getSymbol());
        }
        return 0;
    }

    public int updateStockInfoTransaction(ServiceAFweb serviceAFWeb, StockInfoTranObj stockInfoTran) {
        ServiceAFweb.lastfun = "updateStockInfoTransaction";

        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return 0;
        }
        String NormalizeSymbol = stockInfoTran.getNormalizeName();
        ArrayList<AFstockInfo> StockInfoArray = stockInfoTran.getStockInfoList();
        AFstockObj stock = serviceAFWeb.StoGetStockObjBySym(NormalizeSymbol);
        int result = updateStockInfoTransactionProcess(serviceAFWeb, stock, StockInfoArray);
        if (result > 0) {

            //workaround unknown defect- somehow cannot find the Internet from stock to add the error update
            //workaround unknown defect- somehow cannot find the Internet from stock to add the error update
            stock = serviceAFWeb.StoGetStockObjBySym(NormalizeSymbol);
            long updateDate = stock.getUpdatedatel();
            long updateDate5D = TimeConvertion.addDays(updateDate, 5);
            Calendar dateNow = TimeConvertion.getCurrentCalendar();
            if (updateDate5D > dateNow.getTimeInMillis()) {
                return 1;
            }
            int failCnt = stock.getFailedupdate() + 1;
            // too many failure
            if (failCnt > CKey.FAIL_STOCK_CNT) {
                stock.setStatus(ConstantKey.DISABLE);;
            }
            stock.setFailedupdate(failCnt);
            serviceAFWeb.StoUpdateStockStatusDBServ(stock);
            //workaround unknown dfect- somehow cannot find the Internet from stock to add the error update
            //workaround unknown defect- somehow cannot find the Internet from stock to add the error update    
        }
        return 0;
    }

    // StockArray require oldest date to earliest
    // StockArray require oldest date to earliest    
    private int updateStockInfoTransactionProcess(ServiceAFweb serviceAFWeb, AFstockObj stock, ArrayList<AFstockInfo> StockArray) {

//        logger.info("> addStockInfoTransaction " + stock.getSymbol() + " - " + StockArray.size());
        try {
            if (stock == null) {
                return 0;
            }
            if (StockArray == null) {
                return 0;
            }
            if (StockArray.size() == 0) {
                return 1;
            }
            Calendar dateNow = TimeConvertion.getCurrentCalendar();
            long stockinfoDBEndDay = 0;
            ArrayList stockinfoDBArray = getStockInfo_workaround(stock.getSymbol(), 1, dateNow);

            if (stockinfoDBArray != null && stockinfoDBArray.size() == 1) {
                AFstockInfo stockinfoDB = (AFstockInfo) stockinfoDBArray.get(0);
                stockinfoDBEndDay = stockinfoDB.getEntrydatel();
            }
            AFstockInfo stockinfoStaticDB = null;
            if (CKey.STATIC_STOCKH == true) {
                if ((stockinfoDBArray == null) || (stockinfoDBArray.size() == 0)) {

                    ArrayList<AFstockInfo> stockInfoArrayStatic = ServiceAFweb.SysGetAllStaticStockHistoryServ(stock.getSymbol());
                    if (stockInfoArrayStatic == null) {
                        stockInfoArrayStatic = new ArrayList();
                    }
                    if (stockInfoArrayStatic.size() > 0) {
//                        logger.info("> getStockHistorical" + stock.getSymbol() + " " + stockInfoArrayStatic.size());
                        stockinfoStaticDB = stockInfoArrayStatic.get(0);
                        stockinfoDBEndDay = stockinfoStaticDB.getEntrydatel();
                    }
                }
            }
            // jdbc transaction
            ArrayList sqlTranList = new ArrayList();

            if (stock.getSubstatus() == ConstantKey.INITIAL) {
                String sqlDelete = "DELETE From stockinfo where stockid=" + stock.getId();
                sqlTranList.add(sqlDelete);
                stock.setSubstatus(ConstantKey.OPEN);

            }
            int resultAdd = 0;
            for (int i = 0; i < StockArray.size(); i++) {
                AFstockInfo stockinfoTemp = StockArray.get(i);
                long stockinfoRTEndDay = stockinfoTemp.getEntrydatel();

                stockinfoDBEndDay = TimeConvertion.endOfDayInMillis(stockinfoDBEndDay);
                stockinfoRTEndDay = TimeConvertion.endOfDayInMillis(stockinfoRTEndDay);

                if (stockinfoRTEndDay < stockinfoDBEndDay) {
                    continue;
                } else if (stockinfoRTEndDay == stockinfoDBEndDay) {
                    if (CKey.STATIC_STOCKH == true) {
                        if (stockinfoStaticDB != null) {
                            if (stockinfoStaticDB.getEntrydatel() == stockinfoRTEndDay) {
                                // ignore to update the static db file
                                continue;
                            }
                        }
                    }
                    resultAdd++;
                    String updateSQL
                            = "update stockinfo set entrydatedisplay='" + stockinfoTemp.getEntrydatedisplay() + "', entrydatel=" + stockinfoTemp.getEntrydatel() + ", "
                            + "fopen=" + stockinfoTemp.getFopen() + ", fclose=" + stockinfoTemp.getFclose() + ", high=" + stockinfoTemp.getHigh() + ", "
                            + "low=" + stockinfoTemp.getLow() + ", volume=" + stockinfoTemp.getVolume() + ", adjustclose=" + stockinfoTemp.getAdjustclose()
                            + ", sym='" + stockinfoTemp.getSym() + "'"
                            + " where entrydatel=" + stockinfoTemp.getEntrydatel() + " and stockid='" + stock.getId() + "'";

                    //update current stockinfo
                    sqlTranList.add(updateSQL);
                    continue;
                }
                resultAdd++;
                // Add stockinfo
                String insertSQL
                        = "insert into stockinfo (entrydatedisplay, entrydatel, fopen, fclose, high, low ,volume, adjustclose, sym, stockid) VALUES "
                        + "('" + new java.sql.Date(stockinfoTemp.getEntrydatel()) + "'," + stockinfoTemp.getEntrydatel() + ","
                        + stockinfoTemp.getFopen() + "," + stockinfoTemp.getFclose() + "," + stockinfoTemp.getHigh() + "," + stockinfoTemp.getLow() + "," + stockinfoTemp.getVolume()
                        + "," + stockinfoTemp.getAdjustclose() + ",'" + stockinfoTemp.getSym() + "'," + stock.getId() + ")";
                sqlTranList.add(insertSQL);

            }
            //must sepalate stock and stockinfo to exec one by one for 2 db 
            int sqlResult = 0;

            sqlResult = updateSQLStockInfoArrayList(serviceAFWeb, sqlTranList);

            ArrayList sqlStockTranList = new ArrayList();

            //clear Fail update count
            long dateNowLong = dateNow.getTimeInMillis();
            stock.setUpdatedatedisplay(new java.sql.Date(dateNowLong));
            stock.setUpdatedatel(dateNowLong);
            stock.setFailedupdate(0);
            // part of transaction
            String sqlUpdateStockSQL
                    = "update stock set substatus=" + stock.getSubstatus() + ", stockname='" + stock.getStockname()
                    + "', updatedatedisplay='" + stock.getUpdatedatedisplay() + "',updatedatel=" + stock.getUpdatedatel() + ", failedupdate="
                    + stock.getFailedupdate() + " where symbol='" + stock.getSymbol() + "'";
            sqlStockTranList.add(sqlUpdateStockSQL);
            // process all transaction
            sqlResult = updateSQLStockInfoArrayList(serviceAFWeb, sqlStockTranList);
            if (sqlResult == 1) {
                return 1; //resultAdd; 
            }
        } catch (Exception e) {
            logger.info("> addStockInfoTransaction exception " + stock.getSymbol() + " - " + e.getMessage());
        }
        return 0;
    }

    public int updateSQLStockInfoArrayList(ServiceAFweb serviceAFWeb, ArrayList SQLTran) {
        return stockInfoImp.updateSQLStockInfoArrayList(SQLTran);
    }

    public int deleteAllStockInfo(ServiceAFweb serviceAFWeb) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return 0;
        }
        logger.info("> deleteAllStockInfo");
        AccountObj accountObj = serviceAFWeb.SysGetAdminObjFromCache();
        ArrayList<String> stockNameArray = serviceAFWeb.AccGetAccountStockNameListServ(accountObj.getId());
        for (int i = 0; i < stockNameArray.size(); i++) {
            String symbol = stockNameArray.get(i);
            if (symbol.equals("T.T")) {
                continue;
            }
            AFstockObj stockObj = serviceAFWeb.StoGetStockObjBySym(symbol);
            if (stockObj == null) {
                continue;
            }
            if (CKey.STATIC_STOCKH == true) {

                long endStaticDay = 0;
                ArrayList<AFstockInfo> stockInfoArrayStatic = ServiceAFweb.SysGetAllStaticStockHistoryServ(symbol);
                if (stockInfoArrayStatic == null) {
                    stockInfoArrayStatic = new ArrayList();
                }
                if (stockInfoArrayStatic.size() > 0) {
//                logger.info("> getStockHistorical" + NormalizeSymbol + " " + stockInfoArrayStatic.size());
                    AFstockInfo stockInfo = stockInfoArrayStatic.get(0);
                    endStaticDay = TimeConvertion.endOfDayInMillis(stockInfo.getEntrydatel());
                    endStaticDay = TimeConvertion.addDays(endStaticDay, -3);
                    stockInfoImp.deleteStockInfoBySymDate(stockObj.getSymbol(), endStaticDay);
                }

            }
        }
        return 1;
    }

    public ArrayList<String> getAllIdStockInfoSQL(String sql) {
        return stockInfoImp.getAllIdSQL(sql);
    }

//    public String getAllStockInfoDBSQL(ServiceAFweb serviceAFWeb, String sql) {
//        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
//         // ignore backup and resotre
//            if ((CKey.backupFlag == true) || (CKey.restoreFlag == true)) {
//                ;
//            } else {
//                return "";
//            }            
//        }
//        return stockInfoImp.getAllStockInfoDBSQL(sql);
//    }
    public ArrayList<AFstockInfo> getAllStockInfoDBSQLArray(ServiceAFweb serviceAFWeb, String sql) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            // ignore backup and resotre
            if ((CKey.backupFlag == true) || (CKey.restoreFlag == true)) {
                ;
            } else {
                return null;
            }
        }
        return stockInfoImp.getAllStockInfoDBSQLArray(sql);
    }

    public String getAllStockInfoDBSQL(ServiceAFweb serviceAFWeb, String sql) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            // ignore backup and resotre
            if ((CKey.backupFlag == true) || (CKey.restoreFlag == true)) {
                ;
            } else {
                return "";
            }
        }
        try {
            ArrayList<AFstockInfo> entries = stockInfoImp.getAllStockInfoDBSQLArray(sql);
            String nameST = new ObjectMapper().writeValueAsString(entries);
            return nameST;
        } catch (JsonProcessingException ex) {
        }
        return null;
    }

    public boolean restStockInfoDB(ServiceAFweb serviceAFWeb) {
        return stockInfoImp.restStockInfoDB();
    }

    public boolean cleanStockInfoDB(ServiceAFweb serviceAFWeb) {
        return stockInfoImp.cleanStockInfoDB();
    }

    public void setStockInfoDataSource(DataSource dataSource, String URL) {

        stockInfoImp.setStockInfoDataSource(dataSource, URL);
    }

    public int initStockInfoDB(ServiceAFweb serviceAFWeb) {
        return stockInfoImp.initStockInfoDB();
    }

/////////////////////////    
}
