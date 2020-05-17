/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.account;

import com.afweb.model.*;
import com.afweb.model.account.*;
import com.afweb.model.stock.*;

import com.afweb.service.ServiceAFweb;

import com.afweb.signal.*;
import com.afweb.stock.*;

import com.afweb.util.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

import java.util.List;
import java.util.TimeZone;
import java.util.logging.Level;

import java.util.logging.Logger;

/**
 *
 * @author eddy
 */
public class AccountProcess {

    protected static Logger logger = Logger.getLogger("AccountProcess");

    private ServiceAFweb serviceAFWeb = null;
    private int timerCnt = 0;
    public static String LocalPCSignalPath = "T:/Netbean/signal/";
    private static ArrayList accountIdNameArray = new ArrayList();
    private static ArrayList accountFundIdNameArray = new ArrayList();

    ///
    public void InitSystemData() {
        timerCnt = 0;
        accountIdNameArray = new ArrayList();
    }

    public void ProcessSystemMaintance(ServiceAFweb serviceAFWeb) {
        this.serviceAFWeb = serviceAFWeb;

        timerCnt++;
        if (timerCnt < 0) {
            timerCnt = 0;
        }

        Calendar dateNow = TimeConvertion.getCurrentCalendar();
        long lockDateValue = dateNow.getTimeInMillis();
        String LockName = "ACC_" + CKey.AF_SYSTEM;
        long lockReturn = serviceAFWeb.setLockNameProcess(LockName, ConstantKey.ACC_LOCKTYPE, lockDateValue, ServiceAFweb.getServerObj().getSrvProjName() + "_ProcessSystemMaintance");
        if (lockReturn > 0) {

            // 2 minute evey 2 minutes
            if (timerCnt % 2 == 0) {
                // delete stock based on all customer account exclude the ADMIN_USERNAME account 
                ProcessStockkMaintance();

                // add or remove stock in ADMIN_USERNAME account based on all stocks in the system
                ProcessAdminAccount(serviceAFWeb);

                // cleanup Lock entry pass 30 min
                ProcessAllLockCleanup();
                // cleanup Lock entry pass 30 min
            } else if (timerCnt % 5 == 0) {
                // disable cusotmer with no activity in 2 days
                ProcessCustomerDisableMaintance();

                // reomve customer with no activity in 4 days  
                ProcessCustomerRemoveMaintance();

                //delete stock if disable
                ProcessStockInfodeleteMaintance();
            }
        }
        serviceAFWeb.removeNameLock(LockName, ConstantKey.ACC_LOCKTYPE);

    }
    //////////////////////////////////////////////

    private void ProcessStockInfodeleteMaintance() {
        //delete stock if disable
        ArrayList stockNameList = serviceAFWeb.getExpiredStockNameList(20);
        if (stockNameList == null) {
            return;
        }
        int numCnt = 0;
        for (int i = 0; i < stockNameList.size(); i++) {
            String symbol = (String) stockNameList.get(i);

            serviceAFWeb.deleteStockInfo(symbol);
            numCnt++;
            if (numCnt > 10) {
                break;
            }
        }
    }

    private void ProcessCustomerRemoveMaintance() {
        // reomve customer with no activity in 4 days        
        ArrayList custList = serviceAFWeb.getExpiredCustomerList(0);
        if (custList == null) {
            return;
        }
        int numCnt = 0;
        Calendar dateNow = TimeConvertion.getCurrentCalendar();
        long dateNowLong = dateNow.getTimeInMillis();
        long cust4DayAgo = TimeConvertion.addDays(dateNowLong, -4); // 4 day ago and no update             
        for (int i = 0; i < custList.size(); i++) {
            CustomerObj custObj = (CustomerObj) custList.get(i);
            if (custObj.getUpdatedatel() < cust4DayAgo) {

                //remove customer
                serviceAFWeb.removeCustomer(custObj.getUsername());

                String tzid = "America/New_York"; //EDT
                TimeZone tz = TimeZone.getTimeZone(tzid);
                AccountObj accountAdminObj = serviceAFWeb.getAdminObjFromCache();
                java.sql.Date d = new java.sql.Date(dateNowLong);
//                                DateFormat format = new SimpleDateFormat("M/dd/yyyy hh:mm a z");
                DateFormat format = new SimpleDateFormat(" hh:mm a");
                format.setTimeZone(tz);
                String ESTdate = format.format(d);

                String msg = ESTdate + " " + custObj.getUsername() + " Customer removed - 4 day after expired.";
                this.AddCommMessage(serviceAFWeb, accountAdminObj, msg);
                numCnt++;
                if (numCnt > 10) {
                    break;
                }

            }

        }
    }

    public void ProcessCustomerDisableMaintanceTest(ServiceAFweb serviceAFWeb) {
        this.serviceAFWeb = serviceAFWeb;
        ProcessCustomerDisableMaintance();
    }

    private void ProcessCustomerDisableMaintance() {
        // disable cusotmer with no activity in 2 days
        ArrayList custList = serviceAFWeb.getExpiredCustomerList(0);

        if (custList == null) {
            return;
        }
        Calendar dateNow = TimeConvertion.getCurrentCalendar();
        long dateNowLong = dateNow.getTimeInMillis();
        long cust2DayAgo = TimeConvertion.addDays(dateNowLong, -2); // 2 day ago and no update  
        for (int i = 0; i < custList.size(); i++) {
            CustomerObj custObj = (CustomerObj) custList.get(i);
            if (custObj.getStatus() == ConstantKey.DISABLE) {
                continue;
            }
            float bal = custObj.getBalance();
            float payment = custObj.getPayment();
            float outstand = bal - payment;
            if (outstand >= 0) {  //No out standing payment 
                continue;
            }
            if (custObj.getUpdatedatel() < cust2DayAgo) {
                // disable customer
                custObj.setStatus(ConstantKey.DISABLE);
            }

            serviceAFWeb.updateCustStatusSubStatus(custObj.getUsername(), custObj.getStatus() + "", custObj.getSubstatus() + "");

            String tzid = "America/New_York"; //EDT
            TimeZone tz = TimeZone.getTimeZone(tzid);
            AccountObj accountAdminObj = serviceAFWeb.getAdminObjFromCache();
            java.sql.Date d = new java.sql.Date(dateNowLong);
//                                DateFormat format = new SimpleDateFormat("M/dd/yyyy hh:mm a z");
            DateFormat format = new SimpleDateFormat(" hh:mm a");
            format.setTimeZone(tz);
            String ESTdate = format.format(d);

            String msg = ESTdate + " " + custObj.getUsername() + " Customer disabled - 2 day after expired.";
            this.AddCommMessage(serviceAFWeb, accountAdminObj, msg);

        }

    }
//////////////////

    private void ProcessAllLockCleanup() {
        // clean up old lock name
        // clean Lock entry pass 30 min
        ArrayList<AFLockObject> lockArray = serviceAFWeb.getAllLock();
        Calendar dateNow = TimeConvertion.getCurrentCalendar();
        int numCnt = 0;
        if (lockArray != null) {
            for (int i = 0; i < lockArray.size(); i++) {
                AFLockObject lockObj = lockArray.get(i);
                long lastUpdate = lockObj.getLockdatel();
                long lastUpdateAdd30 = TimeConvertion.addMinutes(lastUpdate, 30); // remove lock for 30min

                if (lockObj.getType() == ConstantKey.ADMIN_SIGNAL_LOCKTYPE) {
                    // TR Best takes a long time
                    lastUpdateAdd30 = TimeConvertion.addMinutes(lastUpdate, StockDB.MaxMinuteAdminSignalTrading); // remove lock for 90min
                }
                if (lastUpdateAdd30 < dateNow.getTimeInMillis()) {
                    serviceAFWeb.removeNameLock(lockObj.getLockname(), lockObj.getType());
                    numCnt++;
                    if (numCnt > 10) {
                        break;
                    }
                }
            }
        }
    }

