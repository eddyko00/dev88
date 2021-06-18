/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.processcustacc;

import com.afweb.account.*;
import com.afweb.chart.*;
import com.afweb.model.*;
import com.afweb.model.account.*;
import com.afweb.model.stock.AFstockInfo;
import com.afweb.model.stock.AFstockObj;
import com.afweb.nn.NNormalObj;
import com.afweb.nnsignal.TradingSignalProcess;
import com.afweb.processaccounting.AccountingProcess;
import com.afweb.processbilling.BillingProcess;
import com.afweb.processnn.TradingNNprocess;

import com.afweb.service.ServiceAFweb;

import com.afweb.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Logger;

/**
 *
 * @author koed
 */
public class CustAccService {

    protected static Logger logger = Logger.getLogger("CustAccService");

    // result 1 = success, 2 = existed,  0 = fail
    public LoginObj addCustomerPassword(ServiceAFweb serviceAFWeb, String EmailUserName, String Password, String FirstName, String LastName, String planSt) {
        LoginObj loginObj = new LoginObj();
        loginObj.setCustObj(null);
        WebStatus webStatus = new WebStatus();
        webStatus.setResultID(0);
        loginObj.setWebMsg(webStatus);
        loginObj.setWebMsg(webStatus);
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return loginObj;
        }

        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();
        boolean validEmail = NameObj.isEmailValid(EmailUserName);
        if (validEmail == true) {
            CustomerObj newCustomer = new CustomerObj();
            newCustomer.setUsername(UserName);
            newCustomer.setPassword(Password);
            newCustomer.setType(CustomerObj.INT_CLIENT_BASIC_USER);
            newCustomer.setEmail(EmailUserName);
            newCustomer.setFirstname(FirstName);
            newCustomer.setLastname(LastName);
            int plan = 0;
            plan = Integer.parseInt(planSt);
            if (plan == ConstantKey.INT_PP_BASIC) {
                ;
            } else if (plan == ConstantKey.INT_PP_STANDARD) {
                ;
            } else if (plan == ConstantKey.INT_PP_PEMIUM) {
                ;
            }

            // result 1 = success, 2 = existed,  0 = fail
            int result = serviceAFWeb.getAccountImp().addCustomer(newCustomer, plan);
            if (result == 1) {
                CustomerObj custObj = serviceAFWeb.getAccountImp().getCustomerPassword(UserName, Password);
                if (custObj != null) {
                    // set pending for new customer
                    if (custObj.getStatus() == ConstantKey.OPEN) {
                        custObj.setStatus(ConstantKey.PENDING);
                        serviceAFWeb.getAccountImp().updateCustStatusSubStatus(custObj, custObj.getStatus(), custObj.getSubstatus());
                    }
                }
            }
//
            String tzid = "America/New_York"; //EDT
            TimeZone tz = TimeZone.getTimeZone(tzid);
            AccountObj accountAdminObj = serviceAFWeb.getAdminObjFromCache();
            Calendar dateNow = TimeConvertion.getCurrentCalendar();
            long dateNowLong = dateNow.getTimeInMillis();
            java.sql.Date d = new java.sql.Date(dateNowLong);
            DateFormat format = new SimpleDateFormat(" hh:mm a");
            format.setTimeZone(tz);
            String ESTdate = format.format(d);
            String msg = ESTdate + " " + newCustomer.getUsername() + " Cust signup Result:" + result;
            CommMsgImp commMsg = new CommMsgImp();

            commMsg.AddCommMessage(serviceAFWeb, accountAdminObj, ConstantKey.COM_SIGNAL, msg);
//            
            webStatus.setResultID(result);
            return loginObj;
        }
        webStatus.setResultID(0);
        return loginObj;
    }

    public LoginObj getCustomerEmailLogin(ServiceAFweb serviceAFWeb, String EmailUserName, String Password) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return null;
        }
        CustomerObj custObj = null;

        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();
        WebStatus webStatus = new WebStatus();
        webStatus.setResultID(-1);

        custObj = serviceAFWeb.getAccountImp().getCustomerPassword(UserName, Password);
        if (custObj != null) {
            webStatus.setResultID(custObj.getStatus());
            if (custObj.getStatus() != ConstantKey.OPEN) {
                custObj = null;
            }
        }
        LoginObj loginObj = new LoginObj();
        if (custObj != null) {
            custObj.setPassword(CKey.MASK_PASS);
        }
        loginObj.setCustObj(custObj);
        loginObj.setWebMsg(webStatus);
        return loginObj;
    }

    public ArrayList getAccountList(ServiceAFweb serviceAFWeb, String EmailUserName, String Password) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return null;
        }

        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();

        return serviceAFWeb.getAccountImp().getAccountList(UserName, Password);

    }

    public LoginObj getCustomerAccLogin(ServiceAFweb serviceAFWeb, String EmailUserName, String AccountIDSt) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return null;
        }
        CustomerObj custObj = null;

        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();
        AccountObj accountObj = getAccountByCustomerAccountID(serviceAFWeb, EmailUserName, null, AccountIDSt);
        if (accountObj != null) {
            custObj = serviceAFWeb.getAccountImp().getCustomerPassword(UserName, null);
        }
        LoginObj loginObj = new LoginObj();
        if (custObj != null) {
            custObj.setPassword(CKey.MASK_PASS);
        }
        loginObj.setCustObj(custObj);
        WebStatus webStatus = new WebStatus();
        webStatus.setResultID(1);
        if (custObj == null) {
            webStatus.setResultID(0);
        }
        loginObj.setWebMsg(webStatus);
        return loginObj;

    }

    // result 1 = success, 2 = existed,  0 = fail
    public LoginObj updateCustomerPassword(ServiceAFweb serviceAFWeb, String EmailUserName, String AccountID, String Email, String Password, String FirstName, String LastName, String Plan) {

        CustomerObj custObj = null;
        LoginObj loginObj = new LoginObj();
        loginObj.setCustObj(null);
        WebStatus webStatus = new WebStatus();
        webStatus.setResultID(0);
        loginObj.setWebMsg(webStatus);
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return loginObj;
        }
        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();

        custObj = serviceAFWeb.getAccountImp().getCustomerPassword(UserName, null);
        if (custObj == null) {
            return loginObj;
        }
        if (custObj.getStatus() != ConstantKey.OPEN) {
            return loginObj;
        }

        String portfolio = custObj.getPortfolio();
        CustPort custPortfilio = new CustPort();
        if ((portfolio != null) && (portfolio.length() > 0)) {
            try {
                portfolio = portfolio.replaceAll("#", "\"");
                custPortfilio = new ObjectMapper().readValue(portfolio, CustPort.class);
            } catch (Exception ex) {
            }
        }

        if ((Email != null) && (Email.length() > 0)) {
            boolean validEmail = NameObj.isEmailValid(Email);
            if (validEmail == true) {
                custObj.setEmail(Email);
            } else {
                // error code 2 for invalid email address
                webStatus.setResultID(2);
                return loginObj;
            }
        }
        if ((Password != null) && (Password.length() > 0)) {
            // ignore "***"
            if (!Password.equals(CKey.MASK_PASS)) {
                custObj.setPassword(Password);
            }
            // error code 3 for invalid password
        }
        if ((FirstName != null) && (FirstName.length() > 0)) {
            custObj.setFirstname(FirstName);
        }
        if ((LastName != null) && (LastName.length() > 0)) {
            custObj.setLastname(LastName);
        }
        if ((Plan != null) && (Plan.length() > 0)) {
            try {

                int planid = Integer.parseInt(Plan);
                // update pending plan
                // -1 no change, 0, 10, 20
                if (planid == -1) {
                    // no change
                } else {
                    if (custObj.getType() == CustomerObj.INT_API_USER) {
                        ;
                    } else {
                        if ((planid == ConstantKey.INT_PP_BASIC) || (planid == ConstantKey.INT_PP_STANDARD)
                                || (planid == ConstantKey.INT_PP_PEMIUM) || (planid == ConstantKey.INT_PP_DELUXEX2)) {
                            custPortfilio.setnPlan(planid);
                        } else {
                            // error
                            return loginObj;
                        }
                    }
                }
            } catch (Exception e) {
            }
        }
        int result = 0;
        try {
            int accountid = Integer.parseInt(AccountID);
            result = serviceAFWeb.getAccountImp().updateCustomer(custObj, accountid);

            String portfStr = new ObjectMapper().writeValueAsString(custPortfilio);
            result = serviceAFWeb.getAccountImp().updateCustomerPortfolio(custObj.getUsername(), portfStr);
        } catch (Exception ex) {
            logger.info("> updateCustomerPassword exception " + ex.getMessage());
        }

        String tzid = "America/New_York"; //EDT
        TimeZone tz = TimeZone.getTimeZone(tzid);
        AccountObj accountAdminObj = serviceAFWeb.getAdminObjFromCache();
        Calendar dateNow = TimeConvertion.getCurrentCalendar();
        long dateNowLong = dateNow.getTimeInMillis();
        java.sql.Date d = new java.sql.Date(dateNowLong);
        DateFormat format = new SimpleDateFormat(" hh:mm a");
        format.setTimeZone(tz);
        String ESTdate = format.format(d);
        String msg = ESTdate + " " + custObj.getUsername() + " Cust update Result:" + result;
        CommMsgImp commMsg = new CommMsgImp();
        commMsg.AddCommMessage(serviceAFWeb, accountAdminObj, ConstantKey.COM_SIGNAL, msg);
//                            
        webStatus.setResultID(result);
