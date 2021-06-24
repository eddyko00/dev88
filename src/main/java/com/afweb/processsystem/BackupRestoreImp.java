/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.processsystem;

import com.afweb.account.AccountDB;
import com.afweb.stockinfo.StockInfoDB;
import com.afweb.model.RequestObj;
import com.afweb.model.account.*;

import com.afweb.model.stock.*;

import com.afweb.service.ServiceAFweb;
import com.afweb.stock.*;
import com.afweb.util.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author eddy
 */
public class BackupRestoreImp {

    protected static Logger logger = Logger.getLogger("BackkupkRestoreImp");
////////////////////////////////////////    
////////////////uploadDBData////////////////////////    
////////////////uploadDBData////////////////////////        
////////////////////////////////////////    

    public boolean restoreNNonlyDBData(ServiceAFweb serviceAFWeb) {
        int ret = restoreDBneuralnetPro(serviceAFWeb, "neuralnet");
//        if (CKey.NN_DATA_DB == false) {
//            ret = restoreDBneuralnetDataPro(serviceAFWeb, "neuralnetdata");
//        }
        return true;
    }

//    public static String ServiceAFweb.FileLocalPath = "T:/Netbean/db/";
    public boolean restoreDBData(ServiceAFweb serviceAFWeb) {
//        this.serviceAFWeb = serviceAFWeb;

        if (FileUtil.FileTest(ServiceAFweb.FileLocalPath + "customer.txt") == false) {
            logger.info(">>>>>>> Exit restoreDBData - No customer.txt");
            return false;
        }

        int ret = restoreDBcustomer(serviceAFWeb);
        if (ret == 0) {
            return false;
        }

        restoreDBaccount(serviceAFWeb);
        restoreDBstock(serviceAFWeb);
        restoreDBaccountstock_tradingrule(serviceAFWeb);
        restoreDBneuralnet(serviceAFWeb);
        restoreDBtransationorder(serviceAFWeb);
        restoreDBcomm(serviceAFWeb);
        restoreDBbilling(serviceAFWeb);
        restoreDBperformance(serviceAFWeb);
        restoreDBdummy(serviceAFWeb);

        restoreDBstockinfo(serviceAFWeb);
        restoreDBdummyInfo(serviceAFWeb);

        return true;

    }

    private int restoreDBdummy(ServiceAFweb serviceAFWeb) {

        logger.info("> restoreDBdummy ");
        ArrayList<String> writeSQLArray = new ArrayList();
        String sql = StockDB.createDummytable();
        writeSQLArray.add(sql);
        try {
            RequestObj sqlObj = new RequestObj();
            sqlObj.setCmd(ServiceAFweb.UpdateSQLList + "");
            String st = new ObjectMapper().writeValueAsString(writeSQLArray);
            sqlObj.setReq(st);
            RequestObj sqlObjresp = serviceAFWeb.SystemSQLRequest(sqlObj);
            String output = sqlObjresp.getResp();
            if (output == null) {
                return 0;
            }
            return 1;
        } catch (JsonProcessingException ex) {
            logger.info("> sendRequestObj - exception " + ex);
        }
        return 0;
    }

    private int restoreDBneuralnet(ServiceAFweb serviceAFWeb) {

        int ret = restoreDBneuralnetPro(serviceAFWeb, "neuralnet");
        ret = restoreDBneuralnetPro(serviceAFWeb, "neuralnet1");
        ret = restoreDBneuralnetDataPro(serviceAFWeb, "neuralnetdata");
        return ret;
    }

    private int restoreDBneuralnetPro(ServiceAFweb serviceAFWeb, String tableName) {
        int fileCont = 0;
        int ret = 0;
        while (true) {
            String fileName = ServiceAFweb.FileLocalPath + tableName + "_" + fileCont + ".txt";
            if (FileUtil.FileTest(fileName) == false) {
                break;
            }
            //////only require for VMware local
            if (CKey.DELAY_RESTORE == true) {
                if (fileCont > 0) {
                    logger.info("> 60 sec delay");
                    ServiceAFweb.AFSleep1Sec(60);
                }
            }
            //////only require for VMware local
            ret = restoreDBneuralnet_2(serviceAFWeb, tableName, fileCont);
            fileCont++;

        }
        return ret;
    }

    private int restoreDBneuralnet_2(ServiceAFweb serviceAFWeb, String tableName, int fileCont) {

        try {
            ArrayList<String> writeArray = new ArrayList();
            String fileName = tableName + "_" + fileCont + ".txt";
            FileUtil.FileReadTextArray(ServiceAFweb.FileLocalPath + fileName, writeArray);
            ArrayList<String> writeSQLArray = new ArrayList();

            logger.info("> restoreDBneuralnetPro " + writeArray.size());
            int index = 0;
            for (int i = 0; i < writeArray.size(); i++) {
                String output = writeArray.get(i);
                AFneuralNet item = new ObjectMapper().readValue(output, AFneuralNet.class);
                String sql = StockDB.insertNeuralNet(tableName, item);
                writeSQLArray.add(sql);

                index++;
                if (index > 5) {  //5) {
                    index = 0;
                    int ret = sendRequestObj(serviceAFWeb, writeSQLArray);
                    if (ret == 0) {
                        return 0;
                    }
                    writeSQLArray.clear();
                    logger.info("> restoreDBneuralnetPro " + fileName + " total=" + writeArray.size() + " index=" + i);
                    ServiceAFweb.AFSleep();

                }
                if (i > 10) {
                    if ((i % 20) == 0) {
                        ServiceAFweb.AFSleep1Sec(3);
                    }
                }
            }
            return sendRequestObj(serviceAFWeb, writeSQLArray);

        } catch (IOException ex) {
            logger.info("> restoreDBneuralnetPro - exception " + ex);
        }
        return 0;
    }

    private int restoreDBneuralnetDataPro(ServiceAFweb serviceAFWeb, String tableName) {
        int fileCont = 0;
        int ret = 0;
        while (true) {
            String fileName = ServiceAFweb.FileLocalPath + tableName + "_" + fileCont + ".txt";
            if (FileUtil.FileTest(fileName) == false) {
                break;
            }
            //////only require for VMware local
            if (CKey.DELAY_RESTORE == true) {
                if (fileCont > 0) {
                    logger.info("> 60 sec delay");
                    ServiceAFweb.AFSleep1Sec(60);
                }
            }
            //////only require for VMware local
            ret = restoreDBneuralnetData_2(serviceAFWeb, tableName, fileCont);
            fileCont++;

        }
        return ret;
    }

