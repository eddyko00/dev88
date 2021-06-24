/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.processstockinfo;

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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author eddyko
 */
public class BackupRestoreInfo {

    protected static Logger logger = Logger.getLogger("BackupRestoreInfo");

    public boolean restoreDBData(ServiceAFweb serviceAFWeb) {
        restoreDBstockinfo(serviceAFWeb);
        restoreDBdummyInfo(serviceAFWeb);

        return true;
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

    private int sendRequestObj(ServiceAFweb serviceAFWeb, ArrayList<String> writeSQLArray) {
        logger.info("> sendRequestObj " + writeSQLArray.size());

        if (writeSQLArray.size() == 0) {
            return 1;
        }
        return serviceAFWeb.updateSQLStockInfoArrayList(writeSQLArray);
    }

    private int restoreDBdummyInfo(ServiceAFweb serviceAFWeb) {

        logger.info("> restoreDBdummyInfo ");
        ArrayList<String> writeSQLArray = new ArrayList();
        String sql = StockInfoDB.createDummyInfotable();
        writeSQLArray.add(sql);
        return serviceAFWeb.updateSQLStockInfoArrayList(writeSQLArray);

    }

    //////////////////////////////////////////////////////////////////
    public boolean downloadDBData(ServiceAFweb serviceAFWeb) {
//        this.serviceAFWeb = serviceAFWeb;

        saveDBstockinfo(serviceAFWeb);

        return true;
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

            String output = serviceAFWeb.getAllStockInfoDBSQLServ(sql);
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

    private ArrayList<String> getDBDataTableId(ServiceAFweb serviceAFWeb, String table) {

        String sql = "select id from " + table + " order by id asc";
        ArrayList<String> array = serviceAFWeb.getAllIdStockInfoSQLServ(sql);

        return array;

    }
    ///////////////////////////////////////////////////////////
}
