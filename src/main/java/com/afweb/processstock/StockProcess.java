/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.processstock;

import com.afweb.processcustacc.AccountTranImp;
import com.afweb.nnsignal.NNCalProcess;

import com.afweb.model.*;

import com.afweb.model.account.*;

import com.afweb.model.stock.*;

import com.afweb.service.ServiceAFweb;
import com.afweb.signal.*;
import com.afweb.dbsys.SysDB;
import com.afweb.dbsys.SysImp;

import com.afweb.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Calendar;

import java.util.logging.Logger;

/**
 *
 * @author koed
 */
public class StockProcess {

    protected static Logger logger = Logger.getLogger("StockProcess");

    ///////////////
    private static ArrayList stockUpdateNameArray = new ArrayList();

    private ArrayList updateStockUpdateNameArray(ServiceAFweb serviceAFWeb) {
        if (stockUpdateNameArray != null && stockUpdateNameArray.size() > 0) {
            return stockUpdateNameArray;
        }
        ArrayList stockNameArray = serviceAFWeb.StoGetAllOpenStockNameServ();
        if (stockNameArray != null) {
            stockUpdateNameArray = stockNameArray;
        }
        return stockUpdateNameArray;
    }

    public int UpdateAllStockTrend(ServiceAFweb serviceAFWeb) {
        ServiceAFweb.lastfun = "UpdateAllStockTrend";

        //SimpleDateFormat etDf = new SimpleDateFormat("MM/dd/yyyy 'at' hh:mma 'ET'");
//        Calendar dateNow = TimeConvertion.getCurrentCalendar();
//        long lockDateValue = dateNow.getTimeInMillis();
//        int hr = DateUtil.getHourNow();
//        boolean mkopen = DateUtil.isMarketOpen();
//        if (mkopen == false) {  //
//            String LockName = "MK_CLOSE_" + ServiceAFweb.getServerObj().getServerName();
//            int lockReturn = serviceAFWeb.SysSetLockName(LockName, ConstantKey.NN_LOCKTYPE, lockDateValue, ServiceAFweb.getServerObj().getSrvProjName() + "_updateAllStockProcess_" + hr);
////            if (CKey.NN_DEBUG == true) {
////                lockReturn = 1;
////            }
//            if (lockReturn == 0) {
//                //no unlock and just wait for 90 minutes
//                return 0;
//            }
//            logger.info("UpdateAllStock marke close " + hr);
//        }
        updateStockUpdateNameArray(serviceAFWeb);
        int result = 0;
        try {
            if ((stockUpdateNameArray == null) || (stockUpdateNameArray.size() == 0)) {
                return 0;
            }
            long currentTime = System.currentTimeMillis();
            long lockDate1Min = TimeConvertion.addMinutes(currentTime, 1);

            for (int i = 0; i < 10; i++) {
                currentTime = System.currentTimeMillis();
                if (CKey.NN_DEBUG != true) {
                    if (lockDate1Min < currentTime) {
                        break;
                    }
                }
                if (stockUpdateNameArray.size() == 0) {
                    break;
                }
                String NormalizeSymbol = (String) stockUpdateNameArray.get(0);
                stockUpdateNameArray.remove(0);
                result = updateAllStockTrendProcess(serviceAFWeb, NormalizeSymbol, true);

            }
        } catch (Exception ex) {
        }
        return result;
    }

    public int updateAllStockTrendProcess(ServiceAFweb serviceAFWeb, String NormalizeSymbol, boolean updateTrend) {
        ServiceAFweb.lastfun = "updateAllStockTrendProcess";

//        logger.info("> updateAllStockTrendProcess " + NormalizeSymbol);
        AFstockObj stock = null;
        // eddy testing
        // yahoo crumb fail not working

        try {

            stock = serviceAFWeb.StoGetStockObjBySym(NormalizeSymbol);
            if (stock == null) {
                return 0;
            }
            if (stock.getStatus() != ConstantKey.OPEN) {
                return 0;
            }

            long lockDateValue = TimeConvertion.getCurrentCalendar().getTimeInMillis();

            String LockName = "TRE_" + NormalizeSymbol;
            int lockReturn = serviceAFWeb.SysSetLockName(LockName, ConstantKey.STOCK_LOCKTYPE, lockDateValue, ServiceAFweb.getServerObj().getSrvProjName() + "_updateAllStockProcess");
            if (CKey.NN_DEBUG == true) {
                lockReturn = 1;
            }
            if (lockReturn > 0) {

                long lastupdate1 = stock.getUpdatedatel();
                long lastUpdate5Day = TimeConvertion.addDays(lastupdate1, 5); // 5 days
                long currentTime = System.currentTimeMillis();
                if (lastUpdate5Day > currentTime) {
                    //////// Update Long and short term trend 
                    int resultCalcuate = calculateTrend(serviceAFWeb, stock, 0);
                    // udpate other trends 

                    // send SQL update
                    String sockUpdateSQL = SysDB.SQLupdateStockSignal(stock);
                    ArrayList sqlList = new ArrayList();
                    sqlList.add(sockUpdateSQL);
                    serviceAFWeb.StoUpdateSQLArrayList(sqlList);

                    serviceAFWeb.SysLockRemoveLockName(LockName, ConstantKey.STOCK_LOCKTYPE);
                    return 1;
                }
            } else {
                if (ServiceAFweb.mydebugtestflag == true) {
                    logger.info("> updateAllStockTrendProcess lockReturn fail " + lockDateValue + " " + NormalizeSymbol);
                }
            }
        } catch (Exception ex) {
            logger.info("> updateAllStock exception " + ex.getMessage());
        }
        return 0;
    }