    private int restoreDBneuralnetData_2(ServiceAFweb serviceAFWeb, String tableName, int fileCont) {

        try {
            ArrayList<String> writeArray = new ArrayList();
            String fileName = tableName + "_" + fileCont + ".txt";
            FileUtil.FileReadTextArray(ServiceAFweb.FileLocalPath + fileName, writeArray);
            ArrayList<String> writeSQLArray = new ArrayList();

            logger.info("> restoreDBneuralnetDataPro " + writeArray.size());
            int index = 0;
            for (int i = 0; i < writeArray.size(); i++) {
                String output = writeArray.get(i);
                AFneuralNetData item = new ObjectMapper().readValue(output, AFneuralNetData.class);
                String sql = StockDB.insertNeuralNetData(tableName, item);
                writeSQLArray.add(sql);
                index++;
                if (index > 500) {  //500) {
                    index = 0;
                    int ret = sendRequestObj(serviceAFWeb, writeSQLArray);
                    if (ret == 0) {
                        return 0;
                    }
                    writeSQLArray.clear();
                    logger.info("> restoreDBneuralnetDataPro " + fileName + " total=" + writeArray.size() + " index=" + i);

                    ServiceAFweb.AFSleep();
//                    if (CKey.DELAY_RESTORE == true) {
//                        ServiceAFweb.AFSleep1Sec(6);
//                    }
                }

            }
            return sendRequestObj(serviceAFWeb, writeSQLArray);

        } catch (IOException ex) {
            logger.info("> restoreDBneuralnetDataPro - exception " + ex);
        }
        return 0;
    }

    private int restoreDBstockinfo(ServiceAFweb serviceAFWeb) {
        int fileCont = 0;
        String tableName = "stockinfo";
        int ret = 0;
        while (true) {
            String fileName = ServiceAFweb.FileLocalPath + tableName + "_" + fileCont + ".txt";
            if (FileUtil.FileTest(fileName) == false) {
                break;
            }
            //////only require for VMware local
            if (CKey.DELAY_RESTORE == true) {
                if (fileCont > 0) {
                    logger.info("> 60 sec delay");
                    ServiceAFweb.AFSleep1Sec(60);
                }
            }
            //////only require for VMware local
            ret = restoreDBstockinfo_2(serviceAFWeb, fileCont);
            fileCont++;

        }
        return ret;
    }

    private int restoreDBstockinfo_2(ServiceAFweb serviceAFWeb, int fileCont) {
        String tableName = "stockinfo";
        try {
            ArrayList<String> writeArray = new ArrayList();
            String fileName = tableName + "_" + fileCont + ".txt";
            FileUtil.FileReadTextArray(ServiceAFweb.FileLocalPath + fileName, writeArray);
            ArrayList<String> writeSQLArray = new ArrayList();

            logger.info("> restoreDBstockinfo " + writeArray.size());
            int index = 0;
            for (int i = 0; i < writeArray.size(); i++) {
                String output = writeArray.get(i);
                AFstockInfo item = new ObjectMapper().readValue(output, AFstockInfo.class);
                String sql = StockInfoDB.insertStockInfo(item);
                writeSQLArray.add(sql);
                index++;
                if (index > 500) {  //500) {
                    index = 0;
                    int ret = sendRequestObj(serviceAFWeb, writeSQLArray);
                    if (ret == 0) {
                        return 0;
                    }
                    writeSQLArray.clear();
                    logger.info("> restoreDBstockinfo " + fileName + " total=" + writeArray.size() + " index=" + i);

                    ServiceAFweb.AFSleep();
                }

            }
            return sendRequestObj(serviceAFWeb, writeSQLArray);

        } catch (IOException ex) {
            logger.info("> restoreDBaccount - exception " + ex);
        }
        return 0;
    }

    private int restoreDBdummyInfo(ServiceAFweb serviceAFWeb) {

        logger.info("> restoreDBdummyInfo ");
        ArrayList<String> writeSQLArray = new ArrayList();
        String sql = StockInfoDB.createDummyInfotable();
        writeSQLArray.add(sql);
        try {
            RequestObj sqlObj = new RequestObj();
            sqlObj.setCmd(ServiceAFweb.UpdateSQLList + "");
            String st = new ObjectMapper().writeValueAsString(writeSQLArray);
            sqlObj.setReq(st);
            RequestObj sqlObjresp = serviceAFWeb.SystemSQLRequest(sqlObj);
            String output = sqlObjresp.getResp();
            if (output == null) {
                return 0;
            }
            return 1;
        } catch (JsonProcessingException ex) {
            logger.info("> sendRequestObj - exception " + ex);
        }
        return 0;
    }
    
    
    private int sendRequestObj(ServiceAFweb serviceAFWeb, ArrayList<String> writeSQLArray) {
        logger.info("> sendRequestObj " + writeSQLArray.size());
        try {
            if (writeSQLArray.size() == 0) {
                return 1;
            }
            RequestObj sqlObj = new RequestObj();
            sqlObj.setCmd(ServiceAFweb.UpdateSQLList + "");
            String st = new ObjectMapper().writeValueAsString(writeSQLArray);
            sqlObj.setReq(st);
            RequestObj sqlObjresp = serviceAFWeb.SystemSQLRequest(sqlObj);
            String output = sqlObjresp.getResp();
            if (output == null) {
                return 0;
            }
            return 1;
        } catch (JsonProcessingException ex) {
            logger.info("> sendRequestObj - exception " + ex);
        }
        return 0;
    }

    private int restoreDBstock(ServiceAFweb serviceAFWeb) {
        String tableName = "stock";
        try {
            ArrayList<String> writeArray = new ArrayList();
            FileUtil.FileReadTextArray(ServiceAFweb.FileLocalPath + tableName + ".txt", writeArray);
            ArrayList<String> writeSQLArray = new ArrayList();
            logger.info("> restoreDBstock " + writeArray.size());
            for (int i = 0; i < writeArray.size(); i++) {
                String output = writeArray.get(i);
                AFstockObj item = new ObjectMapper().readValue(output, AFstockObj.class);
                String sql = StockDB.insertStock(item);
                writeSQLArray.add(sql);

            }
            return sendRequestObj(serviceAFWeb, writeSQLArray);

        } catch (IOException ex) {
            logger.info("> restoreDBaccount - exception " + ex);
        }
        return 0;
    }

    private int restoreDBaccount(ServiceAFweb serviceAFWeb) {
        String tableName = "account";
        try {
            ArrayList<String> writeArray = new ArrayList();
            FileUtil.FileReadTextArray(ServiceAFweb.FileLocalPath + tableName + ".txt", writeArray);
            ArrayList<String> writeSQLArray = new ArrayList();
            logger.info("> restoreDBaccount " + writeArray.size());
            for (int i = 0; i < writeArray.size(); i++) {
                String output = writeArray.get(i);
                AccountObj item = new ObjectMapper().readValue(output, AccountObj.class);
                String sql = AccountDB.insertAccountObj(item);
                writeSQLArray.add(sql);
            }
            return sendRequestObj(serviceAFWeb, writeSQLArray);

        } catch (IOException ex) {
            logger.info("> restoreDBaccount - exception " + ex);
        }
        return 0;
    }

