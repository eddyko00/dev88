/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.model;

/**
 *
 * @author eddy
 */
public class ConstantKey {

    public static final String VERSION = "ver1.0";
    public static final int STOCK_LOCKTYPE = 0;
    public static final int STOCK_UPDATE_LOCKTYPE = 5;
    public static final int ACC_LOCKTYPE = 10;
    public static final int TR_LOCKTYPE = 20;
    public static final int SRV_LOCKTYPE = 30;
    public static final int ADMIN_SIGNAL_LOCKTYPE = 40;
    public static final int SIGNAL_LOCKTYPE = 45;
    public static final int NN_LOCKTYPE = 50;
    public static final int NN_TR_LOCKTYPE = 51;
    public static final int NN_ST_LOCKTYPE = 52;
    public static final int FUND_LOCKTYPE = 60;
    public static final int H2_LOCKTYPE = 100;

    public static final String PP_BASIC = "BASIC";
    public static final int INT_PP_BASIC = 0;
    public static final int INT_PP_BASIC_NUM = 2;
    public static final float INT_PP_BASIC_PRICE = 10;
    public static final String PP_PREMIUM = "PREMIUM";
    public static final int INT_PP_PREMIUM = 10;
    public static final int INT_PP_REMIUM_NUM = 10;
    public static final float INT_PP_REMIUM_PRICE = 20;
    public static final String PP_DELUXE = "DELUXE";
    public static final int INT_PP_DELUXE = 20;
    public static final int INT_PP_DELUXE_NUM = 20;
    public static final float INT_PP_DELUXE_PRICE = 35;

    public static final String MSG_OPEN = "ENABLE";
    public static final int OPEN = 0;

    public static final String MSG_CLOSE = "CLOSE";
    public static final int CLOSE = 1;

    public static final String MSG_ENABLE = "ENABLE";
    public static final int ENABLE = 0;

    public static final String MSG_DISABLE = "DISABLE";
    public static final int DISABLE = 1;

    public static final String MSG_PENDING = "PENDING";
    public static final int PENDING = 2;

    public static final String MSG_NO_ACTIVE = "NO_ACTIVATE";
    public static final int NOACT = 4;

    public static final String MSG_COMPLETED = "COMPLETED";
    public static final int COMPLETED = 5;

/////// SubStatus        
    public static final String MSG_NEW = "NEW";
    public static final int NEW = 1;

    public static final String MSG_EXISTED = "EXISTED";
    public static final int EXISTED = 2;

    public static final String MSG_NOTEXISTED = "NOTEXISTED";
    public static final int NOTEXISTED = 3;

    public static final String MSG_INITIAL = "INITIAL";
    public static final int INITIAL = 2;

    public static final String MSG_STOCK_SPLIT = "STOCK_SPLIT";
    public static final int STOCK_SPLIT = 10;

    public static final String MSG_STOCK_DELTA = "STOCK_DELTA";
    public static final int STOCK_DELTA = 12;
//// communication type
    public static final int INT_COM_CFG = -1;

    public static final String COM_SIGNAL = "MSG_SIG";
    public static final String COM_ADD_ACC_MSG = "ADDACC";
    public static final int INT_COM_SIGNAL = 0;
//
    public static final String COM_SPLIT = "MSG_SPLIT";
    public static final int INT_COM_SPLIT = 2;

    public static final String BILLING = "BILLING";
    public static final int INT_BILLING = 10;
//// Android configuration      
    public static final String nullSt = "null";    // fix mapper object translation

    public static final String S_PENDING_ST = "pending";     // no trade
    public static final String S_NEUTRAL_ST = "neutral";
    public static final String S_LONG_BUY_ST = "long_buy";
    public static final String S_BUY_ST = "buy";
    public static final String S_SHORT_SELL_ST = "short_sell";
    public static final String S_SELL_ST = "sell";
    public static final String S_STOPLOSS_BUY_ST = "stop_loss_buy"; // stop loss buy
    public static final String S_STOPLOSS_SELL_ST = "stop_loss_sell"; // stop loss sell
    public static final String S_EXIT_LONG_ST = "exit_long";
    public static final String S_EXIT_SHORT_ST = "exit_short";

