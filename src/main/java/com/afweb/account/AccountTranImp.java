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
import com.afweb.signal.TradingSignalProcess;

import com.afweb.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.logging.Logger;

/**
 *
 * @author eddyko
 */
public class AccountTranImp {

    protected static Logger logger = Logger.getLogger("AccountTranImp");

    public void updateTradingsignal(ServiceAFweb serviceAFWeb, AccountObj accountAdminObj, AccountObj accountObj, String symbol) {
        ServiceAFweb.lastfun = "updateTradingsignal";
        CommMsgImp commMsg = new CommMsgImp();

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

                    trObj.setPerf(trAdminObj.getPerf());
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

                                    CustomerObj cust = serviceAFWeb.getCustomerByAccoutObj(accountObj);
                                    if (cust.getType() == CustomerObj.INT_API_USER) {
                                        DateFormat formatD = new SimpleDateFormat("M/dd/yyyy hh:mm a");
                                        formatD.setTimeZone(tz);
                                        String ESTdateD = formatD.format(d);
                                        commMsg.AddCommAPISignalMessage(serviceAFWeb, accountObj, trTradingACCObj, ESTdateD, symbol, sig);

                                    } else {
                                        String accTxt = "acc-" + cust.getId();
                                        String msg = ESTtime + " " + accTxt + " " + symbol + " Sig:" + sig;
                                        commMsg.AddCommSignalMessage(serviceAFWeb, accountObj, trTradingACCObj, msg);

                                        // send email
                                        DateFormat formatD = new SimpleDateFormat("M/dd/yyyy hh:mm a");
                                        formatD.setTimeZone(tz);
                                        String ESTdateD = formatD.format(d);
                                        String msgD = ESTdateD + " " + accTxt + " " + symbol + " Sig:" + sig;
                                        commMsg.AddEmailCommMessage(serviceAFWeb, accountObj, trTradingACCObj, msgD);
                                    }
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
        CommMsgImp commMsg = new CommMsgImp();
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
                    CustomerObj cust = serviceAFWeb.getCustomerByAccoutObj(accFundObj);
                    String accTxt = "acc-" + cust.getId();
                    String msg = ESTtime + " " + accTxt + " " + symbol + " Sig:" + sig;
                    // comm message is in the trading account instead of multfund account
                    commMsg.AddCommSignalMessage(serviceAFWeb, accTrading, trFundACCObj, msg);

                    ///// broadcase PUBSUB message using account Fund object
                    commMsg.AddCommPUBSUBMessage(serviceAFWeb, accFundObj, trFundACCObj, msg);;

                    commMsg.AddCommSignalMessage(serviceAFWeb, accTrading, trFundACCObj, msg);

                    // send email
                    DateFormat formatD = new SimpleDateFormat("M/dd/yyyy hh:mm a");
                    formatD.setTimeZone(tz);
                    String ESTdateD = formatD.format(d);
                    String msgD = ESTdateD + " " + accTxt + " " + symbol + " Sig:" + sig;
                    commMsg.AddEmailCommMessage(serviceAFWeb, accTrading, trFundACCObj, msgD);
//                  logger.info("> updateTradingsignal update " + msg);

                    return 1;
                }
            }
        }
        return 0;
    }
//

