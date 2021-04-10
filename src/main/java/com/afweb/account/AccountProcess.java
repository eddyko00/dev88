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
import java.util.logging.Logger;

/**
 *
 * @author eddy
 */
public class AccountProcess {

    protected static Logger logger = Logger.getLogger("AccountProcess");

//    private ServiceAFweb serviceAFWeb = null;
    private static int acTimerCnt = 0;
    public static String LocalPCSignalPath = "T:/Netbean/signal/";
    private static ArrayList accountIdNameArray = new ArrayList();
    private static ArrayList accountFundIdNameArray = new ArrayList();

    ///
    public void InitSystemData() {
        acTimerCnt = 0;
        accountIdNameArray = new ArrayList();
    }

    public void ProcessSystemMaintance(ServiceAFweb serviceAFWeb) {
        acTimerCnt++;
        if (acTimerCnt < 0) {
            acTimerCnt = 0;
        }
//        logger.info(">>>>>>>>>>>>>> ProcessSystemMaintance " + acTimerCnt);
        Calendar dateNow = TimeConvertion.getCurrentCalendar();
        long lockDateValue = dateNow.getTimeInMillis();
        String LockName = "ACC_" + CKey.AF_SYSTEM;
        long lockReturn = serviceAFWeb.setLockNameProcess(LockName, ConstantKey.ACC_LOCKTYPE, lockDateValue, ServiceAFweb.getServerObj().getSrvProjName() + "_ProcessSystemMaintance");
        if (lockReturn > 0) {

            if ((acTimerCnt % 2) == 0) {
                // disable customer will be handle by billing process
                // disable cusotmer with no activity in 2 days
//                ProcessCustomerDisableMaintance(serviceAFWeb);

                // reomve customer with no activity in 4 days  
                ProcessCustomerRemoveMaintance(serviceAFWeb);
                // add or remove stock in ADMIN_USERNAME account based on all stocks in the system
                ProcessAdminAccount(serviceAFWeb);
                //delete stock if disable
                ProcessStockInfodeleteMaintance(serviceAFWeb);
            } else if ((acTimerCnt % 3) == 0) {
                ;
            } else {
                // delete stock based on all customer account exclude the ADMIN_USERNAME account 
                ProcessStockkMaintance(serviceAFWeb);

                // add or remove stock in ADMIN_USERNAME account based on all stocks in the system
                ProcessAdminAccount(serviceAFWeb);

                // cleanup Lock entry pass 30 min
                ProcessAllLockCleanup(serviceAFWeb);
                // cleanup Lock entry pass 30 min
            }
        }
        serviceAFWeb.removeNameLock(LockName, ConstantKey.ACC_LOCKTYPE);

    }
    //////////////////////////////////////////////

    public void ProcessStockInfodeleteMaintance(ServiceAFweb serviceAFWeb) {
//        logger.info(">>>>>>>>>>>>>> ProcessStockInfodeleteMaintance ");
        ArrayList stockRemoveList = serviceAFWeb.getRemoveStockNameList(20);
        //delete stock if disable

        ArrayList stockNameList = serviceAFWeb.getExpiredStockNameList(20);
        // delete stock info if disable
        if (stockNameList == null) {
            return;
        }
        if (stockNameList.size() == 0) {
            return;
        }
        int numCnt = 0;
        try {

            for (int i = 0; i < stockNameList.size(); i++) {
                String symbol = (String) stockNameList.get(i);
                serviceAFWeb.removeStockInfo(symbol);

                AFstockObj stock = serviceAFWeb.getRealTimeStockImp(symbol);
                stock.setStatus(ConstantKey.COMPLETED);
                //send SQL update
                String sockUpdateSQL = StockDB.SQLupdateStockStatus(stock);
                ArrayList sqlList = new ArrayList();
                sqlList.add(sockUpdateSQL);
                serviceAFWeb.SystemUpdateSQLList(sqlList);

                numCnt++;
                if (numCnt > 10) {
                    break;
                }
            }
        } catch (Exception ex) {
        }
    }