    public static final int S_PENDING = -1;     // no trade
    public static final int S_NEUTRAL = 0;
    public static final int SS_LONG_BUY = 11;
    public static final int S_LONG_BUY = 1;
    public static final int S_BUY = 1;
    public static final int SS_SHORT_SELL = 22;
    public static final int S_SHORT_SELL = 2;
    public static final int S_SELL = 2;
    public static final int S_STOPLOSS_BUY = 3; // stop loss buy
    public static final int S_STOPLOSS_SELL = 4; // stop loss sell
    public static final int S_EXIT_LONG = 5;
    public static final int S_EXIT_SHORT = 6;
    public static final int S_EXIT = 8;

    //trading rule
    //******** must be capital for hte TR name ***********************
    public static final String TR_ACC = "TR_ACC";  // transaction account
    public static final int INT_TR_ACC = 0;

    public static final String TR_MV = "TR_MV";  // simulation 
    public static final int INT_TR_MV = 1;
//    public static final int INT_MV_10 = 10;
    public static final int INT_MV_20 = 20;
    public static final int INT_MV_50 = 50; //50;

    public static final String TR_MACD = "TR_MACD";
    public static final int INT_TR_MACD = 2;
    public static final int INT_MACD_12 = 12;
    public static final int INT_MACD_26 = 26;
    public static final int INT_MACD_9 = 9;

    public static final String TR_RSI = "TR_RSI";
    public static final int INT_TR_RSI = 3;
    public static final int INT_RSI_14 = 14;

    public static final int INT_TR_RSI1 = 110;
    public static final int INT_RSI_5 = 5;    
    public static final int INT_TR_RSI2 = 111;
    public static final int INT_RSI_7 = 7;


    public static final String TR_NN1 = "TR_NN1"; //NN for MACD fast
    public static final int INT_TR_NN1 = 4;

    public static final String TR_NN2 = "TR_NN2"; //NN for RSI
    public static final int INT_TR_NN2 = 5;

    public static final String TR_NN3 = "TR_NN3"; //NN for 
    public static final int INT_TR_NN3 = 6;
    
    /// make sure to updat this size whend adding more TR
    /// remember to add InitStaticData in ServiceAFweb.java
    public static final int SIZE_TR = 7;
    
    public static final String TR_BB = "TR_BB"; //NN for 
    public static final int INT_TR_BB = 12;
    public static final int INT_BB_M_20 = 20;
    public static final int INT_BB_SD_2 = 2;
    public static final int INT_BB_M_10 = 10;
    public static final int INT_BB_M_5 = 5;
    public static final int INT_BB_SD_1 = 1;

    
    public static final String TR_NN30 = "TR_NN30"; //NN for Trend 
    public static final int INT_TR_NN30 = 30;    
    
    /// make sure to updat this size whend adding more TR
    /// remember to add InitStaticData in ServiceAFweb.java
    public static final String TR_NN200 = "TR_NN200";
    public static final int INT_TR_NN200 = 200;

    public static final String TR_NN100 = "TR_NN100"; //NN for MV
    public static final int INT_TR_NN100 = 100;

    public static final String TR_MACD0 = "TR_MACD0";
    public static final int INT_TR_MACD0 = 102;
    public static final int INT_MACD0_3 = 3;
    public static final int INT_MACD0_6 = 6;
    public static final int INT_MACD0_2 = 2;

    public static final String TR_MACD1 = "TR_MACD1";
    public static final int INT_TR_MACD1 = 100;
    public static final int INT_MACD1_6 = 6;
    public static final int INT_MACD1_12 = 12;
    public static final int INT_MACD1_4 = 4;

    public static int getTRtypeByName(String trname) {
        trname = trname.toUpperCase();
        if (trname.equals(TR_ACC)) {
            return INT_TR_ACC;
        } else if (trname.equals(TR_MV)) {
            return INT_TR_MV;
        } else if (trname.equals(TR_MACD)) {
            return INT_TR_MACD;
        } else if (trname.equals(TR_RSI)) {
            return INT_TR_RSI;
        } else if (trname.equals(TR_NN1)) {
            return INT_TR_NN1;
        } else if (trname.equals(TR_NN2)) {
            return INT_TR_NN2;
        } else if (trname.equals(TR_NN3)) {
            return INT_TR_NN3;
        }
        return 0;
    }
}