    private int restoreDBcustomer(ServiceAFweb serviceAFWeb) {
        String tableName = "customer";
        try {

            ArrayList<String> writeArray = new ArrayList();
            FileUtil.FileReadTextArray(ServiceAFweb.FileLocalPath + tableName + ".txt", writeArray);
            ArrayList<String> writeSQLArray = new ArrayList();
            logger.info("> restoreDBcustomer " + writeArray.size());
            for (int i = 0; i < writeArray.size(); i++) {
                String output = writeArray.get(i);
                CustomerObj item = new ObjectMapper().readValue(output, CustomerObj.class);
                String sql = AccountDB.insertCustomer(item);
                writeSQLArray.add(sql);
            }
            return sendRequestObj(serviceAFWeb, writeSQLArray);

        } catch (IOException ex) {
            logger.info("> restoreDBcustomer - exception " + ex);
        }
        return 0;
    }

    private int restoreDBaccountstock_tradingrule(ServiceAFweb serviceAFWeb) {
        String tableName = "tradingrule";
        try {
            ArrayList<String> writeArray = new ArrayList();
            FileUtil.FileReadTextArray(ServiceAFweb.FileLocalPath + tableName + ".txt", writeArray);
            ArrayList<String> writeSQLArray = new ArrayList();
            logger.info("> restoreDBaccountstock " + writeArray.size());
            int index = 0;
            for (int i = 0; i < writeArray.size(); i++) {
                String output = writeArray.get(i);
                TradingRuleObj item = new ObjectMapper().readValue(output, TradingRuleObj.class);
                String sql = AccountDB.insertAccountStock(item);
                writeSQLArray.add(sql);
                index++;
                if (index > 500) {
                    index = 0;
                    int ret = sendRequestObj(serviceAFWeb, writeSQLArray);
                    if (ret == 0) {
                        return 0;
                    }
                    writeSQLArray.clear();
                    ServiceAFweb.AFSleep();
                }

            }
            return sendRequestObj(serviceAFWeb, writeSQLArray);

        } catch (IOException ex) {
            logger.info("> restoreDBaccountstock - exception " + ex);
        }
        return 0;
    }

    private int restoreDBbilling(ServiceAFweb serviceAFWeb) {
        String tableName = "billing";
        try {
            ArrayList<String> writeArray = new ArrayList();
            String fileName = ServiceAFweb.FileLocalPath + tableName + ".txt";
            if (FileUtil.FileTest(fileName) == false) {
                return 0;
            }
            FileUtil.FileReadTextArray(fileName, writeArray);
            ArrayList<String> writeSQLArray = new ArrayList();
            logger.info("> restoreDBbilling " + writeArray.size());
            int index = 0;
            for (int i = 0; i < writeArray.size(); i++) {
                String output = writeArray.get(i);
                BillingObj item = new ObjectMapper().readValue(output, BillingObj.class);
                String sql = AccountDB.insertBillingObj(item);
                writeSQLArray.add(sql);
                index++;
                if (index > 500) {
                    index = 0;
                    int ret = sendRequestObj(serviceAFWeb, writeSQLArray);
                    if (ret == 0) {
                        return 0;
                    }
                    writeSQLArray.clear();
                    logger.info("> restoreDBbilling " + fileName + " total=" + writeArray.size() + " index=" + i);

                    ServiceAFweb.AFSleep();
                }

            }
            return sendRequestObj(serviceAFWeb, writeSQLArray);

        } catch (IOException ex) {
            logger.info("> restoreDBbilling - exception " + ex);
        }
        return 0;
    }

    private int restoreDBcomm(ServiceAFweb serviceAFWeb) {
        String tableName = "comm";
        try {
            ArrayList<String> writeArray = new ArrayList();
            String fileName = ServiceAFweb.FileLocalPath + tableName + ".txt";
            if (FileUtil.FileTest(fileName) == false) {
                return 0;
            }
            FileUtil.FileReadTextArray(fileName, writeArray);
            ArrayList<String> writeSQLArray = new ArrayList();
            logger.info("> restoreDBcomm " + writeArray.size());
            int index = 0;
            for (int i = 0; i < writeArray.size(); i++) {
                String output = writeArray.get(i);
                CommObj item = new ObjectMapper().readValue(output, CommObj.class);
                String sql = AccountDB.insertCommObj(item);
                writeSQLArray.add(sql);
                index++;
                if (index > 500) {
                    index = 0;
                    int ret = sendRequestObj(serviceAFWeb, writeSQLArray);
                    if (ret == 0) {
                        return 0;
                    }
                    writeSQLArray.clear();
                    logger.info("> restoreDBcomm " + fileName + " total=" + writeArray.size() + " index=" + i);

                    ServiceAFweb.AFSleep();
                }

            }
            return sendRequestObj(serviceAFWeb, writeSQLArray);

        } catch (IOException ex) {
            logger.info("> restoreDBcomm - exception " + ex);
        }
        return 0;
    }

    private int restoreDBtransationorder(ServiceAFweb serviceAFWeb) {
        String tableName = "transationorder";
        try {
            ArrayList<String> writeArray = new ArrayList();
            String fileName = ServiceAFweb.FileLocalPath + tableName + ".txt";
            if (FileUtil.FileTest(fileName) == false) {
                return 0;
            }
            FileUtil.FileReadTextArray(fileName, writeArray);
            ArrayList<String> writeSQLArray = new ArrayList();
            logger.info("> restoreDBtransationorder " + writeArray.size());
            int index = 0;
            for (int i = 0; i < writeArray.size(); i++) {
                String output = writeArray.get(i);
                TransationOrderObj item = new ObjectMapper().readValue(output, TransationOrderObj.class);
                String sql = AccountDB.insertTransaction(item);
                writeSQLArray.add(sql);
                index++;
                if (index > 200) {
                    index = 0;
                    int ret = sendRequestObj(serviceAFWeb, writeSQLArray);
                    if (ret == 0) {
                        return 0;
                    }
                    writeSQLArray.clear();
                    logger.info("> restoreDBtransationorder total=" + writeArray.size() + " index=" + i);

                    ServiceAFweb.AFSleep();
                }

            }
            return sendRequestObj(serviceAFWeb, writeSQLArray);

        } catch (IOException ex) {
            logger.info("> restoreDBtransationorder - exception " + ex);
        }
        return 0;
    }

    private int restoreDBperformance(ServiceAFweb serviceAFWeb) {
        String tableName = "performance";
        try {
            ArrayList<String> writeArray = new ArrayList();
            String fileName = ServiceAFweb.FileLocalPath + tableName + ".txt";
            if (FileUtil.FileTest(fileName) == false) {
                return 0;
            }
            FileUtil.FileReadTextArray(fileName, writeArray);
            ArrayList<String> writeSQLArray = new ArrayList();
            logger.info("> restoreDBperformance " + writeArray.size());
            for (int i = 0; i < writeArray.size(); i++) {
                String output = writeArray.get(i);
                PerformanceObj item = new ObjectMapper().readValue(output, PerformanceObj.class);
                String sql = AccountDB.insertPerformance(item);
                writeSQLArray.add(sql);
            }
            return sendRequestObj(serviceAFWeb, writeSQLArray);

        } catch (IOException ex) {
            logger.info("> restoreDBperformance - exception " + ex);
        }
        return 0;
    }