    private void ProcessStockkMaintance() {
        // delete stock based on all customer account exclude the ADMIN_USERNAME account 
        // do Simulation trading
        logger.info("> ProcessStockkMaintance ");

        AccountObj accountAdminObj = serviceAFWeb.getAdminObjFromCache();
        if (accountAdminObj == null) {
            return;
        }

        ArrayList StockNameList = serviceAFWeb.getAllOpenStockNameArray();

        if (StockNameList == null) {
            return;
        }

        ArrayList AllAccountStockNameList = serviceAFWeb.SystemAllAccountStockNameListExceptionAdmin(accountAdminObj.getId());

        if (AllAccountStockNameList == null) {
            return;
        }
        ArrayList addedList = new ArrayList();
        ArrayList removeList = new ArrayList();
        int numCnt = 0;
        boolean result = compareStockList(AllAccountStockNameList, StockNameList, addedList, removeList);
        if (result == true) {
            //addedList should be 0
            for (int i = 0; i < removeList.size(); i++) {
                String NormalizeSymbol = (String) removeList.get(i);
                logger.info("> ProcessStockkMaintance remove stock " + NormalizeSymbol);
                serviceAFWeb.disableStock(NormalizeSymbol);
                numCnt++;
                if (numCnt > 10) {
                    break;
                }
            }
        }
    }

    public void ProcessFundAccount(ServiceAFweb serviceAFWeb) {
        // add or remove stock in mutual fund account based on all stocks in the system
        //        logger.info("> ProcessFundAccount ......... ");

        this.serviceAFWeb = serviceAFWeb;

//        logger.info("> UpdateAccountSignal ");
        AccountObj accountAdminObj = serviceAFWeb.getAdminObjFromCache();
        if (accountAdminObj == null) {
            return;
        }
        if (accountFundIdNameArray == null) {
            accountFundIdNameArray = new ArrayList();
        }
        if (accountFundIdNameArray.size() == 0) {
            ArrayList accountIdList = serviceAFWeb.SystemAllOpenAccountIDList();
            if (accountIdList == null) {
                return;
            }
            accountFundIdNameArray = accountIdList;
        }
        Calendar dateNow = TimeConvertion.getCurrentCalendar();
        long lockDateValue = dateNow.getTimeInMillis();
        String LockName = "ALL_FUND";
        long lockReturn = serviceAFWeb.setLockNameProcess(LockName, ConstantKey.FUND_LOCKTYPE, lockDateValue, ServiceAFweb.getServerObj().getSrvProjName() + "_ProcessAllAccountTradingSignal");
        if (CKey.NN_DEBUG == true) {
            lockReturn = 1;
        }
        if (lockReturn > 0) {

            long currentTime = System.currentTimeMillis();
            long lockDate2Min = TimeConvertion.addMinutes(currentTime, 2);

            for (int k = 0; k < 10; k++) {
                currentTime = System.currentTimeMillis();
                if (lockDate2Min < currentTime) {
                    break;
                }
                if (accountFundIdNameArray.size() == 0) {
                    break;
                }
                try {
                    String accountIdSt = (String) accountFundIdNameArray.get(0);
                    accountFundIdNameArray.remove(0);
                    int accountId = Integer.parseInt(accountIdSt);
                    AccountObj accountObj = serviceAFWeb.getAccountImp().getAccountObjByAccountID(accountId);
                    if (accountObj.getType() == AccountObj.INT_MUTUAL_FUND_ACCOUNT) {
                        ProcessFundAccountUpdate(serviceAFWeb, accountObj);
                    }
                } catch (Exception e) {
                    logger.info("> ProcessFundAccount Exception " + e.getMessage());
                }
            }
            serviceAFWeb.removeNameLock(LockName, ConstantKey.FUND_LOCKTYPE);
        }
    }

    public int ProcessFundAccountUpdate(ServiceAFweb serviceAFWeb, AccountObj accountObj) {
        String portfolio = accountObj.getPortfolio();
        FundM fundMgr = null;
        try {
            portfolio = portfolio.replaceAll("#", "\"");
            fundMgr = new ObjectMapper().readValue(portfolio, FundM.class);
        } catch (Exception ex) {
        }

        if (fundMgr == null) {
            fundMgr = new FundM();
            try {
                String portfStr = new ObjectMapper().writeValueAsString(fundMgr);
                serviceAFWeb.getAccountImp().updateAccountPortfolio(accountObj.getAccountname(), portfStr);
            } catch (JsonProcessingException ex) {
            }
        }

        ArrayList portfolioArray = fundMgr.getFunL();

        ArrayList accountList = serviceAFWeb.getAccountImp().getAccountListByCustomerId(accountObj.getCustomerid());
        if (accountList == null) {
            return 0;
        }
        for (int k = 0; k < accountList.size(); k++) {
            AccountObj accObj = (AccountObj) accountList.get(k);
            if (accObj.getType() == AccountObj.INT_TRADING_ACCOUNT) {
                ArrayList AccountStockNameList = serviceAFWeb.SystemAccountStockNameList(accObj.getId());
                if (AccountStockNameList == null) {
                    return 0;
                }

                int numCnt = 0;
                ArrayList addedList = new ArrayList();
                ArrayList removeList = new ArrayList();
                boolean result = compareStockList(portfolioArray, AccountStockNameList, addedList, removeList);
                if (result == true) {
                    for (int i = 0; i < addedList.size(); i++) {
                        String symbol = (String) addedList.get(i);
                        int resultAdd = serviceAFWeb.addAccountStockSymbol(accObj, symbol);
                        if (resultAdd > 0) {
                            logger.info("> ProcessFundAccount add TR stock " + accObj.getAccountname() + " " + symbol
                            );
                        }
                        numCnt++;
                        if (numCnt > 10) {
                            break;
                        }
                        ServiceAFweb.AFSleep();

                    }
                    /////////
                    for (int i = 0; i < removeList.size(); i++) {
                        String symbol = (String) removeList.get(i);
                        TrandingSignalProcess TRprocessImp = new TrandingSignalProcess();
                        AFstockObj stock = serviceAFWeb.getStockImp().getRealTimeStock(symbol, null);
                        if (stock == null) {
                            continue;
                        }
                        int resultRemove = serviceAFWeb.removeAccountStockSymbol(accObj, symbol);
                        if (resultRemove > 0) {
                            logger.info("> ProcessFundAccount remove TR stock " + accObj.getAccountname() + " " + symbol);
                        }
                        numCnt++;
                        if (numCnt > 10) {
                            break;
                        }
                        ServiceAFweb.AFSleep();
                    }
                }
                return 1;
            }
        }
        return 0;
    }

