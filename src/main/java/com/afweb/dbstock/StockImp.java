package com.afweb.dbstock;

import com.afweb.stockinternet.StockInternetImpDao;
import com.afweb.dbstockinfo.StockInfoDB;
import com.afweb.model.*;
import com.afweb.model.stock.*;
import com.afweb.nn.*;

import com.afweb.nnBP.NNBPservice;
import com.afweb.service.ServiceAFweb;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Calendar;

import java.util.logging.Logger;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 *
 * @author eddy
 */
public class StockImp {

    protected static Logger logger = Logger.getLogger("StockImp");

    public static int SHORT_TERM_TREND = 16;
    public static int LONG_TERM_TREND = 50;

    private StockDB stockdb = new StockDB();
    private StockInfoDB stockInfodb = new StockInfoDB();

    public void setDataSource(DataSource dataSource, String URL) {
        stockdb.setDataSource(dataSource, URL);

    }

    public ArrayList getAllRemoveStockNameList(int length) {
        ArrayList returnStocNamekArray = new ArrayList();
        returnStocNamekArray = stockdb.getAllDisableStockName(ConstantKey.COMPLETED);
        return returnStocNamekArray;
    }

    public ArrayList getAllDisableStockNameList(int length) {
        ArrayList returnStocNamekArray = new ArrayList();
        returnStocNamekArray = stockdb.getAllDisableStockName(ConstantKey.DISABLE);
        return returnStocNamekArray;
    }

    public int addStock(String NormalizeSymbol) {
        return stockdb.addStock(NormalizeSymbol);
    }

    public int deleteStock(AFstockObj stockObj) {
        if (stockObj == null) {
            return 0;
        }
        return stockdb.deleteStock(stockObj);
    }

    public int disableStock(String NormalizeSymbol) {
        return stockdb.disableStock(NormalizeSymbol);
    }

    public AFstockObj getStockByStockID(int StockID, Calendar dateNow) {
        return stockdb.getStockByStockID(StockID, dateNow);
    }

    public AFstockObj getStockByName(String NormalizeSymbol, Calendar dateNow) {
//        logger.info("> getRealTimeStock " + NormalizeSymbol);
        AFstockObj stock = stockdb.getStock(NormalizeSymbol, dateNow);
        return stock;
    }

    public ArrayList getOpenStockNameArray() {
        ArrayList stockArray = getStockObjArray(0);
        if (stockArray == null) {
            return null;
        }
        ArrayList returnStocNamekArray = new ArrayList();
        for (int i = 0; i < stockArray.size(); i++) {
            AFstockObj stock = (AFstockObj) stockArray.get(i);
            returnStocNamekArray.add(stock.getSymbol());
        }
        return returnStocNamekArray;
    }

    public ArrayList getStockObjArray(int length) {
        ArrayList returnStockArray = new ArrayList();
        ArrayList stockArray = stockdb.getAllOpenStock();
        if (stockArray != null && stockArray.size() > 0) {
            if (length == 0) {
                // all stock
                return stockArray;
            }
            if (length > stockArray.size()) {
                length = stockArray.size();
            }
            for (int i = 0; i < length; i++) {
                AFstockObj stock = (AFstockObj) stockArray.get(i);
                returnStockArray.add(stock);
            }
        }
        return returnStockArray;
    }

    public int updateStockStatusDB(AFstockObj stock) {
        return stockdb.updateStockStatusDB(stock);
    }

    public int updateSQLArrayList(ArrayList SQLTran) {
        return stockdb.updateSQLArrayList(SQLTran);
    }

    public String getAllStockDBSQL(String sql) {
        return stockdb.getAllStockDBSQL(sql);
    }

    public ArrayList<String> getAllIdSQL(String sql) {
        return stockdb.getAllIdSQL(sql);
    }

//////////////////////////////////////    
    public String getAllLockDBSQL(String sql) {
        return stockdb.getAllLockDBSQL(sql);
    }

    public ArrayList getAllLock() {
        return stockdb.getAllLock();
    }

    public int setLockName(String name, int type, long lockDateValue, String comment) {
        return stockdb.setLockName(name, type, lockDateValue, comment);
    }

    public AFLockObject getLockName(String name, int type) {
        return stockdb.getLockName(name, type);
    }

    public int setRenewLock(String name, int type, long lockDateValue) {
        return stockdb.setRenewLock(name, type, lockDateValue);
    }

    public int removeLock(String name, int type) {
        return stockdb.removeLock(name, type);
    }
////////////////////////////////////////////////////////////////
    
    public boolean restStockDB() {
        return stockdb.restStockDB();
    }

    public boolean cleanStockDB() {
        return stockdb.cleanStockDB();
    }

//    public boolean cleanNNonlyStockDB() {
//        return stockdb.cleanNNonlyStockDB();
//    }

    public int deleteAllLock() {
        return stockdb.deleteAllLock();
    }

    // 0 - new db, 1 - db already exist, -1 db error
    public int initStockDB() {
        return stockdb.initStockDB();
    }

    public String getAllSQLquery(String sql) {
        return stockdb.getAllSQLqueryDBSQL(sql);
    }

    public ArrayList getAllNameSQL(String sql) {
        return stockdb.getAllNameSQL(sql);
    }

    public ArrayList getAllSymbolSQL(String sql) {
        return stockdb.getAllSymbolSQL(sql);
    }

    public String getRemoteMYSQL(String sql) {
        try {
            return stockdb.getRemoteMYSQL(sql);
        } catch (Exception ex) {
            logger.info("> getRemoteMYSQL exception " + ex.getMessage());
            return null;
        }
    }

    public int updateRemoteMYSQL(String sql) {
        try {
            return stockdb.updateRemoteMYSQL(sql);
        } catch (Exception ex) {
            logger.info("> getRemoteMYSQL exception " + ex.getMessage());
            return 0;
        }
    }

///////////////////////////////////////////////    
/////////////////////////////////////////////////
}