    /////////////////////////////////////////////////////////// 
    ////////////////downloadDBData/////////////////////////////////////////// 
    ///////////////downloadDBData//////////////////////////////////////////// 
    /////////////////////////////////////////////////////////// 
//    public boolean downloadDBDataTest(ServiceAFweb serviceAFWeb) {
////        this.serviceAFWeb = serviceAFWeb;
//        saveDBneuralnetDataPro(serviceAFWeb, "neuralnetdata");
//        return true;
//    }

    public boolean downloadDBData(ServiceAFweb serviceAFWeb) {
//        this.serviceAFWeb = serviceAFWeb;

//        saveDBstockinfo(serviceAFWeb);
        saveDBcustomer(serviceAFWeb);
        saveDBaccount(serviceAFWeb);
        saveDBaccountstock_tradingrule(serviceAFWeb);
        saveDBneuralnet(serviceAFWeb);
        saveDBtransationorder(serviceAFWeb);
        saveDBcomm(serviceAFWeb);
        saveDBbilling(serviceAFWeb);
        saveDBperformance(serviceAFWeb);
        saveDBstock(serviceAFWeb);

        return true;
    }

    private int saveDBperformance(ServiceAFweb serviceAFWeb) {

        String tableName = "performance";
        ArrayList<String> idList = getDBDataTableId(serviceAFWeb, tableName);
        int len = idList.size();
        ArrayList<String> writeArray = new ArrayList();

        if (len > 0) {
            for (int id = 0; id < len; id += 500) {
                String first = idList.get(id);
                if ((id + 500) < len) {
                    String last = idList.get(id - 1 + 500);
                    int ret = saveDBperformance(serviceAFWeb, tableName, first, last, writeArray);
                    if (ret == 0) {
                        return 0;
                    }
                    ServiceAFweb.AFSleep();
                    continue;
                }
                if ((id + 500) >= len) {
                    String last = idList.get(len - 1);
                    int ret = saveDBperformance(serviceAFWeb, tableName, first, last, writeArray);
                    if (ret == 0) {
                        return 0;
                    }
                    break;
                }

            }
            FileUtil.FileWriteTextArray(ServiceAFweb.FileLocalPath + tableName + ".txt", writeArray);
            return 1;
        }
        return 0;

    }

    private int saveDBperformance(ServiceAFweb serviceAFWeb, String tableName, String first, String last, ArrayList<String> writeArray) {
        try {
            logger.info("> saveDBperformance - " + first + " " + last);
            RequestObj sqlObj = new RequestObj();
            sqlObj.setCmd(ServiceAFweb.AllPerformance + "");
            String sql = "select * from " + tableName + " where id >= " + first + " and id <= " + last + " order by id asc";
            if (first.equals(last)) {
                sql = "select * from " + tableName + " where id = " + first;
            }
            sqlObj.setReq(sql);

            RequestObj sqlObjresp = serviceAFWeb.SystemSQLRequest(sqlObj);
            String output = sqlObjresp.getResp();
            if (output == null) {
                return 0;
            }
            if (output.equals("null")) {
                return 0;
            }
            ArrayList<PerformanceObj> array = null;
            PerformanceObj[] arrayItem = new ObjectMapper().readValue(output, PerformanceObj[].class);
            List<PerformanceObj> listItem = Arrays.<PerformanceObj>asList(arrayItem);
            array = new ArrayList<PerformanceObj>(listItem);

            for (int i = 0; i < array.size(); i++) {
                PerformanceObj obj = array.get(i);
                String st = new ObjectMapper().writeValueAsString(obj);
                writeArray.add(st);
            }
            return 1;

        } catch (Exception ex) {
            logger.info("> saveDBperformance " + ex);
        }
        return 0;
    }

    private int saveDBbilling(ServiceAFweb serviceAFWeb) {

        String tableName = "billing";
        ArrayList<String> idList = getDBDataTableId(serviceAFWeb, tableName);
        int len = idList.size();
        ArrayList<String> writeArray = new ArrayList();

        if (len > 0) {
            for (int id = 0; id < len; id += 500) {
                String first = idList.get(id);
                if ((id + 500) < len) {
                    String last = idList.get(id - 1 + 500);
                    int ret = saveDBbilling(serviceAFWeb, tableName, first, last, writeArray);
                    if (ret == 0) {
                        return 0;
                    }
                    ServiceAFweb.AFSleep();
                    continue;
                }
                if ((id + 500) >= len) {
                    String last = idList.get(len - 1);
                    int ret = saveDBbilling(serviceAFWeb, tableName, first, last, writeArray);
                    if (ret == 0) {
                        return 0;
                    }
                    break;
                }

            }
            FileUtil.FileWriteTextArray(ServiceAFweb.FileLocalPath + tableName + ".txt", writeArray);
            return 1;
        }
        return 0;

    }

    private int saveDBbilling(ServiceAFweb serviceAFWeb, String tableName, String first, String last, ArrayList<String> writeArray) {

        try {
            logger.info("> saveDBbilling - " + first + " " + last);
            RequestObj sqlObj = new RequestObj();
            sqlObj.setCmd(ServiceAFweb.AllBilling + "");
            String sql = "select * from " + tableName + " where id >= " + first + " and id <= " + last + " order by id asc";
            if (first.equals(last)) {
                sql = "select * from " + tableName + " where id = " + first;
            }
            sqlObj.setReq(sql);

            RequestObj sqlObjresp = serviceAFWeb.SystemSQLRequest(sqlObj);
            String output = sqlObjresp.getResp();
            if (output == null) {
                return 0;
            }
            if (output.equals("null")) {
                return 0;
            }
            ArrayList<BillingObj> array = null;
            BillingObj[] arrayItem = new ObjectMapper().readValue(output, BillingObj[].class);
            List<BillingObj> listItem = Arrays.<BillingObj>asList(arrayItem);
            array = new ArrayList<BillingObj>(listItem);

            for (int i = 0; i < array.size(); i++) {
                BillingObj obj = array.get(i);
                String st = new ObjectMapper().writeValueAsString(obj);
                writeArray.add(st);
            }
            return 1;

        } catch (Exception ex) {
            logger.info("> saveDBbilling " + ex);
        }
        return 0;
    }

