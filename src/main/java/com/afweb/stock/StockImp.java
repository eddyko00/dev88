package com.afweb.stock;

import com.afweb.model.ConstantKey;
import com.afweb.model.StockInfoTranObj;
import com.afweb.model.stock.*;
import com.afweb.nn.NNInputDataObj;
import com.afweb.nn.NNInputOutObj;

//import com.afweb.nn.NeuralNetwork;
import com.afweb.nnBP.NNBPservice;
import com.afweb.service.ServiceAFweb;

import com.afweb.util.CKey;
import com.afweb.util.TimeConvertion;
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
//
//    public static int NegativeSignal(int signal) {
//        if (signal == CKey.S_LONG_BUY) {
//            signal = (CKey.S_SHORT_SELL);
//        } else if (signal == CKey.S_BUY) {
//            signal = (CKey.S_SELL);
//        } else if (signal == CKey.S_SHORT_SELL) {
//            signal = (CKey.S_LONG_BUY);
//        } else if (signal == CKey.S_SELL) {
//            signal = (CKey.S_BUY);
//        } else if (signal == CKey.S_EXIT_LONG) {
//            signal = (CKey.S_EXIT_SHORT);
//        } else if (signal == CKey.S_EXIT_SHORT) {
//            signal = (CKey.S_EXIT_LONG);
//        }
//        return signal;
//    }
//
//    public static String getSignalName(int marginType, int signal) {
//        if (marginType == CKey.ENABLE) {
//            switch (signal) {
//                case S_PENDING:
//                    return "Pending";
//                case S_NEUTRAL:
//                    return "Exit";
//                case S_LONG_BUY:
//                    return "EnterLong (Buy)";
//                case S_SHORT_SELL:
//                    return "ShortSell (Sell)";
//                case S_EXIT_LONG:
//                    return "ExitLong (Sell)";
//                case S_EXIT_SHORT:
//                    return "ExitShort (Sell)";
//            }
//        } else {
//            switch (signal) {
//                case S_NEUTRAL:
//                    return "Sell";
//                case S_BUY:
//                    return "Buy";
//                case S_SELL:
//                    return "Sell";
//            }
//        }
//        return "";
//    }
    public static int SHORT_TERM_TREND = 16;
    public static int LONG_TERM_TREND = 50;

    private StockDB stockdb = new StockDB();

    public void setDataSource(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        stockdb.setJdbcTemplate(jdbcTemplate);
        stockdb.setDataSource(dataSource);

    }

    public ArrayList getAllDisableStockNameList(int length) {
        ArrayList returnStocNamekArray = new ArrayList();
        returnStocNamekArray = stockdb.getAllDisableStockName();
//        ArrayList stockArray = stockdb.getAllDisableStock();
//        if (stockArray != null && stockArray.size() > 0) {
//            if (length == 0) {
//                // all stock
//                length = stockArray.size();
//            }
//            if (length > stockArray.size()) {
//                length = stockArray.size();
//            }
//            for (int i = 0; i < length; i++) {
//                AFstockObj stock = (AFstockObj) stockArray.get(i);
//                returnStocNamekArray.add(stock.getSymbol());
//            }
//        }
        return returnStocNamekArray;
    }

    public int addStock(String NormalizeSymbol) {
        StockInternet stockInternet = new StockInternet();
        AFstockObj stockRT = stockInternet.GetRealTimeStockInternet(NormalizeSymbol);
        if (stockRT == null) {
            return 0;
        }
        return stockdb.addStock(NormalizeSymbol);
    }

