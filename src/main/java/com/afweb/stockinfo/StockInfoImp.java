/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.stockinfo;

import com.afweb.model.stock.*;

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

    public int deleteStockInfoBySym(String sym) {
        return stockInfodb.deleteStockInfoBySym(sym);
    }

    public int deleteStockInfoBySymDate(String sym, long datel) {
        return stockInfodb.deleteStockInfoBySymDate(sym, datel);
    }

    ///////////////
//    StockImp stockImp = new StockImp();
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

}
