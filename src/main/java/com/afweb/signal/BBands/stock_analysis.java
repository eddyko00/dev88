/**
 * Author: Ali Ahad Mukhida
 */
package com.afweb.signal.BBands;

import com.afweb.model.stock.*;
import com.afweb.service.ServiceAFweb;
import com.afweb.signal.*;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

public class stock_analysis {
    
    protected static Logger logger = Logger.getLogger("stock_analysis");
    
    public static void BBandstest(ServiceAFweb serviceAFWeb) {
        String symbol = "AAPL";
        AFstockObj stock = serviceAFWeb.getStockImp().getRealTimeStock(symbol, null);
        int size1yearAll = 20 * 12 * 1 + (50 * 3);
        ArrayList<AFstockInfo> StockArray = serviceAFWeb.getStockHistorical(symbol, size1yearAll);
        ArrayList<BBObj> BBArray = new ArrayList();
        for (int i = 0; i < StockArray.size(); i++) {
            BBObj bbObj = BBands(StockArray, i, 20, 2);
            BBArray.add(bbObj);
        }
        logger.info("BBArray size=" + BBArray.size());
    }

    //Bollinger-Bands signal
    public static BBObj BBSignal(ArrayList StockRecArray, int DataOffset) {
        BBObj bbObj = new BBObj();
        
        return bbObj;
    }

    //Bollinger-Bands
    //The default values are 20 for period, and 2 for standard deviations,
    public static BBObj BBands(ArrayList StockRecArray, int DataOffset, int MAvg, int SD) {
        BBObj bbObj = new BBObj();
        try {
//            MAvg = 20;  // smooth moving average
//            SD = 2;  // standard deviation 
            
            int dataSize = MAvg + 20;
            if (StockRecArray.size() < DataOffset + dataSize) {
                logger.warning("> BBands incorrect StockRecArray size" + StockRecArray.size());
                return bbObj;
            }
            double[] close = new double[dataSize];
            
            for (int i = 0; i < dataSize; i++) {
                AFstockInfo stocktmp = (AFstockInfo) StockRecArray.get(i + DataOffset);
                close[dataSize - 1 - i] = stocktmp.getFclose();
            }

            // Object to calculate simple moving average
            SMA sma = new SMA();
            double[] average = sma.simple_moving_average(MAvg, close);

            // Object to calculate bollinger bands
            BBands bands = new BBands();
            double[] lowerBand = bands.lower_band(SD, average, close, MAvg);
            double[] upperBand = bands.upper_band(SD, average, close, MAvg);
            
            bbObj.lowerBand = lowerBand[lowerBand.length - 1];
            bbObj.upperBand = upperBand[upperBand.length - 1];
            return bbObj;
        } catch (Exception ex) {
            logger.info("> BBands exception " + ex.getMessage());
        }
        return bbObj;
    }
    
    public static void mainBBands(String[] args) {
        Scanner scan = new Scanner(System.in);
        
        System.out.print("The prices of a stock in CSV format: ");
//        String file = scan.next();
        String file = "T:/temp/google";
        
        System.out.print("The value of M: ");
//        int M = scan.nextInt();
        int M = 5;
        
        System.out.print("The value of D: ");
//        int D = scan.nextInt();
        int D = 2;
        try {
            // Object to read the stock data
            StockData read = new StockData();
            double[] closing_rate = read.Openfile(file);

            // Object to calculate simple moving average
            SMA sma = new SMA();
            double[] average = sma.simple_moving_average(M, closing_rate);

            // Object to calculate bollinger bands
            BBands bands = new BBands();
            double[] lowerBand = bands.lower_band(D, average, closing_rate, M);
            double[] upperBand = bands.upper_band(D, average, closing_rate, M);

            // Printing the data to a new CSV file
            PrintStream fw = new PrintStream(file + "_ta.csv");
            try {
                String header = M + "-day SMA,LowerBand(-" + D + "S.D.),UpperBand(+" + D + "S.D.)\n";
                fw.print(header);
                
                for (int i = 0; i < average.length; i++) {
                    fw.printf("%.5f,%.5f,%.5f\n", average[i], lowerBand[i], upperBand[i]);
                }
                fw.close();
            } catch (Exception e) {
                System.out.println("File writing errors");
                System.out.println(e);
            }
            
        } catch (FileNotFoundException e) {
            System.out.println(e);
        }
        
    }
    
}
