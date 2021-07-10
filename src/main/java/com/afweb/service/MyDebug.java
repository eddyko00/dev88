/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.service;

/**
 *
 * @author eddyko
 */
public class MyDebug {
    
///////////////////////////Update Signal////////////////////////////////////    
//            ArrayList StockArray = this.InfGetStockHistorical(symbol, 100);
//            AccountObj accountAdminObj = SysGetAdminObjFromCache();
////            TRprocessImp.updateAdminTradingsignal(this, accountAdminObj, symbol);
//            TRprocessImp.upateAdminTransaction(this, accountAdminObj, symbol);
///////////////////////////Update Stock info history//////////////////////////////////// 
//            AFstockObj stock = this.StoGetStockObjBySym(symbol);
//            this.ProcessUpdateAllStockInfo();
//            StockInfoProcess infoProc = new StockInfoProcess();
//            infoProc.updateAllStockInfoProcess(this, symbol);
///////////////////////////////////////////////////////////////    
///////////////////////////////////////////////////////////////    
///////////////////////////////////////////////////////////////    
    
    
//            String symbolL[] = ServiceAFweb.allStock;
//            TradingNNprocess.CreateAllStockHistoryFile(this, symbolL, "nnAllStock");
//            ArrayList<AFstockInfo> stockInfoList = TradingNNprocess.getAllStockHistoryFile(this, "MSFT", "nnAllStock");
//            if (stockInfoList != null) {
//                logger.info("stockInfoList " + stockInfoList.size());
//            }
//            symbol = "T.TO";
//            trNN = ConstantKey.INT_TR_NN2;
//            TR_NN = trNN;
//            nnName = ConstantKey.TR_NN2;
//            BPnameSym = CKey.NN_version + "_" + nnName + "_" + symbol;
//
//            getStockImp().deleteNeuralNetDataByBPname(BPnameSym);
//            trNN = ConstantKey.INT_TR_NN3;
//            TR_NN = trNN;
//            nnName = ConstantKey.TR_NN3;
//            BPnameSym = CKey.NN_version + "_" + nnName + "_" + symbol;
//            AFneuralNet nnObj1 = nn3ProcBySig.ProcessTrainSignalNeuralNet(this, BPnameSym, TR_NN, symbol);
//            symbol = "GLD";
//            nnName = ConstantKey.TR_NN3;
//            BPnameSym = CKey.NN_version + "_" + nnName + "_" + symbol;
//
//            AccountObj accountAdminObj = getAdminObjFromCache();
//            int retSatus = NNProcessImp.ClearStockNNTranHistory(this, nnName, symbol);
////
//            nn3testflag = true;
//            TRprocessImp.updateAdminTradingsignal(this, accountAdminObj, symbol);
//            TRprocessImp.upateAdminTransaction(this, accountAdminObj, symbol);
//////// clear TR_NN3
//            nnName = ConstantKey.TR_NN3;
//            BPnameSym = CKey.NN_version + "_" + nnName;
//            removeNeuralNetDataAllSymbolByTR(BPnameSym);
//////// clear TR_NN3    
    