    private int saveDBcomm(ServiceAFweb serviceAFWeb) {

        String tableName = "comm";
        ArrayList<String> idList = getDBDataTableId(serviceAFWeb, tableName);
        int len = idList.size();
        ArrayList<String> writeArray = new ArrayList();

        if (len > 0) {
            for (int id = 0; id < len; id += 500) {
                String first = idList.get(id);
                if ((id + 500) < len) {
                    String last = idList.get(id - 1 + 500);
                    int ret = saveDBcomm(serviceAFWeb, tableName, first, last, writeArray);
                    if (ret == 0) {
                        return 0;
                    }
                    ServiceAFweb.AFSleep();
                    continue;
                }
                if ((id + 500) >= len) {
                    String last = idList.get(len - 1);
                    int ret = saveDBcomm(serviceAFWeb, tableName, first, last, writeArray);
                    if (ret == 0) {
                        return 0;
                    }
                    break;
                }

            }
            FileUtil.FileWriteTextArray(ServiceAFweb.FileLocalPath + tableName + ".txt", writeArray);
            return 1;
        }
        return 0;

    }

    private int saveDBcomm(ServiceAFweb serviceAFWeb, String tableName, String first, String last, ArrayList<String> writeArray) {

        try {
            logger.info("> saveDBcomm - " + first + " " + last);
            RequestObj sqlObj = new RequestObj();
            sqlObj.setCmd(ServiceAFweb.AllComm + "");
            String sql = "select * from " + tableName + " where id >= " + first + " and id <= " + last + " order by id asc";
            if (first.equals(last)) {
                sql = "select * from " + tableName + " where id = " + first;
            }
            sqlObj.setReq(sql);

            RequestObj sqlObjresp = serviceAFWeb.SystemSQLRequest(sqlObj);
            String output = sqlObjresp.getResp();
            if (output == null) {
                return 0;
            }
            if (output.equals("null")) {
                return 0;
            }

            ArrayList<CommObj> array = null;
            CommObj[] arrayItem = new ObjectMapper().readValue(output, CommObj[].class);
            List<CommObj> listItem = Arrays.<CommObj>asList(arrayItem);
            array = new ArrayList<CommObj>(listItem);

            for (int i = 0; i < array.size(); i++) {
                CommObj obj = array.get(i);
                String st = new ObjectMapper().writeValueAsString(obj);
                writeArray.add(st);
            }
            return 1;

        } catch (Exception ex) {
            logger.info("> saveDBcomm " + ex);
        }
        return 0;
    }

    private int saveDBtransationorder(ServiceAFweb serviceAFWeb) {

        String tableName = "transationorder";
        ArrayList<String> idList = getDBDataTableId(serviceAFWeb, tableName);
        int len = idList.size();
        ArrayList<String> writeArray = new ArrayList();

        if (len > 0) {
            for (int id = 0; id < len; id += 500) {
                String first = idList.get(id);
                if ((id + 500) < len) {
                    String last = idList.get(id - 1 + 500);
                    int ret = saveDBtransationorder(serviceAFWeb, tableName, first, last, writeArray);
                    if (ret == 0) {
                        return 0;
                    }
                    ServiceAFweb.AFSleep();
                    continue;
                }
                if ((id + 500) >= len) {
                    String last = idList.get(len - 1);
                    int ret = saveDBtransationorder(serviceAFWeb, tableName, first, last, writeArray);
                    if (ret == 0) {
                        return 0;
                    }
                    break;
                }

            }
            FileUtil.FileWriteTextArray(ServiceAFweb.FileLocalPath + tableName + ".txt", writeArray);
            return 1;
        }
        return 0;

    }

    private int saveDBtransationorder(ServiceAFweb serviceAFWeb, String tableName, String first, String last, ArrayList<String> writeArray) {

        try {
            logger.info("> saveDBtransationorder - " + first + " " + last);
            RequestObj sqlObj = new RequestObj();
            sqlObj.setCmd(ServiceAFweb.AllTransationorder + "");
            String sql = "select * from " + tableName + " where id >= " + first + " and id <= " + last + " order by id asc";
            if (first.equals(last)) {
                sql = "select * from " + tableName + " where id = " + first;
            }
            sqlObj.setReq(sql);

            RequestObj sqlObjresp = serviceAFWeb.SystemSQLRequest(sqlObj);
            String output = sqlObjresp.getResp();
            if (output == null) {
                return 0;
            }
            if (output.equals("null")) {
                return 0;
            }
            ArrayList<TransationOrderObj> array = null;
            TransationOrderObj[] arrayItem = new ObjectMapper().readValue(output, TransationOrderObj[].class);
            List<TransationOrderObj> listItem = Arrays.<TransationOrderObj>asList(arrayItem);
            array = new ArrayList<TransationOrderObj>(listItem);

            for (int i = 0; i < array.size(); i++) {
                TransationOrderObj obj = array.get(i);
                String st = new ObjectMapper().writeValueAsString(obj);
                writeArray.add(st);
            }
            return 1;

        } catch (Exception ex) {
            logger.info("> saveDBtransationorder " + ex);
        }
        return 0;
    }

    private int saveDBaccountstock_tradingrule(ServiceAFweb serviceAFWeb) {

        String tableName = "tradingrule";
        ArrayList<String> idList = getDBDataTableId(serviceAFWeb, tableName);
        int len = idList.size();
        ArrayList<String> writeArray = new ArrayList();

        if (len > 0) {
            for (int id = 0; id < len; id += 500) {
                String first = idList.get(id);
                if ((id + 500) < len) {
                    String last = idList.get(id - 1 + 500);
                    int ret = saveDBaccountstock(serviceAFWeb, tableName, first, last, writeArray);
                    if (ret == 0) {
                        return 0;
                    }
                    ServiceAFweb.AFSleep();
                    continue;
                }
                if ((id + 500) >= len) {
                    String last = idList.get(len - 1);
                    int ret = saveDBaccountstock(serviceAFWeb, tableName, first, last, writeArray);
                    if (ret == 0) {
                        return 0;
                    }
                    break;
                }

            }
            FileUtil.FileWriteTextArray(ServiceAFweb.FileLocalPath + tableName + ".txt", writeArray);
            return 1;
        }
        return 0;

    }

    private int saveDBaccountstock(ServiceAFweb serviceAFWeb, String tableName, String first, String last, ArrayList<String> writeArray) {

        try {

            logger.info("> saveDBaccountstock - " + first + " " + last);
            RequestObj sqlObj = new RequestObj();
            sqlObj.setCmd(ServiceAFweb.AllAccountStock + "");
            String sql = "select tradingrule.*, tradingrule.trname as symbol from " + tableName + " where id >= " + first + " and id <= " + last + " order by id asc";
            if (first.equals(last)) {
                sql = "select tradingrule.*, tradingrule.trname as symbol from " + tableName + " where id = " + first;
            }
            sqlObj.setReq(sql);

            RequestObj sqlObjresp = serviceAFWeb.SystemSQLRequest(sqlObj);
            String output = sqlObjresp.getResp();
            if (output == null) {
                return 0;
            }

            ArrayList<TradingRuleObj> array = null;
            TradingRuleObj[] arrayItem = new ObjectMapper().readValue(output, TradingRuleObj[].class);
            List<TradingRuleObj> listItem = Arrays.<TradingRuleObj>asList(arrayItem);
            array = new ArrayList<TradingRuleObj>(listItem);

            for (int i = 0; i < array.size(); i++) {
                TradingRuleObj obj = array.get(i);
                if (ServiceAFweb.mydebugtestflag == true) {
                    AccData accData = serviceAFWeb.getAccData(obj.getComment());
                    String nameSt = "";
                    try {
                        nameSt = new ObjectMapper().writeValueAsString(accData);
                        nameSt = nameSt.replaceAll("\"", "#");
                    } catch (JsonProcessingException ex) {
                    }
                    obj.setComment(nameSt);
                }
                String st = new ObjectMapper().writeValueAsString(obj);
                writeArray.add(st);
            }
            return 1;

        } catch (Exception ex) {
            logger.info("> saveDBaccountstock " + ex);
        }
        return 0;
    }