//    public int AddFailCntStock(String NormalizeSymbol) {
//        AFstockObj stock = getRealTimeStock(NormalizeSymbol, null);
//        if (stock == null) {
//            return 0;
//        }
//        int failCnt = stock.getFailedupdate() + 1;
//        stock.setFailedupdate(failCnt);
//        return stockdb.AddFailCntStock(stock);
//    }
    public int deleteStockInfoByStockId(AFstockObj stockObj) {
        return stockdb.deleteStockInfoByStockId(stockObj);
    }

    public int disableStock(String NormalizeSymbol) {
        return stockdb.disableStock(NormalizeSymbol);
    }

    public int deleteStockInfo(AFstockInfo stockInfo) {
        return stockdb.deleteStockInfo(stockInfo);
    }

    public ArrayList<AFstockInfo> getStockHistoricalRange(String NormalizeSymbol, long start, long end) {
        AFstockObj stock = getRealTimeStock(NormalizeSymbol, null);
        if (stock == null) {
            return null;
        }
        ArrayList StockArray = null;
        StockArray = stockdb.getStockInfo(stock, start, end);
        return StockArray;
    }

    public ArrayList<AFstockInfo> getStockHistorical(String NormalizeSymbol, int length, Calendar dateNow) {

        AFstockObj stock = getRealTimeStock(NormalizeSymbol, dateNow);
        if (stock == null) {
            return null;
        }
        ArrayList StockArray = null;
        StockArray = stockdb.getStockInfo_workaround(stock, length, dateNow);
        return StockArray;
    }

    public AFstockObj getRealTimeStockByStockID(int StockID, Calendar dateNow) {
        return stockdb.getStockByStockID(StockID, dateNow);
    }

    public AFstockObj getRealTimeStock(String NormalizeSymbol, Calendar dateNow) {
//        logger.info("> getRealTimeStock " + NormalizeSymbol);
        AFstockObj stock = stockdb.getStock(NormalizeSymbol, dateNow);
        return stock;
    }

    public ArrayList getOpenStockNameArray() {
        ArrayList stockArray = getStockArray(0);
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

    public ArrayList getStockArray(int length) {
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
        if (CKey.SEPARATE_STOCKINFO_DB == true) {
            String sql = (String) SQLTran.get(0);
            if (sql.indexOf(" stockinfo ") != -1) {
                StockInfoDB stockinfodb = new StockInfoDB();
                return stockinfodb.updateSQLArrayList(SQLTran);
            }
        }
        return stockdb.updateSQLArrayList(SQLTran);
    }

//////////////////////////////////////    
    public String getAllLockDBSQL(String sql) {
        return stockdb.getAllLockDBSQL(sql);
    }

    public String getAllStockDBSQL(String sql) {
        return stockdb.getAllStockDBSQL(sql);
    }

    public String getAllStockInfoDBSQL(String sql) {
        return stockdb.getAllStockInfoDBSQL(sql);
    }

    public String getAllNeuralNetDBSQL(String sql) {
        return stockdb.getAllNeuralNetDBSQL(sql);
    }

    public String getAllNeuralNetDataDBSQL(String sql) {
        return stockdb.getAllNeuralNetDataDBSQL(sql);
    }

    ////////////////////////////////
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

    // require oldest date to earliest
    // require oldest date to earliest
    public int updateStockInfoTransaction(StockInfoTranObj stockInfoTran) {
        String NormalizeSymbol = stockInfoTran.getNormalizeName();
        ArrayList<AFstockInfo> StockInfoArray = stockInfoTran.getStockInfoList();
        AFstockObj stock = stockdb.getStock(NormalizeSymbol, null);
        int result = stockdb.updateStockInfoTransaction(stock, StockInfoArray);
        if (result > 0) {

            //workaround unknown defect- somehow cannot find the Internet from stock to add the error update
            //workaround unknown defect- somehow cannot find the Internet from stock to add the error update
            stock = stockdb.getStock(NormalizeSymbol, null);
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
            updateStockStatusDB(stock);
            //workaround unknown dfect- somehow cannot find the Internet from stock to add the error update
            //workaround unknown defect- somehow cannot find the Internet from stock to add the error update    
        }
        return 0;
    }

    public int updateNeuralNetDataObject(String name, int stockId, NNInputDataObj objData) {
        NNInputOutObj obj = objData.getObj();
        String stData;
        try {
            stData = new ObjectMapper().writeValueAsString(obj);
            return insertNeuralNetDataObject(name, stockId, stData, objData.getUpdatedatel());
        } catch (JsonProcessingException ex) {
        }
        return 0;
    }

    public int deleteNeuralNetData(String name) {
        return stockdb.deleteNeuralNetData(name);
    }

    public int insertNeuralNetDataObject(AFneuralNetData neuralNetData) {
        return stockdb.insertNeuralNetDataObject(neuralNetData);
    }

    private int insertNeuralNetDataObject(String name, int stockId, String data, long updatedatel) {
        return stockdb.insertNeuralNetDataObject(name, stockId, data, updatedatel);
    }

    public int updateNeuralNetStatus0(String name, int status, int type) {
        return stockdb.updateNeuralNetStatus0(name, status, type);
    }

    public int deleteNeuralNet0Table() {
        return stockdb.deleteNeuralNet1Table();
    }

    public int deleteNeuralNet1Table() {
        return stockdb.deleteNeuralNet1Table();
    }

    public int deleteNeuralNet1(String name) {
        return stockdb.deleteNeuralNet1Table();
    }

    public int deleteNeuralNetDataTable() {
        return stockdb.deleteNeuralNetDataTable();
    }

    public int updateNeuralNetStatus1(String name, int status, int type) {
        return stockdb.updateNeuralNetStatus1(name, status, type);
    }

    public int releaseNeuralNetBPObj(String name) {

        ///NeuralNetObj1 transition
        ///NeuralNetObj0 release
        String nameSt = stockdb.getNeuralNetName1(name);
        if (nameSt != null) {
            AFneuralNet nnObj1 = getNeuralNetObjWeight1(name);
            NNBPservice nnTemp = new NNBPservice();
            nnTemp.createNet(nnObj1.getWeight());
            nnTemp.setInputpattern(null);
            nnTemp.setOutputpattern(null);
            String weightSt = nnTemp.getNetObjSt();
            int ret = setCreateNeuralNetObj0(name, weightSt);
            if (ret == 1) {
                stockdb.updateNeuralNetStatus0(name, ConstantKey.OPEN, 0);

                setCreateNeuralNetObj1(name, "");
                return stockdb.updateNeuralNetStatus1(name, ConstantKey.COMPLETED, 0);
            }
        }
        return 0;
    }

    public int updateNeuralNetRef0(String name, String refname) {
        return stockdb.updateNeuralNetRef0(name, refname);
    }

    public int updateNeuralNetRef1(String name, String refname) {
        return stockdb.updateNeuralNetRef1(name, refname);
    }

//    public int initNeuralNetBPObj() {
//        return stockdb.initNeuralNetBPObj();
//    }
//
//    public int releaseNeuralNetObj(String name) {
//
//        String nameSt = stockdb.getNeuralNetName1(name);
//        if (nameSt != null) {
//            AFneuralNet nnObj = getNeuralNetObjWeight1(name);
//            NeuralNetwork nnTemp = new NeuralNetwork();
//            nnTemp.createNeuralNetbyWeight(nnObj.getWeight());
//            nnTemp.setInput(null);
//            nnTemp.setTarget(null);
//            String weightSt = nnTemp.getNeuralNetObjSt();
//            int ret = setCreateNeuralNetObj0(name, weightSt);
//            if (ret == 1) {
//                setCreateNeuralNetObj1(name, "");
//                return stockdb.updateNeuralNetStatus1(name, ConstantKey.COMPLETED, 0);
//            }
//        }
//        return 0;
//    }
    public ArrayList<AFneuralNetData> getNeuralNetDataObj(String name) {
        return stockdb.getNeuralNetDataObj(name);
    }

    public ArrayList<AFneuralNetData> getNeuralNetDataObj(String name, int stockId, long updatedatel) {
        return stockdb.getNeuralNetDataObj(name, stockId, updatedatel);
    }

    public AFneuralNet getNeuralNetObjWeight0(String name) {
        AFneuralNet nn = stockdb.getNeuralNetObjWeight0(name);
        if (CKey.WEIGHT_COMPASS == true) {
            if (nn != null) {
                String weightSt = nn.getWeight();
                if (weightSt != null) {
                    if (weightSt.length() > 0) {
                        weightSt = ServiceAFweb.decompress(weightSt);
                        nn.setWeight(weightSt);
                    }
                }
            }
        }
        return nn;
    }

    public AFneuralNet getNeuralNetObjWeight1(String name) {
        AFneuralNet nn = stockdb.getNeuralNetObjWeight1(name);
        if (CKey.WEIGHT_COMPASS == true) {
            if (nn != null) {
                String weightSt = nn.getWeight();
                if (weightSt != null) {
                    if (weightSt.length() > 0) {
                        weightSt = ServiceAFweb.decompress(weightSt);
                        nn.setWeight(weightSt);
                    }
                }
            }
        }
        return nn;
    }

    public int setCreateNeuralNetObj0(String name, String weight) {
        if (CKey.WEIGHT_COMPASS == true) {
            if (weight != null) {
                if (weight.length() > 0) {
                    String weightSt = ServiceAFweb.compress(weight);
                    weight = weightSt;
                }
            }
        }
        return stockdb.setCreateNeuralNetObj0(name, weight);
    }

    public int setCreateNeuralNetObj1(String name, String weight) {
        if (CKey.WEIGHT_COMPASS == true) {
            if (weight != null) {
                if (weight.length() > 0) {
                    String weightSt = ServiceAFweb.compress(weight);
                    weight = weightSt;
                }
            }
        }
        int ret = stockdb.setCreateNeuralNetObj1(name, weight);
        if (ret == 1) {
            return stockdb.updateNeuralNetStatus1(name, ConstantKey.OPEN, 0);
        }
        return 0;
    }

    ////////////////////////////////
    public boolean restStockDB() {
        return stockdb.restStockDB();
    }

    public boolean cleanNNonlyStockDB() {
        return stockdb.cleanNNonlyStockDB();
    }
        
    public boolean cleanStockDB() {
        return stockdb.cleanStockDB();
    }

    public int deleteAllLock() {
        return stockdb.deleteAllLock();
    }

    public int testStockDB() {
        try {
            int result = stockdb.testStockDB();
        } catch (Exception ex) {
        }
        return -1;  // DB error
    }

    // 0 - new db, 1 - db already exist, -1 db error
    public int initStockDB() {
        try {

            int result = stockdb.initStockDB();

            if (result >= 0) {
                //clear lock
                if ((CKey.SQL_DATABASE == CKey.REMOTE_MS_SQL) || (CKey.SQL_DATABASE == CKey.REMOTE_MYSQL)) {
                    ;
                } else {
                    stockdb.deleteAllLock();
                }
                //dummy stock
                stockdb.addStock("T_T");
                //    public static String primaryStock[] = {"AAPL", "SPY", "DIA", "QQQ", "HOU.TO", "HOD.TO", "T.TO", "FAS", "FAZ", "RY.TO", "XIU.TO"};               
                for (int i = 0; i < ServiceAFweb.primaryStock.length; i++) {
                    String stockN = ServiceAFweb.primaryStock[i];
                    stockdb.addStock(stockN);
                }
                if (result == 0) {
//                    initNeuralNetBPObj();

                    return 0; // new db

                }
                return 1; // DB already exist
            }
        } catch (Exception ex) {

        }
        return -1;  // DB error
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

}