    ///////////////////////
//            nn3testflag = true;
//            nn3ProcBySig.NeuralNetNN3CreateJava(this, ConstantKey.TR_NN3);
//            TradingNNprocess NNProcessImp = new TradingNNprocess();
//            int retSatus = NNProcessImp.ClearStockNNTranHistory(this, ConstantKey.TR_NN3, symbol);
//            AccountObj accountAdminObj = getAdminObjFromCache();
//            TRprocessImp.updateAdminTradingsignal(this, accountAdminObj, symbol);
//            TRprocessImp.upateAdminTransaction(this, accountAdminObj, symbol);
//            TRprocessImp.upateAdminPerformance(this, accountAdminObj, symbol);
//            symbol = "GLD";
//            retSatus = NNProcessImp.ClearStockNNTranHistory(this, ConstantKey.TR_NN3, symbol);
//            TRprocessImp.updateAdminTradingsignal(this, accountAdminObj, symbol);
//            TRprocessImp.upateAdminTransaction(this, accountAdminObj, symbol);
//            TRprocessImp.upateAdminPerformance(this, accountAdminObj, symbol);
//            //
//
//            symbol = "GLD";
//            trNN = ConstantKey.INT_TR_MACD;
//            TR_NN = trNN;
//            nnName = ConstantKey.TR_MACD;
//            BPnameSym = CKey.NN_version + "_" + nnName + "_" + symbol;
//
//            AccountObj accountAdminObj = getAdminObjFromCache();
//            TRprocessImp.updateAdminTradingsignal(this, accountAdminObj, symbol);
//            TRprocessImp.upateAdminTransaction(this, accountAdminObj, symbol);
//            TRprocessImp.upateAdminPerformance(this, accountAdminObj, symbol);
////            // http://localhost:8080/cust/admin1/acc/1/st/hou_to/tr/TR_nn2/tran/history/chart
//            AccountObj accountAdminObj = getAdminObjFromCache();
//            AFstockObj stock = getRealTimeStockImp(symbol);
//
//            nn3testflag = true;
//            TradingNNprocess NNProcessImp = new TradingNNprocess();
//            int retSatus = NNProcessImp.ClearStockNNTranHistory(this, ConstantKey.TR_NN3, symbol);
//
//            TRprocessImp.updateAdminTradingsignal(this, accountAdminObj, symbol);
//            TRprocessImp.upateAdminTransaction(this, accountAdminObj, symbol);
            // javamain localmysqlflag nn3testflag  mydebugtestflag
//            TRprocessImp.ProcessAdminSignalTrading(this);
//
//            BillingProcess BP = new BillingProcess();
//            BP.processUserBillingAll(this);
            /////////// delete NN2
//            trNN = ConstantKey.INT_TR_NN2;
//            TR_NN = trNN;
//            nnName = ConstantKey.TR_NN2;
//            BPnameSym = CKey.NN_version + "_" + nnName;
//            getStockImp().deleteNeuralNetDataObj(BPnameSym, 0);
//            AccountObj accountObj = getAdminObjFromCache();
//            ArrayList stockNameArray = SystemAccountStockNameList(accountObj.getId());
//            ArrayList stockNameArray = new ArrayList();
//            stockNameArray.add("BB.TO");
//            stockNameArray.add("SU");
//            stockNameArray.add("ENB.TO");            
//            stockNameArray.add("TSLA");              
//            if (stockNameArray != null) {
//                for (int i = 0; i < stockNameArray.size(); i++) {
//                    symbol = (String) stockNameArray.get(i);
//
//                    trNN = ConstantKey.INT_TR_NN2;
//                    TR_NN = trNN;
//                    nnName = ConstantKey.TR_NN2;
//                    BPnameSym = CKey.NN_version + "_" + nnName + "_" + symbol;
//                    getStockImp().deleteNeuralNetDataObj(BPnameSym, 0);
//                }
//            }
//            
//            nn1ProcBySig.ProcessTrainSignalNeuralNet(this, BPnameSym, TR_NN, symbol);
//            symbol = "HOD.TO";
//            trNN = ConstantKey.INT_TR_NN2;
//            TR_NN = trNN;
//            nnName = ConstantKey.TR_NN2;
//            BPnameSym = CKey.NN_version + "_" + nnName + "_" + symbol;
//            AccountObj accountAdminObj = getAdminObjFromCache();
//            TRprocessImp.updateAdminTradingsignal(this, accountAdminObj, symbol);
//            symbol = "AAPL";
//             AccountObj account = getAccountImp().getAccountByType(CKey.G_USERNAME, "guest", AccountObj.INT_TRADING_ACCOUNT);
//            this.getAccountProcessImp().updateTradingAccountBalance(this, account, symbol); 
//            AccountObj accountObj = getAdminObjFromCache();
//            ArrayList stockNameArray = SystemAccountStockNameList(accountObj.getId());
//            for (int i = 0; i < stockNameArray.size(); i++) {
//                symbol = (String) stockNameArray.get(i);
//                trNN = ConstantKey.INT_TR_NN40;
//                TR_NN = trNN;
//                nnName = ConstantKey.TR_NN40;
//                BPnameSym = CKey.NN_version + "_" + nnName + "_" + symbol;
//                getStockImp().deleteNeuralNetDataObj(BPnameSym, 0);
//                getStockImp().deleteNeuralNet0Rel(BPnameSym);
//            }
//            for (int i = 0; i < 30; i++) {
//                nn1ProcBySig.ProcessTrainNN1NeuralNetBySign(this);
//            }
//            nn1ProcBySig.TrainNN1NeuralNetBySign(this, symbol, TR_NN, null);
//            this.getAccountProcessImp().ProcessStockInfodeleteMaintance(this);
//            symbol = "FAS";
//            trNN = ConstantKey.INT_TR_NN1;
//            TR_NN = trNN;
//            nnName = ConstantKey.TR_NN1;
//            BPnameSym = CKey.NN_version + "_" + nnName + "_" + symbol;
//            getStockImp().deleteNeuralNetDataObj(BPnameSym, 0);
//            symbol = "FAZ";
//            trNN = ConstantKey.INT_TR_NN1;
//            TR_NN = trNN;
//            nnName = ConstantKey.TR_NN1;
//            BPnameSym = CKey.NN_version + "_" + nnName + "_" + symbol;
//            getStockImp().deleteNeuralNetDataObj(BPnameSym, 0);
//            BillingProcess billProc = new BillingProcess();
//            for (int i = 0; i < 10; i++) {
//                billProc.processUserBillingAll(this);
//            }       
//
//////////////////////////////// trading Simulation ////////////              
//////////////////////////////// trading Simulation ////////////  
//            symbol = "AAPL";
//
//            mydebugSim = true;
//            Calendar dateNow = TimeConvertion.getCurrentCalendar();
//            SimDateL = dateNow.getTimeInMillis();
//            SimDateL = TimeConvertion.endOfDayInMillis(SimDateL);
////            SimDateL = TimeConvertion.addDays(SimDateL, -10);
//
//            TradingNNprocess NNProcessImp = new TradingNNprocess();
//            AccountObj accountAdminObj = getAdminObjFromCache();
//
//            boolean flag1 = true;
//            if (flag1 == true) {
//                int retSatus = NNProcessImp.ClearStockNNTranHistory(this, nnName, symbol);
//                TRprocessImp.upateAdminTransaction(this, accountAdminObj, symbol);
//            } else {
//
//                for (int i = 0; i < 15; i++) {
//                    SimDateL = TimeConvertion.addDays(SimDateL, 1);
//
//                    TRprocessImp.updateAdminTradingsignal(this, accountAdminObj, symbol);
//                    TRprocessImp.upateAdminTransaction(this, accountAdminObj, symbol);
//                    TRprocessImp.upateAdminPerformance(this, accountAdminObj, symbol);
//                    TRprocessImp.upateAdminTRPerf(this, accountAdminObj, symbol);
//                }
//            }
//////////////////////////////// trading Simulation ////////////  
//////////////////////////////// trading Simulation ////////////  
///////////////////////////////////////////////////// Update new TR NN91 in Admin
//            ///////Adding  new TR in Admin Sotcks
//            AccountObj accountObj = getAdminObjFromCache();
//            ArrayList stockNameList = getAccountImp().getAccountStockNameList(accountObj.getId());
//            if (accountObj.getType() == AccountObj.INT_ADMIN_ACCOUNT) {
//                for (int i = 0; i < stockNameList.size(); i++) {
//                    symbol = (String) stockNameList.get(i);
//                    AFstockObj stock = getRealTimeStockImp(symbol);
//                    TradingRuleObj tr = new TradingRuleObj();
//                    tr.setTrname(ConstantKey.TR_NN91);
//                    tr.setType(ConstantKey.INT_TR_NN91);
//                    tr.setComment("");
//                    int retAdd = this.getAccountImp().accountdb.addAccountStock(accountObj.getId(), stock.getId(), tr);
//                    tr = new TradingRuleObj();
//                    tr.setTrname(ConstantKey.TR_NN92);
//                    tr.setType(ConstantKey.INT_TR_NN92);
//                    tr.setComment("");
//                    retAdd = this.getAccountImp().accountdb.addAccountStock(accountObj.getId(), stock.getId(), tr);
//                    tr = new TradingRuleObj();
//                    tr.setTrname(ConstantKey.TR_NN93);
//                    tr.setType(ConstantKey.INT_TR_NN93);
//                    tr.setComment("");
//                    retAdd = this.getAccountImp().accountdb.addAccountStock(accountObj.getId(), stock.getId(), tr);
//
//                }
//            }
//            ///////Adding  new TR in Admin Sotcks
///////////////////////////////////////////////////// Update stock
//            TRprocessImp.UpdateAllStock(this);
//            AFstockObj stock = getRealTimeStockImp(symbol);
//            TRprocessImp.updateRealTimeStock(this, stock);
/////////////////////////////////////////////////////            
//            if (nn3testflag == true) {
            // javamain localmysqlflag nn3testflag mydebugtestflag
            // http://localhost:8080/cust/admin1/acc/1/st/gld/tr/TR_NN3/tran/history/chart
            // http://localhost:8080/cust/admin1/acc/1/st/gld/tr/TR_NN3/perf
            // https://iiswebsrv.herokuapp.com/cust/admin1/acc/1/st/gld/tr/TR_NN2/tran/history/chart
//                symbol = "HOU.TO"; // "GLD";
//                trNN = ConstantKey.INT_TR_NN3;
//                TR_NN = trNN;
//                nnName = ConstantKey.TR_NN3;
//                BPnameSym = CKey.NN_version + "_" + nnName + "_" + symbol;
//
//                AccountObj accountAdminObj = getAdminObjFromCache();
//                TradingNNprocess NNProcessImp = new TradingNNprocess();
//                NN3ProcessBySignal nn3ProcBySig = new NN3ProcessBySignal();
//                boolean init = true;
//                
//                init = false;
//                if (init == true) {
//                    for (int j = 0; j < 5; j++) {
//                        nn3ProcBySig.TrainNN3NeuralNetBySign(this, symbol, ConstantKey.INT_TR_NN3, null);
//                        NNProcessImp.ReLearnInputNeuralNet(this, symbol, ConstantKey.INT_TR_NN3);
//
//                    }
//                    
//                } else {
//                    int retSatus = NNProcessImp.ClearStockNNTranHistory(this, ConstantKey.TR_NN3, symbol);
//
//                    TRprocessImp.updateAdminTradingsignal(this, accountAdminObj, symbol);
//                    TRprocessImp.upateAdminTransaction(this, accountAdminObj, symbol);
//                    TRprocessImp.upateAdminPerformance(this, accountAdminObj, symbol);
//                }
//            }
////////////////////////////////////////////////////////
//            systemRemoveAllEmail();
//            BillingProcess billProc = new BillingProcess();
//            for (int i = 0; i < 10; i++) {
//                billProc.processUserBillingAll(this);
//            }
//            ArrayList custNameList = getCustomerObjByNameList(CKey.G_USERNAME);
//            CustomerObj customer = (CustomerObj) custNameList.get(0);
//            billProc.updateUserBilling(this, customer);
//            getAccountImp().removeCommByName(CKey.ADMIN_USERNAME, null, ConstantKey.COM_EMAIL);
//            
//
//            EmailProcess eProcess = new EmailProcess();
//            ServiceAFweb.processEmailFlag = true;
//            String tzid = "America/New_York"; //EDT
//            TimeZone tz = TimeZone.getTimeZone(tzid);
//            java.sql.Date d = new java.sql.Date(System.currentTimeMillis());
//            DateFormat format = new SimpleDateFormat(" hh:mm a");
//            format.setTimeZone(tz);
//            String ESTdate = format.format(d);
//            String sig = "exit";
//            String msg = ESTdate + " " + symbol + " Sig:" + sig;
//            AccountObj accountObj = getAccountImp().getAccountByType(CKey.G_USERNAME, "guest", AccountObj.INT_TRADING_ACCOUNT);
//            getAccountImp().addAccountEmailMessage(accountObj, ConstantKey.COM_EMAIL, msg);
//            for (int i = 0; i < 100; i++) {
//                eProcess.ProcessEmailAccount(this);
//                try {
//                    Thread.sleep(30 * 1000);
//                } catch (InterruptedException ex) {
//                    Thread.currentThread().interrupt();
//                }
//            }
////////////////////////////////////////////////////////////////////
//             AFstockObj stock = getRealTimeStockImp(symbol);
//             int resultUpdate = TRprocessImp.updateRealTimeStock(this, stock);
//            getAccountProcessImp().downloadDBData(this);
//            NN1ProcessByTrend nn1trend = new NN1ProcessByTrend();
//            TrandingSignalProcess.forceToGenerateNewNN = false;
//            NN1ProcessBySignal.processRestinputflag = true;
//            NN2ProcessByTrend nn2trend = new NN2ProcessByTrend();
//            nn2trend.processNN40InputNeuralNetTrend(this);
//            nn2trend.processAllNN40StockInputNeuralNetTrend(this);
//            nn1trend.processNN30InputNeuralNetTrend(this);
//            nn1trend.processAllNN30StockInputNeuralNetTrend(this);
//            int ret = this.getAccountProcessImp().saveDBneuralnetProcess(this, "neuralnet");
//            AFneuralNet nnObj1 = nn2ProcBySig.ProcessTrainSignalNeuralNet(this, BPnameSym, TR_NN, symbol);
//            delete NN2            
//            AccountObj accountObj = getAdminObjFromCache();
//            ArrayList stockNameArray = SystemAccountStockNameList(accountObj.getId());
//            if (stockNameArray != null) {
//                for (int i = 0; i < stockNameArray.size(); i++) {
//                    symbol = (String) stockNameArray.get(i);
//
//                    trNN = ConstantKey.INT_TR_NN2;
//                    TR_NN = trNN;
//                    nnName = ConstantKey.TR_NN2;
//                    BPnameSym = CKey.NN_version + "_" + nnName + "_" + symbol;
//                    getStockImp().deleteNeuralNetDataObj(BPnameSym, 0);
//                }
//            }
//
//////////////////////////////////////////////////////////////
//            symbol = "H.TO";
//            trNN = ConstantKey.INT_TR_NN2;
//            TR_NN = trNN;
//            nnName = ConstantKey.TR_NN2;
//            BPnameSym = CKey.NN_version + "_" + nnName + "_" + symbol;
//            getStockImp().deleteNeuralNetDataObj(BPnameSym, 0);
//            trNN = ConstantKey.INT_TR_NN1;
//            TR_NN = trNN;
//            nnName = ConstantKey.TR_NN1;
//            BPnameSym = CKey.NN_version + "_" + nnName + "_" + symbol;
//            getStockImp().deleteNeuralNetDataObj(BPnameSym, 0);            
//            trNN = ConstantKey.INT_TR_NN30;
//            TR_NN = trNN;
//            nnName = ConstantKey.TR_NN30;
//            BPnameSym = CKey.NN_version + "_" + nnName + "_" + symbol;
//            getStockImp().deleteNeuralNetDataObj(BPnameSym, 0);   
//            trNN = ConstantKey.INT_TR_NN40;
//            TR_NN = trNN;
//            nnName = ConstantKey.TR_NN40;
//            BPnameSym = CKey.NN_version + "_" + nnName + "_" + symbol;
//            getStockImp().deleteNeuralNetDataObj(BPnameSym, 0);    
//            
//            int accountId = 3;
//            AccountObj accountObj = SystemAccountObjByAccountID(accountId);
//            getAccountProcessImp().updateTradingTransaction(this, accountObj, symbol);
//            for (int j = 0; j < 3; j++) {
//                AFneuralNet nnObj1 = nn2ProcBySig.ProcessTrainSignalNeuralNet(this, BPnameSym, TR_NN, symbol);
//                getStockImp().deleteNeuralNet1(BPnameSym);
//                
//                NN2ProcessBySignal nn2Process = new NN2ProcessBySignal();
//                nn2Process.inputReTrainNN2StockNeuralNetData(this, trNN, symbol);
//            }
//
///////////////////////////////////////////////////////////////////////
//            symbol = "HOU.TO";
//            AccountObj accountAdminObj = getAdminObjFromCache();
//            TradingNNprocess NNProcessImp = new TradingNNprocess();
////            int retSatus = NNProcessImp.ClearStockNNTranHistory(this, ConstantKey.TR_NN1, symbol);
//
//            TRprocessImp.updateAdminTradingsignal(this, accountAdminObj, symbol);
//            TRprocessImp.upateAdminTransaction(this, accountAdminObj, symbol);
//            TRprocessImp.upateAdminPerformance(this, accountAdminObj, symbol);
//            TrandingSignalProcess TRprocessImp = new TrandingSignalProcess();
//
//            symbol = "T.TO";
//            AccountObj account = getAccountImp().getAccountByType("GUEST", null, AccountObj.INT_TRADING_ACCOUNT);
//            AFstockObj stock = getRealTimeStockImp(symbol);
//            TradingRuleObj trObj = getAccountStockByTRname("GUEST", null, account.getId() + "", symbol, ConstantKey.TR_ACC);
//            ArrayList<PerformanceObj> currentPerfList = this.SystemAccountStockPerfList(account.getId(), stock.getId(), trObj.getTrname(), 1);
//
//////////////////////////////////////////////////////////////
//            String symbol = "HOU.TO";
//            int trNN = ConstantKey.INT_TR_NN2;
//            String nnName = ConstantKey.TR_NN2;
//            String BPnameSym = CKey.NN_version + "_" + nnName + "_" + symbol;
//            // http://localhost:8080/cust/admin1/acc/1/st/hou_to/tr/TR_nn2/tran/history/chart
//            AccountObj accountAdminObj = getAdminObjFromCache();
//            AFstockObj stock = getRealTimeStockImp(symbol);
//
//            TradingNNprocess NNProcessImp = new TradingNNprocess();
//            int retSatus = NNProcessImp.ClearStockNNTranHistory(this, ConstantKey.TR_NN2, symbol);
//
//            TRprocessImp.updateAdminTradingsignal(this, accountAdminObj, symbol);
//            TRprocessImp.upateAdminTransaction(this, accountAdminObj, symbol);
//            TRprocessImp.upateAdminPerformance(this, accountAdminObj, symbol);
//
/////////////////////////////////////////////////////////////
//            getStockImp().deleteNeuralNet1(BPnameSym);
//            AFneuralNet nnObj1 = nn2ProcBySig.ProcessTrainNeuralNet1(this, BPnameSym, trNN, symbol);
//            int ret = nn2ProcBySig.inputReTrainNN2StockNeuralNetData(this, trNN, symbol);
//
//            TrandingSignalProcess TRprocessImp = new TrandingSignalProcess();
//            AccountObj accountAdminObj = getAdminObjFromCache();
//            AFstockObj stock = getRealTimeStockImp(symbol);
//
//            getAccountImp().clearAccountStockTranByAccountID(accountAdminObj, stock.getId(), nnName);
//            TRprocessImp.updateAdminTradingsignal(this, accountAdminObj, symbol);
//            TRprocessImp.upateAdminTransaction(this, accountAdminObj, symbol);
//            TRprocessImp.upateAdminPerformance(this, accountAdminObj, symbol);

    
        ///// only acc reset
//        boolean flagTran_TR_ACC = false;
//        if (flagTran_TR_ACC == true) {
//            SystemClearNNtranAllAcc();
//        }
        // need this only if yahoo get history stock does not work
        // need this only if yahoo get history stock does not work        
//        boolean flaginputStock = false;
//        if (flaginputStock == true) {
//            StockInternet.updateAllStockFile(this);
//        }
        // need this only if yahoo get history stock does not work
        // need this only if yahoo get history stock does not work 
//        boolean saveStockFileFlag = false;
//        if (saveStockFileFlag == true) {
//            ArrayList stockNameArray = getAllOpenStockNameArray();
//            logger.info("updateRealTimeStock " + stockNameArray.size());
//            for (int i = 0; i < stockNameArray.size(); i++) {
//                String sym = (String) stockNameArray.get(i);
//                ArrayList<String> writeArray = new ArrayList();
//                int size1year = 5 * 52;
//                ArrayList StockArray = getStockHistorical(sym, size1year * 4);
//                if (StockArray == null) {
//                    continue;
//                }
//                if (StockArray.size() == 0) {
//                    continue;
//                }
//                String StFileName = FileLocalPath + sym + ".txt";
//                logger.info("saveStockFile Size " + StockArray.size() + " " + StFileName);
//                for (int j = 0; j < StockArray.size(); j++) {
//                    try {
//                        AFstockInfo obj = (AFstockInfo) StockArray.get(j);
//                        String st = new ObjectMapper().writeValueAsString(obj);
//                        writeArray.add(st);
//                    } catch (JsonProcessingException ex) {
//                    }
//                }
//                FileUtil.FileWriteTextArray(StFileName, writeArray);
//            }
//        }
//        boolean flagClearNN0Table = false;
//        if (flagClearNN0Table == true) {
//            this.getStockImp().deleteNeuralNet0Table();
//        }
//
//        boolean flagClearNN1Table = false;
//        if (flagClearNN1Table == true) {
//            this.getStockImp().deleteNeuralNet1Table();
//        }
//
//        boolean flagClearNNdataTable = false;
//        if (flagClearNNdataTable == true) {
//            this.getStockImp().deleteNeuralNetDataTable();
//        }
        ///////////////////////////////////////////////////////////////////////////////////   
    
}
