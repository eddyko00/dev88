package com.afweb.stock;

import com.afweb.stockinternet.StockInternetImpDao;
import com.afweb.stockinfo.StockInfoDB;
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

    public void setDataSource(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        stockdb.setJdbcTemplate(jdbcTemplate);
        stockdb.setDataSource(dataSource);

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

    public String getAllNeuralNetDBSQL(String sql) {
        return stockdb.getAllNeuralNetDBSQL(sql);
    }

    public String getAllNeuralNetDataDBSQL(String sql) {
        return stockdb.getAllNeuralNetDataDBSQL(sql);
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

    public int deleteNeuralNetDataByBPname(String name) {
//        logger.info(">>>>>>>>> deleteNeuralNetDataByBPname " + name);
//        logger.info(">>>>>>>>> deleteNeuralNetDataByBPname " + name);
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

    public int deleteNeuralNet0Rel(String name) {
        return stockdb.deleteNeuralNet0Rel(name);
    }

    public int deleteNeuralNet1(String name) {
        return stockdb.deleteNeuralNet1(name);
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

    // return 0 for error
    public int updateNeuralNetRef0(String name, ReferNameData refnameData) {
        String nameSt = "";

        try {
            nameSt = new ObjectMapper().writeValueAsString(refnameData);
            nameSt = nameSt.replaceAll("\"", "#");
        } catch (JsonProcessingException ex) {
            return 0;
        }
        return stockdb.updateNeuralNetRef0(name, nameSt);
    }

    public int updateNeuralNetRef1(String name, ReferNameData refnameData) {
        String nameSt = "";

        try {
            nameSt = new ObjectMapper().writeValueAsString(refnameData);
            nameSt = nameSt.replaceAll("\"", "#");
        } catch (JsonProcessingException ex) {
            return 0;
        }
        return stockdb.updateNeuralNetRef1(name, nameSt);
    }

    //desc
    public ArrayList<AFneuralNetData> getNeuralNetDataObj(String name, int length) {
        return stockdb.getNeuralNetDataObj(name, length);
    }

    public int deleteNeuralNetDataObjById(int id) {
        return stockdb.deleteNeuralNetDataObjById(id);
    }

    public ArrayList<AFneuralNetData> getNeuralNetDataObj(String name, int stockId, long updatedatel) {
        return stockdb.getNeuralNetDataObj(name, stockId, updatedatel);
    }

    public AFneuralNet getNeuralNetObjWeight0(String name) {
        AFneuralNet nn = stockdb.getNeuralNetObjWeight0(name);
//        if (CKey.WEIGHT_COMPASS == true) {
        if (nn != null) {
            String weightSt = nn.getWeight();
            if (weightSt != null) {
                if (weightSt.length() > 0) {
                    if (weightSt.indexOf("%1F%C2") != -1) {
                        weightSt = ServiceAFweb.decompress(weightSt);
                    }
                    nn.setWeight(weightSt);
                }
            }
        }
//        }
        return nn;
    }

    public AFneuralNet getNeuralNetObjWeight1(String name) {
        AFneuralNet nn = stockdb.getNeuralNetObjWeight1(name);
//        if (CKey.WEIGHT_COMPASS == true) {
        if (nn != null) {
            String weightSt = nn.getWeight();
            if (weightSt != null) {
                if (weightSt.length() > 0) {
                    if (weightSt.indexOf("%1F%C2") != -1) {
                        weightSt = ServiceAFweb.decompress(weightSt);
                    }
                    nn.setWeight(weightSt);
                }
            }
        }
//        }
        return nn;
    }

    public int setCreateNeuralNetObj0(String name, String weight) {

        return stockdb.setCreateNeuralNetObj0(name, weight);
    }

    public int setCreateNeuralNetObjRef0(String name, String weight, String RefName) {
//        if (CKey.WEIGHT_COMPASS == true) {
//            if (weight != null) {
//                if (weight.length() > 0) {
//                    String weightSt = ServiceAFweb.compress(weight);
//                    weight = weightSt;
//                }
//            }
//        }
        return stockdb.setCreateNeuralNetObRefj0(name, weight, RefName);
    }

    public int setCreateNeuralNetObj1(String name, String weight) {
//        if (CKey.WEIGHT_COMPASS == true) {
//            if (weight != null) {
//                if (weight.length() > 0) {
//                    String weightSt = ServiceAFweb.compress(weight);
//                    weight = weightSt;
//                }
//            }
//        }
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

    // 0 - new db, 1 - db already exist, -1 db error
    public int initStockDB() {
        return stockdb.initStockDB();
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
    public StringBuffer getInternetScreenPage(String url) {
        StockInternetImpDao internet = new StockInternetImpDao();
        return internet.getInternetYahooScreenPage(url);
    }

    public AFstockObj getRealTimeStockInternet(String NormalizeSymbol) {
        StockInternetImpDao internet = new StockInternetImpDao();
        return internet.GetRealTimeStockInternet(NormalizeSymbol);
    }
/////////////////////////////////////////////////
}
