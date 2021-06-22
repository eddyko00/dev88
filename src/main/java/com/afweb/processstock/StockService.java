/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.processstock;

import com.afweb.model.*;
import com.afweb.model.stock.*;

import com.afweb.service.ServiceAFweb;
import com.afweb.stock.StockImp;


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Logger;

/**
 *
 * @author koed
 */
public class StockService {

    protected static Logger logger = Logger.getLogger("StockService");
    StockProcess stockProcess = new StockProcess();
    private StockImp stockImp = new StockImp();
    
    public ArrayList getStockArray(ServiceAFweb serviceAFWeb, int length) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return null;
        }

        ArrayList stockList = serviceAFWeb.getStockImp().getStockArray(length);
        return stockList;
    }

    public AFstockObj getStockRealTime(ServiceAFweb serviceAFWeb, String symbol) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return null;
        }

        SymbolNameObj symObj = new SymbolNameObj(symbol);
        String NormalizeSymbol = symObj.getYahooSymbol();

        AFstockObj stock = stockImp.getRealTimeStock(NormalizeSymbol, null);

        if (stock == null) {
            return null;
        }
        if (stock.getStatus() == ConstantKey.OPEN) {
            if (stock.getSubstatus() != ConstantKey.INITIAL) {
                ArrayList StockArray = serviceAFWeb.getStockInfo_workaround(stock, 2, null);

                if (StockArray != null) {
                    if (StockArray.size() >= 2) {
                        AFstockInfo stocktmp = (AFstockInfo) StockArray.get(0);
                        stock.setAfstockInfo(stocktmp);
                        AFstockInfo prevStocktmp = (AFstockInfo) StockArray.get(1);
                        stock.setPrevClose(prevStocktmp.getFclose());
                    }
                }
            }
        }
        if (serviceAFWeb.mydebugSim == true) {
            Calendar cDate = null;
            cDate = Calendar.getInstance();
            cDate.setTimeInMillis(ServiceAFweb.SimDateL);
            ArrayList<AFstockInfo> stockInfolist = serviceAFWeb.getStockHistoricalServ(NormalizeSymbol, 80);
            if (stockInfolist != null) {
                if (stockInfolist.size() > 0) {
                    AFstockInfo stockinfo = stockInfolist.get(0);

                    stock.setAfstockInfo(stockinfo);
                    stock.setUpdatedatel(serviceAFWeb.SimDateL);
                    stock.setUpdatedatedisplay(new java.sql.Date(serviceAFWeb.SimDateL));
                    stock.setPrevClose(stockinfo.getFopen());

                    String tzid = "America/New_York"; //EDT
                    TimeZone tz = TimeZone.getTimeZone(tzid);
                    Date d = new Date(stock.getUpdatedatel());
                    DateFormat format = new SimpleDateFormat("M/dd/yyyy hh:mm a z");
                    format.setTimeZone(tz);
                    String ESTdate = format.format(d);
                    stock.setUpdateDateD(ESTdate);

                }
            }
        }
        return stock;
    }

    public AFstockObj getStockRealTimeBySockID(ServiceAFweb serviceAFWeb, int stockID) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return null;
        }
        AFstockObj stock = stockImp.getRealTimeStockByStockID(stockID, null);

        if (stock == null) {
            return null;
        }
        if (stock.getStatus() == ConstantKey.OPEN) {
            if (stock.getSubstatus() != ConstantKey.INITIAL) {
                ArrayList StockArray = serviceAFWeb.getStockInfo_workaround(stock, 2, null);

                if (StockArray != null) {
                    if (StockArray.size() >= 2) {
                        AFstockInfo stocktmp = (AFstockInfo) StockArray.get(0);
                        stock.setAfstockInfo(stocktmp);
                        AFstockInfo prevStocktmp = (AFstockInfo) StockArray.get(1);
                        stock.setPrevClose(prevStocktmp.getFclose());
                    }
                }
            }
        }
        return stock;
    }

    public int addStock(ServiceAFweb serviceAFWeb, String symbol) {
        StockProcess stockProcess = new StockProcess();
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        SymbolNameObj symObj = new SymbolNameObj(symbol);
        String NormalizeSymbol = symObj.getYahooSymbol();

        AFstockObj stockRT = serviceAFWeb.getRealTimeStockInternetServ(NormalizeSymbol);
        if (stockRT == null) {
            return 0;
        }

        int result = stockImp.addStock(NormalizeSymbol);
        if (result == ConstantKey.NEW) {
            stockProcess.ResetStockUpdateNameArray(serviceAFWeb);
        }
        return result;
    }

    public int deleteStock(ServiceAFweb serviceAFWeb, AFstockObj stock) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return 0;
        }
        return stockImp.deleteStock(stock);
    }

    public int disableStock(ServiceAFweb serviceAFWeb, String symbol) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        SymbolNameObj symObj = new SymbolNameObj(symbol);
        String NormalizeSymbol = symObj.getYahooSymbol();
        return stockImp.disableStock(NormalizeSymbol);
    }

    public boolean checkStock(ServiceAFweb serviceAFWeb, String NormalizeSymbol) {
        AFstockObj stock = getStockRealTime(serviceAFWeb, NormalizeSymbol);
        if (stock == null) {
            return false;
        }
        if (stock.getStatus() != ConstantKey.OPEN) {
            return false;
        }
        if (stock.getAfstockInfo() == null) {
            return false;
        }
        return true;
    }

    public int updateAllStock(ServiceAFweb serviceAFWeb) {
        return stockProcess.updateAllStock(serviceAFWeb);
    }

