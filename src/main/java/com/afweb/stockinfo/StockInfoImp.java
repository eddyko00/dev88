/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.stockinfo;

import com.afweb.model.*;
import com.afweb.model.stock.*;

import com.afweb.stock.*;

import com.afweb.util.*;

import java.util.ArrayList;
import java.util.Calendar;

import java.util.logging.Logger;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 *
 * @author eddyko
 */
public class StockInfoImp {

    protected static Logger logger = Logger.getLogger("StockInfoImp");
    private StockInfoDB stockInfodb = new StockInfoDB();

    public void setDataSource(JdbcTemplate jdbcTemplate, DataSource dataSource) {

        stockInfodb.setJdbcTemplate(jdbcTemplate);
        stockInfodb.setDataSource(dataSource);
    }

    public boolean restStockInfoDB() {
        return stockInfodb.restStockInfoDB();
    }

    public boolean cleanStockInfoDB() {
        return stockInfodb.cleanStockInfoDB();
    }

    public String getAllStockInfoDBSQL(String sql) {
        return stockInfodb.getAllStockInfoDBSQL(sql);
    }

    public int deleteStockInfoByStockId(AFstockObj stockObj) {
        if (stockObj == null) {
            return 0;
        }
        return stockInfodb.deleteStockInfoByStockId(stockObj);
    }

    public int deleteStockInfoByDate(AFstockObj stockObj, long datel) {
        if (stockObj == null) {
            return 0;
        }
        return stockInfodb.deleteStockInfoByDate(stockObj, datel);
    }

    ///////////////
//    StockImp stockImp = new StockImp();
    public ArrayList<AFstockInfo> getStockHistoricalRange(String NormalizeSymbol, long start, long end) {
        ArrayList StockArray = null;
        StockArray = stockInfodb.getStockInfo(NormalizeSymbol, start, end);
        return StockArray;
    }

//    public ArrayList<AFstockInfo> getStockHistorical(String NormalizeSymbol, int length, Calendar dateNow) {
//
//        AFstockObj stock = stockImp.getRealTimeStock(NormalizeSymbol, dateNow);
//        if (stock == null) {
//            return null;
//        }
//        ArrayList StockArray = null;
//        StockArray = stockInfodb.getStockInfo_workaround(stock, length, dateNow);
//        return StockArray;
//    }
//    
    public ArrayList<AFstockInfo> getStockInfo(AFstockObj stock, int length, Calendar dateNow) {
        return stockInfodb.getStockInfo(stock, length, dateNow);
    }

    // Heuoku cannot get the date of the first stockinfo????
    public ArrayList<AFstockInfo> getStockInfo_workaround(String sym, int length, Calendar dateNow) {
        return stockInfodb.getStockInfo_workaround(sym, length, dateNow);
    }

    // require oldest date to earliest
    // require oldest date to earliest
    public int updateStockInfoTransaction(AFstockObj stock, ArrayList<AFstockInfo> StockArray) {
        return stockInfodb.updateStockInfoTransaction(stock, StockArray);
    }

//    public int updateStockInfoTransaction(AFstockObj stock, StockInfoTranObj stockInfoTran) {
//        String NormalizeSymbol = stockInfoTran.getNormalizeName();
//        ArrayList<AFstockInfo> StockInfoArray = stockInfoTran.getStockInfoList();
//        AFstockObj stock = stockImp.getRealTimeStock(NormalizeSymbol, null);
//        int result = stockInfodb.updateStockInfoTransaction(stock, StockInfoArray);
//        if (result > 0) {
//
//            //workaround unknown defect- somehow cannot find the Internet from stock to add the error update
//            //workaround unknown defect- somehow cannot find the Internet from stock to add the error update
//            stock = stockImp.getRealTimeStock(NormalizeSymbol, null);
//            long updateDate = stock.getUpdatedatel();
//            long updateDate5D = TimeConvertion.addDays(updateDate, 5);
//            Calendar dateNow = TimeConvertion.getCurrentCalendar();
//            if (updateDate5D > dateNow.getTimeInMillis()) {
//                return 1;
//            }
//            int failCnt = stock.getFailedupdate() + 1;
//            // too many failure
//            if (failCnt > CKey.FAIL_STOCK_CNT) {
//                stock.setStatus(ConstantKey.DISABLE);;
//            }
//            stock.setFailedupdate(failCnt);
//            stockImp.updateStockStatusDB(stock);
//            //workaround unknown dfect- somehow cannot find the Internet from stock to add the error update
//            //workaround unknown defect- somehow cannot find the Internet from stock to add the error update    
//        }
//        return 0;
//    }
}
