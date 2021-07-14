package com.afweb.dbsys;

import com.afweb.model.*;
import com.afweb.model.stock.*;

import java.util.ArrayList;
import java.util.Calendar;

import java.util.logging.Logger;
import javax.sql.DataSource;

/**
 *
 * @author eddy
 */
public class SysImp {

    protected static Logger logger = Logger.getLogger("SysImp");

    public static int SHORT_TERM_TREND = 16;
    public static int LONG_TERM_TREND = 50;

    private SysDB sysdb = new SysDB();

    public void setDataSource(DataSource dataSource, String URL) {
        sysdb.setDataSource(dataSource, URL);

    }

    public ArrayList getAllRemoveStockNameList(int length) {
        ArrayList returnStocNamekArray = new ArrayList();
        returnStocNamekArray = sysdb.getAllDisableStockName(ConstantKey.COMPLETED);
        return returnStocNamekArray;
    }

    public ArrayList getAllDisableStockNameList(int length) {
        ArrayList returnStocNamekArray = new ArrayList();
        returnStocNamekArray = sysdb.getAllDisableStockName(ConstantKey.DISABLE);
        return returnStocNamekArray;
    }

    public int addStock(String NormalizeSymbol) {
        return sysdb.addStock(NormalizeSymbol);
    }

    public int deleteStock(AFstockObj stockObj) {
        if (stockObj == null) {
            return 0;
        }
        return sysdb.deleteStock(stockObj);
    }

    public int disableStock(String NormalizeSymbol) {
        return sysdb.disableStock(NormalizeSymbol);
    }

    public AFstockObj getStockByStockID(int StockID, Calendar dateNow) {
        return sysdb.getStockByStockID(StockID, dateNow);
    }

    public AFstockObj getStockByName(String NormalizeSymbol, Calendar dateNow) {
//        logger.info("> getRealTimeStock " + NormalizeSymbol);
        AFstockObj stock = sysdb.getStock(NormalizeSymbol, dateNow);
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
        ArrayList stockArray = sysdb.getAllOpenStock();
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
        return sysdb.updateStockStatusDB(stock);
    }

    public int updateSQLArrayList(ArrayList SQLTran) {
        return sysdb.updateSQLArrayList(SQLTran);
    }

    public String getAllStockDBSQL(String sql) {
        return sysdb.getAllStockDBSQL(sql);
    }

    public ArrayList<String> getAllIdSQL(String sql) {
        return sysdb.getAllIdSQL(sql);
    }

//////////////////////////////////////    
    public String getAllLockDBSQL(String sql) {
        return sysdb.getAllLockDBSQL(sql);
    }

    public ArrayList getAllLock() {
        return sysdb.getAllLock();
    }

    public int setLockName(String name, int type, long lockDateValue, String comment) {
        return sysdb.setLockName(name, type, lockDateValue, comment);
    }

    public AFLockObject getLockName(String name, int type) {
        return sysdb.getLockName(name, type);
    }

    public int setRenewLock(String name, int type, long lockDateValue) {
        return sysdb.setRenewLock(name, type, lockDateValue);
    }

    public int removeLock(String name, int type) {
        return sysdb.removeLock(name, type);
    }

    public int deleteAllLock() {
        return sysdb.deleteAllLock();
    }
////////////////////////////////////////////////////////////////

    public boolean restStockDB() {
        return sysdb.restStockDB();
    }

    public boolean cleanStockDB() {
        return sysdb.cleanStockDB();
    }

//    public boolean cleanNNonlyStockDB() {
//        return stockdb.cleanNNonlyStockDB();
//    }
    // 0 - new db, 1 - db already exist, -1 db error
    public int initStockDB() {
        return sysdb.initStockDB();
    }

    public String getAllSQLquery(String sql) {
        return sysdb.getAllSQLqueryDBSQL(sql);
    }

    public ArrayList getAllNameSQL(String sql) {
        return sysdb.getAllNameSQL(sql);
    }

    public ArrayList getAllSymbolSQL(String sql) {
        return sysdb.getAllSymbolSQL(sql);
    }

    public String getRemoteMYSQL(String sql) {
        try {
            return sysdb.getRemoteMYSQL(sql);
        } catch (Exception ex) {
            logger.info("> getRemoteMYSQL exception " + ex.getMessage());
            return null;
        }
    }

    public int updateRemoteMYSQL(String sql) {
        try {
            return sysdb.updateRemoteMYSQL(sql);
        } catch (Exception ex) {
            logger.info("> getRemoteMYSQL exception " + ex.getMessage());
            return 0;
        }
    }

///////////////////////////////////////////////    
/////////////////////////////////////////////////
}