//    public int updateStockInfoTransaction(ServiceAFweb serviceAFWeb, StockInfoTranObj stockInfoTran) {
//        ServiceAFweb.lastfun = "updateStockInfoTransaction";
//
//        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
//            return 0;
//        }
//        return serviceAFWeb.getStockInfoImp().updateStockInfoTransaction(stockInfoTran);
//    }

//    ////////////////////////////////////////////
//    public static HashMap<String, ArrayList> stockInputMapFile = null;
//
//    public static boolean CreateAllStockHistoryFile(ServiceAFweb serviceAFWeb, String symbolL[], String fileName) {
//        HashMap<String, ArrayList> stockInputMap = new HashMap<String, ArrayList>();
//
//        try {
//            ProcessAllStockHistoryCreatJava(serviceAFWeb, symbolL, stockInputMap);
//
//            String inputListRawSt = new ObjectMapper().writeValueAsString(stockInputMap);
//            String inputListSt = ServiceAFweb.compress(inputListRawSt);
//
//            ArrayList msgWrite = new ArrayList();
//
//            int sizeline = 1000;
//            int len = inputListSt.length();
//            int beg = 0;
//            int end = sizeline;
//            while (true) {
//                String st = inputListSt.substring(beg, end);
//
//                msgWrite.add(st);
//
//                if (end >= len) {
//                    break;
//                }
//                beg = end;
//                if (end + sizeline <= len) {
//                    end += sizeline;
//                } else {
//                    end = len;
//                }
//            }
//
//            ////// end
//            String fileN = ServiceAFweb.FileLocalPath + fileName + ".txt";
//            FileUtil.FileWriteTextArray(fileN, msgWrite);
//
//            return true;
//        } catch (Exception ex) {
//        }
//        return false;
//    }
//
//    public static ArrayList<AFstockInfo> getAllStockHistoryFile(ServiceAFweb serviceAFWeb, String symbol, String fileName) {
//        if (stockInputMapFile == null) {
//            try {
//
//                String fileN = ServiceAFweb.FileLocalDebugPath + fileName + ".txt";
//                if (getEnv.checkLocalPC() == true) {
//                    fileN = ServiceAFweb.FileLocalPath + fileName + ".txt";
//                }
//                ArrayList msgRead = new ArrayList();
//                boolean ret = FileUtil.FileReadTextArray(fileN, msgRead);
//                if (ret == true) {
//                    StringBuffer msgWrite = new StringBuffer();
//                    for (int i = 0; i < msgRead.size(); i++) {
//                        msgWrite.append(msgRead.get(i));
//                    }
//                    String inputListSt = ServiceAFweb.decompress(msgWrite.toString());
//                    stockInputMapFile = new ObjectMapper().readValue(inputListSt, HashMap.class);
//                }
//            } catch (Exception ex) {
//
//            }
//
//        }
//        ArrayList<AFstockInfo> stockInfoList = ProcessAllStockHistoryfromStaticCode(symbol, stockInputMapFile);
//        return stockInfoList;
//
//    }
//
//    public static HashMap<String, ArrayList> stockInputMap = null;
//    public static HashMap<String, ArrayList> stockInputMap_1 = null;
//
//    public static boolean CreateAllStockHistoryJava(ServiceAFweb serviceAFWeb, String symbolL[], String fileName, String tagName) {
//        HashMap<String, ArrayList> stockInputMap = new HashMap<String, ArrayList>();
//
//        try {
//
//            ProcessAllStockHistoryCreatJava(serviceAFWeb, symbolL, stockInputMap);
//
//            String inputListRawSt = new ObjectMapper().writeValueAsString(stockInputMap);
//            String inputListSt = ServiceAFweb.compress(inputListRawSt);
//
//            StringBuffer msgWrite = new StringBuffer();
//            msgWrite.append("" ///
//                    + "package com.afweb.nn;\n"
//                    + "import com.afweb.service.ServiceAFweb;\n"
//                    + "\n"
//                    + "/*  This file is generated by system. Do not modify. */\n"
//                    + "public class " + fileName + " {\n"
//                    + "\n");
//
//            int sizeline = 1000;
//            int len = inputListSt.length();
//            int beg = 0;
//            int end = sizeline;
//            int index = 1;
//            int line = 0;
//            while (true) {
//                if (line == 0) {
//                    msgWrite.append(""
//                            + "    public static String " + tagName + index + " = \"\"\n"
//                            + "            + \"\"\n");
//                }
//                line++;
//                String st = inputListSt.substring(beg, end);
//
//                msgWrite.append("+ \"" + st + "\"\n");
//
//                if (end >= len) {
//                    msgWrite.append(""
//                            + "            + \"\";\n");
//
//                    break;
//                }
//                if (line == 20) {
//                    msgWrite.append(""
//                            + "            + \"\";\n");
//                    line = 0;
//                    index++;
//                }
//                beg = end;
//                if (end + sizeline <= len) {
//                    end += sizeline;
//                } else {
//                    end = len;
//                }
//            }
//
//            ////// end
//            msgWrite.append(""
//                    + "}\n"
//                    ///
//                    + ""
//            );
//            String fileN = ServiceAFweb.FileLocalDebugPath + fileName + ".java";
//            FileUtil.FileWriteText(fileN, msgWrite);
//
//            StringBuffer msgWrite_src = new StringBuffer();
//            msgWrite_src.append("" ///
//                    + "package com.afweb.nn;\n"
//                    + "import com.afweb.service.ServiceAFweb;\n"
//                    + "import com.fasterxml.jackson.databind.ObjectMapper;\n"
//                    + "import java.util.ArrayList;\n"
//                    + "import java.util.HashMap;\n"
//                    + "/*  This file is generated by system. Do not modify. */\n"
//                    + "public class " + fileName + "_src {\n"
//                    + "\n");
//            /*
//             */
//            String javaSt = "";
//            javaSt = ""
//                    + "public static HashMap<String, ArrayList> AllStockHistoryStaticCodeInit(HashMap<String, ArrayList> stockInputMap) {\n"
//                    + "StringBuffer inputBuf = new StringBuffer();\n"
//                    + "try {\n"
//                    + "";
//
//            msgWrite_src.append(javaSt + "\n");
//            for (int i = 1; i < index + 1; i++) {
//                javaSt = ""
//                        + "inputBuf.append(" + fileName + "." + tagName + i + ");\n"
//                        + "";
//                msgWrite_src.append(javaSt);
//            }
//            javaSt = ""
//                    + "String inputListSt = ServiceAFweb.decompress(inputBuf.toString());\n"
//                    + "stockInputMap = new ObjectMapper().readValue(inputListSt, HashMap.class);\n"
//                    + "return stockInputMap;\n"
//                    + "} catch (Exception ex) {\n"
//                    + "}\n"
//                    + "return stockInputMap;\n"
//                    + "}\n"
//                    + "";
//            msgWrite_src.append(javaSt + "\n");
//
//            ////// end
//            msgWrite_src.append(""
//                    + "}\n"
//                    ///
//                    + ""
//            );
//            String fileN_src = ServiceAFweb.FileLocalDebugPath + fileName + "_src.java";
//            FileUtil.FileWriteText(fileN_src, msgWrite_src);
//            return true;
//        } catch (Exception ex) {
//        }
//        return false;
//    }
//
//    public static void ProcessAllStockHistoryCreatJava(ServiceAFweb serviceAFWeb, String symbolL[], HashMap<String, ArrayList> stockInputMap) {
//        boolean saveStockDBFlag = true;
//        if (saveStockDBFlag == true) {
//
//            StockInternetImpDao internet = new StockInternetImpDao();
//            ArrayList stockNameArray = new ArrayList();
//
//            if (symbolL == null) {
//                return;
//            }
//
//            for (int i = 0; i < symbolL.length; i++) {
//                stockNameArray.add(symbolL[i]);
//            }
//            logger.info("AllStockHistoryCreatJavaProcess " + stockNameArray.size());
//
//            int sizeyear = 5 * 52 * 5;
//            for (int k = 0; k < stockNameArray.size(); k++) {
//                String symbol = (String) stockNameArray.get(k);
//
//                String StFileName = ServiceAFweb.FileLocalDebugPath + symbol + ".txt";
//
//                ArrayList<String> writeArray = new ArrayList();
//                ArrayList<AFstockInfo> StockArray = null;
//
//                try {
//                    // always the earliest day first  
//                    StockArray = internet.GetStockHistoricalInternet(symbol, sizeyear);
//                } catch (Exception ex) {
//
//                }
//                if (StockArray == null) {
//                    continue;
//                }
//                if (StockArray.size() < 3) {
//                    continue;
//                }
//                // skiping first 3 days (last days is not final
//                for (int j = 5; j < StockArray.size(); j++) {
//                    try {
//                        AFstockInfo obj = StockArray.get(j);
//                        String st = new ObjectMapper().writeValueAsString(obj);
//                        writeArray.add(st);
//                    } catch (Exception ex) {
//                        writeArray = null;
//                        break;
//                    }
//                }
//                if (writeArray == null) {
//                    continue;
//                }
//                FileUtil.FileWriteTextArray(StFileName, writeArray);
//                ///////////////////////
//                FileUtil.FileReadTextArray(StFileName, writeArray);
//                if (writeArray.size() == 0) {
//                    continue;
//                }
//                StockArray = new ArrayList();
//                for (int j = 0; j < writeArray.size(); j++) {
//                    String st = writeArray.get(j);
//                    try {
//                        AFstockInfo stockInfo = new ObjectMapper().readValue(st, AFstockInfo.class);
//                        StockArray.add(stockInfo);
//                    } catch (Exception ex) {
//                    }
//                }
//                //////////
//                if (StockArray == null) {
//                    continue;
//                }
//                logger.info(">>> AllStockHistoryCreatJavaProcess " + symbol + " " + StockArray.size());
//                stockInputMap.put(symbol, StockArray);
//
//            } // loop for stockNameArray
//        }
//    }
//
//    public static ArrayList<AFstockInfo> getAllStockHistory(String symbol) {
//        if (stockInputMap == null) {
//            stockInputMap = nnAllStock_src.AllStockHistoryStaticCodeInit(stockInputMap);
//        }
//        ArrayList<AFstockInfo> stockInfoList = ProcessAllStockHistoryfromStaticCode(symbol, stockInputMap);
//        if (stockInfoList != null) {
//            return stockInfoList;
//        }
//        return getAllStockHistory_1(symbol);
//    }
//
//    private static ArrayList<AFstockInfo> getAllStockHistory_1(String symbol) {
//        if (stockInputMap_1 == null) {
//            stockInputMap_1 = nnAllStock_1_src.AllStockHistoryStaticCodeInit(stockInputMap_1);
//        }
//        return ProcessAllStockHistoryfromStaticCode(symbol, stockInputMap_1);
//
//    }
//
//    private static ArrayList<AFstockInfo> ProcessAllStockHistoryfromStaticCode(String symbol,
//            HashMap<String, ArrayList> stockInMap) {
//
//        ArrayList<AFstockInfo> inputlist = new ArrayList();
//
//        String symbolL[] = ServiceAFweb.ignoreStock;
//        for (int i = 0; i < symbolL.length; i++) {
//            String ignoreSym = symbolL[i];
//            if (ignoreSym.equals(symbol)) {
//                return inputlist;
//            }
//        }
//
//        if (stockInMap == null) {
//            return inputlist;
//        }
//
//        if (symbol != "") {
//            try {
//                inputlist = stockInMap.get(symbol);
//                if (inputlist == null) {
//                    return null;
//                }
//                String inputListRawSt = new ObjectMapper().writeValueAsString(inputlist);
//                AFstockInfo[] arrayItem = new ObjectMapper().readValue(inputListRawSt, AFstockInfo[].class);
//                List<AFstockInfo> listItem = Arrays.<AFstockInfo>asList(arrayItem);
//                inputlist = new ArrayList<AFstockInfo>(listItem);
//                return inputlist;
//            } catch (Exception ex) {
//            }
//        }
//        return inputlist;
//    }
//
///////////////////////////////////////////
}
