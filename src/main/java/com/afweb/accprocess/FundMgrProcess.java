/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.accprocess;

import com.afweb.account.AccountImp;
import com.afweb.model.*;
import com.afweb.model.account.*;
import com.afweb.model.stock.AFstockObj;
import com.afweb.service.*;
import com.afweb.stock.StockInternet;
import com.afweb.util.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import java.util.logging.Logger;

/**
 *
 * @author eddyko
 */
public class FundMgrProcess {

    public static Logger logger = Logger.getLogger("FundMgrProcess");
    private AccountImp accountImp = new AccountImp();

// https://www.theglobeandmail.com/investing/markets/funds/FID899.CF
// Fidelity Global Health Care Fund Series A 
    String FidelityGlobalHealthCare = "https://www.theglobeandmail.com/investing/markets/funds/FID899.CF";

//https://www.theglobeandmail.com/investing/markets/funds/FID890.CF
//Fidelity Technology Innovators Class Series
    String FidelityTechnologyInnovators = "https://www.theglobeandmail.com/investing/markets/funds/FID890.CF";

//https://www.theglobeandmail.com/investing/markets/funds/TDB977.CF
//TD U.S. Blue Chip Equity Fund - Investor Ser
    String TDBlueChipEquity = "https://www.theglobeandmail.com/investing/markets/funds/TDB977.CF";

//https://www.theglobeandmail.com/investing/markets/funds/TDB3423.CF
//TD U.S. Dividend Growth Fund - D Series
    String TDDividendGrowth = "https://www.theglobeandmail.com/investing/markets/funds/TDB3423.CF";

//https://www.theglobeandmail.com/investing/markets/funds/TDB2791.CF
//TD U.S. Monthly Income Fund C$ - Ft8 Series
    String TDMonthlyIncome = "https://www.theglobeandmail.com/investing/markets/funds/TDB2791.CF";

//https://www.theglobeandmail.com/investing/markets/funds/TDB292.CF
//TD Precious Metals Fund Advisor Series 
    String TDPreciousMetals = "https://www.theglobeandmail.com/investing/markets/funds/TDB292.CF";

//https://www.theglobeandmail.com/investing/markets/funds/TDB3055.CF
//TD Global Entertainment & Communications Fd D
    String TDGlobalEntertainment = "https://www.theglobeandmail.com/investing/markets/funds/TDB3055.CF";

//https://www.theglobeandmail.com/investing/markets/funds/TDB3098.CF
//TD Science & Technology Fund - D Series
    String TDScienceTechnology = "https://www.theglobeandmail.com/investing/markets/funds/TDB3098.CF";
//    
//   
    ArrayList stockArrayFidelityGlobalHealthCare = new ArrayList();
    ArrayList stockArrayFidelityTechnologyInnovators = new ArrayList();
    ArrayList stockArrayTDBlueChipEquity = new ArrayList();
    ArrayList stockArrayTDDividendGrowth = new ArrayList();
    ArrayList stockArrayTDMonthlyIncome = new ArrayList();
    ArrayList stockArrayTDPreciousMetals = new ArrayList();
    ArrayList stockArrayTDGlobalEntertainment = new ArrayList();
    ArrayList stockArrayTDScienceTechnology = new ArrayList();

    public void ProcessGetGlobalFundMgr(ServiceAFweb serviceAFWeb) {
        ServiceAFweb.lastfun="ProcessGetGlobalFundMgr";
        logger.info("> ProcessIISWebGlobalFundMgr ");

        Calendar dateNow = TimeConvertion.getCurrentCalendar();
        long lockDateValue = dateNow.getTimeInMillis();
        String LockName = "ALL_FUNDMGR";
        long lockReturn = serviceAFWeb.setLockNameProcess(LockName, ConstantKey.FUND_LOCKTYPE, lockDateValue, ServiceAFweb.getServerObj().getSrvProjName() + "_ProcessAllAccountTradingSignal");
        if (CKey.NN_DEBUG == true) {
            lockReturn = 1;
        }
        if (lockReturn > 0) {
            try {
                updateMutualFundAll();
                updateIndexMutualFundAll();

            } catch (Exception e) {
                logger.info("> ProcessIISWebGlobalFundMgr Exception " + e.getMessage());
            }

            serviceAFWeb.removeNameLock(LockName, ConstantKey.FUND_LOCKTYPE);
        }
    }

    private static ArrayList accountIdNameArray = new ArrayList();
    private static ArrayList accountFundIdNameArray = new ArrayList();