    public void ProcessCustomerRemoveMaintance(ServiceAFweb serviceAFWeb) {
        ServiceAFweb.lastfun = "ProcessCustomerRemoveMaintance";
//        logger.info(">>>>>>>>>>>>>> ProcessCustomerRemoveMaintance " + acTimerCnt);
        // reomve customer with no activity in 4 days        
        ArrayList custList = serviceAFWeb.getExpiredCustomerList(0);
        if (custList == null) {
            return;
        }
        int numCnt = 0;
        Calendar dateNow = TimeConvertion.getCurrentCalendar();
        long dateNowLong = dateNow.getTimeInMillis();
        long cust15DayAgo = TimeConvertion.addDays(dateNowLong, -15); // 15 day ago and no update             
        for (int i = 0; i < custList.size(); i++) {
            CustomerObj custObj = (CustomerObj) custList.get(i);

            // should be disable by ProcessCustomerDisableMaintance after 2 day in activity
            if (custObj.getStatus() != ConstantKey.DISABLE) {
                continue;
            }
            if (custObj.getUpdatedatel() < cust15DayAgo) {

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

                String msg = ESTdate + " " + custObj.getUsername() + " Customer removed - 15 day after expired.";
                if (custObj.getUsername().equals(CKey.E_USERNAME)) {
                    ;
                } else {
                    this.AddCommMessage(serviceAFWeb, accountAdminObj, ConstantKey.COM_SIGNAL, msg);
                }
                numCnt++;
                if (numCnt > 10) {
                    break;
                }

            }

        }
    }

////////////////////
    private void ProcessAllLockCleanup(ServiceAFweb serviceAFWeb) {
        ServiceAFweb.lastfun = "ProcessAllLockCleanup";

        logger.info(">>>>> ProcessAllLockCleanup " + acTimerCnt);
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

    private void ProcessStockkMaintance(ServiceAFweb serviceAFWeb) {
        ServiceAFweb.lastfun = "ProcessStockkMaintance";

        // delete stock based on all customer account exclude the ADMIN_USERNAME account 
        // do Simulation trading
//        logger.info(">>>>>>>>>>>>>> ProcessStockkMaintance " + acTimerCnt);
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

    public void ProcessAddRemoveFundAccount(ServiceAFweb serviceAFWeb) {
        ServiceAFweb.lastfun = "ProcessAddRemoveFundAccount";

        // add or remove stock in mutual fund account based on all stocks in the system
        //        logger.info("> ProcessAddRemoveFundAccount ......... ");
//        this.serviceAFWeb = serviceAFWeb;
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
                        ProcessTradingAccountUpdate(serviceAFWeb, accountObj);
                        ProcessFundAccountUpdate(serviceAFWeb, accountObj, true);

                    }
                } catch (Exception e) {
                    logger.info("> AddRemoveFundAccount Exception " + e.getMessage());
                }
            }
            serviceAFWeb.removeNameLock(LockName, ConstantKey.FUND_LOCKTYPE);
        }
    }

    public int ProcessFundAccountUpdate(ServiceAFweb serviceAFWeb, AccountObj accountObj, boolean buyOnly) {
        ServiceAFweb.lastfun = "ProcessFundAccountUpdate";

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

        ArrayList portAccArray = fundMgr.getAccL();

        ArrayList accountList = serviceAFWeb.getAccountImp().getAccountListByCustomerId(accountObj.getCustomerid());
        if (accountList == null) {
            return 0;
        }
        for (int k = 0; k < accountList.size(); k++) {
            AccountObj accObj = (AccountObj) accountList.get(k);
            if (accObj.getType() == AccountObj.INT_MUTUAL_FUND_ACCOUNT) {
                //////
                ///just for testing
//                ArrayList aList = serviceAFWeb.getAccountImp().getAccountListByCustomerId(1);
//                for (int m = 0; m < aList.size(); m++) {
//                    AccountObj aObj = (AccountObj) aList.get(m);
//                    if (aObj.getType() == AccountObj.INT_TRADING_ACCOUNT) {
//                        accObj = aObj;
//                        break;
//                    }
//                }
                //////
                ArrayList AccountStockNameList = serviceAFWeb.SystemAccountStockNameList(accObj.getId());
                if (AccountStockNameList == null) {
                    return 0;
                }

                int numCnt = 0;
                ArrayList addedList = new ArrayList();
                ArrayList removeList = new ArrayList();
                boolean result = compareStockList(portAccArray, AccountStockNameList, addedList, removeList);
                if (result == true) {
                    for (int i = 0; i < addedList.size(); i++) {
                        String symbol = (String) addedList.get(i);
                        int resultAdd = serviceAFWeb.addAccountStockByAccount(accObj, symbol);
                        if (resultAdd > 0) {
                            logger.info("> ProcessFundAccount add TR stock " + accObj.getAccountname() + " " + symbol);
                        }
                        numCnt++;
                        if (numCnt > 10) {
                            break;
                        }
                        ServiceAFweb.AFSleep();

                    }
                    // calculate performance for the account
                    AccountStockNameList = serviceAFWeb.SystemAccountStockNameList(accObj.getId());
                    if (AccountStockNameList == null) {
                        return 0;
                    }
                    float accountTotal = 0;
                    for (int j = 0; j < AccountStockNameList.size(); j++) {
                        String symbol = (String) AccountStockNameList.get(j);
                        AFstockObj stock = serviceAFWeb.getStockImp().getRealTimeStock(symbol, null);
                        if (stock == null) {
                            continue;
                        }
                        if (stock.getAfstockInfo() == null) {
                            continue;
                        }
                        float curPrice = stock.getAfstockInfo().getFclose();
                        TradingRuleObj trObj = serviceAFWeb.getAccountImp().getAccountStockIDByTRname(accObj.getId(), stock.getId(), ConstantKey.TR_ACC);
                        if (trObj == null) {
                            continue;
                        }
                        float sharebalance = 0;
                        if (trObj.getTrsignal() == ConstantKey.S_BUY) {
                            float delta = (curPrice * trObj.getLongshare()) - trObj.getLongamount();
                            sharebalance = delta;

                        } else if (trObj.getTrsignal() == ConstantKey.S_SELL) {
                            float delta = (curPrice * trObj.getShortshare()) - trObj.getShortamount();
                            sharebalance = -delta;
                        }
                        float total = sharebalance;
                        accountTotal += total;
                    }
//                    logger.info("> ProcessFundAccount " + accObj.getAccountname() + " curProfit " + accountTotal);
                    accObj.setBalance(accountTotal);
                    /////////
                    float totalBal = accObj.getInvestment();

                    try {
                        for (int i = 0; i < removeList.size(); i++) {

                            String symbol = (String) removeList.get(i);
                            AFstockObj stock = serviceAFWeb.getStockImp().getRealTimeStock(symbol, null);
                            if (stock == null) {
                                continue;
                            }
                            if (stock.getAfstockInfo() == null) {
                                continue;
                            }

                            boolean notRemoveFlag = true;
                            if (notRemoveFlag == true) {

                                String trName = ConstantKey.TR_ACC;
                                TradingRuleObj trObj = serviceAFWeb.SystemAccountStockIDByTRname(accObj.getId(), stock.getId(), trName);
                                // need to get the latest TR object after the SystemAddTransactionOrder
                                if (trObj.getStatus() != ConstantKey.PENDING) {
                                    trObj.setStatus(ConstantKey.PENDING);
                                    String updateSQL = AccountDB.SQLUpdateAccountStockStatus(trObj);
                                    ArrayList sqlList = new ArrayList();
                                    sqlList.add(updateSQL);
                                    serviceAFWeb.SystemUpdateSQLList(sqlList);
                                }
                            }

                            ArrayList<TransationOrderObj> thList = serviceAFWeb.getAccountImp().getAccountStockTransList(accObj.getId(), stock.getId(), ConstantKey.TR_ACC, 1);
                            if (thList != null) {
                                if (thList.size() != 0) {
                                    TransationOrderObj thObj = thList.get(0);
                                    int sig = thObj.getTrsignal();
                                    long entrydatel = thObj.getEntrydatel();

                                    long currentTime = System.currentTimeMillis();
                                    long day5befor = TimeConvertion.addDays(currentTime, -5);
                                    if (entrydatel > day5befor) {
                                        continue;
                                    }
                                }
                            }

                            int signal = ConstantKey.S_NEUTRAL;
                            String trName = ConstantKey.TR_ACC;
                            TradingRuleObj tradingRuleObj = serviceAFWeb.SystemAccountStockIDByTRname(accObj.getId(), stock.getId(), trName);
                            int curSignal = tradingRuleObj.getTrsignal();

                            boolean updateTran = true;
                            if (curSignal == ConstantKey.S_BUY) {
                                ;
                            } else if (curSignal == ConstantKey.S_SELL) {
                                ;
                            } else {
                                updateTran = false;
                            }
                            if (updateTran == true) {
                                TradingSignalProcess TRprocessImp = new TradingSignalProcess();

                                tradingRuleObj.setLinktradingruleid(ConstantKey.INT_TR_ACC);
                                ArrayList<TradingRuleObj> UpdateTRList = new ArrayList();
                                UpdateTRList.add(tradingRuleObj);
                                serviceAFWeb.getAccountImp().updateAccountStockSignal(UpdateTRList);

                                //////calcuate performance
                                float curPrice = stock.getAfstockInfo().getFclose();
                                TradingRuleObj trObj = serviceAFWeb.getAccountImp().getAccountStockIDByTRname(accObj.getId(), stock.getId(), ConstantKey.TR_ACC);

                                float sharebalance = 0;
                                if (trObj.getTrsignal() == ConstantKey.S_BUY) {
                                    float delta = (curPrice * trObj.getLongshare()) - trObj.getLongamount();
                                    sharebalance = delta;

                                } else if (trObj.getTrsignal() == ConstantKey.S_SELL) {
                                    if (buyOnly == false) {
                                        float delta = (curPrice * trObj.getShortshare()) - trObj.getShortamount();
                                        sharebalance = -delta;
                                    }
                                }
                                float total = sharebalance;
//                                logger.info("> ProcessFundAccount " + accObj.getAccountname() + " " + symbol + " stockProfit " + total);
                                totalBal += total;

                                TRprocessImp.AddTransactionOrderWithComm(serviceAFWeb, accObj, stock, trName, signal, null, false);

                            }
//            
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

                        // update account Obj performance
                        accObj.setInvestment(totalBal);

                        int substatus = accObj.getSubstatus();
                        float investment = accObj.getInvestment();
                        float balance = accObj.getBalance();
                        float servicefee = investment + balance;
                        serviceAFWeb.getAccountImp().updateAccountStatusByAccountID(accObj.getId(), substatus, investment, balance, servicefee);
                    } catch (Exception e) {
                    }
                }
                return 1;
            }
        }
        return 0;
    }

    public int ProcessTradingAccountUpdate(ServiceAFweb serviceAFWeb, AccountObj accountObj) {
        ServiceAFweb.lastfun = "ProcessTradingAccountUpdate";

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
                        int resultAdd = serviceAFWeb.addAccountStockByAccount(accObj, symbol);
                        if (resultAdd > 0) {
                            logger.info("> ProcessTradingAccountUpdate add TR stock " + accObj.getAccountname() + " " + symbol
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

                        AFstockObj stock = serviceAFWeb.getStockImp().getRealTimeStock(symbol, null);
                        if (stock == null) {
                            continue;
                        }
                        int resultRemove = serviceAFWeb.removeAccountStockSymbol(accObj, symbol);
                        if (resultRemove > 0) {
                            logger.info("> ProcessTradingAccountUpdate remove TR stock " + accObj.getAccountname() + " " + symbol);
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
        ServiceAFweb.lastfun = "ProcessAdminAccount";

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
                int resultAdd = serviceAFWeb.addAccountStockByCustAcc(CKey.ADMIN_USERNAME, null, accountAdminObj.getId() + "", symbol);
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
                int resultRemove = serviceAFWeb.removeAccountStockByUserNameAccId(CKey.ADMIN_USERNAME, null, accountAdminObj.getId() + "", symbol);
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
        ServiceAFweb.lastfun = "ProcessAllAccountTradingSignal";

//        this.serviceAFWeb = serviceAFWeb;
        TradingSignalProcess TRprocessImp = new TradingSignalProcess();
//        logger.info("> ProcessAllAccountTradingSignal ");

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
        long lockReturn = 1;
        lockReturn = serviceAFWeb.setLockNameProcess(LockName, ConstantKey.SIGNAL_LOCKTYPE, lockDateValue, ServiceAFweb.getServerObj().getSrvProjName() + "_ProcessAllAccountTradingSignal");
        boolean testing = false;
        if (testing == true) {
            lockReturn = 1;
        }
        logger.info("ProcessAllAccountTradingSignal " + LockName + " LockName " + lockReturn);

        if (lockReturn > 0) {

            long LastServUpdateTimer = System.currentTimeMillis();
            long lockDate5Min = TimeConvertion.addMinutes(LastServUpdateTimer, 5); // add 3 minutes
            // update Trading signal
            for (int i = 0; i < 10; i++) {

                long currentTime = System.currentTimeMillis();
                if (testing == true) {
                    currentTime = 0;
                }
                if (lockDate5Min < currentTime) {
                    break;
                }
                if (accountIdNameArray.size() == 0) {
                    break;
                }
                String accountIDSt = (String) accountIdNameArray.get(0);
                accountIdNameArray.remove(0);
                int accountId = Integer.parseInt(accountIDSt);
                // ignore admin acount. process all user account
                if (accountId == accountAdminObj.getId()) {
                    continue;
                }
//                logger.info("> ProcessAllAccountTradingSignal id " + accountId);
                AccountObj accountObj = serviceAFWeb.SystemAccountObjByAccountID(accountId);
                if (accountObj == null) {
                    continue;
                }

                ArrayList stockNameArray = serviceAFWeb.SystemAccountStockNameList(accountId);
                if (stockNameArray == null) {
                    continue;
                }
//                logger.info("> ProcessAllAccountTradingSignal " + accountObj.getAccountname() + " stock size=" + stockNameArray.size());

//                if (("acc-3-MutualFund".equals(accountObj.getAccountname()))
//                        || ("acc-4-MutualFund".equals(accountObj.getAccountname()))) {
//                    logger.info("> ProcessAllAccountTradingSignal " + accountObj.getAccountname() + " stock size=" + stockNameArray.size());
//                }
                for (int j = 0; j < stockNameArray.size(); j++) {
                    String symbol = (String) stockNameArray.get(j);
                    boolean ret = TRprocessImp.checkStock(serviceAFWeb, symbol);

                    if (ret == true) {
                        updateTradingsignal(serviceAFWeb, accountAdminObj, accountObj, symbol);
                        updateTradingTransaction(serviceAFWeb, accountObj, symbol);
                    }
                }  // end of stockNameArray.size() for that account
            }
            serviceAFWeb.removeNameLock(LockName, ConstantKey.SIGNAL_LOCKTYPE);
            logger.info("ProcessAllAccountTradingSignal " + LockName + " unlock LockName ");
        }

    }

    public void updateTradingsignal(ServiceAFweb serviceAFWeb, AccountObj accountAdminObj, AccountObj accountObj, String symbol) {
        ServiceAFweb.lastfun = "updateTradingsignal";

        if (serviceAFWeb.getServerObj().isSysMaintenance() == true) {
            return;
        }
//        logger.info("> updateTradingsignal " + symbol + " " + accountObj.getAccountname());
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

/////////////////
            if (accountObj.getType() == AccountObj.INT_MUTUAL_FUND_ACCOUNT) {
                // get trading account. Follow the signal from the trading account
                int ret = this.followFundSignalFromAcc(serviceAFWeb, accountObj, trTradingACCObj, UpdateTRList, symbol);

            } else {
                // get trading link id. Follow the signal from the admin account
                int trLinkId = trTradingACCObj.getLinktradingruleid();
                if (trLinkId != 0) {
                    boolean readySignal = false;

                    //Make sure all admin links are ready before copy the link signal
                    //Make sure all admin links are ready before copy the link signal
                    for (int j = 0; j < tradingRuleAdminList.size(); j++) {
                        TradingRuleObj trAdminObj = (TradingRuleObj) tradingRuleAdminList.get(j);
                        if (trAdminObj.getType() == ConstantKey.INT_TR_NN1) {
                            if (trAdminObj.getSubstatus() == ConstantKey.OPEN) {
                                readySignal = true;
                                break;
                            }
                        }
                    }

                    /////////check market open
                    boolean mkopen = DateUtil.isMarketOpen();
                    if (mkopen == false) {
                        readySignal = false;
                    }
                    /////////check market open
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
                                    String ESTtime = format.format(d);

                                    String sig = "exit";
                                    if (trAdminObj.getTrsignal() == ConstantKey.S_BUY) {
                                        sig = ConstantKey.S_BUY_ST;
                                    } else if (trAdminObj.getTrsignal() == ConstantKey.S_SELL) {
                                        sig = ConstantKey.S_SELL_ST;
                                    }

                                    String accTxt = "acc-" + accountObj.getId();
                                    String msg = ESTtime + " " + accTxt + " " + symbol + " Sig:" + sig;
                                    this.AddCommMessage(serviceAFWeb, accountObj, trTradingACCObj, msg);

                                    // send email
                                    DateFormat formatD = new SimpleDateFormat("M/dd/yyyy hh:mm a");
                                    formatD.setTimeZone(tz);
                                    String ESTdateD = formatD.format(d);
                                    String msgD = ESTdateD + " " + accTxt + " " + symbol + " Sig:" + sig;
                                    this.AddEmailCommMessage(serviceAFWeb, accountObj, trTradingACCObj, msgD);
//                                logger.info("> updateTradingsignal update " + msg);
                                }
                                break;
                            }
                        }
                    }
                } // copy the admin object for default TR_ACC
            }
        }
        TRObj stockTRObj = new TRObj();
        stockTRObj.setTrlist(UpdateTRList);
        serviceAFWeb.updateAccountStockSignal(stockTRObj);
    }

    public int followFundSignalFromAcc(ServiceAFweb serviceAFWeb, AccountObj accFundObj,
            TradingRuleObj trFundACCObj, ArrayList<TradingRuleObj> UpdateTRList, String symbol) {
        ServiceAFweb.lastfun = "followFundSignalFromAcc";

        boolean flag = true;
        /////////check market open
        boolean mkopen = DateUtil.isMarketOpen();
        if (mkopen == false) {
            flag = false;
        }
        /////////check market open
        if (flag == true) {
            // get trading account. Follow the signal from the trading account
            AccountObj accTrading = null;
            ArrayList<AccountObj> accountList = serviceAFWeb.getAccountImp().getAccountListByCustomerId(accFundObj.getCustomerid());
            if (accountList != null) {
                for (int i = 0; i < accountList.size(); i++) {
                    AccountObj acc = accountList.get(i);
                    if (acc.getType() == AccountObj.INT_TRADING_ACCOUNT) {
                        accTrading = acc;
                        break;
                    }
                }
            }
            if (accTrading != null) {
                int stockId = trFundACCObj.getStockid();
                TradingRuleObj trTradingA = serviceAFWeb.SystemAccountStockIDByTRname(accTrading.getId(), stockId, ConstantKey.TR_ACC);
                int newTsSig = trTradingA.getTrsignal();
                long newUpdatedatel = trTradingA.getUpdatedatel();
                int tsSig = trFundACCObj.getTrsignal();

                if (trFundACCObj.getStatus() == ConstantKey.PENDING) {
                    newUpdatedatel = TimeConvertion.getCurrentCalendar().getTimeInMillis();
                    newTsSig = ConstantKey.S_EXIT;
                }
                if (tsSig != newTsSig) {
                    trFundACCObj.setTrsignal(newTsSig);
                    UpdateTRList.add(trFundACCObj);

                    String tzid = "America/New_York"; //EDT
                    TimeZone tz = TimeZone.getTimeZone(tzid);
                    java.sql.Date d = new java.sql.Date(newUpdatedatel);
//                  DateFormat format = new SimpleDateFormat("M/dd/yyyy hh:mm a z");
                    DateFormat format = new SimpleDateFormat(" hh:mm a");
                    format.setTimeZone(tz);
                    String ESTtime = format.format(d);

                    String sig = "exit";
                    if (newTsSig == ConstantKey.S_BUY) {
                        sig = ConstantKey.S_BUY_ST;
                    } else if (newTsSig == ConstantKey.S_SELL) {
                        sig = ConstantKey.S_SELL_ST;
                    }
                    String accTxt = "acc-" + accFundObj.getId();
                    String msg = ESTtime + " " + accTxt + " " + symbol + " Sig:" + sig;
                    // comm message is in the trading account instead of multfund account
                    this.AddCommMessage(serviceAFWeb, accTrading, trFundACCObj, msg);
                    
                    ///// broadcase PUBSUB message using account Fund object
                    AddCommPUBSUBMessage(serviceAFWeb, accFundObj, trFundACCObj, msg);;
                    
                    this.AddCommMessage(serviceAFWeb, accTrading, trFundACCObj, msg);

                    // send email
                    DateFormat formatD = new SimpleDateFormat("M/dd/yyyy hh:mm a");
                    formatD.setTimeZone(tz);
                    String ESTdateD = formatD.format(d);
                    String msgD = ESTdateD + " " + accTxt + " " + symbol + " Sig:" + sig;
                    this.AddEmailCommMessage(serviceAFWeb, accTrading, trFundACCObj, msgD);
//                  logger.info("> updateTradingsignal update " + msg);




                    return 1;
                }
            }
        }
        return 0;
    }

    public int AddCommObjMessage(ServiceAFweb serviceAFWeb, AccountObj accountObj, String name, int type, CommData commDataObj) {
        try {
            return serviceAFWeb.getAccountImp().addAccountCommMessage(accountObj, name, type, commDataObj);

        } catch (Exception e) {
            logger.info("> AddCommMessage exception " + e.getMessage());
        }
        return 0;
    }

    public int AddCommMessage(ServiceAFweb serviceAFWeb, AccountObj accountObj, String name, String messageData) {
        try {
            logger.info("> AddCommMessage  " + messageData);
            return serviceAFWeb.getAccountImp().addAccountMessage(accountObj, name, messageData);

        } catch (Exception e) {
            logger.info("> AddCommMessage exception " + e.getMessage());
        }
        return 0;
    }

    public int AddEmailBillingCommMessage(ServiceAFweb serviceAFWeb, AccountObj accountObj, TradingRuleObj tr, String messageData) {
        try {
            if (tr.getType() == ConstantKey.INT_TR_ACC) {
//                logger.info("> AddCommMessage  " + messageData);
                return serviceAFWeb.getAccountImp().addAccountEmailMessage(accountObj, ConstantKey.COM_ACCBILLMSG, messageData);
            }
        } catch (Exception e) {
            logger.info("> AddEmailBillingCommMessage exception " + e.getMessage());
        }
        return 0;
    }

    public int AddEmailCommMessage(ServiceAFweb serviceAFWeb, AccountObj accountObj, TradingRuleObj tr, String messageData) {
        try {
            if (tr.getType() == ConstantKey.INT_TR_ACC) {
//                logger.info("> AddCommMessage  " + messageData);
                return serviceAFWeb.getAccountImp().addAccountEmailMessage(accountObj, ConstantKey.COM_EMAIL, messageData);
            }
        } catch (Exception e) {
            logger.info("> AddEmailCommMessage exception " + e.getMessage());
        }
        return 0;
    }

    public int AddCommMessage(ServiceAFweb serviceAFWeb, AccountObj accountObj, TradingRuleObj tr, String messageData) {
        try {
            if (tr.getType() == ConstantKey.INT_TR_ACC) {
                logger.info("> AddCommMessage  " + messageData);
                return serviceAFWeb.getAccountImp().addAccountMessage(accountObj, ConstantKey.COM_SIGNAL, messageData);
            }
        } catch (Exception e) {
            logger.info("> AddCommMessage exception " + e.getMessage());
        }
        return 0;
    }
    
    public int AddCommPUBSUBMessage(ServiceAFweb serviceAFWeb, AccountObj accFundObj, TradingRuleObj tr, String messageData) {
        try {
            if (tr.getType() == ConstantKey.INT_TR_ACC) {
                return serviceAFWeb.getAccountImp().addAccountPUBSUBMessage(accFundObj, ConstantKey.COM_PUB, messageData);
            }
        } catch (Exception e) {
            logger.info("> AddCommMessage exception " + e.getMessage());
        }
        return 0;
    }    
    
