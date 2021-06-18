/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.processcustacc;

import com.afweb.account.AccountTranImp;
import com.afweb.account.CommMsgImp;
import com.afweb.model.*;
import com.afweb.model.account.*;
import com.afweb.model.stock.AFstockObj;
import com.afweb.nnsignal.TradingSignalProcess;

import com.afweb.service.ServiceAFweb;

import com.afweb.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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
        if (serviceAFWeb.getServerObj().isSysMaintenance() == true) {
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
        if (serviceAFWeb.getServerObj().isSysMaintenance() == true) {
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
        if (serviceAFWeb.getServerObj().isSysMaintenance() == true) {
            return null;
        }

        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();

        return serviceAFWeb.getAccountImp().getAccountList(UserName, Password);

    }

    public LoginObj getCustomerAccLogin(ServiceAFweb serviceAFWeb, String EmailUserName, String AccountIDSt) {
        if (serviceAFWeb.getServerObj().isSysMaintenance() == true) {
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
        if (serviceAFWeb.getServerObj().isSysMaintenance() == true) {
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
        if (serviceAFWeb.getServerObj().isSysMaintenance() == true) {
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
        if (serviceAFWeb.getServerObj().isSysMaintenance() == true) {
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
        if (serviceAFWeb.getServerObj().isSysMaintenance() == true) {
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

    public int addAccountStockByCustAcc(ServiceAFweb serviceAFWeb, String EmailUserName, String Password, String AccountIDSt, String symbol) {
        if (serviceAFWeb.getServerObj().isSysMaintenance() == true) {
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
        if (serviceAFWeb.getServerObj().isSysMaintenance() == true) {
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
        if (serviceAFWeb.getServerObj().isSysMaintenance() == true) {
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
        if (serviceAFWeb.getServerObj().isSysMaintenance() == true) {
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
        if (serviceAFWeb.getServerObj().isSysMaintenance() == true) {
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
        if (serviceAFWeb.getServerObj().isSysMaintenance() == true) {
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
        if (serviceAFWeb.getServerObj().isSysMaintenance() == true) {
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
        if (serviceAFWeb.getServerObj().isSysMaintenance() == true) {
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
            stock = serviceAFWeb.getRealTimeStockByStockID(stockID);
        } catch (NumberFormatException e) {
            SymbolNameObj symObj = new SymbolNameObj(stockidsymbol);
            String NormalizeSymbol = symObj.getYahooSymbol();
            stock = serviceAFWeb.getStockRealTimeServ(NormalizeSymbol);
        }
        return stock;
    }

    public ArrayList<PerformanceObj> getAccountStockTRPerfHistory(ServiceAFweb serviceAFWeb, String EmailUserName, String Password, String AccountIDSt, String stockidsymbol, String trName, int length) {
        if (serviceAFWeb.getServerObj().isSysMaintenance() == true) {
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
        if (serviceAFWeb.getServerObj().isSysMaintenance() == true) {
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

}