    public int calculateTrend(ServiceAFweb serviceAFWeb, AFstockObj stock, long dateNowL) {
        if (stock == null) {
            return 0;
        }
        int size1year = 5 * 52;
        ArrayList StockArray = serviceAFWeb.InfGetStockHistorical(stock.getSymbol(), size1year);
        if (StockArray == null) {
            return 0;
        }
        if (StockArray.size() < 10) {
            return 0;
        }
        int offset = AccountTranImp.getOffetDate(StockArray, dateNowL);

        float STerm = (float) TechnicalCal.TrendUpDown(StockArray, offset, SysImp.SHORT_TERM_TREND);
        float LTerm = (float) TechnicalCal.TrendUpDown(StockArray, offset, SysImp.LONG_TERM_TREND);
        ADXObj adx = TechnicalCal.AvgDir(StockArray, offset, 14);

        int iSTerm = (int) STerm;
        int iLTerm = (int) LTerm;
        stock.setLongterm(iLTerm);
        stock.setShortterm(iSTerm);
        stock.setDirection((float) adx.trsignal);

        updateStockRecommendation(serviceAFWeb, stock, StockArray);

        return 1;
    }

    public int updateStockRecommendation(ServiceAFweb serviceAFWeb, AFstockObj stock, ArrayList StockArray) {
        if (stock == null) {
            return 0;
        }
        if (stock.getStatus() != ConstantKey.OPEN) {
            return 0;
        }
        // reduce server processing
        if (getEnv.checkLocalPC() == false) {
            return 0;
        }
        String dataSt = stock.getData();

        StockData stockData = new StockData();

        try {
            if ((dataSt != null) && (dataSt.length() > 0)) {
                dataSt = dataSt.replaceAll("#", "\"");
                stockData = new ObjectMapper().readValue(dataSt, StockData.class);
            }

        } catch (Exception ex) {
        }
/////////////////////////////        
        try {
            String trname = ConstantKey.TR_NN1;
            if (stock != null) {
                stock.setTrname(trname);
                AccountObj accountAdminObj = serviceAFWeb.SysGetAdminObjFromCache();
                ArrayList<TradingRuleObj> trObjList = serviceAFWeb.AccGetAccountStockTRListByAccIdStockId(accountAdminObj.getId(), stock.getId());
                if (trObjList != null) {
                    if (trObjList.size() != 0) {

                        for (int j = 0; j < trObjList.size(); j++) {
                            TradingRuleObj trObj = trObjList.get(j);
                            if (trname.equals(trObj.getTrname())) {

                                stock.setTRsignal(trObj.getTrsignal());
                                float totalPercent = serviceAFWeb.AccGetAccountStockBalance(trObj);
                                if (totalPercent == -9999) {
                                    ;
                                } else {
                                    stock.setPerform(totalPercent);
                                }
                                break;
                            }
                        }
                    }
                }

                stockData.setRec(0);
                if (stock.getPerform() > 40) {  // greater than 20 %
                    stockData.setRec(2);
                } else if (stock.getPerform() > 20) {  // greater than 20 %
                    stockData.setRec(1);
                }

                stockData.setUpDn((int) stock.getLongterm());
                stockData.setChDir((int) stock.getDirection());

                if (stock.getLongterm() > 60) {
                    stockData.setTop(1);
                } else if (stock.getLongterm() < -60) {
                    stockData.setTop(-1);
                }

                stockData.setpCl(0);
                if (ServiceAFweb.javamainflag == true) {
                    NNObj nn = NNCalProcess.NNpredict(serviceAFWeb, ConstantKey.INT_TR_NN30, accountAdminObj, stock, StockArray, 0);
                    if (nn != null) {

                        float output1 = nn.getOutput1();
                        float output2 = nn.getOutput2();
                        if ((CKey.PREDICT_THRESHOLD < output1) || (CKey.PREDICT_THRESHOLD < output2)) {

                            AFstockInfo stockinfo = (AFstockInfo) StockArray.get(0);
                            float closeOutput0 = stockinfo.getFclose();
                            float closeOutput = closeOutput0;
                            if (CKey.PREDICT_THRESHOLD < output1) {
                                float closef = (float) 0.9;
                                closef = closef / 15;
                                closeOutput = (closef * closeOutput0) + closeOutput0;
                            } else if (CKey.PREDICT_THRESHOLD < output2) {
                                float closef = (float) -0.9;
                                closef = closef / 15;
                                closeOutput = (closef * closeOutput0) + closeOutput0;
                            }
                            stockData.setpCl(closeOutput);
                        }
                    }
                } // end of javamain
            } // end if stock null
//////////////////////
            String sdataStr = new ObjectMapper().writeValueAsString(stockData);
            sdataStr = sdataStr.replaceAll("\"", "#");
            stock.setData(sdataStr);
            return 1;
        } catch (Exception ex) {
        }
        return 0;
    }

//////////////////////////////////////
}