    private int saveDBaccount(ServiceAFweb serviceAFWeb) {
        try {
            String tableName = "account";
            ArrayList<String> idList = getDBDataTableId(serviceAFWeb, tableName);
            int len = idList.size();
            if (len > 0) {
                String first = idList.get(0);
                String last = idList.get(len - 1);
                logger.info("> saveDBaccount - " + first + " " + last);
                RequestObj sqlObj = new RequestObj();
                sqlObj.setCmd(ServiceAFweb.AllAccount + "");
                String sql = "select * from " + tableName + " where id >= " + first + " and id <= " + last + " order by id asc";
                if (first.equals(last)) {
                    sql = "select * from " + tableName + " where id = " + first;
                }
                sqlObj.setReq(sql);

                RequestObj sqlObjresp = serviceAFWeb.SystemSQLRequest(sqlObj);
                String output = sqlObjresp.getResp();
                if (output == null) {
                    return 0;
                }
                ArrayList<AccountObj> array = null;
                AccountObj[] arrayItem = new ObjectMapper().readValue(output, AccountObj[].class);
                List<AccountObj> listItem = Arrays.<AccountObj>asList(arrayItem);
                array = new ArrayList<AccountObj>(listItem);

                ArrayList<String> writeArray = new ArrayList();
                for (int i = 0; i < array.size(); i++) {
                    AccountObj obj = array.get(i);
                    String st = new ObjectMapper().writeValueAsString(obj);
                    writeArray.add(st);
                }
                FileUtil.FileWriteTextArray(ServiceAFweb.FileLocalPath + tableName + ".txt", writeArray);
                return 1;
            }
        } catch (Exception ex) {
            logger.info("> saveDBaccount " + ex);
        }
        return 0;
    }

    private int saveDBcustomer(ServiceAFweb serviceAFWeb) {
        try {
            String tableName = "customer";
            ArrayList<String> idList = getDBDataTableId(serviceAFWeb, tableName);
            int len = idList.size();
            if (len > 0) {
                String first = idList.get(0);
                String last = idList.get(len - 1);
                logger.info("> saveDBcustomer - " + first + " " + last);
                RequestObj sqlObj = new RequestObj();
                sqlObj.setCmd(ServiceAFweb.AllCustomer + "");
                String sql = "select * from " + tableName + " where id >= " + first + " and id <= " + last + " order by id asc";
                if (first.equals(last)) {
                    sql = "select * from " + tableName + " where id = " + first;
                }
                sqlObj.setReq(sql);

                RequestObj sqlObjresp = serviceAFWeb.SystemSQLRequest(sqlObj);
                String output = sqlObjresp.getResp();
                if (output == null) {
                    return 0;
                }
                ArrayList<CustomerObj> array = null;
                CustomerObj[] arrayItem = new ObjectMapper().readValue(output, CustomerObj[].class);
                List<CustomerObj> listItem = Arrays.<CustomerObj>asList(arrayItem);
                array = new ArrayList<CustomerObj>(listItem);

                ArrayList<String> writeArray = new ArrayList();
                for (int i = 0; i < array.size(); i++) {
                    CustomerObj obj = array.get(i);
                    String st = new ObjectMapper().writeValueAsString(obj);
                    writeArray.add(st);
                }
                FileUtil.FileWriteTextArray(ServiceAFweb.FileLocalPath + tableName + ".txt", writeArray);
                return 1;
            }
        } catch (Exception ex) {
            logger.info("> saveDBcustomer " + ex);
        }
        return 0;
    }

    private int saveDBneuralnet(ServiceAFweb serviceAFWeb) {
        int ret = saveDBneuralnetPro(serviceAFWeb, "neuralnet");
        ret = saveDBneuralnetPro(serviceAFWeb, "neuralnet1");
        ret = saveDBneuralnetDataPro(serviceAFWeb, "neuralnetdata");
        return ret;
    }

//    public int saveDBneuralnetProcess(ServiceAFweb serviceAFWeb, String tableName) {
//
//        ArrayList<String> idList = getDBDataTableId(serviceAFWeb, tableName);
//        int len = idList.size();
//        ArrayList<String> writeArray = new ArrayList();
//        if (len > 0) {
//            for (int id = 0; id < len; id += 5) {
//                String first = idList.get(id);
//                if ((id + 5) < len) {
//                    String last = idList.get(id - 1 + 5);
//                    int ret = saveDBneuralnet(serviceAFWeb, tableName, first, last, writeArray);
//                    if (ret == 0) {
//                        return 0;
//                    }
//                    ServiceAFweb.AFSleep();
//                    continue;
//                }
//                if ((id + 5) >= len) {
//                    String last = idList.get(len - 1);
//                    int ret = saveDBneuralnet(serviceAFWeb, tableName, first, last, writeArray);
//                    if (ret == 0) {
//                        return 0;
//                    }
//                    break;
//                }
//            }
//            FileUtil.FileWriteTextArray(ServiceAFweb.FileLocalPath + tableName + ".txt", writeArray);
//            return 1;
//        }
//
//        return 0;
//    }
//
//    private int saveDBneuralnet(ServiceAFweb serviceAFWeb, String tableName, String first, String last, ArrayList<String> writeArray) {
//        try {
//            logger.info("> saveDBneuralnet - " + tableName + " " + first + " " + last);
//
//            RequestObj sqlObj = new RequestObj();
//            sqlObj.setCmd(ServiceAFweb.AllNeuralNet + "");
//            String sql = "select * from " + tableName + " where id >= " + first + " and id <= " + last + " order by id asc";
//            if (first.equals(last)) {
//                sql = "select * from " + tableName + " where id = " + first;
//            }
//            sqlObj.setReq(sql);
//
//            RequestObj sqlObjresp = serviceAFWeb.SystemSQLRequest(sqlObj);
//            String output = sqlObjresp.getResp();
//            if (output == null) {
//                return 0;
//            }
//            ArrayList<AFneuralNet> array = null;
//            AFneuralNet[] arrayItem = new ObjectMapper().readValue(output, AFneuralNet[].class);
//            List<AFneuralNet> listItem = Arrays.<AFneuralNet>asList(arrayItem);
//            array = new ArrayList<AFneuralNet>(listItem);
//
//            for (int i = 0; i < array.size(); i++) {
//                AFneuralNet obj = array.get(i);
//                String st = new ObjectMapper().writeValueAsString(obj);
//                writeArray.add(st);
//            }
//            return 1;
//
//        } catch (Exception ex) {
//            logger.info("> saveDBneuralnet " + ex);
//        }
//        return 0;
//    }
    private int saveDBneuralnetPro(ServiceAFweb serviceAFWeb, String tableName) {

        ArrayList<String> idList = getDBDataTableId(serviceAFWeb, tableName);
        int len = idList.size();
        ArrayList<String> writeArray = new ArrayList();
        logger.info("> saveDBneuralnetPro " + len);
        int fileCont = 0;
        int loopCnt = 0;
        if (len > 0) {
            for (int id = 0; id < len; id += 15) {
                String first = idList.get(id);
                if ((id + 15) < len) {
                    String last = idList.get(id - 1 + 15);
                    int ret = saveDBneuralnet_2(serviceAFWeb, tableName, first, last, writeArray);
                    if (ret == 0) {
                        return 0;
                    }
                    ServiceAFweb.AFSleep();
                }
                if ((id + 15) >= len) {
                    String last = idList.get(len - 1);
                    int ret = saveDBneuralnet_2(serviceAFWeb, tableName, first, last, writeArray);
                    if (ret == 0) {
                        return 0;
                    }
                    break;
                }
                loopCnt++;
                if (loopCnt > 4) {
                    FileUtil.FileWriteTextArray(ServiceAFweb.FileLocalPath + tableName + "_" + fileCont + ".txt", writeArray);
                    fileCont++;
                    loopCnt = 0;
                    writeArray.clear();
                }
            }
            FileUtil.FileWriteTextArray(ServiceAFweb.FileLocalPath + tableName + "_" + fileCont + ".txt", writeArray);
            return 1;
        }

        return 0;
    }

