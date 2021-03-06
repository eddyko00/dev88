package com.afweb.processcache;

import com.afweb.model.account.AccountObj;
import com.afweb.model.stock.AFneuralNet;
import com.afweb.model.stock.AFstockInfo;
import com.afweb.model.stock.AFstockObj;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Logger;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author eddyko
 */
public class ECacheService {

    protected static Logger logger = Logger.getLogger("ECacheService");

    public static ECache cacheImp = new ECache();
    public static boolean cacheFlag = true;

    public static int TTL = 2 * 60 * 1000; // 2 minutes

    public static void checkCache() {
        Map<String, ECache.Entity> map = cacheImp.DATA_BASE;
        String retSt = "";
        for (Map.Entry<String, ECache.Entity> entry : map.entrySet()) {
            retSt += " key: " + entry.getKey();
            retSt += " value: " + entry.getValue();
        }
        logger.info("> checkCache " + map.size() + " -" + retSt);
    }

    public static int getCurrentCacheSize() {
        Map<String, ECache.Entity> map = cacheImp.DATA_BASE;
        return map.size();

    }

/////////////    
    public static String NN0 = "NN0_";

    public static int putNeuralNetObjWeight0(String name, AFneuralNet aFneuralNet) {
        cacheImp.put(NN0 + name, aFneuralNet, TTL);
        return 1;
    }

    public static AFneuralNet getNeuralNetObjWeight0(String name) {
        return cacheImp.get(NN0 + name);
    }
////////////////
    public static String SH = "SH_";

    public static int putStockHistorical(String name, ArrayList<AFstockInfo> infoArray) {
        cacheImp.put(SH + name, infoArray, TTL);
        return 1;
    }

    public static ArrayList<AFstockInfo> getStockHistorical(String name) {
        return cacheImp.get(SH + name);
    }
////////////////
    public static String AC = "AC_";

    public static int putAccountByCustomerAccountID(String name, AccountObj accObj) {
        cacheImp.put(SH + name, accObj, TTL);
        return 1;
    }

    public static AccountObj getAccountByCustomerAccountID(String name) {
        return cacheImp.get(AC + name);
    }

////////////////
    public static String SAT = "SAT_";

    public static int putStockByAccountIDTRname(String name, AFstockObj stockObj) {
        cacheImp.put(SH + name, stockObj, TTL);
        return 1;
    }

    public static AFstockObj getStockByAccountIDTRname(String name) {
        return cacheImp.get(SAT + name);
    }

}
