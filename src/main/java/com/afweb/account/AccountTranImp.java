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
}