//        loginObj.setCustObj(custObj);
        loginObj.setWebMsg(webStatus);
        return loginObj;

    }

    public AccountObj getAccountByCustomerAccountID(ServiceAFweb serviceAFWeb, String EmailUserName, String Password, String AccountIDSt) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return null;
        }

        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();
        try {
            int accountid = Integer.parseInt(AccountIDSt);
            return serviceAFWeb.getAccountImp().getAccountByCustomerAccountID(UserName, Password, accountid);
        } catch (Exception e) {
        }
        return null;

    }

    public ArrayList<AFstockObj> getStockNameListByAccountID(ServiceAFweb serviceAFWeb, String EmailUserName, String Password, String AccountIDSt) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return null;
        }
        AccountObj accountObj = getAccountByCustomerAccountID(serviceAFWeb, EmailUserName, Password, AccountIDSt);
        if (accountObj != null) {

            ArrayList stockNameList = serviceAFWeb.getAccountImp().getAccountStockNameList(accountObj.getId());
            return stockNameList;
        }
        return null;
    }

    public ArrayList<AFstockObj> getStockListByAccountIDTRname(ServiceAFweb serviceAFWeb, String EmailUserName, String Password, String AccountIDSt, String trname, String filterSt, int lenght) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return null;
        }

        AccountObj accountObj = getAccountByCustomerAccountID(serviceAFWeb, EmailUserName, Password, AccountIDSt);
        if (accountObj != null) {

            ArrayList stockNameList = null;

            ArrayList<String> filterArray = new ArrayList();
            if (filterSt != null) {
                if (filterSt.length() > 0) {
                    String[] filterList = filterSt.split(",");
                    int len = filterList.length;
                    if (len > 50) {
                        len = 50;
                    }
                    for (int i = 0; i < len; i++) {
                        String sym = filterList[i];
                        if (sym.length() > 0) {
                            filterArray.add(sym);
                        }
                    }
                }
            }

            if (filterArray.size() > 0) {
                stockNameList = filterArray;
            } else {
                stockNameList = serviceAFWeb.getAccountImp().getAccountStockNameList(accountObj.getId());
            }

            if (stockNameList != null) {
                if (lenght == 0) {
                    lenght = stockNameList.size();
                } else if (lenght > stockNameList.size()) {
                    lenght = stockNameList.size();
                }

                ArrayList<AFstockObj> returnStockList = new ArrayList();
                for (int i = 0; i < lenght; i++) {
                    String NormalizeSymbol = (String) stockNameList.get(i);

                    AFstockObj stock = serviceAFWeb.getStockRealTimeServ(NormalizeSymbol);
                    if (stock != null) {
                        stock.setTrname(trname);

                        ArrayList<TradingRuleObj> trObjList = serviceAFWeb.getAccountImp().getAccountStockTRListByAccountID(accountObj.getId(), stock.getId());
                        if (trObjList != null) {
                            if (trObjList.size() == 0) {
                                continue;
                            }
                            for (int j = 0; j < trObjList.size(); j++) {
                                TradingRuleObj trObj = trObjList.get(j);
                                if (trname.equals(trObj.getTrname())) {

                                    stock.setTRsignal(trObj.getTrsignal());
                                    float total = getAccountStockRealTimeBalance(serviceAFWeb, trObj);
                                    stock.setPerform(total);
                                    break;
                                }

                            }
                        }

                        returnStockList.add(stock);
                    }
                }
                return returnStockList;
            }
        }
        return null;
    }

    // returen percent
    public float getAccountStockRealTimeBalance(ServiceAFweb serviceAFWeb, TradingRuleObj trObj) {

        float totalPercent = 0;
        float deltaTotal = 0;
        float sharebalance = 0;
        try {
            if (trObj == null) {
                return 0;
            }

            AFstockObj stock = serviceAFWeb.getStockRealTimeServ(trObj.getSymbol());
//            int stockId = trObj.getStockid();            
//            AFstockObj stock = getStockImp().getRealTimeStockByStockID(stockId, null);
            if (stock == null) {
                return 0;
            }
            if (stock.getAfstockInfo() == null) {
                return 0;
            }

            float close = stock.getAfstockInfo().getFclose();
            if (trObj.getTrsignal() == ConstantKey.S_BUY) {
                sharebalance = trObj.getLongamount();
                if (trObj.getLongshare() > 0) {
                    if (close > 0) {
                        deltaTotal = (close - (trObj.getLongamount() / trObj.getLongshare())) * trObj.getLongshare();
                    }
                }
            } else if (trObj.getTrsignal() == ConstantKey.S_SELL) {
                sharebalance = trObj.getShortamount();
                if (trObj.getShortshare() > 0) {
                    if (close > 0) {
                        deltaTotal = ((trObj.getShortamount() / trObj.getShortshare()) - close) * trObj.getShortshare();
                    }
                }
            }
            totalPercent = trObj.getBalance() + sharebalance;
            totalPercent = totalPercent - trObj.getInvestment();

            if (stock.getSubstatus() == 0) {
                totalPercent = totalPercent + deltaTotal;
            }
            totalPercent = (totalPercent / CKey.TRADING_AMOUNT) * 100;
            // rounding 2 decimal round off
            totalPercent = (float) (Math.round(totalPercent * 100.0) / 100.0);
        } catch (Exception ex) {
            logger.info("> getAccountStockRealTimeBalance exception " + ex.getMessage());
        }
        return totalPercent;
    }

    public int addAccountStockByAccount(ServiceAFweb serviceAFWeb, AccountObj accountObj, String symbol) {
        SymbolNameObj symObj = new SymbolNameObj(symbol);
        String NormalizeSymbol = symObj.getYahooSymbol();
        AFstockObj stockObj = serviceAFWeb.getStockImp().getRealTimeStock(NormalizeSymbol, null);
        if (stockObj == null) {
            int result = serviceAFWeb.addStockServ(NormalizeSymbol);
            if (result == 0) {
                return 0;
            }
            //  get the stock object after added into the stockDB
            stockObj = serviceAFWeb.getStockImp().getRealTimeStock(NormalizeSymbol, null);
            if (stockObj == null) {
                return 0;
            }
        }
        if (stockObj.getStatus() != ConstantKey.OPEN) {
            // set to open
            int result = serviceAFWeb.addStockServ(NormalizeSymbol);
            if (result == 0) {
                return 0;
            }
        }
        return serviceAFWeb.getAccountImp().addAccountStockId(accountObj, stockObj.getId(), serviceAFWeb.TRList);
    }

    public int addAccountStockByCustAcc(ServiceAFweb serviceAFWeb, String EmailUserName, String Password, String AccountIDSt, String symbol) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        SymbolNameObj symObj = new SymbolNameObj(symbol);
        String NormalizeSymbol = symObj.getYahooSymbol();
        AFstockObj stockObj = serviceAFWeb.getStockRealTimeServ(NormalizeSymbol);
        if (stockObj == null) {
            int result = serviceAFWeb.addStockServ(NormalizeSymbol);
            if (result == 0) {
                return 0;
            }
            //  get the stock object after added into the stockDB
            stockObj = serviceAFWeb.getStockRealTimeServ(NormalizeSymbol);
            if (stockObj == null) {
                return 0;
            }
        }
        if (stockObj.getStatus() != ConstantKey.OPEN) {
            // set to open
            int result = serviceAFWeb.addStockServ(NormalizeSymbol);
            if (result == 0) {
                return 0;
            }
        }
        AccountObj accountObj = getAccountByCustomerAccountID(serviceAFWeb, EmailUserName, Password, AccountIDSt);
        if (accountObj != null) {
            return serviceAFWeb.getAccountImp().addAccountStockId(accountObj, stockObj.getId(), serviceAFWeb.TRList);
        }
        return 0;
    }

    public int removeAccountStockByUserNameAccId(ServiceAFweb serviceAFWeb, String EmailUserName, String Password, String AccountIDSt, String symbol) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        SymbolNameObj symObj = new SymbolNameObj(symbol);
        String NormalizeSymbol = symObj.getYahooSymbol();
        AFstockObj stockObj = serviceAFWeb.getStockRealTimeServ(NormalizeSymbol);
        if (stockObj != null) {
            AccountObj accountObj = getAccountByCustomerAccountID(serviceAFWeb, EmailUserName, Password, AccountIDSt);
            if (accountObj != null) {
                return removeAccountStockSymbol(serviceAFWeb, accountObj, stockObj.getSymbol());
            }
        }
        return 0;
    }

    //ConstantKey.NOTEXISTED
    public int removeAccountStockSymbol(ServiceAFweb serviceAFWeb, AccountObj accountObj, String symbol) {

        SymbolNameObj symObj = new SymbolNameObj(symbol);
        String NormalizeSymbol = symObj.getYahooSymbol();
        AFstockObj stockObj = serviceAFWeb.getStockRealTimeServ(NormalizeSymbol);
        if (stockObj != null) {

            int signal = ConstantKey.S_NEUTRAL;
            String trName = ConstantKey.TR_ACC;
            TradingRuleObj tradingRuleObj = serviceAFWeb.SystemAccountStockIDByTRname(accountObj.getId(), stockObj.getId(), trName);
            if (tradingRuleObj == null) {
                return ConstantKey.NOTEXISTED;
            }
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

                AccountTranImp accountTran = new AccountTranImp();
                accountTran.AddTransactionOrderWithComm(serviceAFWeb, accountObj, stockObj, trName, signal);
            }

            return serviceAFWeb.getAccountImp().removeAccountStock(accountObj, stockObj.getId());
        }

        return 0;
    }

    public AFstockObj getStockByAccountIDStockID(ServiceAFweb serviceAFWeb, String EmailUserName, String Password, String AccountIDSt, String stockidsymbol) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return null;
        }

        AccountObj accountObj = getAccountByCustomerAccountID(serviceAFWeb, EmailUserName, Password, AccountIDSt);

        int stockID = 0;
        AFstockObj stock = null;
        if (accountObj != null) {
            stock = this.getStockIdSymbol(serviceAFWeb, stockidsymbol);
            if (stock == null) {
                return null;
            }
            stockID = stock.getId();
            ArrayList tradingRuleList = serviceAFWeb.getAccountImp().getAccountStockTRListByAccountID(accountObj.getId(), stockID);
            if (tradingRuleList != null) {
                return stock;
            }
        }
        return null;
    }

    public int addAccountStockTran(ServiceAFweb serviceAFWeb, String EmailUserName, String Password, String AccountIDSt, String stockidsymbol, String trName, int signal) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        AccountObj accountObj = getAccountByCustomerAccountID(serviceAFWeb, EmailUserName, Password, AccountIDSt);
        AFstockObj stock = null;

        if (accountObj != null) {
            stock = this.getStockIdSymbol(serviceAFWeb, stockidsymbol);
            if (stock == null) {
                return 0;
            }

            AccountTranImp accountTran = new AccountTranImp();
            int ret = accountTran.AddTransactionOrderWithComm(serviceAFWeb, accountObj, stock, trName, signal);

            return ret;
        }
        return 0;
    }

    public ArrayList<TradingRuleObj> getAccountStockTRListByAccountID(ServiceAFweb serviceAFWeb, String EmailUserName, String Password, String AccountIDSt, String stockidsymbol) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return null;
        }

        AccountObj accountObj = getAccountByCustomerAccountID(serviceAFWeb, EmailUserName, Password, AccountIDSt);
        AFstockObj stock = null;
        int stockID = 0;
        if (accountObj != null) {
            stock = this.getStockIdSymbol(serviceAFWeb, stockidsymbol);
            if (stock == null) {
                return null;
            }
            stockID = stock.getId();
            return serviceAFWeb.getAccountImp().getAccountStockTRListByAccountID(accountObj.getId(), stockID);
        }
        return null;
    }

    public TradingRuleObj getAccountStockTRByTRname(ServiceAFweb serviceAFWeb, String EmailUserName, String Password, String AccountIDSt, String stockidsymbol, String trname) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return null;
        }

        trname = trname.toUpperCase();
        AccountObj accountObj = getAccountByCustomerAccountID(serviceAFWeb, EmailUserName, Password, AccountIDSt);
        AFstockObj stock = null;
        int stockID = 0;
        if (accountObj != null) {
            stock = this.getStockIdSymbol(serviceAFWeb, stockidsymbol);
            if (stock == null) {
                return null;
            }
            stockID = stock.getId();
            return serviceAFWeb.getAccountImp().getAccountStockIDByTRStockID(accountObj.getId(), stockID, trname);
        }
        return null;
    }

    public int setAccountStockTRoption(ServiceAFweb serviceAFWeb, String EmailUserName, String Password, String AccountIDSt, String stockidsymbol, String trName, String TROptType) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        AccountObj accountObj = getAccountByCustomerAccountID(serviceAFWeb, EmailUserName, Password, AccountIDSt);
        AFstockObj stock = null;

        if (accountObj != null) {
            stock = this.getStockIdSymbol(serviceAFWeb, stockidsymbol);
            if (stock == null) {
                return 0;
            }
            if (trName.toUpperCase().equals(ConstantKey.TR_ACC)) {
                TradingRuleObj tr = serviceAFWeb.getAccountImp().getAccountStockIDByTRStockID(accountObj.getId(), stock.getId(), trName);
                if (tr == null) {
                    return 0;
                }
                int opt = 0;
                try {
                    if (TROptType != null) {
                        opt = Integer.parseInt(TROptType);
                    }
                } catch (NumberFormatException ex) {

                }

                if (opt < ConstantKey.SIZE_TR) {
                    tr.setLinktradingruleid(opt);

                    ArrayList<TradingRuleObj> UpdateTRList = new ArrayList();
                    UpdateTRList.add(tr);
                    return serviceAFWeb.getAccountImp().updateAccountStockSignal(UpdateTRList);
                }
            }
        }
        return 0;

    }

    public ArrayList<TransationOrderObj> getAccountStockTRTranListByAccountID(ServiceAFweb serviceAFWeb, String EmailUserName, String Password, String AccountIDSt, String stockidsymbol, String trName, int length) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return null;
        }

        AccountObj accountObj = getAccountByCustomerAccountID(serviceAFWeb, EmailUserName, Password, AccountIDSt);
        AFstockObj stock = null;

        if (accountObj != null) {
            stock = this.getStockIdSymbol(serviceAFWeb, stockidsymbol);
            if (stock == null) {
                return null;
            }

            if (trName.toUpperCase().equals(ConstantKey.TR_ACC)) {
                return serviceAFWeb.getAccountImp().getAccountStockTransList(accountObj.getId(), stock.getId(), trName.toUpperCase(), length);
            } else {
                AccountObj accountAdminObj = serviceAFWeb.getAdminObjFromCache();
                if (accountAdminObj == null) {
                    return null;
                }
                return serviceAFWeb.getAccountImp().getAccountStockTransList(accountAdminObj.getId(), stock.getId(), trName.toUpperCase(), length);
            }
        }
        return null;
    }

    public ArrayList<StockTRHistoryObj> getAccountStockTRListHistory(ServiceAFweb serviceAFWeb, String EmailUserName, String Password, String AccountIDSt, String stockidsymbol, String trname) {
        TradingSignalProcess TRprocessImp = new TradingSignalProcess();
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return null;
        }

        ArrayList<TradingRuleObj> trObjList = getAccountStockTRListByAccountID(serviceAFWeb, EmailUserName, Password, AccountIDSt, stockidsymbol);
        trname = trname.toUpperCase();
        if (trObjList != null) {
            for (int i = 0; i < trObjList.size(); i++) {
                TradingRuleObj trObj = trObjList.get(i);
                if (trname.equals(trObj.getTrname())) {
                    ArrayList<StockTRHistoryObj> thObjList = TRprocessImp.ProcessTRHistory(serviceAFWeb, trObj, 2, CKey.MONTH_SIZE);
                    return thObjList;
                }
            }
        }
        return null;
    }

    public ArrayList<String> getAccountStockTRListHistoryDisplay(ServiceAFweb serviceAFWeb, String EmailUserName, String Password, String AccountIDSt, String stockidsymbol, String trname) {

        ArrayList<StockTRHistoryObj> thObjList = getAccountStockTRListHistory(serviceAFWeb, EmailUserName, Password, AccountIDSt, stockidsymbol, trname);

        ArrayList<String> writeArray = new ArrayList();
        ArrayList<String> displayArray = new ArrayList();
        int ret = getAccountStockTRListHistoryDisplayProcess(thObjList, writeArray, displayArray);
        if (ret == 1) {
            if (getEnv.checkLocalPC() == true) {
                FileUtil.FileWriteTextArray(serviceAFWeb.FileLocalDebugPath + stockidsymbol + "_" + trname + "_tran.csv", writeArray);
            }
        }
        return displayArray;
    }

    public int getAccountStockTRListHistoryDisplayProcess(ArrayList<StockTRHistoryObj> trObjList, ArrayList<String> writeArray, ArrayList<String> displayArray) {

        if (trObjList == null) {
            return 0;
        }
        for (int i = 0; i < trObjList.size(); i++) {
            StockTRHistoryObj trObj = trObjList.get(i);
            String st = "";
            String stDispaly = "";
            if (writeArray.size() == 0) {
                st = "\"symbol" + "\",\"trname" + "\",\"type";
                /////
                if (trObj.getType() == ConstantKey.INT_TR_MV) {
                    st += "\",\"ema2050" + "\",\"last ema2050" + "\",\"LTerm" + "\",\"STerm" + "\",\"-";

                } else if (trObj.getType() == ConstantKey.INT_TR_MACD) {
                    st += "\",\"macd 12 26" + "\",\"signal 9" + "\",\"diff" + "\",\"-" + "\",\"-";

                } else if (trObj.getType() == ConstantKey.INT_TR_RSI) {
                    st += "\",\"rsi 14" + "\",\"last rsi 14" + "\",\"-" + "\",\"-" + "\",\"-";

                } else {
                    st += "\",\"parm1" + "\",\"parm2" + "\",\"parm3" + "\",\"parm4" + "\",\"name";
                }
                ////
                st += "\",\"close" + "\",\"trsignal" + "\",\"updateDateD" + "\"";
                writeArray.add(st);
                stDispaly = st.replaceAll("\"", "");
                displayArray.add(stDispaly);
            }
            st = "\"" + trObj.getSymbol() + "\",\"" + trObj.getTrname() + "\",\"" + trObj.getType();
            st += "\",\"" + trObj.getParm1() + "\",\"" + trObj.getParm2() + "\",\"" + trObj.getParm3() + "\",\"" + trObj.getParm4() + "\",\"" + trObj.getParmSt1();
            st += "\",\"" + trObj.getClose() + "\",\"" + trObj.getTrsignal() + "\",\"" + trObj.getUpdateDateD() + "\"";

            writeArray.add(st);
            stDispaly = st.replaceAll("\"", "");
            displayArray.add(stDispaly);

        }

        return 1;
    }

    public ArrayList<String> getAccountStockTRPerfHistoryDisplay(ServiceAFweb serviceAFWeb, String EmailUserName, String Password, String AccountIDSt, String stockidsymbol, String trname) {

        ArrayList<StockTRHistoryObj> trObjList = getAccountStockTRListHistory(serviceAFWeb, EmailUserName, Password, AccountIDSt, stockidsymbol, trname);
        ArrayList<String> writeTranArray = new ArrayList();
        ArrayList<String> displayArray = new ArrayList();
        int ret = getAccountStockTRListHistoryDisplayProcess(trObjList, writeTranArray, displayArray);

        ArrayList<PerformanceObj> perfObjList = getAccountStockTRPerfHistory(serviceAFWeb, EmailUserName, Password, AccountIDSt, stockidsymbol, trname, 0);
        ArrayList<String> writePerfArray = new ArrayList();
        ArrayList<String> perfList = new ArrayList();
        ret = getAccountStockTRPerfHistoryDisplayProcess(perfObjList, writePerfArray, perfList);

        ArrayList<String> writeAllArray = new ArrayList();
        if (ret == 1) {
            if (getEnv.checkLocalPC() == true) {
                int j = 0;
                for (int i = 0; i < writeTranArray.size(); i++) {
                    if (i == 0) {
                        String st = writeTranArray.get(i);
                        st += "," + writePerfArray.get(j);
                        j++;
                        writeAllArray.add(st);
                        continue;
                    }
                    StockTRHistoryObj tran = trObjList.get(i - 1);

                    if (j >= perfObjList.size()) {
                        j = perfObjList.size() - 1;
                    }
                    PerformanceObj perf = perfObjList.get(j - 1);

                    if (tran.getUpdateDateD().equals(perf.getUpdateDateD())) {
                        String st = writeTranArray.get(i);
                        st += "," + writePerfArray.get(j);
                        writeAllArray.add(st);
                        j++;
                        if (j >= perfObjList.size()) {
                            j = perfObjList.size() - 1;
                        } else {
                            while (true) {
                                perf = perfObjList.get(j - 1);
                                if (tran.getUpdateDateD().equals(perf.getUpdateDateD())) {
                                    st = writeTranArray.get(i);
                                    st += "," + writePerfArray.get(j);
                                    j++;
                                    if (j >= perfObjList.size()) {
                                        break;
                                    }
                                } else {
                                    break;
                                }
                            }
                        }
                        continue;
                    }
                    String st = writeTranArray.get(i);
                    writeAllArray.add(st);
                }
                if (getEnv.checkLocalPC() == true) {
                    FileUtil.FileWriteTextArray(serviceAFWeb.FileLocalDebugPath + stockidsymbol + "_" + trname + "_perf.csv", writeAllArray);
                }
            }

        }
        return perfList;

    }

    public int getAccountStockTRPerfHistoryDisplayProcess(ArrayList<PerformanceObj> perfObjList, ArrayList<String> writePerfArray, ArrayList<String> perfList) {

        if (perfObjList == null) {
            return 0;
        }

        for (int i = 0; i < perfObjList.size(); i++) {
            PerformanceObj trObj = perfObjList.get(i);
            String st = "";
            String stDispaly = "";
            if (writePerfArray.size() == 0) {
                st = "\"Investment" + "\",\"Balance" + "\",\"Grossprofit"
                        + "\",\"Netprofit" + "\",\"Numtrade" + "\",\"Rating"
                        + "\",\"Close" + "\",\"Trsignal" + "\",\"Numwin" + "\",\"Numloss"
                        + "\",\"Avgwin" + "\",\"Avgloss" + "\",\"Maxwin" + "\",\"Maxloss"
                        + "\",\"Maxholdtime" + "\",\"Minholdtime" + "\",\"updateDateD" + "\"";
                writePerfArray.add(st);
                stDispaly = st.replaceAll("\"", "");
                perfList.add(stDispaly);
            }
            st = "\"" + trObj.getInvestment() + "\",\"" + trObj.getBalance() + "\",\"" + trObj.getGrossprofit()
                    + "\",\"" + trObj.getNetprofit() + "\",\"" + trObj.getNumtrade() + "\",\"" + trObj.getRating()
                    + "\",\"" + trObj.getPerformData().getClose() + "\",\"" + trObj.getPerformData().getTrsignal()
                    + "\",\"" + trObj.getPerformData().getNumwin() + "\",\"" + trObj.getPerformData().getNumloss()
                    + "\",\"" + trObj.getPerformData().getAvgwin() + "\",\"" + trObj.getPerformData().getAvgloss()
                    + "\",\"" + trObj.getPerformData().getMaxwin() + "\",\"" + trObj.getPerformData().getMaxloss()
                    + "\",\"" + trObj.getPerformData().getMaxholdtime() + "\",\"" + trObj.getPerformData().getMinholdtime()
                    + "\",\"" + trObj.getUpdateDateD() + "\"";

            writePerfArray.add(st);
            stDispaly = st.replaceAll("\"", "");
            perfList.add(stDispaly);

        }

        return 1;
    }

    private AFstockObj getStockIdSymbol(ServiceAFweb serviceAFWeb, String stockidsymbol) {
        AFstockObj stock = null;
        try {
            int stockID = Integer.parseInt(stockidsymbol);
            stock = serviceAFWeb.getStockRealTimeByStockIDServ(stockID);
        } catch (NumberFormatException e) {
            SymbolNameObj symObj = new SymbolNameObj(stockidsymbol);
            String NormalizeSymbol = symObj.getYahooSymbol();
            stock = serviceAFWeb.getStockRealTimeServ(NormalizeSymbol);
        }
        return stock;
    }

    public ArrayList<PerformanceObj> getAccountStockTRPerfHistory(ServiceAFweb serviceAFWeb, String EmailUserName, String Password, String AccountIDSt, String stockidsymbol, String trName, int length) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return null;
        }

        AccountObj accountObj = getAccountByCustomerAccountID(serviceAFWeb, EmailUserName, Password, AccountIDSt);
        AFstockObj stock = null;
        if (accountObj != null) {
            stock = this.getStockIdSymbol(serviceAFWeb, stockidsymbol);

            if (stock == null) {
                return null;
            }
            ArrayList<TransationOrderObj> tranOrderList = null;
            if (trName.toUpperCase().equals(ConstantKey.TR_ACC)) {
                tranOrderList = serviceAFWeb.getAccountImp().getAccountStockTransList(accountObj.getId(), stock.getId(), trName.toUpperCase(), 0);
                TradingSignalProcess TRprocessImp = new TradingSignalProcess();
                return TRprocessImp.ProcessTranPerfHistory(serviceAFWeb, tranOrderList, stock, length, true);  // buyOnly = true
            }

            AccountObj accountAdminObj = serviceAFWeb.getAdminObjFromCache();
            if (accountAdminObj == null) {
                return null;
            }
            tranOrderList = serviceAFWeb.getAccountImp().getAccountStockTransList(accountAdminObj.getId(), stock.getId(), trName.toUpperCase(), 0);

            TradingSignalProcess TRprocessImp = new TradingSignalProcess();
            return TRprocessImp.ProcessTranPerfHistory(serviceAFWeb, tranOrderList, stock, length, false);  // buyOnly = false
        }
        return null;
    }

    public ArrayList<PerformanceObj> getAccountStockTRPerfHistoryReinvest(ServiceAFweb serviceAFWeb, String EmailUserName, String Password, String AccountIDSt, String stockidsymbol, String trName, int length) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return null;
        }

        AccountObj accountObj = getAccountByCustomerAccountID(serviceAFWeb, EmailUserName, Password, AccountIDSt);
        AFstockObj stock = null;
        if (accountObj != null) {
            stock = this.getStockIdSymbol(serviceAFWeb, stockidsymbol);

            if (stock == null) {
                return null;
            }
            ArrayList<TransationOrderObj> tranOrderList = null;
            if (trName.toUpperCase().equals(ConstantKey.TR_ACC)) {
                tranOrderList = serviceAFWeb.getAccountImp().getAccountStockTransList(accountObj.getId(), stock.getId(), trName.toUpperCase(), 0);
                TradingSignalProcess TRprocessImp = new TradingSignalProcess();
                return TRprocessImp.ProcessTranPerfHistoryReinvest(serviceAFWeb, tranOrderList, stock, length, true);  // buyOnly = true

            }
            AccountObj accountAdminObj = serviceAFWeb.getAdminObjFromCache();
            if (accountAdminObj == null) {
                return null;
            }
            tranOrderList = serviceAFWeb.getAccountImp().getAccountStockTransList(accountAdminObj.getId(), stock.getId(), trName.toUpperCase(), 0);

            TradingSignalProcess TRprocessImp = new TradingSignalProcess();
            return TRprocessImp.ProcessTranPerfHistoryReinvest(serviceAFWeb, tranOrderList, stock, length, false); //buyOnly = false
        }
        return null;
    }

    public String getAccountStockTRLIstCurrentChartFile(ServiceAFweb serviceAFWeb, String EmailUserName, String Password, String AccountIDSt, String stockidsymbol, String trname, String pathSt) {
        TradingNNprocess NNProcessImp = new TradingNNprocess();
        try {
            CustAccService custAccSrv = new CustAccService();
            ArrayList<TransationOrderObj> thList = custAccSrv.getAccountStockTRTranListByAccountID(serviceAFWeb, EmailUserName, Password, AccountIDSt, stockidsymbol, trname, 0);
            if (thList == null) {
                return null;
            }
            Collections.reverse(thList);

            trname = trname.toUpperCase();
            String symbol = stockidsymbol;
            AFstockObj stock = serviceAFWeb.getStockRealTimeServ(symbol);

            int size1year = 20 * 10;
            ArrayList<AFstockInfo> StockArray = serviceAFWeb.getStockHistoricalServ(stock.getSymbol(), size1year);
            if (StockArray == null) {
                return null;
            }
            Collections.reverse(StockArray);

            List<Date> xDate = new ArrayList<Date>();
            List<Double> yD = new ArrayList<Double>();

            ArrayList<Float> closeList = new ArrayList<Float>();
            for (int i = 0; i < StockArray.size(); i++) {
                AFstockInfo stockinfo = StockArray.get(i);
                float close = stockinfo.getFclose();
                closeList.add(close);
            }
            NNormalObj normal = new NNormalObj();
            normal.initHighLow(closeList);

            List<Date> buyDate = new ArrayList<Date>();
            List<Double> buyD = new ArrayList<Double>();
            List<Date> sellDate = new ArrayList<Date>();
            List<Double> sellD = new ArrayList<Double>();

            for (int j = 0; j < StockArray.size(); j++) {
                AFstockInfo stockinfo = StockArray.get(j);

                Date da = new Date(stockinfo.getEntrydatel());
                xDate.add(da);
                long stockdatel = TimeConvertion.endOfDayInMillis(stockinfo.getEntrydatel());
                float close = stockinfo.getFclose();
                double norClose = normal.getNormalizeValue(close);
                yD.add(norClose);
                for (int i = 0; i < thList.size(); i++) {
                    TransationOrderObj thObj = thList.get(i);
                    long THdatel = TimeConvertion.endOfDayInMillis(thObj.getEntrydatel());
                    if (stockdatel != THdatel) {
                        continue;
                    }

                    TransationOrderObj thObjNext = thObj;
                    if ((thObj.getTrsignal() == ConstantKey.S_BUY) || (thObj.getTrsignal() == ConstantKey.S_SELL)) {
                        ;
                    } else {
                        if (i + 1 < thList.size()) {
                            thObjNext = thList.get(i + 1);
                        }
                        long THdatelNext = TimeConvertion.endOfDayInMillis(thObjNext.getEntrydatel());
                        if (THdatel == THdatelNext) {
                            thObj = thObjNext;
                        }
                    }

                    int signal = thObj.getTrsignal();
                    if (signal == ConstantKey.S_BUY) {
                        buyD.add(norClose);
                        buyDate.add(da);
                    } else if (signal == ConstantKey.S_SELL) {
                        sellD.add(norClose);
                        sellDate.add(da);
                    } else {
                        sellD.add(norClose);
                        sellDate.add(da);
                        buyD.add(norClose);
                        buyDate.add(da);
                    }
                    break;
                }
            }
            if ((pathSt == null) || (pathSt.length() == 0)) {
                pathSt = "t:/Netbean/debug";
            }
            if (getEnv.checkLocalPC() == true) {
                pathSt = "t:/Netbean/debug";
            }
            String filepath = pathSt + "/" + stockidsymbol + "_" + trname;

            ChartService chart = new ChartService();
            chart.saveChartToFile(stockidsymbol + "_" + trname, filepath, xDate, yD, buyDate, buyD, sellDate, sellD);
            return "Save in " + filepath;
        } catch (Exception ex) {
            logger.info("> getAccountStockTRLIstCurrentChartFile exception" + ex.getMessage());
        }
        return "Save failed";
    }

    public byte[] getAccountStockTRLIstCurrentChartDisplay(ServiceAFweb serviceAFWeb, String EmailUserName, String Password, String AccountIDSt, String stockidsymbol,
            String trname, String monthSt) {

        int month = 6;
        if (monthSt != null) {
            try {
                month = Integer.parseInt(monthSt);
                if (month > 48) {
                    month = 48;
                }
            } catch (Exception ex) {
            }
        }

        ArrayList<TransationOrderObj> thList = getAccountStockTRTranListByAccountID(serviceAFWeb, EmailUserName, Password, AccountIDSt, stockidsymbol, trname, 0);

        if (thList == null) {
            thList = new ArrayList();
        }
        String symbol = stockidsymbol;
        AFstockObj stock = serviceAFWeb.getStockRealTimeServ(symbol);
        if (stock == null) {
            return null;
        }
        int sizeLen = 20 * 10;

        if (month > 0) {
            sizeLen = 20 * month;
        }

        // recent date first
        ArrayList<AFstockInfo> StockArray = serviceAFWeb.getStockHistoricalServ(stock.getSymbol(), sizeLen);
        if (StockArray == null) {
            return null;
        }
        if (StockArray.size() < 10) {
            return null;
        }
        // recent date last
        Collections.reverse(StockArray);
        Collections.reverse(thList);

        ArrayList<AFstockInfo> StockArrayTmp = new ArrayList();

        List<Date> xDate = new ArrayList<Date>();
        List<Double> yD = new ArrayList<Double>();

        List<Date> buyDate = new ArrayList<Date>();
        List<Double> buyD = new ArrayList<Double>();
        List<Date> sellDate = new ArrayList<Date>();
        List<Double> sellD = new ArrayList<Double>();

        xDate = new ArrayList<Date>();
        yD = new ArrayList<Double>();
        buyDate = new ArrayList<Date>();
        buyD = new ArrayList<Double>();
        sellDate = new ArrayList<Date>();
        sellD = new ArrayList<Double>();

        StockArrayTmp = new ArrayList();
        for (int i = 0; i < StockArray.size(); i++) {
            StockArrayTmp.add(StockArray.get(i));
        }
        int numBS = this.checkCurrentChartDisplay(StockArrayTmp, xDate, yD, buyDate, buyD, sellDate, sellD, thList);

        ChartService chart = new ChartService();
        byte[] ioStream = chart.streamChartToByte(stockidsymbol + "_" + trname,
                xDate, yD, buyDate, buyD, sellDate, sellD);

        return ioStream;

    }

    private int checkCurrentChartDisplay(ArrayList<AFstockInfo> StockArray, List<Date> xDate, List<Double> yD,
            List<Date> buyDate, List<Double> buyD, List<Date> sellDate, List<Double> sellD,
            ArrayList<TransationOrderObj> thList) {

        for (int j = 0; j < StockArray.size(); j++) {
            AFstockInfo stockinfo = StockArray.get(j);

            Date da = new Date(stockinfo.getEntrydatel());
            xDate.add(da);
            float close = stockinfo.getFclose();
            double norClose = close;
            yD.add(norClose);

        }

        AFstockInfo stockinfo = StockArray.get(0);
        long stockdatel = stockinfo.getEntrydatel();
        for (int i = 0; i < thList.size(); i++) {
            TransationOrderObj thObj = thList.get(i);

            long THdatel = thObj.getEntrydatel(); //TimeConvertion.endOfDayInMillis(thObj.getEntrydatel());
            if (stockdatel > THdatel) {
                continue;
            }
            TransationOrderObj thObjNext = thObj;
            if ((thObj.getTrsignal() == ConstantKey.S_BUY) || (thObj.getTrsignal() == ConstantKey.S_SELL)) {
                ;
            } else {
                if (i + 1 < thList.size()) {
                    thObjNext = thList.get(i + 1);
                }
                long THdatelNext = TimeConvertion.endOfDayInMillis(thObjNext.getEntrydatel());
                if (THdatel == THdatelNext) {
                    thObj = thObjNext;
                }
            }
            int signal = thObj.getTrsignal();

            float close = thObj.getAvgprice();
            double norClose = close;
            Date da = new Date(thObj.getEntrydatel());
            if (signal == ConstantKey.S_BUY) {
                buyD.add(norClose);
                buyDate.add(da);
            } else if (signal == ConstantKey.S_SELL) {
                sellD.add(norClose);
                sellDate.add(da);
            } else {
//                sellD.add(norClose);
//                sellDate.add(da);
//                buyD.add(norClose);
//                buyDate.add(da);
            }
        }

//        /// add this one to show the trend line
        Date da = new Date(stockinfo.getEntrydatel());
        xDate.add(da);
        float close = stockinfo.getFclose();
        double norClose = close;
        yD.add(norClose);

        return buyD.size() + sellD.size();
    }

    public byte[] getFundAccountStockTRLIstCurrentChartDisplay(ServiceAFweb serviceAFWeb, String EmailUserName, String Password, String AccountIDSt, String FundIDSt, String stockidsymbol,
            String trname, String monthSt) {
        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();

        CustomerObj custObj = serviceAFWeb.getAccountImp().getCustomerPassword(UserName, Password);
        if (custObj == null) {
            return null;
        }
        if (custObj.getStatus() != ConstantKey.OPEN) {
            return null;
        }

        String portfolio = custObj.getPortfolio();
        CustPort custPortfilio = null;
        try {
            if ((portfolio != null) && (portfolio.length() > 0)) {
                portfolio = portfolio.replaceAll("#", "\"");
                custPortfilio = new ObjectMapper().readValue(portfolio, CustPort.class);
            }
        } catch (Exception ex) {
        }
        if (custPortfilio == null) {
            return null;
        }

        ArrayList<String> featL = custPortfilio.getFeatL();
        if (featL == null) {
            return null;
        }
        int accFundId = Integer.parseInt(FundIDSt);
        AccountObj accFundObj = serviceAFWeb.getAccountImp().getAccountObjByAccountID(accFundId);
        AFstockObj stock = null;
        stock = this.getStockIdSymbol(serviceAFWeb, stockidsymbol);

        if (stock == null) {
            return null;
        }

        int month = 6;
        if (monthSt != null) {
            try {
                month = Integer.parseInt(monthSt);
                if (month > 48) {
                    month = 48;
                }
            } catch (Exception ex) {
            }
        }

        ArrayList<TransationOrderObj> thList = serviceAFWeb.getAccountImp().getAccountStockTransList(accFundObj.getId(), stock.getId(), trname.toUpperCase(), 0);

        if (thList == null) {
            thList = new ArrayList();
        }

        int sizeLen = 20 * 10;

        if (month > 0) {
            sizeLen = 20 * month;
        }

        // recent date first
        ArrayList<AFstockInfo> StockArray = serviceAFWeb.getStockHistoricalServ(stock.getSymbol(), sizeLen);
        if (StockArray == null) {
            return null;
        }
        if (StockArray.size() < 10) {
            return null;
        }
        // recent date last
        Collections.reverse(StockArray);
        Collections.reverse(thList);

        ArrayList<AFstockInfo> StockArrayTmp = new ArrayList();

        List<Date> xDate = new ArrayList<Date>();
        List<Double> yD = new ArrayList<Double>();

        List<Date> buyDate = new ArrayList<Date>();
        List<Double> buyD = new ArrayList<Double>();
        List<Date> sellDate = new ArrayList<Date>();
        List<Double> sellD = new ArrayList<Double>();

        xDate = new ArrayList<Date>();
        yD = new ArrayList<Double>();
        buyDate = new ArrayList<Date>();
        buyD = new ArrayList<Double>();
        sellDate = new ArrayList<Date>();
        sellD = new ArrayList<Double>();

        StockArrayTmp = new ArrayList();
        for (int i = 0; i < StockArray.size(); i++) {
            StockArrayTmp.add(StockArray.get(i));
        }
        int numBS = this.checkCurrentChartDisplay(StockArrayTmp, xDate, yD, buyDate, buyD, sellDate, sellD, thList);

        ChartService chart = new ChartService();
        byte[] ioStream = chart.streamChartToByte(stockidsymbol + "_" + trname,
                xDate, yD, buyDate, buyD, sellDate, sellD);

        return ioStream;
    }

    public int getAccountStockTRClrTranByAccountID(ServiceAFweb serviceAFWeb, String EmailUserName, String Password, String AccountIDSt, String stockidsymbol, String trName) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        AccountObj accountObj = getAccountByCustomerAccountID(serviceAFWeb, EmailUserName, Password, AccountIDSt);
        AFstockObj stock = null;
        if (accountObj != null) {
            stock = this.getStockIdSymbol(serviceAFWeb, stockidsymbol);

            if (stock == null) {
                return 0;
            }
            return serviceAFWeb.getAccountImp().clearAccountStockTranByAccountID(accountObj, stock.getId(), trName.toUpperCase());
        }
        return 0;
    }

    public ArrayList<PerformanceObj> getAccountStockTRPerfList(ServiceAFweb serviceAFWeb, String EmailUserName, String Password, String AccountIDSt, String stockidsymbol, String trName, int length) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return null;
        }

        AccountObj accountObj = getAccountByCustomerAccountID(serviceAFWeb, EmailUserName, Password, AccountIDSt);
        AFstockObj stock = null;
        if (accountObj != null) {
            stock = this.getStockIdSymbol(serviceAFWeb, stockidsymbol);
            if (stock == null) {
                return null;
            }
            ArrayList<PerformanceObj> perfList = null;
            if (trName.toUpperCase().equals(ConstantKey.TR_ACC)) {
                perfList = serviceAFWeb.getAccountImp().getAccountStockPerfList(accountObj.getId(), stock.getId(), trName, length);
            } else {
                AccountObj accountAdminObj = serviceAFWeb.getAdminObjFromCache();
                if (accountAdminObj == null) {
                    return null;
                }
                perfList = serviceAFWeb.getAccountImp().getAccountStockPerfList(accountAdminObj.getId(), stock.getId(), trName, length);
            }
            return perfList;
        }
        return null;
    }

    public ArrayList getCustomerNList(ServiceAFweb serviceAFWeb, int length) {
        ArrayList result = null;
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return null;
        }

        result = serviceAFWeb.getAccountImp().getCustomerNList(length);
        return result;
    }

    public ArrayList getCustomerList(ServiceAFweb serviceAFWeb, int length) {
        ArrayList result = null;
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return null;
        }
        result = serviceAFWeb.getAccountImp().getCustomerObjList(length);

        return result;
    }

    //only on type=" + CustomerObj.INT_CLIENT_BASIC_USER;
    public ArrayList getExpiredCustomerList(ServiceAFweb serviceAFWeb, int length) {
        ArrayList result = null;
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return null;
        }

        result = serviceAFWeb.getAccountImp().getExpiredCustomerList(length);
        return result;
    }