////////////////////////////////////////////////

    public void updateTradingTransaction(ServiceAFweb serviceAFWeb, AccountObj accountObj, String symbol) {
        ServiceAFweb.lastfun = "updateTradingTransaction";
        if (serviceAFWeb.getServerObj().isSysMaintenance() == true) {
            return;
        }

//        logger.info("> updateTradingTransaction " + symbol + " " + accountObj.getAccountname());
        TradingSignalProcess TRprocessImp = new TradingSignalProcess();
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
            //  entrydatel desc recent transaction first
            tranOrderList = serviceAFWeb.SystemAccountStockTransList(accountObj.getId(), stock.getId(), trName.toUpperCase(), 0);
            if ((tranOrderList != null) && (tranOrderList.size() > 0)) {

                ArrayList<PerformanceObj> performanceList = TRprocessImp.ProcessTranPerfHistory(serviceAFWeb, tranOrderList, stock, 1, true); // buyOnly true for TR_ACC
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

                if (accountObj.getType() == AccountObj.INT_MUTUAL_FUND_ACCOUNT) {
                    if (trObj.getStatus() == ConstantKey.PENDING) {
                        // delete stock
                        serviceAFWeb.getAccountImp().removeAccountStock(accountObj, trObj.getStockid());
                    }
                }
            }

        } catch (Exception ex) {
            logger.info("> updateTradingTransaction Exception" + ex.getMessage());
        }
    }

    /////////testing only