    public void ProcessAdminAccount(ServiceAFweb serviceAFWeb) {
        // add or remove stock in ADMIN_USERNAME account based on all stocks in the system
//        logger.info("> ProcessAdminAccount ......... ");

        AccountObj accountAdminObj = serviceAFWeb.getAdminObjFromCache();
        if (accountAdminObj == null) {
            return;
        }

        ArrayList StockNameList = serviceAFWeb.getAllOpenStockNameArray();

        if (StockNameList == null) {
            return;
        }

        ArrayList AccountStockNameList = serviceAFWeb.SystemAccountStockNameList(accountAdminObj.getId());
        if (AccountStockNameList == null) {
            return;
        }

        ServiceAFweb.getServerObj().setTotalStock(StockNameList.size());
        ServiceAFweb.getServerObj().setTotalStockAcc(AccountStockNameList.size());

        int numCnt = 0;
        ArrayList addedList = new ArrayList();
        ArrayList removeList = new ArrayList();
        boolean result = compareStockList(StockNameList, AccountStockNameList, addedList, removeList);
        if (result == true) {
            for (int i = 0; i < addedList.size(); i++) {
                String symbol = (String) addedList.get(i);
                int resultAdd = serviceAFWeb.addAccountStock(CKey.ADMIN_USERNAME, null, accountAdminObj.getId() + "", symbol);
                logger.info("> ProcessAdminAccount add TR stock " + symbol);
                numCnt++;
                if (numCnt > 10) {
                    break;
                }
                ServiceAFweb.AFSleep();

            }
            /////////
            for (int i = 0; i < removeList.size(); i++) {
                String symbol = (String) removeList.get(i);
                int resultRemove = serviceAFWeb.removeAccountStock(CKey.ADMIN_USERNAME, null, accountAdminObj.getId() + "", symbol);
                logger.info("> ProcessAdminAccount remove TR stock " + symbol);
                numCnt++;
                if (numCnt > 10) {
                    break;
                }

                ServiceAFweb.AFSleep();

            }
        }
    }