//http://localhost:8080/cust/admin1/sys/cust/eddy/status/0/substatus/0
    public int updateCustStatusSubStatus(ServiceAFweb serviceAFWeb, String customername, String statusSt, String substatusSt) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return 0;
        }
//
//        if (checkCallRemoteMysql() == true) {
//            return getServiceAFwebREST().updateCustStatusSubStatus(customername, statusSt, substatusSt);
//        }

        int status;
        int substatus;
        try {
            status = Integer.parseInt(statusSt);
            substatus = Integer.parseInt(substatusSt);
        } catch (NumberFormatException e) {
            return 0;
        }
        CustomerObj custObj = serviceAFWeb.getAccountImp().getCustomerBySystem(customername, null);
        custObj.setStatus(status);
        custObj.setSubstatus(substatus);
        return serviceAFWeb.getAccountImp().updateCustStatusSubStatus(custObj, custObj.getStatus(), custObj.getSubstatus());
    }
    //http://localhost:8080/cust/admin1/sys/cust/eddy/update?substatus=10&investment=0&balance=15&?reason=

    public int updateAddCustStatusPaymentBalance(ServiceAFweb serviceAFWeb, String customername,
            String statusSt, String paymentSt, String balanceSt, String yearSt, String reasonSt) {

        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        customername = customername.toUpperCase();
        NameObj nameObj = new NameObj(customername);
        String UserName = nameObj.getNormalizeName();
        try {
            CustomerObj customer = serviceAFWeb.getAccountImp().getCustomerPasswordNull(UserName);
            if (customer == null) {
                return 0;
            }
            ArrayList accountList = getAccountList(serviceAFWeb, UserName, null);

            if (accountList == null) {
                return 0;
            }
            AccountObj accountObj = null;
            for (int i = 0; i < accountList.size(); i++) {
                AccountObj accountTmp = (AccountObj) accountList.get(i);
                if (accountTmp.getType() == AccountObj.INT_TRADING_ACCOUNT) {
                    accountObj = accountTmp;
                    break;
                }
            }
            if (accountObj == null) {
                return 0;
            }
            String emailSt = "";
            int status = -9999;
            if (statusSt != null) {
                if (!statusSt.equals("")) {
                    status = Integer.parseInt(statusSt);
                    String st = "Disabled";
                    if (status == ConstantKey.OPEN) {
                        st = "Enabled";
                    }
                    emailSt += "\n\r " + customername + " Accout Status change to " + st;
                }
            }
            float payment = -9999;
            if (paymentSt != null) {
                if (!paymentSt.equals("")) {
                    payment = Float.parseFloat(paymentSt);
                    NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.US);
                    String currency = formatter.format(payment);
                    emailSt += "\n\r " + customername + " Accout invoice bill adjust " + currency;

                }
            }
            float balance = -9999;
            if (balanceSt != null) {
                if (!balanceSt.equals("")) {
                    balance = Float.parseFloat(balanceSt);

                    NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.US);
                    String currency = formatter.format(balance);
                    emailSt += "\n\r " + customername + " Accout balance adjust " + currency;

                    ////////update accounting entry
                    String entryName = "";
                    if (reasonSt != null) {
                        if (reasonSt.length() > 0) {
                            entryName = reasonSt;
                        }
                    }
                    if (customer != null) {
                        boolean byPassPayment = BillingProcess.isSystemAccount(customer);
                        if (byPassPayment == false) {
                            int year = 0;
                            if (yearSt != null) {
                                if (yearSt.length() > 0) {
                                    try {
                                        year = Integer.parseInt(yearSt);
                                    } catch (Exception e) {
                                    }
                                }
                            }

                            if (entryName.equals(AccountingProcess.E_USER_WITHDRAWAL)) {
                                // UI will set payment to negative 
                                float withdraw = -balance;
                                int ret = serviceAFWeb.getAccounting().addTransferWithDrawRevenueTax(serviceAFWeb, customer, withdraw, year, entryName + " " + emailSt);
                            } else if (entryName.equals(AccountingProcess.R_USER_PAYMENT)) {
                                int ret = serviceAFWeb.getAccounting().addTransferRevenueTax(serviceAFWeb, customer, balance, year, emailSt);
                            }
                        }
                    }

                }
            }
            int ret = serviceAFWeb.getAccountImp().updateAddCustStatusPaymentBalance(UserName, status, payment, balance);
            if (ret == 1) {
                String tzid = "America/New_York"; //EDT
                TimeZone tz = TimeZone.getTimeZone(tzid);
                java.sql.Date d = new java.sql.Date(TimeConvertion.currentTimeMillis());
//                                DateFormat format = new SimpleDateFormat("M/dd/yyyy hh:mm a z");
                DateFormat format = new SimpleDateFormat(" hh:mm a");
                format.setTimeZone(tz);
                String ESTtime = format.format(d);

                String msg = ESTtime + " " + emailSt;

                serviceAFWeb.getAccountImp().addAccountMessage(accountObj, ConstantKey.ACCT_TRAN, msg);
                AccountObj accountAdminObj = serviceAFWeb.getAdminObjFromCache();
                serviceAFWeb.getAccountImp().addAccountMessage(accountAdminObj, ConstantKey.ACCT_TRAN, msg);

                // send email
                DateFormat formatD = new SimpleDateFormat("M/dd/yyyy hh:mm a");
                formatD.setTimeZone(tz);
                String ESTdateD = formatD.format(d);
                String msgD = ESTdateD + " " + emailSt;
                serviceAFWeb.getAccountImp().addAccountEmailMessage(accountObj, ConstantKey.ACCT_TRAN, msgD);

            }
            return ret;

        } catch (Exception e) {
        }
        return 0;
    }

    public int changeFundCustomer(ServiceAFweb serviceAFWeb, String customername) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return 0;
        }
        CustomerObj custObj = serviceAFWeb.getAccountImp().getCustomerBySystem(customername, null);

        if (custObj == null) {
            return 0;
        }

        if (custObj.getStatus() != ConstantKey.OPEN) {
            return 0;
        }
        custObj.setType(CustomerObj.INT_FUND_USER);
        custObj.setSubstatus(ConstantKey.INT_PP_PEMIUM);
        custObj.setPayment(0);

        int result = serviceAFWeb.getAccountImp().systemUpdateCustAllStatus(custObj);
        if (result == 1) {
            String accountName = "acc-" + custObj.getId() + "-" + AccountObj.MUTUAL_FUND_ACCOUNT;
            result = serviceAFWeb.getAccountImp().addAccountTypeSubStatus(custObj, accountName, AccountObj.INT_MUTUAL_FUND_ACCOUNT, ConstantKey.OPEN);
        }
        /// clear the last build to regenerate new bill
        AccountObj account = serviceAFWeb.getAccountImp().getAccountByType(custObj.getUsername(), null, AccountObj.INT_TRADING_ACCOUNT);
        // get last bill
        ArrayList<BillingObj> billingObjList = serviceAFWeb.getAccountImp().getBillingByCustomerAccountID(custObj.getUsername(), null, account.getId(), 2);
        if (billingObjList != null) {
            if (billingObjList.size() > 0) {
                BillingObj billObj = billingObjList.get(0);
                serviceAFWeb.getAccountImp().removeBillingByCustomerAccountID(custObj.getUsername(), null, account.getId(), billObj.getId());
            }
        }
        String tzid = "America/New_York"; //EDT
        TimeZone tz = TimeZone.getTimeZone(tzid);
        AccountObj accountAdminObj = serviceAFWeb.getAdminObjFromCache();
        Calendar dateNow = TimeConvertion.getCurrentCalendar();
        long dateNowLong = dateNow.getTimeInMillis();
        java.sql.Date d = new java.sql.Date(dateNowLong);
        DateFormat format = new SimpleDateFormat(" hh:mm a");
        format.setTimeZone(tz);
        String ESTdate = format.format(d);
        String msg = ESTdate + " " + custObj.getUsername() + " Cust change to Fund Manager Result:" + result;
        CommMsgImp commMsg = new CommMsgImp();
        commMsg.AddCommMessage(serviceAFWeb, accountAdminObj, ConstantKey.COM_SIGNAL, msg);
        return result;

    }

    public int changeAPICustomer(ServiceAFweb serviceAFWeb, String EmailUserName) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return 0;
        }
        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();
        CustomerObj custObj = serviceAFWeb.getAccountImp().getCustomerBySystem(UserName, null);

        if (custObj == null) {
            return 0;
        }

        if (custObj.getStatus() != ConstantKey.OPEN) {
            return 0;
        }
        custObj.setType(CustomerObj.INT_API_USER);
        custObj.setSubstatus(ConstantKey.INT_PP_API);
        custObj.setPayment(0);

        int result = serviceAFWeb.getAccountImp().systemUpdateCustAllStatus(custObj);
        /// clear the last build to regenerate new bill
        AccountObj account = serviceAFWeb.getAccountImp().getAccountByType(custObj.getUsername(), null, AccountObj.INT_TRADING_ACCOUNT);
        // get last bill
        ArrayList<BillingObj> billingObjList = serviceAFWeb.getAccountImp().getBillingByCustomerAccountID(custObj.getUsername(), null, account.getId(), 2);
        if (billingObjList != null) {
            if (billingObjList.size() > 0) {
                BillingObj billObj = billingObjList.get(0);
                serviceAFWeb.getAccountImp().removeBillingByCustomerAccountID(custObj.getUsername(), null, account.getId(), billObj.getId());
            }
        }

        String tzid = "America/New_York"; //EDT
        TimeZone tz = TimeZone.getTimeZone(tzid);
        AccountObj accountAdminObj = serviceAFWeb.getAdminObjFromCache();
        Calendar dateNow = TimeConvertion.getCurrentCalendar();
        long dateNowLong = dateNow.getTimeInMillis();
        java.sql.Date d = new java.sql.Date(dateNowLong);
        DateFormat format = new SimpleDateFormat(" hh:mm a");
        format.setTimeZone(tz);
        String ESTdate = format.format(d);
        String msg = ESTdate + " " + custObj.getUsername() + " Cust change to API User Result:" + result;
        CommMsgImp commMsg = new CommMsgImp();
        commMsg.AddCommMessage(serviceAFWeb, accountAdminObj, ConstantKey.COM_SIGNAL, msg);
        return result;
    }

    //////////////////////////////////////
    // need ConstantKey.DISABLE status beofore remove customer
    public int removeCustomer(ServiceAFweb serviceAFWeb, String EmailUserName) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return 0;
        }
        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();
        CustomerObj custObj = serviceAFWeb.getAccountImp().getCustomerBySystem(UserName, null);

        if (custObj == null) {
            return 0;
        }
        if (custObj.getStatus() == ConstantKey.OPEN) {
            return 0;
        }
        ArrayList accountList = serviceAFWeb.getAccountImp().getAccountListByCustomerObj(custObj);
        if (accountList != null) {
            for (int i = 0; i < accountList.size(); i++) {
                AccountObj accountObj = (AccountObj) accountList.get(i);
                ArrayList stockNameList = serviceAFWeb.getAccountImp().getAccountStockNameList(accountObj.getId());
                if (stockNameList != null) {
                    for (int j = 0; j < stockNameList.size(); j++) {
                        String symbol = (String) stockNameList.get(j);
                        AFstockObj stock = serviceAFWeb.getStockRealTimeServ(symbol);
                        if (stock != null) {
                            serviceAFWeb.getAccountImp().removeAccountStock(accountObj, stock.getId());
                        }
                    }
                }
                // remove billing
                serviceAFWeb.getAccountImp().removeAccountBilling(accountObj);
                serviceAFWeb.getAccountImp().removeAccountById(accountObj);
            }
        }

        return serviceAFWeb.getAccountImp().removeCustomer(custObj);
    }

    ////////////////////////////////////////////////////
    ///////////////////////////////////////////////////
    public int SystemFundClearfundbalance(ServiceAFweb serviceAFWeb, String EmailUserName, String Password, String AccountIDSt) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();
        try {
            int accountid = Integer.parseInt(AccountIDSt);
            AccountObj accObj = serviceAFWeb.getAccountImp().getAccountByCustomerAccountID(UserName, Password, accountid);
            if (accObj.getType() == AccountObj.INT_MUTUAL_FUND_ACCOUNT) {
                int substatus = accObj.getSubstatus();
                float investment = 0;
                float balance = 0;
                float servicefee = 0;
                serviceAFWeb.getAccountImp().updateAccountStatusByAccountID(accObj.getId(), substatus, investment, balance, servicefee);

                return 1;
            }
        } catch (Exception e) {
        }
        return 0;
    }

    public ArrayList getFundAccounBestFundList(ServiceAFweb serviceAFWeb, String EmailUserName, String Password) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return null;
        }

        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();

        return serviceAFWeb.getAccountImp().getAccounBestFundList(UserName, Password);

    }

    public ArrayList<AccountObj> getFundAccountByCustomerAccountID(ServiceAFweb serviceAFWeb, String EmailUserName, String Password, String AccountIDSt) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return null;
        }

        ArrayList<AccountObj> accountObjList = new ArrayList();

        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();
        try {
            CustomerObj custObj = serviceAFWeb.getAccountImp().getCustomerPassword(UserName, Password);
            if (custObj == null) {
                return null;
            }
            if (custObj.getStatus() != ConstantKey.OPEN) {
                return null;
            }

            String portfolio = custObj.getPortfolio();
            CustPort custPortfilio = null;
            try {
                if ((portfolio != null) && (portfolio.length() > 0)) {
                    portfolio = portfolio.replaceAll("#", "\"");
                    custPortfilio = new ObjectMapper().readValue(portfolio, CustPort.class);
                }
            } catch (Exception ex) {
            }
            if (custPortfilio == null) {
                return null;
            }
            ArrayList<String> featL = custPortfilio.getFeatL();
            if (featL == null) {
                return null;
            }

            for (int i = 0; i < featL.size(); i++) {
                String feat = featL.get(i);
                try {
                    feat = feat.replace("fund", "");
                    int accFundId = Integer.parseInt(feat);
                    AccountObj accFundObj = serviceAFWeb.getAccountImp().getAccountObjByAccountID(accFundId);
                    if (accFundObj.getType() == AccountObj.INT_MUTUAL_FUND_ACCOUNT) {
                        accFundObj.setSubstatus(ConstantKey.OPEN);
                        String delFundFeat = "delfund" + accFundId;
                        for (int j = 0; j < featL.size(); j++) {
                            if (delFundFeat.equals(featL.get(j))) {
                                accFundObj.setSubstatus(ConstantKey.PENDING);
                            }
                        }
                        accountObjList.add(accFundObj);
                    }
                } catch (Exception e) {
                }
            }
            return accountObjList;
        } catch (Exception e) {
        }
        return null;

    }

    public int getFundAccountAddAccundFund(ServiceAFweb serviceAFWeb, String EmailUserName, String Password, String AccountIDSt, String FundIDSt) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();
        try {
            CustomerObj custObj = serviceAFWeb.getAccountImp().getCustomerPassword(UserName, Password);
            if (custObj == null) {
                return 0;
            }
            if (custObj.getStatus() != ConstantKey.OPEN) {
                return 0;
            }

            String portfolio = custObj.getPortfolio();
            CustPort custPortfilio = null;
            try {
                if ((portfolio != null) && (portfolio.length() > 0)) {
                    portfolio = portfolio.replaceAll("#", "\"");
                    custPortfilio = new ObjectMapper().readValue(portfolio, CustPort.class);
                } else {
                    custPortfilio = new CustPort();
                    String portfStr = new ObjectMapper().writeValueAsString(custPortfilio);
                    serviceAFWeb.getAccountImp().updateCustomerPortfolio(custObj.getUsername(), portfStr);
                }
            } catch (Exception ex) {
            }
            if (custPortfilio == null) {
                return 0;
            }
            ArrayList<String> featL = custPortfilio.getFeatL();
            if (featL == null) {
                return 0;
            }
            String fundFeat = "fund" + FundIDSt;
            for (int i = 0; i < featL.size(); i++) {
                String feat = featL.get(i);
                if (fundFeat.equals(feat)) {
                    return 0;
                }
            }

            int accFundId = Integer.parseInt(FundIDSt);
            AccountObj accFundObj = serviceAFWeb.getAccountImp().getAccountObjByAccountID(accFundId);
            if (accFundObj.getType() == AccountObj.INT_MUTUAL_FUND_ACCOUNT) {
                featL.add(fundFeat);
                String portfStr = new ObjectMapper().writeValueAsString(custPortfilio);
                serviceAFWeb.getAccountImp().updateCustomerPortfolio(custObj.getUsername(), portfStr);

                // update billing
                BillingProcess BP = new BillingProcess();
                BP.updateFundFeat(serviceAFWeb, custObj, accFundObj);
                return 1;
            }

        } catch (Exception e) {
        }
        return 0;
    }

    public int getFundAccountRemoveAcocuntFund(ServiceAFweb serviceAFWeb, String EmailUserName, String Password, String AccountIDSt, String FundIDSt) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();

        CustomerObj custObj = serviceAFWeb.getAccountImp().getCustomerPassword(UserName, Password);
        if (custObj == null) {
            return 0;
        }
        if (custObj.getStatus() != ConstantKey.OPEN) {
            return 0;
        }

        String portfolio = custObj.getPortfolio();
        CustPort custPortfilio = null;
        try {
            if ((portfolio != null) && (portfolio.length() > 0)) {
                portfolio = portfolio.replaceAll("#", "\"");
                custPortfilio = new ObjectMapper().readValue(portfolio, CustPort.class);
            }
        } catch (Exception ex) {
        }
        if (custPortfilio == null) {
            return 0;
        }
        ArrayList<String> featL = custPortfilio.getFeatL();
        if (featL == null) {
            return 0;
        }

        String delFundFeat = "delfund" + FundIDSt;

        for (int i = 0; i < featL.size(); i++) {
            String feat = featL.get(i);
            if (delFundFeat.equals(feat)) {
//                alreadyDel = true;
                return 0;
            }
        }
        try {
            featL.add(delFundFeat);
            String portfStr = new ObjectMapper().writeValueAsString(custPortfilio);
            serviceAFWeb.getAccountImp().updateCustomerPortfolio(custObj.getUsername(), portfStr);
            return 1;
        } catch (Exception ex) {
        }
        return 0;

    }

    public ArrayList<AFstockObj> getFundStockListByAccountID(ServiceAFweb serviceAFWeb, String EmailUserName, String Password, String AccountIDSt, String FundIDSt, int lenght) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return null;
        }

        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();

        CustomerObj custObj = serviceAFWeb.getAccountImp().getCustomerPassword(UserName, Password);
        if (custObj == null) {
            return null;
        }
        if (custObj.getStatus() != ConstantKey.OPEN) {
            return null;
        }

        String portfolio = custObj.getPortfolio();
        CustPort custPortfilio = null;
        try {
            if ((portfolio != null) && (portfolio.length() > 0)) {
                portfolio = portfolio.replaceAll("#", "\"");
                custPortfilio = new ObjectMapper().readValue(portfolio, CustPort.class);
            }
        } catch (Exception ex) {
        }
        if (custPortfilio == null) {
            return null;
        }

        ArrayList<String> featL = custPortfilio.getFeatL();
        if (featL == null) {
            return null;
        }

        String fundFeat = "fund" + FundIDSt;
        boolean featureExist = false;
        for (int i = 0; i < featL.size(); i++) {
            String feat = featL.get(i);
            if (fundFeat.equals(feat)) {
                featureExist = true;
                break;
            }
        }
        if (featureExist == false) {
            return null;
        }
        int accFundId = Integer.parseInt(FundIDSt);
        AccountObj accFundObj = serviceAFWeb.getAccountImp().getAccountObjByAccountID(accFundId);
        if (accFundObj == null) {
            return null;
        }
        if (accFundObj.getType() != AccountObj.INT_MUTUAL_FUND_ACCOUNT) {
            return null;
        }

        ArrayList stockNameList = serviceAFWeb.getAccountImp().getAccountStockNameList(accFundObj.getId());
        if (stockNameList != null) {
            if (lenght == 0) {
                lenght = stockNameList.size();
            } else if (lenght > stockNameList.size()) {
                lenght = stockNameList.size();
            }
            ArrayList returnStockList = new ArrayList();

            /// only TR ACC allowed
            String trname = ConstantKey.TR_ACC;

            for (int i = 0; i < lenght; i++) {
                String NormalizeSymbol = (String) stockNameList.get(i);
                AFstockObj stock = serviceAFWeb.getStockRealTimeServ(NormalizeSymbol);
                if (stock != null) {

                    ArrayList<TradingRuleObj> trObjList = serviceAFWeb.getAccountImp().getAccountStockTRListByAccountID(accFundObj.getId(), stock.getId());
                    if (trObjList != null) {
                        for (int j = 0; j < trObjList.size(); j++) {
                            TradingRuleObj trObj = trObjList.get(j);

                            if (trname.equals(ConstantKey.TR_ACC)) {
                                stock.setTRsignal(trObj.getTrsignal());

                                float total = getAccountStockRealTimeBalance(serviceAFWeb, trObj);
                                stock.setPerform(total);

                                break;
                            }

                        }
                    }

                    returnStockList.add(stock);
                }
            }
            return returnStockList;
        }
        return null;
    }

    public TradingRuleObj getFundAccountStockTRByTRname(ServiceAFweb serviceAFWeb, String EmailUserName, String Password, String AccountIDSt, String FundIDSt, String stockidsymbol, String trname) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return null;
        }

        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();

        CustomerObj custObj = serviceAFWeb.getAccountImp().getCustomerPassword(UserName, Password);
        if (custObj == null) {
            return null;
        }
        if (custObj.getStatus() != ConstantKey.OPEN) {
            return null;
        }

        String portfolio = custObj.getPortfolio();
        CustPort custPortfilio = null;
        try {
            if ((portfolio != null) && (portfolio.length() > 0)) {
                portfolio = portfolio.replaceAll("#", "\"");
                custPortfilio = new ObjectMapper().readValue(portfolio, CustPort.class);
            }
        } catch (Exception ex) {
        }
        if (custPortfilio == null) {
            return null;
        }

        ArrayList<String> featL = custPortfilio.getFeatL();
        if (featL == null) {
            return null;
        }

        String fundFeat = "fund" + FundIDSt;
        boolean featureExist = false;
        for (int i = 0; i < featL.size(); i++) {
            String feat = featL.get(i);
            if (fundFeat.equals(feat)) {
                featureExist = true;
                break;
            }
        }
        if (featureExist == false) {
            return null;
        }

        int accFundId = Integer.parseInt(FundIDSt);
        AccountObj accFundObj = serviceAFWeb.getAccountImp().getAccountObjByAccountID(accFundId);
        if (accFundObj == null) {
            return null;
        }
        if (accFundObj.getType() != AccountObj.INT_MUTUAL_FUND_ACCOUNT) {
            return null;
        }

        AFstockObj stock = null;
        stock = this.getStockIdSymbol(serviceAFWeb, stockidsymbol);
        if (stock == null) {
            return null;
        }
        int stockID = stock.getId();
        return serviceAFWeb.getAccountImp().getAccountStockIDByTRStockID(accFundObj.getId(), stockID, trname);

    }

    public ArrayList<TransationOrderObj> getFundAccountStockTRTranListByAccountID(ServiceAFweb serviceAFWeb, String EmailUserName, String Password, String AccountIDSt, String FundIDSt, String stockidsymbol, String trName, int length) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return null;
        }
        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();

        CustomerObj custObj = serviceAFWeb.getAccountImp().getCustomerPassword(UserName, Password);
        if (custObj == null) {
            return null;
        }
        if (custObj.getStatus() != ConstantKey.OPEN) {
            return null;
        }

        String portfolio = custObj.getPortfolio();
        CustPort custPortfilio = null;
        try {
            if ((portfolio != null) && (portfolio.length() > 0)) {
                portfolio = portfolio.replaceAll("#", "\"");
                custPortfilio = new ObjectMapper().readValue(portfolio, CustPort.class);
            }
        } catch (Exception ex) {
        }
        if (custPortfilio == null) {
            return null;
        }

        ArrayList<String> featL = custPortfilio.getFeatL();
        if (featL == null) {
            return null;
        }
        int accFundId = Integer.parseInt(FundIDSt);
        AccountObj accFundObj = serviceAFWeb.getAccountImp().getAccountObjByAccountID(accFundId);

        AFstockObj stock = null;
        if (accFundObj != null) {
            stock = this.getStockIdSymbol(serviceAFWeb, stockidsymbol);

            if (stock == null) {
                return null;
            }

            if (trName.toUpperCase().equals(ConstantKey.TR_ACC)) {
                return serviceAFWeb.getAccountImp().getAccountStockTransList(accFundObj.getId(), stock.getId(), trName.toUpperCase(), length);
            }
        }
        return null;
    }

    public ArrayList<PerformanceObj> getFundAccountStockTRPerfList(ServiceAFweb serviceAFWeb, String EmailUserName, String Password, String AccountIDSt, String FundIDSt, String stockidsymbol, String trName, int length) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return null;
        }
        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();

        CustomerObj custObj = serviceAFWeb.getAccountImp().getCustomerPassword(UserName, Password);
        if (custObj == null) {
            return null;
        }
        if (custObj.getStatus() != ConstantKey.OPEN) {
            return null;
        }

        String portfolio = custObj.getPortfolio();
        CustPort custPortfilio = null;
        try {
            if ((portfolio != null) && (portfolio.length() > 0)) {
                portfolio = portfolio.replaceAll("#", "\"");
                custPortfilio = new ObjectMapper().readValue(portfolio, CustPort.class);
            }
        } catch (Exception ex) {
        }
        if (custPortfilio == null) {
            return null;
        }

        ArrayList<String> featL = custPortfilio.getFeatL();
        if (featL == null) {
            return null;
        }
        int accFundId = Integer.parseInt(FundIDSt);
        AccountObj accFundObj = serviceAFWeb.getAccountImp().getAccountObjByAccountID(accFundId);

        AFstockObj stock = null;
        if (accFundObj != null) {
            stock = this.getStockIdSymbol(serviceAFWeb, stockidsymbol);
            if (stock == null) {
                return null;
            }
            ArrayList<PerformanceObj> perfList = null;
            if (trName.toUpperCase().equals(ConstantKey.TR_ACC)) {
                perfList = serviceAFWeb.getAccountImp().getAccountStockPerfList(accFundObj.getId(), stock.getId(), trName, length);
            } else {
                AccountObj accountAdminObj = serviceAFWeb.getAdminObjFromCache();
                if (accountAdminObj == null) {
                    return null;
                }
                perfList = serviceAFWeb.getAccountImp().getAccountStockPerfList(accountAdminObj.getId(), stock.getId(), trName, length);
            }
            return perfList;
        }
        return null;
    }

    public boolean SystemFundResetGlobal(ServiceAFweb serviceAFWeb) {
        FundMgrProcess fundmgr = new FundMgrProcess();
        logger.info(">ProcessGetGlobalFundMgr start ");
        fundmgr.ProcessGetGlobalFundMgr(serviceAFWeb);
//        fundmgr.ProcessFundMgrAccount(this);
        return true;
    }

    public boolean SystemFundSelectBest(ServiceAFweb serviceAFWeb) {
        FundMgrProcess fundmgr = new FundMgrProcess();
        logger.info(">ProcessSelectBestFundMgrAccount start ");
        fundmgr.ProcessSelectBestFundMgrAccount(serviceAFWeb);
        return true;
    }

    public boolean SystemFundPocessAddRemove(ServiceAFweb serviceAFWeb) {
        logger.info(">ProcessAddRemoveFundAccount start ");
        serviceAFWeb.getAccountProcessImp().ProcessAddRemoveFundAccount(serviceAFWeb);
        return true;
    }
}