//    public void updateTradingAccountBalance(ServiceAFweb serviceAFWeb, AccountObj accountObj, String symbol) {
////        logger.info("> updateTradingTransaction " + symbol + " " + accountObj.getAccountname());
//        TrandingSignalProcess TRprocessImp = new TrandingSignalProcess();
//        try {
//            AFstockObj stock = serviceAFWeb.getRealTimeStockImp(symbol);
//            if (stock != null) {
//                if (stock.getSubstatus() == ConstantKey.STOCK_SPLIT) {
//                    return;
//                }
//            }
//            TradingRuleObj trObj = serviceAFWeb.SystemAccountStockIDByTRname(accountObj.getId(), stock.getId(), ConstantKey.TR_ACC);
//
//            if (trObj == null) {
//                return;
//            }
//            int subStatus = trObj.getSubstatus();
//            if (subStatus == ConstantKey.INITIAL) {
//                serviceAFWeb.SystemAccountStockClrTranByAccountID(accountObj, trObj.getStockid(), trObj.getTrname());
//            }
//
//            // process performance
//            String trName = trObj.getTrname();
//            ArrayList<TransationOrderObj> tranOrderList = serviceAFWeb.SystemAccountStockTransList(accountObj.getId(), stock.getId(), trName.toUpperCase(), 0);
//
//            int currentTS = ConstantKey.S_NEUTRAL;
//            currentTS = trObj.getTrsignal();
//            if ((currentTS == ConstantKey.S_EXIT_LONG) || (currentTS == ConstantKey.S_EXIT_SHORT)) {
//                currentTS = ConstantKey.S_NEUTRAL;
//            }
//
//            int orderTS = ConstantKey.S_NEUTRAL;
//            if ((tranOrderList != null) && (tranOrderList.size() > 0)) {
//                TransationOrderObj tranOrder = tranOrderList.get(0);
//
//                orderTS = tranOrder.getTrsignal();
//                if ((orderTS == ConstantKey.S_EXIT_LONG) || (orderTS == ConstantKey.S_EXIT_SHORT)) {
//                    orderTS = ConstantKey.S_NEUTRAL;
//                }
//            }
//
//            if (currentTS != orderTS) {
//                int ret = TRprocessImp.AddTransactionOrder(serviceAFWeb, accountObj, stock, trObj.getTrname(), trObj.getTrsignal(), null, true);
////                newTran = serviceAFWeb.SystemAddTransactionOrder(accountObj, stock, trObj.getTrname(), trObj.getTrsignal(), null);
//            }
//            // Get transaction again process performance
//            //  entrydatel desc recent transaction first
//            tranOrderList = serviceAFWeb.SystemAccountStockTransList(accountObj.getId(), stock.getId(), trName.toUpperCase(), 0);
//            if ((tranOrderList != null) && (tranOrderList.size() > 0)) {
//
//                // simulate exit tranOrderList
//                long timeMillis = TimeConvertion.currentTimeMillis();
//                TransationOrderObj tranObj = tranOrderList.get(0);
//                int tr = tranObj.getTrsignal();
//                if (tr == ConstantKey.S_LONG_BUY) {
//                    TransationOrderObj trOrder = new TransationOrderObj();
//                    trOrder.setAccountid(trObj.getAccountid());
//                    trOrder.setAvgprice(stock.getAfstockInfo().getFclose());
//                    trOrder.setEntrydatedisplay(new java.sql.Date(timeMillis));
//                    trOrder.setEntrydatel(timeMillis);
//                    trOrder.setShare(trObj.getLongshare());
//                    trOrder.setStatus(ConstantKey.OPEN);
//                    trOrder.setStockid(stock.getId());
//                    trOrder.setSymbol(stock.getSymbol());
//                    trOrder.setTradingruleid(trObj.getId());
//                    trOrder.setTrname(trObj.getTrname());
//                    trOrder.setTrsignal(ConstantKey.S_EXIT_LONG);
//                    trOrder.setType(ConstantKey.INITIAL);
//                    trOrder.setComment("");
//                    tranOrderList.add(0, trOrder);
//                } else if (tr == ConstantKey.S_SHORT_SELL) {
//                    boolean buyOnly = true;
//                    if (buyOnly == false) {
//                        TransationOrderObj trOrder = new TransationOrderObj();
//                        trOrder.setAccountid(trObj.getAccountid());
//                        trOrder.setAvgprice(stock.getAfstockInfo().getFclose());
//                        trOrder.setEntrydatedisplay(new java.sql.Date(timeMillis));
//                        trOrder.setEntrydatel(timeMillis);
//                        trOrder.setShare(trObj.getShortshare());
//                        trOrder.setStatus(ConstantKey.OPEN);
//                        trOrder.setStockid(stock.getId());
//                        trOrder.setSymbol(stock.getSymbol());
//                        trOrder.setTradingruleid(trObj.getId());
//                        trOrder.setTrname(trObj.getTrname());
//                        trOrder.setTrsignal(ConstantKey.S_EXIT_SHORT);
//                        trOrder.setType(ConstantKey.INITIAL);
//                        trOrder.setComment("");
//                        tranOrderList.add(0, trOrder);
//                    }
//                }
//
//                ArrayList<PerformanceObj> performanceList = TRprocessImp.ProcessTranPerfHistory(serviceAFWeb, tranOrderList, stock, 1, true); // buyOnly true for TR_ACC
//                if (performanceList != null) {
//                    if (performanceList.size() == 1) {
//                        PerformanceObj pObj = performanceList.get(0);
//                        pObj.setAccountid(accountObj.getId());
//                        pObj.setStockid(stock.getId());
//                        pObj.setTradingruleid(trObj.getId());
//
//                    }
//                }
//            }
//
//        } catch (Exception ex) {
//            logger.info("> updateTradingAccountBalance Exception" + ex.getMessage());
//        }
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
//
//    //https://ca.finance.yahoo.com/quote/T.TO/history?period1=1200441600&period2=1583539200&interval=1d&filter=history&frequency=1d
//    public void updateAllStockFile(ServiceAFweb serviceAFWeb) {
//
//////////////////////////////////////////////////////        
////    public static String primaryStock[] = {"AAPL", "SPY", "DIA", "QQQ", "HOU.TO", "HOD.TO", "T.TO", "FAS", "FAZ", "RY.TO", "XIU.TO"};
//        for (int i = 0; i < ServiceAFweb.primaryStock.length; i++) {
//            String stockN = ServiceAFweb.primaryStock[i];
//            updateStockFile(serviceAFWeb, stockN);
//        }
//
//    }
//
//    public boolean updateStockFile(ServiceAFweb serviceAFWeb, String NormalizeSymbol) {
//        this.serviceAFWeb = serviceAFWeb;
//        ArrayList inputArray = new ArrayList();
//        String nnFileName = ServiceAFweb.FileLocalPath + NormalizeSymbol + ".csv";
//        if (FileUtil.FileTest(nnFileName) == false) {
//            logger.info("updateStockFile not found " + nnFileName);
//            return false;
//        }
//
//        AFstockObj stock = serviceAFWeb.getRealTimeStockImp(NormalizeSymbol);
//        serviceAFWeb.getStockImp().deleteStockInfoByStockId(stock);
//
//        ArrayList<AFstockInfo> StockArray = new ArrayList();
//        boolean ret = FileUtil.FileReadTextArray(nnFileName, inputArray);
//        if (ret == true) {
//            int LineNum = 0;
//            String inLine = "";
//            for (int i = 0; i < inputArray.size(); i++) {
//                inLine = (String) inputArray.get(i);
//                LineNum++;
//                //Date,Open,High,Low,Close,Adj Close,Volume
//                if (inLine.indexOf("Date,Open") != -1) {
//                    continue;
//                }
//                //1995-04-14,null,null,null,null,null,null
//                if (inLine.indexOf("null,null,null") != -1) {
//                    continue;
//                }
//                if (inLine.indexOf("Dividend") != -1) {
//                    continue;
//                }
//                if (inLine.indexOf("Stock Split") != -1) {
//                    break;
//                }
//                if (inLine.indexOf("-,-,-,-,-") != -1) {
//                    continue;
//                }
//                AFstockInfo StockD = StockInfoUtils.parseCSVLine(inLine);
//                if (StockD == null) {
//                    logger.info("updateStockFile Exception " + NormalizeSymbol + " " + inLine);
//                    break;
//                }
//
//                StockArray.add(StockD);
//            }
//            logger.info("updateStockFile  " + NormalizeSymbol + " " + StockArray.size());
//            if (StockArray.size() == 0) {
//                return false;
//            }
//            ArrayList<AFstockInfo> StockSendArray = new ArrayList();
//            int index = 0;
////            Collections.reverse(StockArray);
//            for (int i = 0; i < StockArray.size(); i++) {
//
//                StockSendArray.add(StockArray.get(i));
//                index++;
//                if (index > 99) {
//                    index = 0;
////                    Collections.reverse(StockSendArray);
//                    StockInfoTranObj stockInfoTran = new StockInfoTranObj();
//                    stockInfoTran.setNormalizeName(NormalizeSymbol);
//                    stockInfoTran.setStockInfoList(StockSendArray);
//                    // require oldest date to earliest
//                    // require oldest date to earliest
//                    int retTran = serviceAFWeb.updateStockInfoTransaction(stockInfoTran);
//                    if (retTran == 0) {
//                        return false;
//                    }
//                    StockSendArray.clear();
//                }
//
//            }
////            Collections.reverse(StockSendArray);
//            StockInfoTranObj stockInfoTran = new StockInfoTranObj();
//            stockInfoTran.setNormalizeName(NormalizeSymbol);
//            stockInfoTran.setStockInfoList(StockSendArray);
////                logger.info("updateRealTimeStock send " + StockSendArray.size());
//
//            // require oldest date to earliest
//            // require oldest date to earliest
//            serviceAFWeb.updateStockInfoTransaction(stockInfoTran);
//        }
//        return true;
//    }

