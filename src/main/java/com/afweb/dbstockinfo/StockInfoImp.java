/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.dbstockinfo;

import com.afweb.model.stock.*;

import java.util.ArrayList;
import java.util.Calendar;

import java.util.logging.Logger;
import javax.sql.DataSource;

/**
 *
 * @author eddyko
 */
public class StockInfoImp {

    protected static Logger logger = Logger.getLogger("StockInfoImp");
    private StockInfoDB stockInfodb = new StockInfoDB();

    public void setStockInfoDataSource(DataSource dataSource, String URL) {

        stockInfodb.setDataSource(dataSource, URL);
    }

    public boolean restStockInfoDB() {
        return stockInfodb.restStockInfoDB();
    }

    public boolean cleanStockInfoDB() {
        return stockInfodb.cleanStockInfoDB();
    }

    public ArrayList<AFstockInfo> getAllStockInfoDBSQLArray(String sql) {
        return stockInfodb.getAllStockInfoDBSQLArray(sql);
    }

//    public String getAllStockInfoDBSQL(String sql) {
//        return stockInfodb.getAllStockInfoDBSQL(sql);
//    }
    public int deleteStockInfoBySym(String sym) {
        return stockInfodb.deleteStockInfoBySym(sym);
    }

    public int deleteStockInfoBySymDate(String sym, long datel) {
        return stockInfodb.deleteStockInfoBySymDate(sym, datel);
    }

    ///////////////
    public ArrayList<String> getAllStockInfoUniqueNameList() {
        return stockInfodb.getAllStockInfoUniqueNameList();
    }

    public ArrayList<AFstockInfo> getStockHistoricalRange(String NormalizeSymbol, long start, long end) {
        ArrayList StockArray = null;
        StockArray = stockInfodb.getStockInfoBySymRange(NormalizeSymbol, start, end);

        return StockArray;
    }

    public ArrayList<AFstockInfo> getStockInfo(String sym, int length, Calendar dateNow) {
        return stockInfodb.getStockInfoBySym(sym, length, dateNow);
    }

    // Heuoku cannot get the date of the first stockinfo????
    public ArrayList<AFstockInfo> getStockInfo_workaround(String sym, int length, Calendar dateNow) {
        return stockInfodb.getStockInfo_workaround(sym, length, dateNow);
    }

    public int updateSQLStockInfoArrayList(ArrayList SQLTran) {
        return stockInfodb.updateSQLInfoArrayList(SQLTran);
    }

    public int initStockInfoDB() {
        return stockInfodb.initStockInfoDB();
    }

    public ArrayList getAllIdInfoSQL(String sql) {
        return stockInfodb.getAllIdInfoSQL(sql);
    }
//////////////////////////////////////    

    public String getAllLockDBSQL(String sql) {
        return stockInfodb.getAllLockDBSQL(sql);
    }

    public ArrayList getAllLock() {
        return stockInfodb.getAllLock();
    }

    public int setLockName(String name, int type, long lockDateValue, String comment) {
        return stockInfodb.setLockName(name, type, lockDateValue, comment);
    }

    public AFLockObject getLockName(String name, int type) {
        return stockInfodb.getLockName(name, type);
    }

    public int setRenewLock(String name, int type, long lockDateValue) {
        return stockInfodb.setRenewLock(name, type, lockDateValue);
    }

    public int removeLock(String name, int type) {
        return stockInfodb.removeLock(name, type);
    }

    public int deleteAllLock() {
        return stockInfodb.deleteAllLock();
    }
////////////////////////////////////////////////////////////////

}