    public static boolean compareStockList(ArrayList<String> masterList, ArrayList<String> current, ArrayList<String> addedList, ArrayList<String> removeList) {
        try {
            for (String a : masterList) {
                if (!current.contains(a)) {
                    addedList.add(a);
                }
            }
            for (String a : current) {
                if (!masterList.contains(a)) {
                    removeList.add(a);
                }
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }
////////////////////////////////////////

    // for all account 
    public void ProcessAllAccountTradingSignal(ServiceAFweb serviceAFWeb) {
        this.serviceAFWeb = serviceAFWeb;
        TrandingSignalProcess TRprocessImp = new TrandingSignalProcess();
        logger.info("> ProcessAllAccountTradingSignal ");
        AccountObj accountAdminObj = serviceAFWeb.getAdminObjFromCache();
        if (accountAdminObj == null) {
            return;
        }
        if (accountIdNameArray == null) {
            accountIdNameArray = new ArrayList();
        }
        if (accountIdNameArray.size() == 0) {
            ArrayList accountIdList = serviceAFWeb.SystemAllOpenAccountIDList();
            if (accountIdList == null) {
                return;
            }
            accountIdNameArray = accountIdList;
        }
        Calendar dateNow = TimeConvertion.getCurrentCalendar();
        long lockDateValue = dateNow.getTimeInMillis();
        String LockName = "ALL_SIGNAL";
        long lockReturn = serviceAFWeb.setLockNameProcess(LockName, ConstantKey.SIGNAL_LOCKTYPE, lockDateValue, ServiceAFweb.getServerObj().getSrvProjName() + "_ProcessAllAccountTradingSignal");
        if (CKey.NN_DEBUG == true) {
            lockReturn = 1;
        }
        if (lockReturn > 0) {
            int maxAccountCnt = 0;
            long LastServUpdateTimer = System.currentTimeMillis();

            // update Trading signal
            while (accountIdNameArray.size() > 0) {
                String accountIDSt = (String) accountIdNameArray.get(0);
                accountIdNameArray.remove(0);
                int accountId = Integer.parseInt(accountIDSt);
                // ignore admin acount. process all user account
                if (accountId == accountAdminObj.getId()) {
                    continue;
                }
                AccountObj accountObj = serviceAFWeb.SystemAccountObjByAccountID(accountId);
                if (accountObj == null) {
                    continue;
                }

                ArrayList stockNameArray = serviceAFWeb.SystemAccountStockNameList(accountId);
                if (stockNameArray == null) {
                    continue;
                }
                long currentTime = System.currentTimeMillis();
                long lockDate2Min = TimeConvertion.addMinutes(LastServUpdateTimer, 2); // add 2 minutes
                if (CKey.NN_DEBUG == true) {
                    currentTime = 0;
                }
                if (lockDate2Min < currentTime) {
                    break;
                }
//                logger.info("> ProcessAllAccountTradingSignal " + accountObj.getAccountname());
                for (int j = 0; j < stockNameArray.size(); j++) {
                    String symbol = (String) stockNameArray.get(j);
                    int ret = TRprocessImp.updateStockProcess(serviceAFWeb, symbol);
                    if (ret > 0) {
                        updateTradingsignal(serviceAFWeb, accountAdminObj, accountObj, symbol);
                        upateTradingTransaction(serviceAFWeb, accountObj, symbol);
                    }
                }
                maxAccountCnt++;
                if (maxAccountCnt > 10) {
                    break;
                }
            }
            serviceAFWeb.removeNameLock(LockName, ConstantKey.SIGNAL_LOCKTYPE);
        }

    }

    public void updateTradingsignal(ServiceAFweb serviceAFWeb, AccountObj accountAdminObj, AccountObj accountObj, String symbol) {

        // update Trading signal
        ArrayList<TradingRuleObj> tradingRuleAdminList = serviceAFWeb.SystemAccountStockListByAccountID(accountAdminObj.getId(), symbol);

        ArrayList<TradingRuleObj> tradingRuleList = serviceAFWeb.SystemAccountStockListByAccountID(accountObj.getId(), symbol);

        if ((tradingRuleList == null) || (tradingRuleAdminList == null)) {
            return;
        }
        TradingRuleObj trTradingACCObj = null;
        // update Trading signal
        ArrayList<TradingRuleObj> UpdateTRList = new ArrayList();
        Calendar dateNowUpdate = TimeConvertion.getCurrentCalendar();

        for (int j = 0; j < tradingRuleList.size(); j++) {

            TradingRuleObj trObj = tradingRuleList.get(j);

            if (trObj.getType() == ConstantKey.INT_TR_ACC) {
                trObj.setUpdatedatedisplay(new java.sql.Date(dateNowUpdate.getTimeInMillis()));
                trObj.setUpdatedatel(dateNowUpdate.getTimeInMillis());
                trTradingACCObj = trObj;
                continue;
            }

            for (int k = 0; k < tradingRuleAdminList.size(); k++) {
                TradingRuleObj trAdminObj = tradingRuleAdminList.get(k);
                if (trObj.getType() == trAdminObj.getType()) {
                    trObj.setStatus(trAdminObj.getStatus());
                    trObj.setSubstatus(trAdminObj.getSubstatus());
                    trObj.setTrsignal(trAdminObj.getTrsignal());
                    trObj.setUpdatedatel(trAdminObj.getUpdatedatel());
                    trObj.setUpdatedatedisplay(new java.sql.Date(trAdminObj.getUpdatedatel()));

                    trObj.setInvestment(trAdminObj.getInvestment());
                    trObj.setBalance(trAdminObj.getBalance());
                    trObj.setLongshare(trAdminObj.getLongshare());
                    trObj.setLongamount(trAdminObj.getLongamount());
                    trObj.setShortamount(trAdminObj.getShortamount());
                    trObj.setShortshare(trAdminObj.getShortshare());

                    trObj.setComment(trAdminObj.getComment());

                    UpdateTRList.add(trObj);
                }
            }
        }
        if (trTradingACCObj != null) {
            int subStatus = trTradingACCObj.getSubstatus();

            if (subStatus == ConstantKey.INITIAL) {
                serviceAFWeb.SystemAccountStockClrTranByAccountID(accountObj, trTradingACCObj.getStockid(), trTradingACCObj.getTrname());
                // udpate tr Status to open

                trTradingACCObj.setSubstatus(ConstantKey.OPEN);
                String updateSQL = AccountDB.SQLUpdateAccountStockStatus(trTradingACCObj);
                ArrayList sqlList = new ArrayList();
                sqlList.add(updateSQL);
                serviceAFWeb.SystemUpdateSQLList(sqlList);
            }

            int trLinkId = trTradingACCObj.getLinktradingruleid();
            if (trLinkId != 0) {
                boolean readySignal = true;

                //Make sure all admin links are ready before copy the link signal
                //Make sure all admin links are ready before copy the link signal
                for (int j = 0; j < tradingRuleAdminList.size(); j++) {
                    TradingRuleObj trAdminObj = (TradingRuleObj) tradingRuleAdminList.get(j);
                    //Make sure all admin links are ready before copy the link signal
                    if ((trAdminObj.getType() >= ConstantKey.INT_TR_MV) && (trAdminObj.getType() <= ConstantKey.INT_TR_NN2)) {
                        if (trAdminObj.getSubstatus() != ConstantKey.OPEN) {
                            readySignal = false;
                            break;
                        }
                    }
                }
                if (readySignal == true) {

                    for (int j = 0; j < tradingRuleAdminList.size(); j++) {
                        TradingRuleObj trAdminObj = (TradingRuleObj) tradingRuleAdminList.get(j);
                        if (trLinkId == trAdminObj.getType()) {
                            if (trAdminObj.getSubstatus() != ConstantKey.OPEN) {
                                break;
                            }
                            if (trTradingACCObj.getTrsignal() != trAdminObj.getTrsignal()) {
                                trTradingACCObj.setTrsignal(trAdminObj.getTrsignal());

                                UpdateTRList.add(trTradingACCObj);
                                String tzid = "America/New_York"; //EDT
                                TimeZone tz = TimeZone.getTimeZone(tzid);
                                java.sql.Date d = new java.sql.Date(trAdminObj.getUpdatedatel());
//                                DateFormat format = new SimpleDateFormat("M/dd/yyyy hh:mm a z");
                                DateFormat format = new SimpleDateFormat(" hh:mm a");
                                format.setTimeZone(tz);
                                String ESTdate = format.format(d);

                                String sig = "exit";
                                if (trAdminObj.getTrsignal() == ConstantKey.S_BUY) {
                                    sig = ConstantKey.S_BUY_ST;
                                } else if (trAdminObj.getTrsignal() == ConstantKey.S_SELL) {
                                    sig = ConstantKey.S_SELL_ST;
                                }
                                String msg = ESTdate + " " + symbol + " Sig:" + sig;
                                this.AddCommMessage(serviceAFWeb, accountObj, trTradingACCObj, msg);
//                                logger.info("> updateTradingsignal update " + msg);
                            }
                            break;
                        }
                    }
                }
            } // copy the admin object for default TR_ACC
        }
        TRObj stockTRObj = new TRObj();
        stockTRObj.setTrlist(UpdateTRList);
        serviceAFWeb.updateAccountStockSignal(stockTRObj);
    }

    public int AddCommObjMessage(ServiceAFweb serviceAFWeb, AccountObj accountObj, CommData commDataObj) {
        try {
            return serviceAFWeb.getAccountImp().addAccountCommMessage(accountObj, commDataObj);

        } catch (Exception e) {
            logger.info("> AddCommMessage exception " + e.getMessage());
        }
        return 0;
    }

    public int AddCommMessage(ServiceAFweb serviceAFWeb, AccountObj accountObj, String messageData) {
        try {
            logger.info("> AddCommMessage  " + messageData);
            return serviceAFWeb.getAccountImp().addAccountMessage(accountObj, messageData);

        } catch (Exception e) {
            logger.info("> AddCommMessage exception " + e.getMessage());
        }
        return 0;
    }

    public int AddCommMessage(ServiceAFweb serviceAFWeb, AccountObj accountObj, TradingRuleObj tr, String messageData) {
        try {

            if (tr.getType() == ConstantKey.INT_TR_ACC) {
                logger.info("> AddCommMessage  " + messageData);
                return serviceAFWeb.getAccountImp().addAccountMessage(accountObj, messageData);
            }

        } catch (Exception e) {
            logger.info("> AddCommMessage exception " + e.getMessage());
        }
        return 0;
    }

    public void upateTradingTransaction(ServiceAFweb serviceAFWeb, AccountObj accountObj, String symbol) {
        TrandingSignalProcess TRprocessImp = new TrandingSignalProcess();
        try {
            AFstockObj stock = serviceAFWeb.getRealTimeStockImp(symbol);
            if (stock != null) {
                if (stock.getSubstatus() == ConstantKey.STOCK_SPLIT) {
                    return;
                }
            }
            TradingRuleObj trObj = serviceAFWeb.SystemAccountStockIDByTRname(accountObj.getId(), stock.getId(), ConstantKey.TR_ACC);

            if (trObj == null) {
                return;
            }
            int subStatus = trObj.getSubstatus();
            if (subStatus == ConstantKey.INITIAL) {
                serviceAFWeb.SystemAccountStockClrTranByAccountID(accountObj, trObj.getStockid(), trObj.getTrname());
            }

            // process performance
            String trName = trObj.getTrname();
            ArrayList<TransationOrderObj> tranOrderList = serviceAFWeb.SystemAccountStockTransList(accountObj.getId(), stock.getId(), trName.toUpperCase(), 0);

            int currentTS = ConstantKey.S_NEUTRAL;
            currentTS = trObj.getTrsignal();
            if ((currentTS == ConstantKey.S_EXIT_LONG) || (currentTS == ConstantKey.S_EXIT_SHORT)) {
                currentTS = ConstantKey.S_NEUTRAL;
            }

            int orderTS = ConstantKey.S_NEUTRAL;
            if ((tranOrderList != null) && (tranOrderList.size() > 0)) {
                TransationOrderObj tranOrder = tranOrderList.get(0);

                orderTS = tranOrder.getTrsignal();
                if ((orderTS == ConstantKey.S_EXIT_LONG) || (orderTS == ConstantKey.S_EXIT_SHORT)) {
                    orderTS = ConstantKey.S_NEUTRAL;
                }
            }

            if (currentTS != orderTS) {
                int ret = TRprocessImp.AddTransactionOrder(serviceAFWeb, accountObj, stock, trObj.getTrname(), trObj.getTrsignal(), null, true);
//                newTran = serviceAFWeb.SystemAddTransactionOrder(accountObj, stock, trObj.getTrname(), trObj.getTrsignal(), null);
            }
            // Get transaction again process performance
            tranOrderList = serviceAFWeb.SystemAccountStockTransList(accountObj.getId(), stock.getId(), trName.toUpperCase(), 0);
            if ((tranOrderList != null) && (tranOrderList.size() > 0)) {

                ArrayList<PerformanceObj> performanceList = TRprocessImp.ProcessTranPerfHistory(serviceAFWeb, tranOrderList, stock, 1);
                if (performanceList != null) {
                    if (performanceList.size() == 1) {
                        PerformanceObj pObj = performanceList.get(0);
                        pObj.setAccountid(accountObj.getId());
                        pObj.setStockid(stock.getId());
                        pObj.setTradingruleid(trObj.getId());
                        ArrayList<PerformanceObj> currentPerfList = serviceAFWeb.SystemAccountStockPerfList(accountObj.getId(), stock.getId(), trObj.getTrname(), 1);
                        String SQLPerf = "";
                        if ((currentPerfList != null) && (currentPerfList.size() > 0)) {
                            PerformanceObj currentpObj = currentPerfList.get(0);
                            pObj.setId(currentpObj.getId());
                            SQLPerf = AccountDB.SQLUpdateAccountStockPerformance(pObj);
                        } else {
                            SQLPerf = AccountDB.SQLaddAccountStockPerformance(pObj);
                        }
                        ArrayList sqlList = new ArrayList();
                        sqlList.add(SQLPerf);
                        serviceAFWeb.SystemUpdateSQLList(sqlList);
                    }
                }
            }

        } catch (Exception ex) {
            logger.info("> upateTradingTransaction Exception" + ex.getMessage());
        }
    }

//    public void testCalculateSignal(String symbol) {
//        AFstockObj stock = serviceAFWeb.getRealTimeStockImp(symbol);
//        Calendar dateNow = TimeConvertion.getCurrentCalendar();
//        long endofDate = TimeConvertion.endOfDayInMillis(dateNow.getTimeInMillis());
//        dateNow = TimeConvertion.getCurrentCalendar(endofDate);
//
//        int size1year = 5 * 52 * 2;
//        ArrayList StockArray = serviceAFWeb.getStockHistorical(stock.getSymbol(), size1year);
//        if (StockArray == null) {
//            return;
//        }
//        if (StockArray.size() < 10) {
//            return;
//        }
//        double high = 0;
//        double low = 999999;
//        for (int k = 0; k < StockArray.size(); k++) {
//            AFstockInfo stockinfo = (AFstockInfo) StockArray.get(k);
//            if (stockinfo.getFclose() > high) {
//                high = stockinfo.getFclose();
//            }
//            if (stockinfo.getFclose() < low) {
//                low = stockinfo.getFclose();
//            }
//        }
//
//        ArrayList<String> writeArray = new ArrayList();
//        String dupLastSt = "";
//
//        for (int i = 0; i < 20 * 17; i++) { // 7 months
//            long endofDayL = TimeConvertion.addDays(dateNow.getTimeInMillis(), -i);
//            Date stockInfofirstEndday = TimeConvertion.endOfDay(new Date(endofDayL));
//            endofDayL = stockInfofirstEndday.getTime();
//            int offset = getOffetDate(StockArray, endofDayL);
//
//            AFstockInfo stockinfo = (AFstockInfo) StockArray.get(offset);
//
//            float STerm = (float) TechnicalCal.TrendUpDown(StockArray, offset, StockImp.SHORT_TERM_TREND);
//            float LTerm = (float) TechnicalCal.TrendUpDown(StockArray, offset, StockImp.LONG_TERM_TREND);
//            ADXObj adx = TechnicalCal.AvgDir(StockArray, offset, StockImp.SHORT_TERM_TREND);
//
//            EMAObj ema2050 = TechnicalCal.EMASignal(StockArray, offset, 10, 50);
//            RSIObj rsi = TechnicalCal.RSI(StockArray, offset, 14);
//            MACDObj macd = TechnicalCal.MACD(StockArray, offset, 12, 26, 9);
//
//            String dupSt = "" + stockinfo.getEntrydatedisplay() + stockinfo.getFclose();
//            if (dupLastSt.equals(dupSt)) {
//                continue;
//            }
//            dupLastSt = dupSt;
//            Calendar setDate = TimeConvertion.getCurrentCalendar(endofDayL);
//            String stdate = new Timestamp(setDate.getTime().getTime()).toString();
//            stdate = stdate.substring(0, 10);
//            if (writeArray.size() == 0) {
//                String stTitle = "\"date" + "\",\"date" + "\",\"close" + "\",\"normalclose" + "\",\"long" + "\",\"short" + "\",\"avgdir" + "\",\"avgdir.TR" + "\"";
//                stTitle += ",\"ema2050.ema" + "\",\"ema2050.trsignal" + "\",\"rsi" + "\",\"rsi.TR" + "\",\"macd" + "\",\"macd.TR" + "\"";
//                writeArray.add(stTitle);
//            }
//            double normalClose = TechnicalCal.getNormalize100(stockinfo.getFclose(), high, low);
//            String st = "\"" + stdate + "\",\"" + stockinfo.getEntrydatedisplay() + "\",\"" + stockinfo.getFclose() + "\",\"" + normalClose + "\",\"" + LTerm + "\",\"" + STerm + "\",\"" + adx.adx + "\",\"" + adx.trsignal + "\"";
//            st += ",\"" + ema2050.ema + "\",\"" + ema2050.trsignal + "\",\"" + rsi.rsi + "\",\"" + rsi.trsignal + "\",\"" + macd.macd + "\",\"" + macd.trsignal + "\"";
//
//            writeArray.add(st);
//        }
//        FileUtil.FileWriteTextArray(LocalPCSignalPath + symbol + ".csv", writeArray);
//    }
    public static int getOffetDate(ArrayList StockArray, long offsetDate) {
        int offset = 0;
        if (StockArray == null) {
            return 0;
        }
        if (offsetDate == 0) {
            return 0;
        }
        for (int i = 0; i < StockArray.size(); i++) {
            AFstockInfo stocktmp = (AFstockInfo) StockArray.get(i);
            if (stocktmp.getEntrydatel() <= offsetDate) {
                break;
            }
            offset++;
        }

        if ((StockArray.size() - offset) < 50) {
            offset = 0;
        }
        return offset;
    }

    //https://ca.finance.yahoo.com/quote/T.TO/history?period1=1200441600&period2=1583539200&interval=1d&filter=history&frequency=1d
    public void updateAllStockFile(ServiceAFweb serviceAFWeb) {
        
        updateStockFile(serviceAFWeb, "AEM");
        updateStockFile(serviceAFWeb, "BABA");
        updateStockFile(serviceAFWeb, "INTC");
        updateStockFile(serviceAFWeb, "NFLX");
        updateStockFile(serviceAFWeb, "TMO");        
        updateStockFile(serviceAFWeb, "ABT");
        updateStockFile(serviceAFWeb, "AMT");
        updateStockFile(serviceAFWeb, "AMZN");
        updateStockFile(serviceAFWeb, "BABA");
        updateStockFile(serviceAFWeb, "FB");
        updateStockFile(serviceAFWeb, "FNV");
        updateStockFile(serviceAFWeb, "MSFT");
        updateStockFile(serviceAFWeb, "NEM");
        updateStockFile(serviceAFWeb, "ROG");
        updateStockFile(serviceAFWeb, "V");
        updateStockFile(serviceAFWeb, "XLNX");


        
////////////////////////////////////////////////////        
        updateStockFile(serviceAFWeb, "SPY");
        updateStockFile(serviceAFWeb, "DIA");
        updateStockFile(serviceAFWeb, "QQQ");
        updateStockFile(serviceAFWeb, "HOU.TO");
        updateStockFile(serviceAFWeb, "HOD.TO");
        updateStockFile(serviceAFWeb, "T.TO");
        updateStockFile(serviceAFWeb, "FAS");
        updateStockFile(serviceAFWeb, "FAZ");
        updateStockFile(serviceAFWeb, "XIU.TO");
        updateStockFile(serviceAFWeb, "RY.TO");
        updateStockFile(serviceAFWeb, "AAPL");
    }

    public boolean updateStockFile(ServiceAFweb serviceAFWeb, String NormalizeSymbol) {
        this.serviceAFWeb = serviceAFWeb;
        ArrayList inputArray = new ArrayList();
        String nnFileName = ServiceAFweb.FileLocalPath + NormalizeSymbol + ".csv";
        if (FileUtil.FileTest(nnFileName) == false) {
            logger.info("updateStockFile not found " + nnFileName);
            return false;
        }

        AFstockObj stock = serviceAFWeb.getRealTimeStockImp(NormalizeSymbol);
        serviceAFWeb.getStockImp().deleteStockInfoByStockId(stock);

        ArrayList<AFstockInfo> StockArray = new ArrayList();
        boolean ret = FileUtil.FileReadTextArray(nnFileName, inputArray);
        if (ret == true) {
            int LineNum = 0;
            String inLine = "";
            for (int i = 0; i < inputArray.size(); i++) {
                inLine = (String) inputArray.get(i);
                LineNum++;
                //Date,Open,High,Low,Close,Adj Close,Volume
                if (inLine.indexOf("Date,Open") != -1) {
                    continue;
                }
                //1995-04-14,null,null,null,null,null,null
                if (inLine.indexOf("null,null,null") != -1) {
                    continue;
                }
                if (inLine.indexOf("Dividend") != -1) {
                    continue;
                }
                if (inLine.indexOf("Stock Split") != -1) {
                    break;
                }
                if (inLine.indexOf("-,-,-,-,-") != -1) {
                    continue;
                }
                AFstockInfo StockD = StockInfoUtils.parseCSVLine(inLine);
                if (StockD == null) {
                    logger.info("updateStockFile Exception " + NormalizeSymbol + " " + inLine);
                    break;
                }

                StockArray.add(StockD);
            }
            logger.info("updateStockFile  " + NormalizeSymbol + " " + StockArray.size());
            if (StockArray.size() == 0) {
                return false;
            }
            ArrayList<AFstockInfo> StockSendArray = new ArrayList();
            int index = 0;
//            Collections.reverse(StockArray);
            for (int i = 0; i < StockArray.size(); i++) {

                StockSendArray.add(StockArray.get(i));
                index++;
                if (index > 99) {
                    index = 0;
//                    Collections.reverse(StockSendArray);
                    StockInfoTranObj stockInfoTran = new StockInfoTranObj();
                    stockInfoTran.setNormalizeName(NormalizeSymbol);
                    stockInfoTran.setStockInfoList(StockSendArray);
                    // require oldest date to earliest
                    // require oldest date to earliest
                    int retTran = serviceAFWeb.updateStockInfoTransaction(stockInfoTran);
                    if (retTran == 0) {
                        return false;
                    }
                    StockSendArray.clear();
                }

            }
//            Collections.reverse(StockSendArray);
            StockInfoTranObj stockInfoTran = new StockInfoTranObj();
            stockInfoTran.setNormalizeName(NormalizeSymbol);
            stockInfoTran.setStockInfoList(StockSendArray);
//                logger.info("updateRealTimeStock send " + StockSendArray.size());

            // require oldest date to earliest
            // require oldest date to earliest
            serviceAFWeb.updateStockInfoTransaction(stockInfoTran);
        }
        return true;
    }

////////////////////////////////////////    
////////////////uploadDBData////////////////////////    
////////////////uploadDBData////////////////////////        
////////////////////////////////////////    
//    public static String ServiceAFweb.FileLocalPath = "T:/Netbean/db/";
    public boolean restoreDBData(ServiceAFweb serviceAFWeb) {
        this.serviceAFWeb = serviceAFWeb;

        if (FileUtil.FileTest(ServiceAFweb.FileLocalPath + "customer.txt") == false) {
            return false;
        }

        int ret = restoreDBcustomer();
        if (ret == 0) {
            return false;
        }
        restoreDBaccount();
        restoreDBstock();
        restoreDBaccountstock_tradingrule();
        restoreDBneuralnet();

        restoreDBtransationorder();
        restoreDBcomm();
        restoreDBbilling();
        restoreDBperformance();
        restoreDBstockinfo();
        restoreDBdummy();

        return true;

    }

    private int restoreDBdummy() {

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

    private int restoreDBneuralnet() {

        int ret = restoreDBneuralnetProcess("neuralnet");
        ret = restoreDBneuralnetProcess("neuralnet1");
        ret = restoreDBneuralnetDataProcess("neuralnetdata");
        return ret;
    }

    private int restoreDBneuralnetProcess(String tableName) {

        try {
            ArrayList<String> writeArray = new ArrayList();
            String fName = ServiceAFweb.FileLocalPath + tableName + ".txt";
            if (FileUtil.FileTest(fName) == false) {
                return 0;
            }
            FileUtil.FileReadTextArray(fName, writeArray);
            ArrayList<String> writeSQLArray = new ArrayList();
            logger.info("> restoreDBneuralnet " + tableName + " " + writeArray.size());
            int index = 0;
            for (int i = 0; i < writeArray.size(); i++) {
                String output = writeArray.get(i);
                AFneuralNet item = new ObjectMapper().readValue(output, AFneuralNet.class);
                String sql = StockDB.insertNeuralNet(tableName, item);
                writeSQLArray.add(sql);
                index++;
                if (index > 5) {
                    index = 0;
                    int ret = sendRequestObj(writeSQLArray);
                    if (ret == 0) {
                        return 0;
                    }
                    writeSQLArray.clear();
                    ServiceAFweb.AFSleep();
                }

            }
            return sendRequestObj(writeSQLArray);

        } catch (IOException ex) {
            logger.info("> restoreDBneuralnetProcess - exception " + ex);
        }
        return 0;
    }

    private int restoreDBneuralnetDataProcess(String tableName) {

        try {
            ArrayList<String> writeArray = new ArrayList();
            String fName = ServiceAFweb.FileLocalPath + tableName + ".txt";
            if (FileUtil.FileTest(fName) == false) {
                return 0;
            }
            FileUtil.FileReadTextArray(fName, writeArray);
            ArrayList<String> writeSQLArray = new ArrayList();
            logger.info("> restoreDBneuralnetData " + tableName + " " + writeArray.size());
            int index = 0;
            for (int i = 0; i < writeArray.size(); i++) {
                String output = writeArray.get(i);
                AFneuralNetData item = new ObjectMapper().readValue(output, AFneuralNetData.class);
                String sql = StockDB.insertNeuralNetData(tableName, item);
                writeSQLArray.add(sql);
                index++;
                if (index > 500) {
                    index = 0;
                    int ret = sendRequestObj(writeSQLArray);
                    if (ret == 0) {
                        return 0;
                    }
                    writeSQLArray.clear();
                    ServiceAFweb.AFSleep();
                }

            }
            return sendRequestObj(writeSQLArray);

        } catch (IOException ex) {
            logger.info("> restoreDBneuralnetDataProcess - exception " + ex);
        }
        return 0;
    }

    private int restoreDBstockinfo() {
        int fileCont = 0;
        String tableName = "stockinfo";
        int ret = 0;
        while (true) {
            String fileName = ServiceAFweb.FileLocalPath + tableName + "_" + fileCont + ".txt";
            if (FileUtil.FileTest(fileName) == false) {
                break;
            }
            ret = restoreDBstockinfo(fileCont);
            fileCont++;
        }
        return ret;
    }

    private int restoreDBstockinfo(int fileCont) {
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
                String sql = StockDB.insertStockInfo(item);
                writeSQLArray.add(sql);
                index++;
                if (index > 500) {
                    index = 0;
                    int ret = sendRequestObj(writeSQLArray);
                    if (ret == 0) {
                        return 0;
                    }
                    writeSQLArray.clear();
                    logger.info("> restoreDBstockinfo " + fileName + " total=" + writeArray.size() + " index=" + i);

                    ServiceAFweb.AFSleep();
                }

            }
            return sendRequestObj(writeSQLArray);

        } catch (IOException ex) {
            logger.info("> restoreDBaccount - exception " + ex);
        }
        return 0;
    }

    private int sendRequestObj(ArrayList<String> writeSQLArray) {
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

    private int restoreDBstock() {
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
            return sendRequestObj(writeSQLArray);

        } catch (IOException ex) {
            logger.info("> restoreDBaccount - exception " + ex);
        }
        return 0;
    }

    private int restoreDBaccount() {
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
            return sendRequestObj(writeSQLArray);

        } catch (IOException ex) {
            logger.info("> restoreDBaccount - exception " + ex);
        }
        return 0;
    }