////////////////////////////////////////    
////////////////uploadDBData////////////////////////    
////////////////uploadDBData////////////////////////        
////////////////////////////////////////    
    public boolean restoreNNonlyDBData(ServiceAFweb serviceAFWeb) {
        int ret = restoreDBneuralnetPro(serviceAFWeb, "neuralnet");
        if (CKey.NN_DATA_DB == false) {
            ret = restoreDBneuralnetDataPro(serviceAFWeb, "neuralnetdata");
        }
        return true;
    }

//    public static String ServiceAFweb.FileLocalPath = "T:/Netbean/db/";
    public boolean restoreDBData(ServiceAFweb serviceAFWeb) {
//        this.serviceAFWeb = serviceAFWeb;

        if (FileUtil.FileTest(ServiceAFweb.FileLocalPath + "customer.txt") == false) {
            return false;
        }

        int ret = restoreDBcustomer(serviceAFWeb);
        if (ret == 0) {
            return false;
        }
        restoreDBstockinfo(serviceAFWeb);

        restoreDBaccount(serviceAFWeb);
        restoreDBstock(serviceAFWeb);
        restoreDBaccountstock_tradingrule(serviceAFWeb);
        restoreDBneuralnet(serviceAFWeb);

        restoreDBtransationorder(serviceAFWeb);
        restoreDBcomm(serviceAFWeb);
        restoreDBbilling(serviceAFWeb);
        restoreDBperformance(serviceAFWeb);
//        restoreDBstockinfo();
        restoreDBdummy(serviceAFWeb);

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

//    private int restoreDBneuralnetProcess(ServiceAFweb serviceAFWeb, String tableName) {
//
//        try {
//            ArrayList<String> writeArray = new ArrayList();
//            String fName = ServiceAFweb.FileLocalPath + tableName + ".txt";
//            if (FileUtil.FileTest(fName) == false) {
//                return 0;
//            }
//            FileUtil.FileReadTextArray(fName, writeArray);
//            ArrayList<String> writeSQLArray = new ArrayList();
//            logger.info("> restoreDBneuralnet " + tableName + " " + writeArray.size());
//            int index = 0;
//            for (int i = 0; i < writeArray.size(); i++) {
//                String output = writeArray.get(i);
//                AFneuralNet item = new ObjectMapper().readValue(output, AFneuralNet.class);
//                String sql = StockDB.insertNeuralNet(tableName, item);
//                writeSQLArray.add(sql);
//                index++;
//                if (index > 5) {
//                    index = 0;
//                    int ret = sendRequestObj(serviceAFWeb, writeSQLArray);
//                    if (ret == 0) {
//                        return 0;
//                    }
//                    writeSQLArray.clear();
//                    ServiceAFweb.AFSleep();
//                }
//
//            }
//            return sendRequestObj(serviceAFWeb, writeSQLArray);
//
//        } catch (IOException ex) {
//            logger.info("> restoreDBneuralnetProcess - exception " + ex);
//        }
//        return 0;
//    }
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

//    private int restoreDBneuralnetDataProcess(ServiceAFweb serviceAFWeb, String tableName) {
//
//        try {
//            ArrayList<String> writeArray = new ArrayList();
//            String fName = ServiceAFweb.FileLocalPath + tableName + ".txt";
//            if (FileUtil.FileTest(fName) == false) {
//                return 0;
//            }
//            FileUtil.FileReadTextArray(fName, writeArray);
//            ArrayList<String> writeSQLArray = new ArrayList();
//            logger.info("> restoreDBneuralnetData " + tableName + " " + writeArray.size());
//            int index = 0;
//            for (int i = 0; i < writeArray.size(); i++) {
//                String output = writeArray.get(i);
//                AFneuralNetData item = new ObjectMapper().readValue(output, AFneuralNetData.class);
//                String sql = StockDB.insertNeuralNetData(tableName, item);
//                writeSQLArray.add(sql);
//                index++;
//                if (index > 500) {
//                    index = 0;
//                    int ret = sendRequestObj(serviceAFWeb, writeSQLArray);
//                    if (ret == 0) {
//                        return 0;
//                    }
//                    writeSQLArray.clear();
//                    ServiceAFweb.AFSleep();
//                    //////only require for VMware local
//                    if (CKey.DELAY_RESTORE == true) {
//                        ServiceAFweb.AFSleep1Sec(6);
//                    }
//                }
//
//            }
//            return sendRequestObj(serviceAFWeb, writeSQLArray);
//
//        } catch (IOException ex) {
//            logger.info("> restoreDBneuralnetDataProcess - exception " + ex);
//        }
//        return 0;
//    }
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
                String sql = StockDB.insertStockInfo(item);
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
                if (index > 400) {
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
    public boolean downloadDBDataTest(ServiceAFweb serviceAFWeb) {
//        this.serviceAFWeb = serviceAFWeb;
        saveDBneuralnetDataPro(serviceAFWeb, "neuralnetdata");
        return true;
    }

    public boolean downloadDBData(ServiceAFweb serviceAFWeb) {
//        this.serviceAFWeb = serviceAFWeb;

        saveDBstockinfo(serviceAFWeb);
        saveDBcustomer(serviceAFWeb);
        saveDBaccount(serviceAFWeb);
        saveDBaccountstock_tradingrule(serviceAFWeb);
        saveDBneuralnet(serviceAFWeb);
        saveDBtransationorder(serviceAFWeb);
        saveDBcomm(serviceAFWeb);
        saveDBbilling(serviceAFWeb);
        saveDBperformance(serviceAFWeb);
        saveDBstock(serviceAFWeb);
//        saveDBstockinfo();

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
                    AccData accData = serviceAFWeb.getAccData(obj);
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

//    private int saveDBneuralnetDataProcess(ServiceAFweb serviceAFWeb, String tableName) {
//
//        ArrayList<String> idList = getDBDataTableId(serviceAFWeb, tableName);
//        int len = idList.size();
//        ArrayList<String> writeArray = new ArrayList();
//        if (len > 0) {
//            for (int id = 0; id < len; id += 500) {
//                String first = idList.get(id);
//                if ((id + 500) < len) {
//                    String last = idList.get(id - 1 + 500);
//                    int ret = saveDBneuralnetData(serviceAFWeb, tableName, first, last, writeArray);
//                    if (ret == 0) {
//                        return 0;
//                    }
//                    ServiceAFweb.AFSleep();
//                    continue;
//                }
//                if ((id + 500) >= len) {
//                    String last = idList.get(len - 1);
//                    int ret = saveDBneuralnetData(serviceAFWeb, tableName, first, last, writeArray);
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
//    private int saveDBneuralnetData(ServiceAFweb serviceAFWeb, String tableName, String first, String last, ArrayList<String> writeArray) {
//        try {
//            logger.info("> saveDBneuralnetData - " + tableName + " " + first + " " + last);
//
//            RequestObj sqlObj = new RequestObj();
//            sqlObj.setCmd(ServiceAFweb.AllNeuralNetData + "");
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
//            ArrayList<AFneuralNetData> array = null;
//            AFneuralNetData[] arrayItem = new ObjectMapper().readValue(output, AFneuralNetData[].class);
//            List<AFneuralNetData> listItem = Arrays.<AFneuralNetData>asList(arrayItem);
//            array = new ArrayList<AFneuralNetData>(listItem);
//
//            for (int i = 0; i < array.size(); i++) {
//                AFneuralNetData obj = array.get(i);
//                String st = new ObjectMapper().writeValueAsString(obj);
//                writeArray.add(st);
//            }
//            return 1;
//
//        } catch (Exception ex) {
//            logger.info("> saveDBneuralnetData " + ex);
//        }
//        return 0;
//    }
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
