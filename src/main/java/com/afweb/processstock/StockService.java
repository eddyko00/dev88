/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.processstock;

import com.afweb.model.*;
import com.afweb.model.stock.*;

import com.afweb.service.ServiceAFweb;
import com.afweb.dbsys.SysImp;

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
    private StockProcess stockProcess = new StockProcess();
    private SysImp sysImp = new SysImp();

    // need to move to account service
    public RequestObj SQLRequestStock(ServiceAFweb serviceAFWeb, RequestObj sqlObj) {

        String nameST = "";
        int stockId = 0;
        String stockIdSt = "";

        ArrayList<String> nameList = null;

        try {
            String typeCd = sqlObj.getCmd();
            int type = Integer.parseInt(typeCd);

            switch (type) {

//                case ServiceAFweb.AllSymbol:
//                    nameList = getAllSymbolSQL(sqlObj.getReq());
//                    nameST = new ObjectMapper().writeValueAsString(nameList);
//                    sqlObj.setResp(nameST);
//                    return sqlObj;
//                case ServiceAFweb.AllStock:
//                    nameST = getAllStockDBSQL(sqlObj.getReq());
//                    sqlObj.setResp(nameST);
//                    return sqlObj;
//                case ServiceAFweb.RealTimeStockByStockID:  //RealTimeStockByStockID = 119; //"119"; 
//                    stockIdSt = sqlObj.getReq();
//                    stockId = Integer.parseInt(stockIdSt);
//                    AFstockObj stockObj = getStockBySockID(serviceAFWeb, stockId);
//                    nameST = new ObjectMapper().writeValueAsString(stockObj);
//                    sqlObj.setResp(nameST);
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

    public int UpdateAllStockTrend(ServiceAFweb serviceAFWeb) {
        return stockProcess.UpdateAllStockTrend(serviceAFWeb);
    }
//////////////////////////////////////////    

    public String getAllStockDBSQL(String sql) {
        return sysImp.getAllStockDBSQL(sql);
    }

    public ArrayList getAllSymbolSQL(String sql) {
        return sysImp.getAllSymbolSQL(sql);
    }

    public ArrayList getAllRemoveStockNameList(int length) {
        return sysImp.getAllRemoveStockNameList(length);
    }

    public ArrayList getAllDisableStockNameList(int length) {
        return sysImp.getAllDisableStockNameList(length);
    }

    public ArrayList getStockObjArray(ServiceAFweb serviceAFWeb, int length) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return null;
        }

        ArrayList stockList = sysImp.getStockObjArray(length);
        return stockList;
    }

    public AFstockObj getStockByName(ServiceAFweb serviceAFWeb, String symbol) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return null;
        }

        SymbolNameObj symObj = new SymbolNameObj(symbol);
        String NormalizeSymbol = symObj.getYahooSymbol();

        AFstockObj stock = sysImp.getStockByName(NormalizeSymbol, null);

        if (stock == null) {
            return null;
        }
        if (stock.getStatus() == ConstantKey.OPEN) {
            if (stock.getSubstatus() != ConstantKey.INITIAL) {
//                ArrayList StockArray = serviceAFWeb.InfGetStockInfo(stock, 2, null);
                ArrayList StockArray = serviceAFWeb.InfGetStockInfo_workaround(stock, 2, null);

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
            ArrayList<AFstockInfo> stockInfolist = serviceAFWeb.InfGetStockHistorical(NormalizeSymbol, 80);
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
        AFstockObj stock = sysImp.getStockByStockID(stockID, null);

        if (stock == null) {
            return null;
        }
        if (stock.getStatus() == ConstantKey.OPEN) {
            if (stock.getSubstatus() != ConstantKey.INITIAL) {
                ArrayList StockArray = serviceAFWeb.InfGetStockInfo_workaround(stock, 2, null);

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
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        SymbolNameObj symObj = new SymbolNameObj(symbol);
        String NormalizeSymbol = symObj.getYahooSymbol();

        AFstockObj stockRT = serviceAFWeb.SysGetRealTimeStockInternet(NormalizeSymbol);
        if (stockRT == null) {
            return 0;
        }

        int result = sysImp.addStock(NormalizeSymbol);
        if (result == ConstantKey.NEW) {
//            stockProcess.ResetStockUpdateNameArray(serviceAFWeb);
        }
        return result;
    }

    public int deleteStock(ServiceAFweb serviceAFWeb, AFstockObj stock) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return 0;
        }
        return sysImp.deleteStock(stock);
    }

    public int disableStock(ServiceAFweb serviceAFWeb, String symbol) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        SymbolNameObj symObj = new SymbolNameObj(symbol);
        String NormalizeSymbol = symObj.getYahooSymbol();
        return sysImp.disableStock(NormalizeSymbol);
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
        return sysImp.updateStockStatusDB(stock);
    }

    public ArrayList<String> getAllOpenStockNameArray(ServiceAFweb serviceAFWeb) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return null;
        }

        ArrayList stockNameList = sysImp.getOpenStockNameArray();
        return stockNameList;
    }

    public int updateSQLArrayList(ServiceAFweb serviceAFWeb, ArrayList SQLTran) {
        return sysImp.updateSQLArrayList(SQLTran);
    }

///////////////////////////////////////////        
}