    private int saveDBneuralnet_2(ServiceAFweb serviceAFWeb, String tableName, String first, String last, ArrayList<String> writeArray) {
        try {
            logger.info("> saveDBneuralnetDataPro - " + first + " " + last);

            RequestObj sqlObj = new RequestObj();
            sqlObj.setCmd(ServiceAFweb.AllNeuralNet + "");
            String sql = "select * from " + tableName + " where id >= " + first + " and id <= " + last + " order by id asc";
            if (first.equals(last)) {
                sql = "select * from " + tableName + " where id = " + first;
            }
            sqlObj.setReq(sql);

            RequestObj sqlObjresp = serviceAFWeb.SystemSQLRequest(sqlObj);
            String output = sqlObjresp.getResp();
            if (output == null) {
                return 0;
            }
            ArrayList<AFneuralNet> array = null;
            AFneuralNet[] arrayItem = new ObjectMapper().readValue(output, AFneuralNet[].class);
            List<AFneuralNet> listItem = Arrays.<AFneuralNet>asList(arrayItem);
            array = new ArrayList<AFneuralNet>(listItem);

            for (int i = 0; i < array.size(); i++) {
                AFneuralNet obj = array.get(i);
                String st = new ObjectMapper().writeValueAsString(obj);
                writeArray.add(st);
            }
            return 1;

        } catch (Exception ex) {
            logger.info("> saveDBneuralnetDataPro " + ex);
        }
        return 0;
    }

    private int saveDBneuralnetDataPro(ServiceAFweb serviceAFWeb, String tableName) {

        ArrayList<String> idList = getDBDataTableId(serviceAFWeb, tableName);
        int len = idList.size();
        ArrayList<String> writeArray = new ArrayList();
        logger.info("> saveDBneuralnetDataPro " + len);
        int fileCont = 0;
        int loopCnt = 0;
        if (len > 0) {
            for (int id = 0; id < len; id += 500) {
                String first = idList.get(id);
                if ((id + 500) < len) {
                    String last = idList.get(id - 1 + 500);
                    int ret = saveDBneuralnetData_2(serviceAFWeb, tableName, first, last, writeArray);
                    if (ret == 0) {
                        return 0;
                    }
                    ServiceAFweb.AFSleep();
                }
                if ((id + 500) >= len) {
                    String last = idList.get(len - 1);
                    int ret = saveDBneuralnetData_2(serviceAFWeb, tableName, first, last, writeArray);
                    if (ret == 0) {
                        return 0;
                    }
                    break;
                }
                loopCnt++;
                if (loopCnt > 10) {
                    FileUtil.FileWriteTextArray(ServiceAFweb.FileLocalPath + tableName + "_" + fileCont + ".txt", writeArray);
                    fileCont++;
                    loopCnt = 0;
                    writeArray.clear();
                }
            }
            FileUtil.FileWriteTextArray(ServiceAFweb.FileLocalPath + tableName + "_" + fileCont + ".txt", writeArray);
            return 1;
        }

        return 0;
    }

    private int saveDBneuralnetData_2(ServiceAFweb serviceAFWeb, String tableName, String first, String last, ArrayList<String> writeArray) {
        try {
            logger.info("> saveDBneuralnetDataPro - " + first + " " + last);

            RequestObj sqlObj = new RequestObj();
            sqlObj.setCmd(ServiceAFweb.AllNeuralNetData + "");
            String sql = "select * from " + tableName + " where id >= " + first + " and id <= " + last + " order by id asc";
            if (first.equals(last)) {
                sql = "select * from " + tableName + " where id = " + first;
            }
            sqlObj.setReq(sql);

            RequestObj sqlObjresp = serviceAFWeb.SystemSQLRequest(sqlObj);
            String output = sqlObjresp.getResp();
            if (output == null) {
                return 0;
            }
            ArrayList<AFneuralNetData> array = null;
            AFneuralNetData[] arrayItem = new ObjectMapper().readValue(output, AFneuralNetData[].class);
            List<AFneuralNetData> listItem = Arrays.<AFneuralNetData>asList(arrayItem);
            array = new ArrayList<AFneuralNetData>(listItem);

            for (int i = 0; i < array.size(); i++) {
                AFneuralNetData obj = array.get(i);
                String st = new ObjectMapper().writeValueAsString(obj);
                writeArray.add(st);
            }
            return 1;

        } catch (Exception ex) {
            logger.info("> saveDBneuralnetDataPro " + ex);
        }
        return 0;
    }

