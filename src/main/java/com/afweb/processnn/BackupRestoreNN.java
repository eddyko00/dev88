/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.processnn;

import com.afweb.dbnndata.NNetdataDB;
import com.afweb.model.RequestObj;

import com.afweb.model.stock.*;

import com.afweb.service.ServiceAFweb;
import com.afweb.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import java.util.logging.Logger;

/**
 *
 * @author eddyko
 */
public class BackupRestoreNN {

    protected static Logger logger = Logger.getLogger("BackupRestoreInfo");

    private int sendRequestObj(ServiceAFweb serviceAFWeb, ArrayList<String> writeSQLArray) {
        logger.info("> sendRequestObj " + writeSQLArray.size());
        try {
            if (writeSQLArray.size() == 0) {
                return 1;
            }
            return serviceAFWeb.updateSQLNNArrayListServ(writeSQLArray);

        } catch (Exception ex) {
            logger.info("> sendRequestObj - exception " + ex);
        }
        return 0;
    }

    public boolean restoreDBDataNN(ServiceAFweb serviceAFWeb) {
        logger.info(">>>>>>>> restoreDBDataNN ");

        restoreDBneuralnet(serviceAFWeb);
        restoreDBdummyNN(serviceAFWeb);

        return true;
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
                String sql = NNetdataDB.insertNeuralNet(tableName, item);
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
                String sql = NNetdataDB.insertNeuralNetData(tableName, item);
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

    private int restoreDBdummyNN(ServiceAFweb serviceAFWeb) {

        logger.info("> restoreDBdummyNN ");
        ArrayList<String> writeSQLArray = new ArrayList();
        String sql = NNetdataDB.createDummyNNdatatable();
        writeSQLArray.add(sql);
        return serviceAFWeb.updateSQLStockInfoArrayListServ(writeSQLArray);

    }

    public boolean restoreNNonlyDBData(ServiceAFweb serviceAFWeb) {
        int ret = restoreDBneuralnetPro(serviceAFWeb, "neuralnet");
        return true;
    }

    //////////////////////////////////////////////////////////////////
    public boolean downloadDBDataNN(ServiceAFweb serviceAFWeb) {
        logger.info(">>>>>>>> downloadDBDataNN ");
        saveDBneuralnet(serviceAFWeb);

        return true;
    }

    private int saveDBneuralnet(ServiceAFweb serviceAFWeb) {
        int ret = saveDBneuralnetPro(serviceAFWeb, "neuralnet");
        ret = saveDBneuralnetPro(serviceAFWeb, "neuralnet1");
        ret = saveDBneuralnetDataPro(serviceAFWeb, "neuralnetdata");
        return ret;
    }

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
//

    private ArrayList<String> getDBDataTableId(ServiceAFweb serviceAFWeb, String table) {
        try {
            RequestObj sqlObj = new RequestObj();
            sqlObj.setCmd(ServiceAFweb.AllIdInfo + "");
            String sql = "select id from " + table + " order by id asc";
            return serviceAFWeb.getAllIdStockInfoSQLServ(sql);

//            sqlObj.setReq(sql);
//            RequestObj sqlObjresp = serviceAFWeb.SystemSQLRequest(sqlObj);
//            String output = sqlObjresp.getResp();
//            ArrayList<String> array = null;
//
//            String[] arrayItem = new ObjectMapper().readValue(output, String[].class);
//            List<String> listItem = Arrays.<String>asList(arrayItem);
//            array = new ArrayList<String>(listItem);
//            return array;
        } catch (Exception ex) {
            logger.info("> getDBDataTableId " + ex);
        }
        return null;

    }
    ///////////////////////////////////////////////////////////
}