    public void ProcessSelectBestFundMgrAccount(ServiceAFweb serviceAFWeb) {
        ServiceAFweb.lastfun="ProcessSelectBestFundMgrAccount";
//        logger.info("> ProcessSelectBestFundMgrAccount ");
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
        String LockName = "ALL_FUNDMGR";
        long lockReturn = serviceAFWeb.setLockNameProcess(LockName, ConstantKey.FUND_LOCKTYPE, lockDateValue, ServiceAFweb.getServerObj().getSrvProjName() + "_ProcessAllAccountTradingSignal");
        if (CKey.NN_DEBUG == true) {
            lockReturn = 1;
        }
        if (lockReturn > 0) {
            long currentTime = System.currentTimeMillis();
            long lockDate2Min = TimeConvertion.addMinutes(currentTime, 2);

            int numProc = 0;
            for (int k = 0; k < 10; k++) {
                currentTime = System.currentTimeMillis();
//                if (lockDate2Min < currentTime) {
//                    break;
//                }
                if (accountFundIdNameArray.size() == 0) {
                    break;
                }
                if (numProc > 3) {
                    break;
                }
                try {
                    String accountIdSt = (String) accountFundIdNameArray.get(0);
                    accountFundIdNameArray.remove(0);
                    int accountId = Integer.parseInt(accountIdSt);
                    AccountObj accountObj = serviceAFWeb.getAccountImp().getAccountObjByAccountID(accountId);
                    if (accountObj.getType() == AccountObj.INT_MUTUAL_FUND_ACCOUNT) {
                        updateMutualFundBestStock(serviceAFWeb, accountObj);
                        numProc++;
                    }
                } catch (Exception e) {
                    logger.info("> SelectBestFundMgrAccount Exception " + e.getMessage());
                }
            }
            serviceAFWeb.removeNameLock(LockName, ConstantKey.FUND_LOCKTYPE);
        }
    }

    // will execuate once per month
    public int updateMutualFundBestStock(ServiceAFweb serviceAFWeb, AccountObj accountObj) {
        ServiceAFweb.lastfun="updateMutualFundBestStock";        
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
                CustomerObj customer = getAccountImp().getCustomerByCustID(accObj.getCustomerid());
                if (customer == null) {
                    return 0;
                }

                // only perform auto select stock from IISWeb Manager Fund
                if (customer.getUsername().equals(CKey.FUND_MANAGER_USERNAME)) {
                    ;
                } else if (customer.getUsername().equals(CKey.INDEXFUND_MANAGER_USERNAME)) {
                    ;
                } else {
                    return 0;
                }
//                AccountObj accountAdminObj = serviceAFWeb.getAdminObjFromCache();

                ArrayList<PerformanceObj> perfList = new ArrayList();
                for (int i = 0; i < AccountStockNameList.size(); i++) {
                    String stockN = (String) AccountStockNameList.get(i);

                    String stockidsymbol = stockN;

                    String trName = ConstantKey.TR_ACC;
                    AFstockObj stock = serviceAFWeb.getStockImp().getRealTimeStock(stockN, null);
                    TradingRuleObj trObj = serviceAFWeb.getAccountImp().getAccountStockIDByTRStockID(accObj.getId(), stock.getId(), trName);

                    float close = stock.getAfstockInfo().getFclose();
                    float deltaTotal = 0;
                    float sharebalance = 0;
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
                                deltaTotal = (close - (trObj.getShortamount() / trObj.getShortshare())) * trObj.getShortshare();
                            }
                        }
                    }
                    float total = trObj.getBalance() + sharebalance;
                    total = total - trObj.getInvestment();

                    if (stock.getSubstatus() == 0) {
                        total = total + deltaTotal;
                    }
                    PerformanceObj perfObj = new PerformanceObj();
                    perfObj.setName(stockidsymbol);
                    perfObj.setGrossprofit(total);
                    perfList.add(perfObj);

                    // assume default NN1
                    // assume default NN1     