    private int saveDBstockinfo(ServiceAFweb serviceAFWeb) {

        String tableName = "stockinfo";
        ArrayList<String> idList = getDBDataTableId(serviceAFWeb, tableName);
        int len = idList.size();
        ArrayList<String> writeArray = new ArrayList();
        logger.info("> saveDBstockinfo " + len);
        int fileCont = 0;
        int loopCnt = 0;
        if (len > 0) {
            for (int id = 0; id < len; id += 500) {
                String first = idList.get(id);
                if ((id + 500) < len) {
                    String last = idList.get(id - 1 + 500);
                    int ret = saveDBstockinfo(serviceAFWeb, tableName, first, last, writeArray);
                    if (ret == 0) {
                        return 0;
                    }
                    ServiceAFweb.AFSleep();
                }
                if ((id + 500) >= len) {
                    String last = idList.get(len - 1);
                    int ret = saveDBstockinfo(serviceAFWeb, tableName, first, last, writeArray);
                    if (ret == 0) {
                        return 0;
                    }
                    break;
                }
                loopCnt++;
                if (loopCnt > 15) {
                    FileUtil.FileWriteTextArray(ServiceAFweb.FileLocalPath + tableName + "_" + fileCont + ".txt", writeArray);
                    fileCont++;
                    loopCnt = 0;
                    writeArray.clear();
                }
            }
            FileUtil.FileWriteTextArray(ServiceAFweb.FileLocalPath + tableName + "_" + fileCont + ".txt", writeArray);
            return 1;
        }

        return 0;
    }

    private int saveDBstockinfo(ServiceAFweb serviceAFWeb, String tableName, String first, String last, ArrayList<String> writeArray) {
        try {
            logger.info("> saveDBstockinfo - " + first + " " + last);

            RequestObj sqlObj = new RequestObj();
            sqlObj.setCmd(ServiceAFweb.AllStockInfo + "");
            String sql = "select * from " + tableName + " where id >= " + first + " and id <= " + last + " order by id asc";
            if (first.equals(last)) {
                sql = "select * from " + tableName + " where id = " + first;
            }
            sqlObj.setReq(sql);

            RequestObj sqlObjresp = serviceAFWeb.SystemSQLRequest(sqlObj);
            String output = sqlObjresp.getResp();
            if (output == null) {
                return 0;
            }
            ArrayList<AFstockInfo> array = null;
            AFstockInfo[] arrayItem = new ObjectMapper().readValue(output, AFstockInfo[].class);
            List<AFstockInfo> listItem = Arrays.<AFstockInfo>asList(arrayItem);
            array = new ArrayList<AFstockInfo>(listItem);

            for (int i = 0; i < array.size(); i++) {
                AFstockInfo obj = array.get(i);
                String st = new ObjectMapper().writeValueAsString(obj);
                writeArray.add(st);
            }
            return 1;

        } catch (Exception ex) {
            logger.info("> saveDBstockinfo " + ex);
        }
        return 0;
    }

    private int saveDBstock(ServiceAFweb serviceAFWeb) {
        try {
            String tableName = "stock";
            ArrayList<String> idList = getDBDataTableId(serviceAFWeb, tableName);
            int len = idList.size();
            if (len > 0) {
                String first = idList.get(0);
                String last = idList.get(len - 1);
                logger.info("> saveDBstock - " + first + " " + last);
                RequestObj sqlObj = new RequestObj();
                sqlObj.setCmd(ServiceAFweb.AllStock + "");
                String sql = "select * from " + tableName + " where id >= " + first + " and id <= " + last + " order by id asc";
                if (first.equals(last)) {
                    sql = "select * from " + tableName + " where id = " + first;
                }
                sqlObj.setReq(sql);

                RequestObj sqlObjresp = serviceAFWeb.SystemSQLRequest(sqlObj);
                String output = sqlObjresp.getResp();
                if (output == null) {
                    return 0;
                }
                ArrayList<AFstockObj> array = null;
                AFstockObj[] arrayItem = new ObjectMapper().readValue(output, AFstockObj[].class);
                List<AFstockObj> listItem = Arrays.<AFstockObj>asList(arrayItem);
                array = new ArrayList<AFstockObj>(listItem);

                ArrayList<String> writeArray = new ArrayList();
                for (int i = 0; i < array.size(); i++) {
                    AFstockObj obj = array.get(i);
                    String st = new ObjectMapper().writeValueAsString(obj);
                    writeArray.add(st);
                }
                FileUtil.FileWriteTextArray(ServiceAFweb.FileLocalPath + tableName + ".txt", writeArray);
                return 1;
            }
        } catch (Exception ex) {
            logger.info("> saveDBstock " + ex);
        }
        return 0;
    }

    private int saveDBlockobject(ServiceAFweb serviceAFWeb) {
        try {
            String tableName = "lockobject";
            ArrayList<String> idList = getDBDataTableId(serviceAFWeb, tableName);
            int len = idList.size();
            if (len > 0) {
                String first = idList.get(0);
                String last = idList.get(len - 1);
                logger.info("> saveDBlockobject - " + first + " " + last);
                RequestObj sqlObj = new RequestObj();
                sqlObj.setCmd(ServiceAFweb.AllLock + "");
                String sql = "select * from " + tableName + " where id >= " + first + " and id <= " + last + " order by id asc";
                if (first.equals(last)) {
                    sql = "select * from " + tableName + " where id = " + first;
                }
                sqlObj.setReq(sql);

                RequestObj sqlObjresp = serviceAFWeb.SystemSQLRequest(sqlObj);
                String output = sqlObjresp.getResp();
                if (output == null) {
                    return 0;
                }
                ArrayList<AFLockObject> array = null;
                AFLockObject[] arrayItem = new ObjectMapper().readValue(output, AFLockObject[].class);
                List<AFLockObject> listItem = Arrays.<AFLockObject>asList(arrayItem);
                array = new ArrayList<AFLockObject>(listItem);

                ArrayList<String> writeArray = new ArrayList();
                for (int i = 0; i < array.size(); i++) {
                    AFLockObject obj = array.get(i);
                    String st = new ObjectMapper().writeValueAsString(obj);
                    writeArray.add(st);
                }
                FileUtil.FileWriteTextArray(ServiceAFweb.FileLocalPath + tableName + ".txt", writeArray);
                return 1;
            }
        } catch (Exception ex) {
            logger.info("> saveDBlockobject " + ex);
        }
        return 0;
    }

    private ArrayList<String> getDBDataTableId(ServiceAFweb serviceAFWeb, String table) {
        try {
            RequestObj sqlObj = new RequestObj();
            sqlObj.setCmd(ServiceAFweb.AllId + "");
            String sql = "select id from " + table + " order by id asc";
            sqlObj.setReq(sql);

            RequestObj sqlObjresp = serviceAFWeb.SystemSQLRequest(sqlObj);
            String output = sqlObjresp.getResp();
            ArrayList<String> array = null;

            String[] arrayItem = new ObjectMapper().readValue(output, String[].class);
            List<String> listItem = Arrays.<String>asList(arrayItem);
            array = new ArrayList<String>(listItem);
            return array;

        } catch (IOException ex) {
            logger.info("> getDBDataTableId " + ex);
        }
        return null;
    }
    ///////////////////////////////////////////////////////////

}
