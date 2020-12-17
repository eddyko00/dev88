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
            BBObj bbObj = TechnicalCal.BBSignal(StockArray, i);
            BBArray.add(bbObj);
        }
        logger.info("BBArray size=" + BBArray.size());
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
