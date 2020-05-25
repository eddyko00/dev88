/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.account;

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

//    public static final int OPEN = 0;
//    public static final String INT_ST_OPEN = "0";
//    public static final String MSG_CLOSE = "CLOSE";
//    public static final int CLOSE = 1;
//    public static final String INT_ST_CLOSE = "1";
//    public static final String MSG_ENABLE = "ENABLE";
//    public static final int ENABLE = 0;
//    public static final String INT_ST_ENABLE = "0";
//    public static final String MSG_DISABLE = "DISABLE";
//    public static final int DISABLE = 1;
//    public static final String INT_ST_DISABLE = "1";
//    //
//    public static final String MSG_INITIAL = "INITIAL";
//    public static final int INITIAL = 2;
//    public static final String INT_ST_INITIAL = "2";
//    //
//    public static final String MSG_PENDING = "PENDING";
//    public static final int PENDING = 3;
//    public static final String INT_ST_PENDING = "3";
//    //
//    public static final String MSG_PROCESS = "PROCESS";
//    public static final int PROCESS = 4;
//    public static final String INT_ST_PROCESS = "4";
//    //
//    public static final String MSG_FAIL = "FAIL";
//    public static final int FAIL = 5;
//    public static final String INT_ST_FAIL = "5";
//    //    
//    public static final String MSG_DELETE = "DELETE";
//    public static final int DELETE = 9;
//    public static final String INT_ST_DELETE = "9";
//    public static final String MSG_CANCEL = "CANCEL";
//    public static final int CANCEL = 10;
//    public static final String INT_ST_CANCEL = "10";
//    //
//    public static final String MSG_COMPLETE = "COMPLETE";
//    public static final int COMPLETE = 11;
//    public static final String INT_ST_COMPLETE = "11";
//    public static final String MSG_PARTIAL_COMPLETE = "PARTIAL_COMPLETE";
//    public static final int PARTIAL_COMPLETE = 12;
//    public static final String INT_ST_PARTIAL_COMPLETE = "12";
//    public static final int NO_PAYMENT_1 = 55;
//    public static final int NO_PAYMENT_2 = 56;
//    //Billing type
//    public static final int BILLING_SYSTEM = 121;
//    public static final int BILLING_MONTHLY = 122;
//    public static final int BILLING_SERVICE = 125;
//
//    public static String getBillingType(int type) {
//        String bType = "MTM";
//        switch (type) {
//            case BILLING_SYSTEM:
//                bType = "SYS";
//                break;
//
//            case BILLING_SERVICE:
//                bType = "SRV";
//                break;
//        }
//        return bType;
//    }
    //Payment method
    public static final int PAYMENT_INIT = 100;
    public static final int PAYMENT_CREDIT_CARD = 101;
    public static final int PAYMENT_PAY_PAL = 102;
    public static final int PAYMENT_CHEQUE = 103;
    public static final int PAYMENT_CASH = 104;
    public static final int PAYMENT_CREDIT = 105;
    public static final int PAYMENT_ADJUST = 106;

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

    private static ArrayList accountIdNameArray = new ArrayList();
    private static ArrayList accountFundIdNameArray = new ArrayList();

    public void ProcessFundMgrAccount(ServiceAFweb serviceAFWeb) {

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
        String LockName = "ALL_FUNDMGR";
        long lockReturn = serviceAFWeb.setLockNameProcess(LockName, ConstantKey.FUND_LOCKTYPE, lockDateValue, ServiceAFweb.getServerObj().getSrvProjName() + "_ProcessAllAccountTradingSignal");
        if (CKey.NN_DEBUG == true) {
            lockReturn = 1;
        }
        if (lockReturn > 0) {
            FundMgrProcess fundmgr = new FundMgrProcess();
            fundmgr.updateMutualFundAll();

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
                        updateMutualFundBestStock(serviceAFWeb, accountObj);
                    }
                } catch (Exception e) {
                    logger.info("> ProcessFundAccount Exception " + e.getMessage());
                }
            }
            serviceAFWeb.removeNameLock(LockName, ConstantKey.FUND_LOCKTYPE);
        }
    }

    public int updateMutualFundBestStock(ServiceAFweb serviceAFWeb, AccountObj accountObj) {
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
                AccountObj accountAdminObj = serviceAFWeb.getAdminObjFromCache();

                ArrayList<PerformanceObj> perfList = new ArrayList();
                for (int i = 0; i < AccountStockNameList.size(); i++) {
                    String stockN = (String) AccountStockNameList.get(i);
                    String EmailUserName = CKey.ADMIN_USERNAME;
                    String Password = null;
                    String AccountIDSt = "" + accountAdminObj.getId();
                    String stockidsymbol = stockN;
                    String trName = ConstantKey.TR_NN2;
                    int length = 1;
                    ArrayList<PerformanceObj> stockPerList = serviceAFWeb.getAccountStockPerfHistory(EmailUserName, Password, AccountIDSt, stockidsymbol, trName, length);
                    if (stockPerList != null) {
                        if (stockPerList.size() > 0) {
                            PerformanceObj perfObj = stockPerList.get(0);
                            perfObj.setName(stockidsymbol);

                            perfList.add(perfObj);
                        }
                    }
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

                Collections.reverse(perfList);
                ArrayList accPortList = new ArrayList();
                for (PerformanceObj perObj : perfList) {
                    System.out.println(perObj.getName() + " " + perObj.getGrossprofit());
                    if (perObj.getGrossprofit() > 0) {
                        if (accPortList.size() < 3) {
                            accPortList.add(perObj.getName());
                        }
                    }
                }

                fundMgr.setAccL(accPortList);

                String portfStr;
                try {
                    portfStr = new ObjectMapper().writeValueAsString(fundMgr);
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

        CustomerObj custObj = getAccountImp().getCustomerStatus(CKey.FUND_MANAGER_USERNAME, null);
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
//
//    public boolean getGlobeFundStockListTest() {
//
//        int nStock = 1;
//        String fundURL = FidelityGlobalHealthCare;
//        this.getGlobeFundStockList(fundURL, stockArrayFidelityGlobalHealthCare, nStock);
//
//        fundURL = FidelityTechnologyInnovators;
//        this.getGlobeFundStockList(fundURL, stockArrayFidelityTechnologyInnovators, nStock);
//
//        fundURL = TDBlueChipEquity;
//        this.getGlobeFundStockList(fundURL, stockArrayTDBlueChipEquity, nStock);
//
//        fundURL = TDDividendGrowth;
//        this.getGlobeFundStockList(fundURL, stockArrayTDDividendGrowth, nStock);
//
//        fundURL = TDMonthlyIncome;
//        this.getGlobeFundStockList(fundURL, stockArrayTDMonthlyIncome, nStock);
//
//        fundURL = TDPreciousMetals;
//        this.getGlobeFundStockList(fundURL, stockArrayTDPreciousMetals, nStock);
//
//        fundURL = TDGlobalEntertainment;
//        this.getGlobeFundStockList(fundURL, stockArrayTDGlobalEntertainment, nStock);
//
//        fundURL = TDScienceTechnology;
//        this.getGlobeFundStockList(fundURL, stockArrayTDScienceTechnology, nStock);
//
//        return true;
//
//    }

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

    public String copyMutualFund(String fundName, String mutualFundName) {
////        applog.debugLog("copyMutualFund", fundName);
//
//        AFtradingAccount tradingAccount;
//        tradingAccount = TradingAccount.getTradingAccount(fundName);
//        if (tradingAccount == null) {
//            return CKey.WS_ACC_NOT_FOUND_MSG;
//        }
////        AFtradingOpt tradeOption = TradingAccount.getAccountTradeOption(tradingAccount);
//        // why we need to the disable auto trade???
////        tradingOpt.setAutoTradeEnable(CKey.DISABLE);
////        tradingOpt.save();
//
//        AFtradingAccount mutualAccount;
//        mutualAccount = TradingAccount.getMutualFundAccount(mutualFundName);
//        if (mutualAccount == null) {
//            return CKey.WS_ACC_NOT_FOUND_MSG;
//        }
//        if (fundName.equals(mutualFundName)) {
//            return CKey.WS_SUCCESS;
//        }
//        ArrayList stockArray = new ArrayList();
//
//        // update accountObj
//        ArrayList addArray = new ArrayList();
//        ArrayList delArray = new ArrayList();
//        String[] cStockArray = SQLObject.getAccountStockSymbol(fundName);
//        if (cStockArray != null) {
//
//            ArrayList currentStock = new ArrayList();
//            for (int i = 1; i < cStockArray.length; i++) {
//                currentStock.add(cStockArray[i]);
//            }
//
//            cStockArray = SQLObject.getAccountStockSymbol(mutualFundName);
//            for (int i = 1; i < cStockArray.length; i++) {
//                stockArray.add(cStockArray[i]);
//            }
//            Collections.sort(currentStock);
//            Collections.sort(stockArray);
//            FundUtil.compareAddFundStock(currentStock, stockArray, addArray);
//            FundUtil.compareDeleteFundStock(currentStock, stockArray, delArray);
//
//            if (addArray.size() > 0) {
//                for (int k = 0; k < addArray.size(); k++) {
//                    String stockSym = (String) addArray.get(k);
////                        AFstockObj StockD = StockUtil.getStockShortYahoo(yahooName);
////                        if (StockD == null) {
////                            applog.debugLog("updateMutualFundFile", " invalid stock " + stockSym);
////                            continue;
////                        }
//
//                    BuyStock(fundName, CKey.S_NEUTRAL, stockSym, 0, "");
//                }
//            }
//
//            if (delArray.size() > 0) {
//                for (int k = 0; k < delArray.size(); k++) {
//                    String stockSym = (String) delArray.get(k);
//                    // force to delete immediately
//                    deleteStock(fundName, stockSym, "0", true);
//                }
//            }
//        }
//
//        return CKey.WS_SUCCESS;
// 
        return "";
    }

    public String RemoveStockOnPlan(String fundName, int stockSize) {

//        AFtradingAccount tradingAccount;
//        tradingAccount = TradingAccount.getTradingAccount(fundName);
//        if (tradingAccount == null) {
//            return CKey.WS_ACC_NOT_FOUND_MSG;
//        }
//
//        ArrayList stockArray = new ArrayList();
//
//        ArrayList delArray = new ArrayList();
//        String[] cStockArray = SQLObject.getAccountStockSymbol(fundName);
//        if (cStockArray != null) {
//            if (cStockArray.length <= (stockSize + 1)) {
//                return CKey.WS_SUCCESS;
//            }
//
//            ArrayList currentStock = new ArrayList();
//            for (int i = 1; i < cStockArray.length; i++) {
//                currentStock.add(cStockArray[i]);
//            }
//
//            for (int i = 1; i < (stockSize + 1); i++) {
//                if (i >= cStockArray.length) {
//                    break;
//                }
//                stockArray.add(cStockArray[i]);
//            }
//            Collections.sort(currentStock);
//            Collections.sort(stockArray);
//            FundUtil.compareDeleteFundStock(currentStock, stockArray, delArray);
//
//            if (delArray.size() > 0) {
//                for (int k = 0; k < delArray.size(); k++) {
//                    String stockSym = (String) delArray.get(k);
//                    // force to delete immediately
//                    deleteStock(fundName, stockSym, "0", true);
//                }
//            }
//        }
//        return CKey.WS_SUCCESS;
        return "";
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