//                    String EmailUserName = customer.getUsername();
//                    String Password = null;
//                    String AccountIDSt = "" + accObj.getId();                    
//                    String trName = ConstantKey.TR_NN1;
//                    int length = 1;
//                    ArrayList<PerformanceObj> stockPerList = serviceAFWeb.getAccountStockPerfHistory(EmailUserName, Password, AccountIDSt, stockidsymbol, trName, length);
//                    if (stockPerList != null) {
//                        if (stockPerList.size() > 0) {
//                            PerformanceObj perfObj = stockPerList.get(0);
//                            perfObj.setName(stockidsymbol);
//
//                            perfList.add(perfObj);
//                        }
//                    }
                }

                ////////sort the best perfList low number to high
                float a;
                float b;
                PerformanceObj c;
                PerformanceObj d;
                for (int i = 0; i < perfList.size(); i++) {
                    for (int j = i; j < perfList.size() - 1; j++) {
                        a = perfList.get(i).getGrossprofit();
                        b = perfList.get(j + 1).getGrossprofit();
                        c = perfList.get(i);
                        d = perfList.get(j + 1);
                        if (a > b) {
                            PerformanceObj temp = d;
                            perfList.set(j + 1, c);
                            perfList.set(i, temp);
                        }
                    }
                }
                // change high to low
                int MAX_FUND_LIST = 4;
                Collections.reverse(perfList);
                ArrayList accPortList = new ArrayList();
                for (PerformanceObj perObj : perfList) {
//                    logger.info(perObj.getName() + " " + perObj.getGrossprofit());
                    if (perObj.getGrossprofit() > 0) {
                        if (accPortList.size() < MAX_FUND_LIST) {
                            logger.info("> updateMutualFundBestStock " + perObj.getName() + " " + perObj.getGrossprofit());
                            accPortList.add(perObj.getName());
                        }
                    }
                }

                fundMgr.setAccL(accPortList);

                String portfStr;
                try {
                    portfStr = new ObjectMapper().writeValueAsString(fundMgr);
                    logger.info("> updateMutualFundBestStock " + accountObj.getAccountname() + " " + portfStr);
                    getAccountImp().updateAccountPortfolio(accountObj.getAccountname(), portfStr);
                } catch (JsonProcessingException e) {
                    logger.info("> updateMutualFundBestStock Exception " + e.getMessage());
                }

                return 1;
            }
        }
        return 0;
    }

    ////////// update FUND_MANAGER_USERNAME account
    public boolean updateIndexMutualFundAll() {
        ServiceAFweb.lastfun = "updateIndexMutualFundAll";    
        logger.info("updateIndexMutualFundAll");

        CustomerObj custObj = getAccountImp().getCustomerBySystem(CKey.INDEXFUND_MANAGER_USERNAME, null);
        ArrayList accountList = getAccountImp().getAccountListByCustomerObj(custObj);
        if (accountList != null) {
            for (int i = 0; i < accountList.size(); i++) {
                AccountObj accountObj = (AccountObj) accountList.get(i);
                if (accountObj.getType() == AccountObj.INT_MUTUAL_FUND_ACCOUNT) {
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
                            getAccountImp().updateAccountPortfolio(accountObj.getAccountname(), portfStr);
                        } catch (Exception ex) {
                        }
                    }
                    ArrayList portfolioArray = new ArrayList();
                    
                    //etfStock[] = {"SPY", "DIA", "QQQ", "XIU.TO", "GLD", "FAS", "HOU.TO", "IWM", "IYR"};
                    for (int j=0; j<ServiceAFweb.etfStock.length; j++) {
                        String etf = ServiceAFweb.etfStock[j];
                        portfolioArray.add(etf);                        
                    }

                    fundMgr.setFunL(portfolioArray);
                    String portfStr;
                    try {
                        portfStr = new ObjectMapper().writeValueAsString(fundMgr);
                        getAccountImp().updateAccountPortfolio(accountObj.getAccountname(), portfStr);
                    } catch (JsonProcessingException ex) {
                    }
                    break;
                }
            }
        }
        return true;
    }

    public boolean updateMutualFundAll() {

        logger.info("updateMutualFundAll");

        ArrayList stockArray = new ArrayList();
        boolean ret = getGlobeFundStockList(stockArray);
        if (ret == false) {
            return ret;
        }

        ArrayList portfolioArray = new ArrayList();

        Set<String> set = new HashSet<>();
        for (int i = 0; i < stockArray.size(); i++) {
            String stock = (String) stockArray.get(i);
            if (!set.add(stock)) {
                continue;
            }

            portfolioArray.add(stock);
        }

        CustomerObj custFundObj = getAccountImp().getCustomerBySystem(CKey.FUND_MANAGER_USERNAME, null);
        ArrayList accountList = getAccountImp().getAccountListByCustomerObj(custFundObj);
        if (accountList != null) {
            for (int i = 0; i < accountList.size(); i++) {
                AccountObj accountObj = (AccountObj) accountList.get(i);
                if (accountObj.getType() == AccountObj.INT_MUTUAL_FUND_ACCOUNT) {
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
                            getAccountImp().updateAccountPortfolio(accountObj.getAccountname(), portfStr);
                        } catch (Exception ex) {
                        }
                    }
                    fundMgr.setFunL(portfolioArray);
                    String portfStr;
                    try {
                        portfStr = new ObjectMapper().writeValueAsString(fundMgr);
                        getAccountImp().updateAccountPortfolio(accountObj.getAccountname(), portfStr);
                    } catch (JsonProcessingException ex) {
                    }

                    break;
                }
            }
        }
        return true;
    }

    public boolean getGlobeFundStockList(ArrayList stockArray) {
        int nStock = 1; //2; //3;

//        this.getGlobeFundStockList(TDMonthlyIncome, stockArrayTDMonthlyIncome, nStock);
        String fundURL = FidelityGlobalHealthCare;
        boolean ret = this.getGlobeFundStockList(fundURL, stockArrayFidelityGlobalHealthCare, nStock);
        stockArray.addAll(stockArrayFidelityGlobalHealthCare);
        if (ret == false) {
            return false;
        }
        fundURL = FidelityTechnologyInnovators;
        ret = this.getGlobeFundStockList(fundURL, stockArrayFidelityTechnologyInnovators, nStock);
        if (ret == false) {
            return false;
        }
        stockArray.addAll(stockArrayFidelityTechnologyInnovators);
        fundURL = TDBlueChipEquity;
        ret = this.getGlobeFundStockList(fundURL, stockArrayTDBlueChipEquity, nStock + 1);
        if (ret == false) {
            return false;
        }
        stockArray.addAll(stockArrayTDBlueChipEquity);
        fundURL = TDDividendGrowth;
        ret = this.getGlobeFundStockList(fundURL, stockArrayTDDividendGrowth, nStock + 1);
        if (ret == false) {
            return false;
        }
        stockArray.addAll(stockArrayTDDividendGrowth);
        fundURL = TDMonthlyIncome;
        ret = this.getGlobeFundStockList(fundURL, stockArrayTDMonthlyIncome, nStock + 1);
        if (ret == false) {
            return false;
        }
        stockArray.addAll(stockArrayTDMonthlyIncome);
        fundURL = TDPreciousMetals;
        ret = this.getGlobeFundStockList(fundURL, stockArrayTDPreciousMetals, nStock);
        if (ret == false) {
            return false;
        }
        stockArray.addAll(stockArrayTDPreciousMetals);
        fundURL = TDGlobalEntertainment;
        ret = this.getGlobeFundStockList(fundURL, stockArrayTDGlobalEntertainment, nStock);
        if (ret == false) {
            return false;
        }
        stockArray.addAll(stockArrayTDGlobalEntertainment);
        fundURL = TDScienceTechnology;
        ret = this.getGlobeFundStockList(fundURL, stockArrayTDScienceTechnology, nStock);
        if (ret == false) {
            return false;
        }
        stockArray.addAll(stockArrayTDScienceTechnology);
        logger.info("getGlobeFundStockList size " + stockArray.size());
        if (stockArray.size() < (8 * nStock)) {
            logger.info("getGlobeFundStockList fail < 8*nStock ");
            return false;
        }
        return true;
    }

    public boolean getGlobeFundStockList(String fundURL, ArrayList stockArray, int maxStock) {

        StockInternet internet = new StockInternet();
        StringBuffer strBuilderFund = internet.getInternetYahooScreenPage(fundURL);

        String str1 = strBuilderFund.toString();
        String str2 = StringTag.replaceAll("&quot;", "", str1);
        StringBuffer str3 = new StringBuffer(str2);

        StringTag strTagFund = new StringTag(str3);
        Set<String> set = new HashSet<>();

        try {
            if (strTagFund.IsEmpty() == false) {
                String StField = "";
                String stStockListSt = strTagFund.GetFirstTag("funds-holding-table", 0);
                String[] stockL = stStockListSt.split("symbol:");
                for (int i = 0; i < stockL.length; i++) {
                    if (i == 0) {
                        continue;
                    }
                    String stockNameSt = stockL[i];
                    String[] stockNameL = stockNameSt.split(",");
                    String stock = stockNameL[0];
                    String stockName = stockNameL[1];
                    if (stock.equals("-")) {
                        continue;
                    } else if (stock.equals("")) {
                        continue;
                    } else if (stock.indexOf("$") != -1) {
                        continue;
                    } else if (stock.indexOf("0") != -1) {
                        continue;
                    } else if (stock.indexOf(".") != -1) {
                        if (stock.indexOf(".T") != -1) {
                            stock = stock.replace(".T", ".TO");
                        } else {
                            logger.info("getGlobeFundStockList unknown market " + stock);
                            continue;
                        }
                    }
                    stockName = stockName.replace("symbolName:", "");

                    AFstockObj stockObj = internet.GetRealTimeStockInternet(stock);
                    if (stockObj == null) {
                        continue;
                    }
                    if (set.add(stock)) {
                        stockArray.add(stock);
                    }
                    if (maxStock != 0) {
                        if (stockArray.size() >= maxStock) {
                            break;
                        }
                    }
                }
            }
            return true;

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }

    /**
     * @return the accountImp
     */
    public AccountImp getAccountImp() {
        return accountImp;
    }

    /**
     * @param accountImp the accountImp to set
     */
    public void setAccountImp(AccountImp accountImp) {
        this.accountImp = accountImp;
    }

}
