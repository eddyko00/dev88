/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.processsystem;

import com.afweb.dbaccount.AccountDB;
import com.afweb.dbstock.StockDB;
import com.afweb.model.*;
import com.afweb.model.account.*;

import com.afweb.model.stock.*;

import com.afweb.service.ServiceAFweb;

import com.afweb.util.*;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 *
 * @author eddyko
 */
public class SystemMaintProcess {

    protected static Logger logger = Logger.getLogger("SystemMaintProcess");

    // split 1:3 stock more price /3 (value 3)  
    // Split 3:1 stock less price *3 (value -3)
    public int StockSplitBySym(ServiceAFweb serviceAFWeb, String symbol, float split) {
        logger.info(">StockSplitBySym");

        AFstockObj stock = serviceAFWeb.StoGetStockObjBySym(symbol);

        if (stock.getSubstatus() != ConstantKey.STOCK_SPLIT) {
            return 0;
        }
        symbol = symbol.toUpperCase();

        // split 1:3 stock more price /3 (value 3)  
        // Split 3:1 stock less price *3 (value -3)
        double splitValue = 0;
        if (split > 1) {
            splitValue = split;
        } else if (split < -1) {
            splitValue = - 1 / split;
        }
        if (splitValue == 0) {
            return 0;
        }

        int size1yearAll = 20 * 12 * 5 + (50 * 3);
        ArrayList<AFstockInfo> StockInfoArray = serviceAFWeb.InfGetStockHistorical(stock.getSymbol(), size1yearAll);
        if (StockInfoArray == null) {
            return 0;
        }
        if (StockInfoArray.size() < 100) {
            return 0;
        }
        ArrayList accountIdList = serviceAFWeb.AccGetAllOpenAccountID();
        if (accountIdList == null) {
            return 0;
        }
        for (int i = 0; i < accountIdList.size(); i++) {
            String accountIdSt = (String) accountIdList.get(i);
            int accountId = Integer.parseInt(accountIdSt);
            AccountObj accountObj = serviceAFWeb.AccGetAccountObjByAccountIDServ(accountId);
            if (accountObj == null) {
                continue;
            }

            ArrayList stockNameList = serviceAFWeb.AccGetAccountStockNameListServ(accountObj.getId());
            if (stockNameList == null) {
                continue;
            }
            if (stockNameList.size() == 0) {
                continue;
            }
            boolean foundS = false;
            for (int j = 0; j < stockNameList.size(); j++) {
                String stockN = (String) stockNameList.get(j);
                if (stockN.equals(symbol)) {
                    foundS = true;
                    break;
                }
            }
            if (foundS == false) {
                continue;
            }
            ArrayList<TradingRuleObj> trList = serviceAFWeb.AccGetAccountStockTRListByAccIdStockId(accountObj.getId(), stock.getId());
            if (trList == null) {
                continue;
            }
            for (int j = 0; j < trList.size(); j++) {
                TradingRuleObj TRObj = trList.get(j);
                if (accountObj.getType() == AccountObj.INT_ADMIN_ACCOUNT) {
                    ;
                } else {
                    if (TRObj.getTrname().equals(ConstantKey.TR_ACC)) {
                        ;
                    } else {
                        continue;
                    }
                }
                ArrayList<TransationOrderObj> thList = serviceAFWeb.AccGetAccountStockTransList(accountObj.getId(),
                        stock.getId(), TRObj.getTrname(), 0);
                int ret = this.stockSplitProcess(serviceAFWeb, accountObj, stock, TRObj, thList, splitValue);
            }
        }
        return 1;
    }

    public int stockSplitProcess(ServiceAFweb serviceAFWeb, AccountObj accountObj, AFstockObj stock,
            TradingRuleObj TRObj, ArrayList<TransationOrderObj> thList, double splitValue) {

        if (thList == null) {
            return 0;
        }
        if (thList.size() == 0) {
            return 0;
        }
        ArrayList transSQL = new ArrayList();

        for (int k = 0; k < thList.size(); k++) {
            TransationOrderObj thObj = thList.get(k);
            double avgprice = thObj.getAvgprice() / splitValue;
            double share = thObj.getShare() * splitValue;

            float priceTH = (float) (Math.round(avgprice * 100.0) / 100.0);
            thObj.setAvgprice(priceTH);

            float shareTH = (float) (Math.round(share * 100.0) / 100.0);
            thObj.setShare((float) shareTH);

            String trSql = AccountDB.updateSplitTransactionSQL(thObj);
            transSQL.add(trSql);
        }

        logger.info("> processStockSplit " + accountObj.getAccountname() + " " + TRObj.getTrname() + " total update:" + transSQL.size());

        int ret = 0;
        if (transSQL.size() > 0) {
            ret = serviceAFWeb.AccUpdateTransactionOrder(transSQL);
        }
        if (ret == 1) {
            //udpate performance logic
            //udpate performance logic
        }

        return 1;
    }

    public int StockSplitDisableBySym(ServiceAFweb serviceAFWeb, String sym) {
        AFstockObj stock = serviceAFWeb.StoGetStockObjBySym(sym);
        if (stock.getSubstatus() == ConstantKey.STOCK_SPLIT) {
            stock.setSubstatus(ConstantKey.OPEN);
            String sockNameSQL = StockDB.SQLupdateStockStatus(stock);
            ArrayList sqlList = new ArrayList();
            sqlList.add(sockNameSQL);
            serviceAFWeb.StoUpdateSQLArrayList(sqlList);
            logger.info("StockSplitDisableBySym " + sym + " Stock Split cleared");
            return 1;
        }
        return 0;
    }

    public int StockSplitEnableBySym(ServiceAFweb serviceAFWeb, String sym) {
        AFstockObj stock = serviceAFWeb.StoGetStockObjBySym(sym);
        if (stock.getSubstatus() == ConstantKey.OPEN) {
            stock.setSubstatus(ConstantKey.STOCK_SPLIT);
            String sockNameSQL = StockDB.SQLupdateStockStatus(stock);
            ArrayList sqlList = new ArrayList();
            sqlList.add(sockNameSQL);
            serviceAFWeb.StoUpdateSQLArrayList(sqlList);
            logger.info("StockSplitDisableBySym " + sym + " Stock Split enabled");
            return 1;
        }
        return 0;
    }

    public void StockSplitByCom(ServiceAFweb serviceAFWeb) {
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
                        StockSplitBySym(serviceAFWeb, commData.getSymbol(), commData.getSplit());
                    }
                }
            }

        }
    }

    ///////////////////////////
}
