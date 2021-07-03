/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.processsystem;

import com.afweb.model.*;
import com.afweb.model.account.*;

import com.afweb.model.stock.*;
import com.afweb.nnsignal.*;

import com.afweb.service.ServiceAFweb;

import com.afweb.util.*;
import java.util.logging.Logger;

/**
 *
 * @author eddyko
 */
public class SystemMaintProcess {

    protected static Logger logger = Logger.getLogger("SystemMaintProcess");

    public void StockSplitProcess(ServiceAFweb serviceAFWeb) {
        ///////////////////////////////////////////////////////////////////////////////////   
        /// update stock split process
        ///////////////////////////////////////////////////////////////////////////////////
        boolean stocksplitflag = false;
        if (stocksplitflag == true) {
            /////////need manually enter the communication id
            /////////need manually enter the communication id

            int commid = 1; // 216; // 215;
            CommObj commObj = serviceAFWeb.AccGetCommObjByID(commid);
            logger.info("stocksplitflag process commid " + commid);
            if (commObj != null) {
                CommData commData = serviceAFWeb.AccGetCommDataObj(commObj);
                if (commData != null) {
                    String sym = commData.getSymbol();
                    boolean retBoolean = true;
                    AFstockObj stock = serviceAFWeb.StoGetStockObjBySym(sym);
//                    if (stock.getSubstatus() == ConstantKey.OPEN) {
//                        stock.setSubstatus(ConstantKey.STOCK_SPLIT);
//                        String sockNameSQL = StockDB.SQLupdateStockStatus(stock);
//                        ArrayList sqlList = new ArrayList();
//                        sqlList.add(sockNameSQL);
//                        SystemUpdateSQLList(sqlList);
//                    }

                    if (stock.getSubstatus() != ConstantKey.STOCK_SPLIT) {
                        return;
                    }
                    // select 5 year and apply and download
                    // https://ca.finance.yahoo.com/quote/AAPL/history
                    // select 5 year and apply and download
                    //"T:/Netbean/db/";
                    String nnFileName = serviceAFWeb.FileLocalPath + sym + ".csv";
                    if (FileUtil.FileTest(nnFileName) == false) {
                        logger.info("updateStockFile not found " + nnFileName);
                        return;
                    }
                    serviceAFWeb.InfRemoveStockInfo(sym);

                    // update file
                    retBoolean = serviceAFWeb.SysUpdateStockFileServ(serviceAFWeb, sym);

                    if (retBoolean == true) {
                        TradingSignalProcess TRprocessImp = new TradingSignalProcess();
                        TRprocessImp.processStockSplit(serviceAFWeb, commData.getSymbol(), commData.getSplit());
                    }
                }
            }

        }
    }

    ///////////////////////////
}