    private int restoreDBcustomer() {
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
            return sendRequestObj(writeSQLArray);

        } catch (IOException ex) {
            logger.info("> restoreDBcustomer - exception " + ex);
        }
        return 0;
    }

    private int restoreDBaccountstock_tradingrule() {
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
                    int ret = sendRequestObj(writeSQLArray);
                    if (ret == 0) {
                        return 0;
                    }
                    writeSQLArray.clear();
                    ServiceAFweb.AFSleep();
                }

            }
            return sendRequestObj(writeSQLArray);

        } catch (IOException ex) {
            logger.info("> restoreDBaccountstock - exception " + ex);
        }
        return 0;
    }

    private int restoreDBbilling() {
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
                    int ret = sendRequestObj(writeSQLArray);
                    if (ret == 0) {
                        return 0;
                    }
                    writeSQLArray.clear();
                    logger.info("> restoreDBbilling " + fileName + " total=" + writeArray.size() + " index=" + i);

                    ServiceAFweb.AFSleep();
                }

            }
            return sendRequestObj(writeSQLArray);

        } catch (IOException ex) {
            logger.info("> restoreDBbilling - exception " + ex);
        }
        return 0;
    }

    private int restoreDBcomm() {
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
                    int ret = sendRequestObj(writeSQLArray);
                    if (ret == 0) {
                        return 0;
                    }
                    writeSQLArray.clear();
                    logger.info("> restoreDBcomm " + fileName + " total=" + writeArray.size() + " index=" + i);

                    ServiceAFweb.AFSleep();
                }

            }
            return sendRequestObj(writeSQLArray);

        } catch (IOException ex) {
            logger.info("> restoreDBcomm - exception " + ex);
        }
        return 0;
    }

    private int restoreDBtransationorder() {
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
                if (index > 500) {
                    index = 0;
                    int ret = sendRequestObj(writeSQLArray);
                    if (ret == 0) {
                        return 0;
                    }
                    writeSQLArray.clear();
                    logger.info("> restoreDBtransationorder " + fileName + " total=" + writeArray.size() + " index=" + i);

                    ServiceAFweb.AFSleep();
                }

            }
            return sendRequestObj(writeSQLArray);

        } catch (IOException ex) {
            logger.info("> restoreDBtransationorder - exception " + ex);
        }
        return 0;
    }

    private int restoreDBperformance() {
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
            return sendRequestObj(writeSQLArray);

        } catch (IOException ex) {
            logger.info("> restoreDBperformance - exception " + ex);
        }
        return 0;
    }

    /////////////////////////////////////////////////////////// 
    ////////////////downloadDBData/////////////////////////////////////////// 
    ///////////////downloadDBData//////////////////////////////////////////// 
    /////////////////////////////////////////////////////////// 
    public boolean downloadDBDataTest(ServiceAFweb serviceAFWeb) {
        this.serviceAFWeb = serviceAFWeb;
        saveDBneuralnetDataProcess("neuralnetdata");
        return true;
    }

    public boolean downloadDBData(ServiceAFweb serviceAFWeb) {
        this.serviceAFWeb = serviceAFWeb;

        saveDBcustomer();
        saveDBaccount();
        saveDBaccountstock_tradingrule();
        saveDBneuralnet();
        saveDBtransationorder();
        saveDBcomm();
        saveDBbilling();
        saveDBperformance();
        saveDBstock();
        saveDBstockinfo();

        return true;
    }

    private int saveDBperformance() {

        String tableName = "performance";
        ArrayList<String> idList = getDBDataTableId(tableName);
        int len = idList.size();
        ArrayList<String> writeArray = new ArrayList();

        if (len > 0) {
            for (int id = 0; id < len; id += 500) {
                String first = idList.get(id);
                if ((id + 500) < len) {
                    String last = idList.get(id - 1 + 500);
                    int ret = saveDBperformance(tableName, first, last, writeArray);
                    if (ret == 0) {
                        return 0;
                    }
                    ServiceAFweb.AFSleep();
                    continue;
                }
                if ((id + 500) >= len) {
                    String last = idList.get(len - 1);
                    int ret = saveDBperformance(tableName, first, last, writeArray);
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

    private int saveDBperformance(String tableName, String first, String last, ArrayList<String> writeArray) {
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

    private int saveDBbilling() {

        String tableName = "billing";
        ArrayList<String> idList = getDBDataTableId(tableName);
        int len = idList.size();
        ArrayList<String> writeArray = new ArrayList();

        if (len > 0) {
            for (int id = 0; id < len; id += 500) {
                String first = idList.get(id);
                if ((id + 500) < len) {
                    String last = idList.get(id - 1 + 500);
                    int ret = saveDBbilling(tableName, first, last, writeArray);
                    if (ret == 0) {
                        return 0;
                    }
                    ServiceAFweb.AFSleep();
                    continue;
                }
                if ((id + 500) >= len) {
                    String last = idList.get(len - 1);
                    int ret = saveDBbilling(tableName, first, last, writeArray);
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

    private int saveDBbilling(String tableName, String first, String last, ArrayList<String> writeArray) {

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

    private int saveDBcomm() {

        String tableName = "comm";
        ArrayList<String> idList = getDBDataTableId(tableName);
        int len = idList.size();
        ArrayList<String> writeArray = new ArrayList();

        if (len > 0) {
            for (int id = 0; id < len; id += 500) {
                String first = idList.get(id);
                if ((id + 500) < len) {
                    String last = idList.get(id - 1 + 500);
                    int ret = saveDBcomm(tableName, first, last, writeArray);
                    if (ret == 0) {
                        return 0;
                    }
                    ServiceAFweb.AFSleep();
                    continue;
                }
                if ((id + 500) >= len) {
                    String last = idList.get(len - 1);
                    int ret = saveDBcomm(tableName, first, last, writeArray);
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

    private int saveDBcomm(String tableName, String first, String last, ArrayList<String> writeArray) {

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

    private int saveDBtransationorder() {

        String tableName = "transationorder";
        ArrayList<String> idList = getDBDataTableId(tableName);
        int len = idList.size();
        ArrayList<String> writeArray = new ArrayList();

        if (len > 0) {
            for (int id = 0; id < len; id += 500) {
                String first = idList.get(id);
                if ((id + 500) < len) {
                    String last = idList.get(id - 1 + 500);
                    int ret = saveDBtransationorder(tableName, first, last, writeArray);
                    if (ret == 0) {
                        return 0;
                    }
                    ServiceAFweb.AFSleep();
                    continue;
                }
                if ((id + 500) >= len) {
                    String last = idList.get(len - 1);
                    int ret = saveDBtransationorder(tableName, first, last, writeArray);
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

    private int saveDBtransationorder(String tableName, String first, String last, ArrayList<String> writeArray) {

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

    private int saveDBaccountstock_tradingrule() {

        String tableName = "tradingrule";
        ArrayList<String> idList = getDBDataTableId(tableName);
        int len = idList.size();
        ArrayList<String> writeArray = new ArrayList();

        if (len > 0) {
            for (int id = 0; id < len; id += 500) {
                String first = idList.get(id);
                if ((id + 500) < len) {
                    String last = idList.get(id - 1 + 500);
                    int ret = saveDBaccountstock(tableName, first, last, writeArray);
                    if (ret == 0) {
                        return 0;
                    }
                    ServiceAFweb.AFSleep();
                    continue;
                }
                if ((id + 500) >= len) {
                    String last = idList.get(len - 1);
                    int ret = saveDBaccountstock(tableName, first, last, writeArray);
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

    private int saveDBaccountstock(String tableName, String first, String last, ArrayList<String> writeArray) {

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
                String st = new ObjectMapper().writeValueAsString(obj);
                writeArray.add(st);
            }
            return 1;

        } catch (Exception ex) {
            logger.info("> saveDBaccountstock " + ex);
        }
        return 0;
    }

    private int saveDBaccount() {
        try {
            String tableName = "account";
            ArrayList<String> idList = getDBDataTableId(tableName);
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

    private int saveDBcustomer() {
        try {
            String tableName = "customer";
            ArrayList<String> idList = getDBDataTableId(tableName);
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

    private int saveDBneuralnet() {
        int ret = saveDBneuralnetProcess("neuralnet");
        ret = saveDBneuralnetProcess("neuralnet1");
        ret = saveDBneuralnetDataProcess("neuralnetdata");
        return ret;
    }

    private int saveDBneuralnetProcess(String tableName) {

        ArrayList<String> idList = getDBDataTableId(tableName);
        int len = idList.size();
        ArrayList<String> writeArray = new ArrayList();
        if (len > 0) {
            for (int id = 0; id < len; id += 5) {
                String first = idList.get(id);
                if ((id + 5) < len) {
                    String last = idList.get(id - 1 + 5);
                    int ret = saveDBneuralnet(tableName, first, last, writeArray);
                    if (ret == 0) {
                        return 0;
                    }
                    ServiceAFweb.AFSleep();
                    continue;
                }
                if ((id + 5) >= len) {
                    String last = idList.get(len - 1);
                    int ret = saveDBneuralnet(tableName, first, last, writeArray);
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

    private int saveDBneuralnet(String tableName, String first, String last, ArrayList<String> writeArray) {
        try {
            logger.info("> saveDBneuralnet - " + tableName + " " + first + " " + last);

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
            logger.info("> saveDBneuralnet " + ex);
        }
        return 0;
    }

    private int saveDBneuralnetDataProcess(String tableName) {

        ArrayList<String> idList = getDBDataTableId(tableName);
        int len = idList.size();
        ArrayList<String> writeArray = new ArrayList();
        if (len > 0) {
            for (int id = 0; id < len; id += 500) {
                String first = idList.get(id);
                if ((id + 500) < len) {
                    String last = idList.get(id - 1 + 500);
                    int ret = saveDBneuralnetData(tableName, first, last, writeArray);
                    if (ret == 0) {
                        return 0;
                    }
                    ServiceAFweb.AFSleep();
                    continue;
                }
                if ((id + 500) >= len) {
                    String last = idList.get(len - 1);
                    int ret = saveDBneuralnetData(tableName, first, last, writeArray);
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

    private int saveDBneuralnetData(String tableName, String first, String last, ArrayList<String> writeArray) {
        try {
            logger.info("> saveDBneuralnetData - " + tableName + " " + first + " " + last);

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
            logger.info("> saveDBneuralnetData " + ex);
        }
        return 0;
    }

    private int saveDBstockinfo() {

        String tableName = "stockinfo";
        ArrayList<String> idList = getDBDataTableId(tableName);
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
                    int ret = saveDBstockinfo(tableName, first, last, writeArray);
                    if (ret == 0) {
                        return 0;
                    }
                    ServiceAFweb.AFSleep();
                }
                if ((id + 500) >= len) {
                    String last = idList.get(len - 1);
                    int ret = saveDBstockinfo(tableName, first, last, writeArray);
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

    private int saveDBstockinfo(String tableName, String first, String last, ArrayList<String> writeArray) {
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

    private int saveDBstock() {
        try {
            String tableName = "stock";
            ArrayList<String> idList = getDBDataTableId(tableName);
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

    private int saveDBlockobject() {
        try {
            String tableName = "lockobject";
            ArrayList<String> idList = getDBDataTableId(tableName);
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

    private ArrayList<String> getDBDataTableId(String table) {
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
    ///////////////////////////////////////////////////////////
    public static double[] daMACD = {
        459.99,
        448.85,
        446.06,
        450.81,
        442.8,
        448.97,
        444.57,
        441.4,
        430.47,
        420.05,
        431.14,
        425.66,
        430.58,
        431.72,
        437.87,
        428.43,
        428.35,
        432.5,
        443.66,
        455.72,
        454.49,
        452.08,
        452.73,
        461.91,
        463.58,
        461.14,
        452.08,
        442.66,
        428.91,
        429.79,
        431.99,
        427.72,
        423.2,
        426.21,
        426.98,
        435.69,
        434.33,
        429.8,
        419.85,
        426.24,
        402.8,
        392.05,
        390.53,
        398.67,
        406.13,
        405.46,
        408.38,
        417.2,
        430.12,
        442.78,
        439.29,
        445.52,
        449.98,
        460.71,
        458.66,
        463.84,
        456.77,
        452.97,
        454.74,
        443.86,
        428.85,
        434.58,
        433.26,
        442.93,
        439.66,
        441.35

    };

    public static double[] daRSI = {
        45.15,
        46.26,
        46.5,
        46.23,
        46.08,
        46.03,
        46.83,
        47.69,
        47.54,
        49.25,
        49.23,
        48.2,
        47.57,
        47.61,
        48.08,
        47.21,
        46.76,
        46.68,
        46.21,
        47.47,
        47.98,
        47.13,
        46.58,
        46.03,
        46.54,
        46.79,
        45.83,
        45.93,
        45.8,
        46.69,
        47.05,
        47.3,
        48.1,
        47.93,
        47.03,
        47.58,
        47.38,
        48.1,
        48.47,
        47.6,
        47.74,
        48.21,
        48.56,
        48.15,
        47.81,
        47.41,
        45.66,
        45.75,
        45.07,
        43.77,
        43.25,
        44.68,
        45.11,
        45.8,
        45.74,
        46.23,
        46.81,
        46.87,
        46.04,
        44.78,
        44.58,
        44.14,
        45.66,
        45.89,
        46.73,
        46.86,
        46.95,
        46.74,
        46.67,
        45.3,
        45.4,
        45.54,
        44.96,
        44.47,
        44.68,
        45.91,
        46.03,
        45.98,
        46.32,
        46.53,
        46.28,
        46.14,
        45.92,
        44.8,
        44.38,
        43.48,
        44.28,
        44.87,
        44.98,
        43.96,
        43.58,
        42.93,
        42.46,
        42.8,
        43.27,
        43.89,
        45,
        44.03,
        44.37,
        44.71,
        45.38,
        45.54

    };

}