////////////////////////////////////////////////
    public void updateTradingTransaction(ServiceAFweb serviceAFWeb, AccountObj accountObj, String symbol) {
        ServiceAFweb.lastfun = "updateTradingTransaction";
        if (serviceAFWeb.getServerObj().isSysMaintenance() == true) {
            return;
        }

//        logger.info("> updateTradingTransaction " + symbol + " " + accountObj.getAccountname());
        TradingSignalProcess TRprocessImp = new TradingSignalProcess();
        try {
            AFstockObj stock = serviceAFWeb.getStockRealTime(symbol);
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
                int ret = AddTransactionOrder(serviceAFWeb, accountObj, stock, trObj.getTrname(), trObj.getTrsignal(), null, true);
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
    ///////////////////////////////////////////////////////////
        //////////////////////////////////////// transaction order
    // user add buy or sell transaction manually
    public int AddTransactionOrderWithComm(ServiceAFweb serviceAFWeb, AccountObj accountObj, AFstockObj stock, String trName, int tranSignal) {

        int ret = AddTransactionOrder(serviceAFWeb, accountObj, stock, trName, tranSignal, null, false);
        if (ret == 1) {
            TradingRuleObj trObj = serviceAFWeb.SystemAccountStockIDByTRname(accountObj.getId(), stock.getId(), trName);

            String tzid = "America/New_York"; //EDT
            TimeZone tz = TimeZone.getTimeZone(tzid);
            java.sql.Date d = new java.sql.Date(trObj.getUpdatedatel());
//                                DateFormat format = new SimpleDateFormat("M/dd/yyyy hh:mm a z");
            DateFormat format = new SimpleDateFormat(" hh:mm a");
            format.setTimeZone(tz);
            String ESTdate = format.format(d);

            String sig = "exit";
            if (trObj.getTrsignal() == ConstantKey.S_BUY) {
                sig = ConstantKey.S_BUY_ST;
            } else if (trObj.getTrsignal() == ConstantKey.S_SELL) {
                sig = ConstantKey.S_SELL_ST;
            }
            CommMsgImp commMsgImp = new CommMsgImp();
            CustomerObj cust = serviceAFWeb.getCustomerByAccoutObj(accountObj);
            if (cust.getType() == CustomerObj.INT_API_USER) {
                DateFormat formatD = new SimpleDateFormat("M/dd/yyyy hh:mm a");
                formatD.setTimeZone(tz);
                String ESTdateD = formatD.format(d);

                commMsgImp.AddCommAPISignalMessage(serviceAFWeb, accountObj, trObj, ESTdateD, stock.getSymbol(), sig);

            } else {
                String accTxt = "acc-" + accountObj.getId();
                String msg = ESTdate + " " + accTxt + " " + stock.getSymbol() + " Sig:" + sig;
                commMsgImp.AddCommSignalMessage(serviceAFWeb, accountObj, trObj, msg);
            }
        }
        return ret;

    }

    public int AddTransactionOrder(ServiceAFweb serviceAFWeb, AccountObj accountObj, AFstockObj stock, String trName, int tranSignal, Calendar tranDate, boolean fromSystem) {
        try {

//            if (ServiceAFweb.mydebugSim == true) {
//                if (ServiceAFweb.SimDateTranL != 0) {
//                    Calendar cDate = Calendar.getInstance();
//                    cDate.setTimeInMillis(ServiceAFweb.SimDateTranL);
//                    tranDate = cDate;
//                }
//            }
            ArrayList<TransationOrderObj> currTranOrderList = serviceAFWeb.SystemAccountStockTransList(accountObj.getId(), stock.getId(), trName, 1);
            TradingRuleObj tradingRuleObj = serviceAFWeb.SystemAccountStockIDByTRname(accountObj.getId(), stock.getId(), trName);

            ArrayList transObjList = AddTransactionOrderProcess(currTranOrderList, tradingRuleObj, accountObj, stock, trName, tranSignal, tranDate, fromSystem);

            if ((transObjList != null) && (transObjList.size() > 0)) {
                ArrayList<String> transSQL = new ArrayList();
                for (int i = 0; i < transObjList.size(); i += 2) {
                    TransationOrderObj trOrder = (TransationOrderObj) transObjList.get(i);
                    TradingRuleObj trObj = (TradingRuleObj) transObjList.get(i + 1);

                    String trOrderSql = AccountDB.SQLaddAccountStockTransaction(trOrder);

                    transSQL.add(trOrderSql);
                    String trSql = AccountDB.SQLUpdateAccountStockSignal(trObj);
                    transSQL.add(trSql);
                }
                return serviceAFWeb.SystemuUpdateTransactionOrder(transSQL);
            }
        } catch (Exception e) {
            logger.info("> AddTransactionOrder exception " + e.getMessage());
        }
        return 0;
    }

    public ArrayList AddTransactionOrderProcess(ArrayList<TransationOrderObj> currTranOrderList, TradingRuleObj trObj, AccountObj accountObj, AFstockObj stock, String trName, int currentSignal, Calendar tranDate, boolean fromSystem) {
        ServiceAFweb.lastfun = "AddTransactionOrderProcess";

        ArrayList transSQL = new ArrayList();
        Calendar daOffset0 = tranDate;
        Calendar daOffset = tranDate;
        try {
            if (stock.getAfstockInfo() == null) {
                return null;
            }
            if (tranDate == null) {
                Calendar cDate = Calendar.getInstance();
                cDate.setTimeInMillis(stock.getUpdatedatel());

                daOffset0 = cDate;
                daOffset = cDate;
//                daOffset0 = TimeConvertion.getCurrentCalendar();
//                daOffset = TimeConvertion.getCurrentCalendar();
            }
            boolean buyOnly = false;
            if (trObj.getType() == ConstantKey.INT_TR_ACC) {
                buyOnly = true;
            }
            //get the current transaction order to see the last transaction
            int currentTranOrderSiganl = ConstantKey.S_NEUTRAL;
            if (currTranOrderList != null) {
                if (currTranOrderList.size() > 0) {
                    TransationOrderObj tranOrder = currTranOrderList.get(0);
                    currentTranOrderSiganl = tranOrder.getTrsignal();
                    if ((currentTranOrderSiganl == ConstantKey.S_EXIT_LONG) || (currentTranOrderSiganl == ConstantKey.S_EXIT_SHORT)) {
                        currentTranOrderSiganl = ConstantKey.S_NEUTRAL;
                    }
                }
            }
            int tranSiganl_0 = ConstantKey.S_NEUTRAL;
            int tranSiganl_1 = ConstantKey.S_NEUTRAL;

            switch (currentSignal) {
                case ConstantKey.S_LONG_BUY:
                    if (currentTranOrderSiganl == ConstantKey.S_LONG_BUY) {
                        return null;
                    }
                    if (currentTranOrderSiganl == ConstantKey.S_SHORT_SELL) {
                        tranSiganl_0 = ConstantKey.S_EXIT_SHORT;
                        tranSiganl_1 = ConstantKey.S_LONG_BUY;
                    }
                    if (currentTranOrderSiganl == ConstantKey.S_NEUTRAL) {
                        tranSiganl_0 = ConstantKey.S_LONG_BUY;
                    }
                    break;
                case ConstantKey.S_SHORT_SELL:
                    if (currentTranOrderSiganl == ConstantKey.S_SHORT_SELL) {
                        return null;
                    }
                    if (currentTranOrderSiganl == ConstantKey.S_LONG_BUY) {
                        tranSiganl_0 = ConstantKey.S_EXIT_LONG;
                        tranSiganl_1 = ConstantKey.S_SHORT_SELL;
                    }
                    if (currentTranOrderSiganl == ConstantKey.S_NEUTRAL) {
                        tranSiganl_0 = ConstantKey.S_SHORT_SELL;
                    }
                    break;
                case ConstantKey.S_NEUTRAL:
                    if (currentTranOrderSiganl == ConstantKey.S_LONG_BUY) {
                        tranSiganl_0 = ConstantKey.S_EXIT_LONG;
                        tranSiganl_1 = ConstantKey.S_NEUTRAL;
                    }
                    if (currentTranOrderSiganl == ConstantKey.S_SHORT_SELL) {
                        tranSiganl_0 = ConstantKey.S_EXIT_SHORT;
                        tranSiganl_1 = ConstantKey.S_NEUTRAL;
                    }
                    break;
            }

            if (tranSiganl_1 != ConstantKey.S_NEUTRAL) {
                long newDaL = TimeConvertion.addMiniSeconds(daOffset0.getTimeInMillis(), -10);
                daOffset0 = TimeConvertion.getCurrentCalendar(newDaL);
            }

            int retTrans = 1;
            if (tranSiganl_0 != ConstantKey.S_NEUTRAL) {
                //process buysell

                int signal = tranSiganl_0;
                Calendar dateOffset = daOffset0;
                switch (signal) {
                    case ConstantKey.S_LONG_BUY:
                        retTrans = TransactionOrderLONG_BUY(trObj, stock, signal, dateOffset, transSQL);
                        break;
                    case ConstantKey.S_EXIT_LONG:
                        retTrans = TransactionOrderEXIT_LONG(trObj, stock, signal, dateOffset, transSQL);
                        break;
                    case ConstantKey.S_SHORT_SELL:
                        retTrans = TransactionOrderSHORT_SELL(trObj, stock, signal, dateOffset, transSQL, buyOnly);
                        break;
                    case ConstantKey.S_EXIT_SHORT:
                        retTrans = TransactionOrderEXIT_SHORT(trObj, stock, signal, dateOffset, transSQL, buyOnly);
                        break;
                    default:
                        break;
                }
            }

            // exit if error
            if (retTrans == 0) {
                return null;
            }
            retTrans = 1;
            if (tranSiganl_1 != ConstantKey.S_NEUTRAL) {
                //process buysell
                int signal = tranSiganl_1;
                Calendar dateOffset = daOffset;
                switch (signal) {
                    case ConstantKey.S_LONG_BUY:
                        retTrans = TransactionOrderLONG_BUY(trObj, stock, signal, dateOffset, transSQL);
                        break;
                    case ConstantKey.S_EXIT_LONG:
                        retTrans = TransactionOrderEXIT_LONG(trObj, stock, signal, dateOffset, transSQL);
                        break;
                    case ConstantKey.S_SHORT_SELL:
                        retTrans = TransactionOrderSHORT_SELL(trObj, stock, signal, dateOffset, transSQL, buyOnly);
                        break;
                    case ConstantKey.S_EXIT_SHORT:
                        retTrans = TransactionOrderEXIT_SHORT(trObj, stock, signal, dateOffset, transSQL, buyOnly);
                        break;
                    default:
                        break;
                }
            }
            // exit if error
            if (retTrans == 0) {
                return null;
            }
            if (fromSystem == false) {
                if (trObj.getType() == ConstantKey.INT_TR_ACC) {
                    if (trObj.getLinktradingruleid() != 0) {
                        logger.info("> transactionOrder not allow when linking other TS");
                        return null;
                    }
                }
            }
            if (transSQL.size() > 0) {
//                int ret = updateTransactionOrder(transSQL);
                return transSQL;
            }

        } catch (Exception e) {
            logger.info("> transactionOrder exception " + e.getMessage());
        }
        return null;
    }

    static TradingRuleObj duplicateTRObj(TradingRuleObj trObj) {
        TradingRuleObj dupObj = null;
        String st;
        try {
            st = new ObjectMapper().writeValueAsString(trObj);
            dupObj
                    = new ObjectMapper().readValue(st, TradingRuleObj.class
                    );
            dupObj.setUpdatedatedisplay(new java.sql.Date(dupObj.getUpdatedatel()));
            return dupObj;
        } catch (Exception ex) {
            logger.info("> duplicateTRObj Exception" + ex.getMessage());
        }
        return null;
    }

    private int TransactionOrderEXIT_SHORT(TradingRuleObj trObj, AFstockObj stock, int siganl, Calendar dateOffset, ArrayList transSQL, boolean buyOnly) {
        float curPrice = stock.getAfstockInfo().getFclose();

        float originalPrice = trObj.getShortamount() / trObj.getShortshare();
        float deltaPrice = curPrice - originalPrice; //final price - original price
        deltaPrice = -deltaPrice; // negative for exit short
        float netPrice = originalPrice + deltaPrice;

        float amount = trObj.getShortshare() * netPrice;

        if (buyOnly == true) {
            // TR ACC can only support BUY transaction
        } else {
            trObj.setBalance(trObj.getBalance() + amount);
        }
        // add trading order
        TransationOrderObj trOrder = new TransationOrderObj();
        trOrder.setAccountid(trObj.getAccountid());
        trOrder.setAvgprice(curPrice);
        trOrder.setEntrydatedisplay(new java.sql.Date(dateOffset.getTimeInMillis()));
        trOrder.setEntrydatel(dateOffset.getTimeInMillis());
        trOrder.setShare(trObj.getShortshare());
        trOrder.setStatus(ConstantKey.OPEN);
        trOrder.setStockid(stock.getId());
        trOrder.setSymbol(stock.getSymbol());
        trOrder.setTradingruleid(trObj.getId());
        trOrder.setTrname(trObj.getTrname());
        trOrder.setTrsignal(siganl);  //EXIT_SHORT
        trOrder.setType(ConstantKey.INITIAL);
        trOrder.setComment("");
        transSQL.add(trOrder);

//        String trOrderSql = AccountDB.SQLaddAccountStockTransaction(trOrder);
//        transSQL.add(trOrderSql);
        // add trading order                                                
        trObj.setTrsignal(trOrder.getTrsignal());
        trObj.setUpdatedatedisplay(trOrder.getEntrydatedisplay());
        trObj.setUpdatedatel(trOrder.getEntrydatel());
        trObj.setShortshare(0);
        trObj.setShortamount(0);
        //update trObj

        TradingRuleObj trandingRuleObj = duplicateTRObj(trObj);
        transSQL.add(trandingRuleObj);

//        String trSql = AccountDB.SQLUpdateAccountStockSignal(trObj);
//        transSQL.add(trSql);
        //update trObj       
        return 1;
    }

    private int TransactionOrderSHORT_SELL(TradingRuleObj trObj, AFstockObj stock, int siganl, Calendar dateOffset, ArrayList transSQL, boolean buyOnly) {
        float curPrice = stock.getAfstockInfo().getFclose();
        float shareTmp = CKey.TRADING_AMOUNT / curPrice;  //$6000
        shareTmp += 0.5;
        if (shareTmp == 0) {
            shareTmp = 1;
        }
        int shareInt = (int) shareTmp;
        float amount = curPrice * shareInt;

        if (buyOnly == true) {
            // TR ACC can only support BUY transaction
            shareInt = 0;
            amount = 0;
        } else {

            if (trObj.getBalance() < amount) {
                trObj.setInvestment(trObj.getInvestment() + amount);

            } else {
                trObj.setBalance(trObj.getBalance() - amount);
            }
        }
        // add trading order
        TransationOrderObj trOrder = new TransationOrderObj();
        trOrder.setAccountid(trObj.getAccountid());
        trOrder.setAvgprice(curPrice);
        trOrder.setEntrydatedisplay(new java.sql.Date(dateOffset.getTimeInMillis()));
        trOrder.setEntrydatel(dateOffset.getTimeInMillis());
        trOrder.setShare(shareInt);
        trOrder.setStatus(ConstantKey.OPEN);
        trOrder.setStockid(stock.getId());
        trOrder.setSymbol(stock.getSymbol());
        trOrder.setTradingruleid(trObj.getId());
        trOrder.setTrname(trObj.getTrname());
        trOrder.setTrsignal(siganl);  //SHORT_SELL
        trOrder.setType(ConstantKey.INITIAL);
        trOrder.setComment("");
        transSQL.add(trOrder);

//        String trOrderSql = AccountDB.SQLaddAccountStockTransaction(trOrder);
//        transSQL.add(trOrderSql);
        // add trading order                                                
        trObj.setTrsignal(trOrder.getTrsignal());
        trObj.setUpdatedatedisplay(trOrder.getEntrydatedisplay());
        trObj.setUpdatedatel(trOrder.getEntrydatel());
        trObj.setShortshare(shareInt);
        trObj.setShortamount(amount);
        //update trObj
        TradingRuleObj trandingRuleObj = duplicateTRObj(trObj);
        transSQL.add(trandingRuleObj);
//        String trSql = AccountDB.SQLUpdateAccountStockSignal(trObj);
//        transSQL.add(trSql);
        //update trObj       

        return 1;
    }

    private int TransactionOrderEXIT_LONG(TradingRuleObj trObj, AFstockObj stock, int siganl, Calendar dateOffset, ArrayList transSQL) {
        float curPrice = stock.getAfstockInfo().getFclose();
        float amount = curPrice * trObj.getLongshare();

        trObj.setBalance(trObj.getBalance() + amount);

        // add trading order
        TransationOrderObj trOrder = new TransationOrderObj();
        trOrder.setAccountid(trObj.getAccountid());
        trOrder.setAvgprice(curPrice);
        trOrder.setEntrydatedisplay(new java.sql.Date(dateOffset.getTimeInMillis()));
        trOrder.setEntrydatel(dateOffset.getTimeInMillis());
        trOrder.setShare(trObj.getLongshare());
        trOrder.setStatus(ConstantKey.OPEN);
        trOrder.setStockid(stock.getId());
        trOrder.setSymbol(stock.getSymbol());
        trOrder.setTradingruleid(trObj.getId());
        trOrder.setTrname(trObj.getTrname());
        trOrder.setTrsignal(siganl);  //EXIT_LONG
        trOrder.setType(ConstantKey.INITIAL);
        trOrder.setComment("");
        transSQL.add(trOrder);

//        String trOrderSql = AccountDB.SQLaddAccountStockTransaction(trOrder);
//        transSQL.add(trOrderSql);
        // add trading order                                                
        trObj.setTrsignal(trOrder.getTrsignal());
        trObj.setUpdatedatedisplay(trOrder.getEntrydatedisplay());
        trObj.setUpdatedatel(trOrder.getEntrydatel());
        trObj.setLongshare(0);
        trObj.setLongamount(0);
        //update trObj
        TradingRuleObj trandingRuleObj = duplicateTRObj(trObj);
        transSQL.add(trandingRuleObj);
//        String trSql = AccountDB.SQLUpdateAccountStockSignal(trObj);
//        transSQL.add(trSql);
        //update trObj       

        return 1;
    }

    private int TransactionOrderLONG_BUY(TradingRuleObj trObj, AFstockObj stock, int siganl, Calendar dateOffset, ArrayList transSQL) {
        float curPrice = stock.getAfstockInfo().getFclose();
        float shareTmp = CKey.TRADING_AMOUNT / curPrice;  //$6000
        shareTmp += 0.5;
        if (shareTmp == 0) {
            shareTmp = 1;
        }
        int shareInt = (int) shareTmp;
        float amount = curPrice * shareInt;

        if (trObj.getBalance() < amount) {
            trObj.setInvestment(trObj.getInvestment() + amount);

        } else {
            trObj.setBalance(trObj.getBalance() - amount);
        }
        // add trading order
        TransationOrderObj trOrder = new TransationOrderObj();
        trOrder.setAccountid(trObj.getAccountid());
        trOrder.setAvgprice(curPrice);
        trOrder.setEntrydatedisplay(new java.sql.Date(dateOffset.getTimeInMillis()));
        trOrder.setEntrydatel(dateOffset.getTimeInMillis());
        trOrder.setShare(shareInt);
        trOrder.setStatus(ConstantKey.OPEN);
        trOrder.setStockid(stock.getId());
        trOrder.setSymbol(stock.getSymbol());
        trOrder.setTradingruleid(trObj.getId());
        trOrder.setTrname(trObj.getTrname());
        trOrder.setTrsignal(siganl);  //LONG_BUY
        trOrder.setType(ConstantKey.INITIAL);
        trOrder.setComment("");
        transSQL.add(trOrder);

//        String trOrderSql = AccountDB.SQLaddAccountStockTransaction(trOrder);
//        transSQL.add(trOrderSql);
        // add trading order                                                
        trObj.setTrsignal(trOrder.getTrsignal());
        trObj.setUpdatedatedisplay(trOrder.getEntrydatedisplay());
        trObj.setUpdatedatel(trOrder.getEntrydatel());
        trObj.setLongshare(shareInt);
        trObj.setLongamount(amount);
        //update trObj
        TradingRuleObj trandingRuleObj = duplicateTRObj(trObj);
        transSQL.add(trandingRuleObj);
//        String trSql = AccountDB.SQLUpdateAccountStockSignal(trObj);
//        transSQL.add(trSql);
        //update trObj       

        return 1;
    }

    ////////////////////////////////////////
}
