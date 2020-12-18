/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.signal;

import com.afweb.model.ConstantKey;
import com.afweb.model.stock.AFstockInfo;
import com.afweb.signal.BBands.*;
import com.afweb.stock.StockImp;
import com.jasonlam604.stocktechnicals.indicators.*;

import java.util.ArrayList;
import java.util.logging.Level;

import java.util.logging.Logger;

/**
 *
 * @author eddy
 */
public class TechnicalCal {
//https://stockcharts.com/school/doku.php?id=chart_school:technical_indicators:commodity_channel_index_cci
//https://stockcharts.com/school/doku.php?id=chart_school:technical_indicators:relative_strength_index_rsi
//https://stockcharts.com/school/doku.php?id=chart_school:technical_indicators:moving_averages

    protected static Logger logger = Logger.getLogger("TechnicalCal");

    public static MACDObj MACD(ArrayList StockRecArray, int DataOffset, int fastPeriod, int slowPeriod, int signalPeriod) {
        try {
            MovingAverageConvergenceDivergence macd = new MovingAverageConvergenceDivergence();
            int dataSize = slowPeriod * 2;
            if (StockRecArray.size() < DataOffset + dataSize) {
                logger.warning("> MACD incorrect StockRecArray size" + StockRecArray.size());
                return null;
            }
            double[] close = new double[dataSize];

            for (int i = 0; i < dataSize; i++) {
                AFstockInfo stocktmp = (AFstockInfo) StockRecArray.get(i + DataOffset);
                close[dataSize - 1 - i] = stocktmp.getFclose();
            }
            MovingAverageConvergenceDivergence result = macd.calculate(close, fastPeriod, slowPeriod, signalPeriod);
            MACDObj macdObj = new MACDObj();
            macdObj.macd = result.getMACD()[dataSize - 1];
            macdObj.signal = result.getSignal()[dataSize - 1];
            macdObj.diff = result.getDiff()[dataSize - 1];
            macdObj.crossover = result.getCrossover()[dataSize - 1];

            macdObj.trsignal = ConstantKey.S_BUY;
            if (macdObj.diff < 0) {
                macdObj.trsignal = ConstantKey.S_SELL;
            }
            return macdObj;
        } catch (Exception ex) {
            Logger.getLogger(TechnicalCal.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public static RSIObj RSI(ArrayList StockRecArray, int DataOffset, int period) {
        RSIObj rsiObj = new RSIObj();
        rsiObj.lastRsi = 50;

        int j = 0;

        for (int i = 0; i < StockRecArray.size(); i++) {
            double rsiValue = RSIdata(StockRecArray, DataOffset + j, period);
            if (rsiValue == -1) {
                break;
            }
            if (j == 0) {
                rsiObj.rsi = rsiValue;
            }
            if ((rsiValue > 70) || (rsiValue < 30)) {
                rsiObj.lastRsi = rsiValue;
                rsiObj.lastOffset = j - DataOffset;
                break;
            }
            j++;
        }

        rsiObj.trsignal = ConstantKey.S_NEUTRAL;

        if (rsiObj.lastRsi < 30) {
            rsiObj.trsignal = ConstantKey.S_BUY;
        }
        if (rsiObj.lastRsi > 70) {
            rsiObj.trsignal = ConstantKey.S_SELL;
        }
        return rsiObj;
    }

    private static double RSIdata(ArrayList StockRecArray, int DataOffset, int period) {
        try {
            RelativeStrengthIndex rsi = new RelativeStrengthIndex();
            int dataSize = period * 2;
            if (StockRecArray.size() < DataOffset + dataSize) {
                logger.warning("> RSI incorrect StockRecArray size" + StockRecArray.size());
                return -1;
            }
            double[] close = new double[dataSize];

            for (int i = 0; i < dataSize; i++) {
                AFstockInfo stocktmp = (AFstockInfo) StockRecArray.get(i + DataOffset);
                close[dataSize - 1 - i] = stocktmp.getFclose();
            }
            RelativeStrengthIndex result = rsi.calculate(close, period);
            double[] retRSI = result.getRSI();
            return retRSI[dataSize - 1];
        } catch (Exception ex) {
            Logger.getLogger(TechnicalCal.class.getName()).log(Level.SEVERE, null, ex);
        }
        return -1;
    }

    public static EMAObj EMASignal(ArrayList StockRecArray, int DataOffset, int fast, int slow) {
        EMAObj emaObj = new EMAObj();

        int j = 0;

        for (int i = 0; i < StockRecArray.size(); i++) {
            double fastValue = EMA(StockRecArray, DataOffset + j, fast);
            double slowValue = EMA(StockRecArray, DataOffset + j, slow);

            if ((fastValue == -1) || (slowValue == -1)) {
                break;
            }
            double signal = fastValue - slowValue;
            if (j == 0) {
                emaObj.ema = signal;
                emaObj.trsignal = ConstantKey.S_BUY;
                if (emaObj.ema < 0) {
                    emaObj.trsignal = ConstantKey.S_SELL;
                }
            }

            if (emaObj.ema > 0) {
                if (signal < 0) {
                    emaObj.lastema = signal;
                    emaObj.lastOffset = j - DataOffset;
                    emaObj.trsignal = ConstantKey.S_BUY;
                    if (emaObj.ema < 0) {
                        emaObj.trsignal = ConstantKey.S_SELL;
                    }
                    break;
                }
            }
            if (emaObj.ema < 0) {
                if (signal > 0) {
                    emaObj.lastema = signal;
                    emaObj.lastOffset = j - DataOffset;
                    emaObj.trsignal = ConstantKey.S_BUY;
                    if (emaObj.ema < 0) {
                        emaObj.trsignal = ConstantKey.S_SELL;
                    }
                    break;
                }
            }
            j++;
            if (j > 10) {
                emaObj.lastema = 0;
                emaObj.lastOffset = j - DataOffset;
                emaObj.trsignal = ConstantKey.S_NEUTRAL;
                if (emaObj.ema < 0) {
                    emaObj.trsignal = ConstantKey.S_SELL;
                }
                break;
            }
        }

        emaObj.trsignal = ConstantKey.S_BUY;
        if (emaObj.ema < 0) {
            emaObj.trsignal = ConstantKey.S_SELL;
        }

        return emaObj;
    }

    public static double EMA(ArrayList StockRecArray, int DataOffset, int period) {
        try {
            ExponentialMovingAverage movingAverage = new ExponentialMovingAverage();
            int dataSize = period + 20;
            if (StockRecArray.size() < DataOffset + dataSize) {
                logger.warning("> EMV incorrect StockRecArray size" + StockRecArray.size());
                return -1;
            }
            double[] close = new double[dataSize];

            for (int i = 0; i < dataSize; i++) {
                AFstockInfo stocktmp = (AFstockInfo) StockRecArray.get(i + DataOffset);
                close[dataSize - 1 - i] = stocktmp.getFclose();
            }
            ExponentialMovingAverage result = movingAverage.calculate(close, period);
            double[] retEMA = result.getEMA();
            return retEMA[dataSize - 1];
        } catch (Exception ex) {
            Logger.getLogger(TechnicalCal.class.getName()).log(Level.SEVERE, null, ex);
        }
        return -1;
    }

    // period = 14
    public static ADXObj AvgDir(ArrayList StockRecArray, int DataOffset, int period) {
        AverageDirectionalIndex ADITechnical = new AverageDirectionalIndex();
        ADXObj adxObj = new ADXObj();
        int dataSize = period * 2;
        if (StockRecArray.size() < DataOffset + dataSize) {
            logger.warning("> AvgDir incorrect StockRecArray size" + StockRecArray.size());
            return adxObj;
        }
        double[] high = new double[dataSize];
        double[] low = new double[dataSize];
        double[] close = new double[dataSize];

        for (int i = 0; i < dataSize; i++) {
            AFstockInfo stocktmp = (AFstockInfo) StockRecArray.get(i + DataOffset);
            high[dataSize - 1 - i] = stocktmp.getHigh();
            low[dataSize - 1 - i] = stocktmp.getLow();
            close[dataSize - 1 - i] = stocktmp.getFclose();
        }

        try {
            AverageDirectionalIndex result = ADITechnical.calculate(high, low, close, period);

//            String st = "ADX " + result.getADX()[dataSize - 1] + ",ND " + result.getNegativeDirectionalIndicator()[dataSize - 1] + ",PD " + result.getPositiveDirectionalIndicator()[dataSize - 1];
//            logger.info(st);
            adxObj.adx = result.getADX()[dataSize - 1];
            double PD = result.getPositiveDirectionalIndicator()[dataSize - 1];
            double ND = result.getNegativeDirectionalIndicator()[dataSize - 1];
            if (PD < ND) {
                adxObj.adx = adxObj.adx * -1;
            }
            if (adxObj.adx > 20) {
                adxObj.trsignal = ConstantKey.S_BUY;
            }
            if (adxObj.adx < -20) {
                adxObj.trsignal = ConstantKey.S_SELL;
            }
            return adxObj;
        } catch (Exception ex) {
            Logger.getLogger(TechnicalCal.class.getName()).log(Level.SEVERE, null, ex);
        }
        return adxObj;
    }
    // period = 25 for short term
    // period = 100 for long term

    public static double TrendUpDown(ArrayList StockRecArray, int DataOffset, int period) {

        double Trend1, Trend2;
        try {
            if (period < StockImp.LONG_TERM_TREND) {
                Trend1 = TrendUpDownProcess(StockRecArray, DataOffset, period);
                return Trend1;
            }
            Trend1 = TrendUpDownProcess(StockRecArray, DataOffset, period / 2);
            Trend2 = TrendUpDownProcess(StockRecArray, DataOffset + (period / 2), period / 2);
            Trend1 = (Trend1 + Trend2) / 2;
            return Trend1;
        } catch (Exception ex) {
            Logger.getLogger(TechnicalCal.class.getName()).log(Level.SEVERE, null, ex);
        }
        return 0;
    }

    public static double TrendUpDownProcess(ArrayList StockRecArray, int DataOffset, int period) {

        int dataSize = period * 2;
        if (StockRecArray.size() < DataOffset + dataSize + 1) {
            logger.warning("> TrendUpDownProcess incorrect StockRecArray size" + StockRecArray.size());
            return 0;
        }

//        double[] high = new double[dataSize + 1];
//        for (int i = 0; i < dataSize + 1; i++) {
//            AFstockInfo stocktmp = (AFstockInfo) StockRecArray.get(i + DataOffset);
//            high[dataSize - 1 - i + 1] = stocktmp.getHigh();
//        }
        double TrendUp;
        double TrendDown;
        TrendUp = AroonUp(StockRecArray, DataOffset, period);
        TrendDown = AroonDown(StockRecArray, DataOffset, period);

        return TrendUp - TrendDown;
    }

    //Aroon Up
    //Mult( Sub (1, Div(BarsSinceHigh (High, Period), Sub (period,1))), 100)
    //The Aroon Up indicator is calculated by taking the number of days since the highest price for the given period occured and converting it to a strength from 100 to 0, with higher values indicating a more recent new high.
    public static double AroonUp(ArrayList StockRecArray, int DataOffset, int period) {
        double ArUp = 0;
        ArUp = (double) 100 * ((double) 1 - (BarsSinceHighest(StockRecArray, DataOffset, period) / (period - 1)));
        return ArUp;
    }
    //Aroon Down

    public static double AroonDown(ArrayList StockRecArray, int DataOffset, int period) {
        double ArDown = 0;
        ArDown = (double) 100 * ((double) 1 - (BarsSinceLowest(StockRecArray, DataOffset, period) / (period - 1)));
        return ArDown;
    }

    //Bars Since Highest
    public static double BarsSinceHighest(ArrayList StockRecArray, int DataOffset, int period) {
        double Highest = 0;
        int HighBars = 0;
        if (StockRecArray.size() <= period + DataOffset) {
            return (double) 0;
        }
        int j = 0;
        for (int i = DataOffset; i < period + DataOffset; i++) {
            AFstockInfo stockinfo = (AFstockInfo) StockRecArray.get(i);
            if ((stockinfo.getHigh() > Highest) || j == 0) {
                Highest = stockinfo.getHigh();
                HighBars = j;
            }
            j++;
        }
        return HighBars;
    }

    //Bars Since Lowest
    public static double BarsSinceLowest(ArrayList StockRecArray, int DataOffset, int period) {
        double Lowest = 0;
        int LowBars = 0;
        if (StockRecArray.size() <= period + DataOffset) {
            return (double) 0;
        }
        int j = 0;
        for (int i = DataOffset; i < period + DataOffset; i++) {
            AFstockInfo stockinfo = (AFstockInfo) StockRecArray.get(i);
            if ((stockinfo.getLow() < Lowest) || j == 0) {
                Lowest = stockinfo.getLow();
                LowBars = j;
            }
            j++;
        }
        return LowBars;
    }

    //Bars Since Lowest value
    public static double BarsSinceLowestValue(ArrayList StockRecArray, int DataOffset, int period) {
        double Lowest = 0;
//        int LowBars = 0;
        if (StockRecArray.size() <= period + DataOffset) {
            return (double) 0;
        }
        int j = 0;
        for (int i = DataOffset; i < period + DataOffset; i++) {
            AFstockInfo stockinfo = (AFstockInfo) StockRecArray.get(i);
            float average = stockinfo.getLow();

            if ((average < Lowest) || j == 0) {
                Lowest = average;
//                LowBars = j;
            }
            j++;
        }
        return Lowest;
    }

    public static double BarsSinceHighestValue(ArrayList StockRecArray, int DataOffset, int period) {
        double Highest = 0, HighBars = 0;
        if (StockRecArray.size() < period + DataOffset) {
            return (double) 0;
        }

        int j = 0;
        for (int i = DataOffset; i < period + DataOffset; i++) {
            AFstockInfo stockinfo = (AFstockInfo) StockRecArray.get(i);
            float average = stockinfo.getHigh();
            if ((average > Highest) || j == 0) {
                Highest = average;
                HighBars = j;
            }
            j++;
        }

        return Highest;
    }

    ///////////////////////////////////////////////////
    // map the range between 0 to 100
    public static double getNormalize100(double value, double high, double low) {
        double normalizeValue = 0;

        double midpoint = low + ((high - low) / 2);
        double normalHigh = high - midpoint;
        double factor = 100 / normalHigh;

        normalizeValue = (value - midpoint) * factor;
        normalizeValue = (normalizeValue + 100) / 2;

        if (normalizeValue > 100) {
            normalizeValue = 100;
        }

        if (normalizeValue < 0.1) {
            normalizeValue = 0;
        }
        return normalizeValue;
    }

    ///////////
    //Bollinger-Bands signal
    //In Buy trady, the price hit the Bollinger Band, the RSI (when the price touches the bottom band) 
    //needs to be in between 50 and 30.
    //In a sell trade the RSI would need to be in between the 50-70 mark and going downward
    public static BBObj BBSignal(ArrayList StockRecArray, int DataOffset, int MAvg, int SD, int RSI) {
        BBObj bbObj = new BBObj();

        int j = 0;

        for (int i = 0; i < StockRecArray.size(); i++) {
            bbObj = BBandData(StockRecArray, DataOffset + j, MAvg, SD);
            AFstockInfo stockinfo = (AFstockInfo) StockRecArray.get(DataOffset);
            if (bbObj.lowerBand == 0) {
                return bbObj;
            }
            if (bbObj.upperBand == 0) {
                return bbObj;
            }
            float closeP = stockinfo.getFclose();
            double perLower = Math.abs(100 * (closeP - bbObj.lowerBand) / bbObj.lowerBand);
            double perUpper = Math.abs(100 * (bbObj.upperBand - closeP) / closeP);
            double rsiValue = RSIdata(StockRecArray, DataOffset, RSI);
            bbObj.rsiValue = rsiValue;

            if (rsiValue == -1) {
                return bbObj;
            }

            if (j == 0) {
                bbObj.perlowerBandValue = perLower;
                bbObj.perupperBandValue = perUpper;
            }
            bbObj.lastperlowerBandValue = perLower;
            bbObj.lastperupperBandValue = perUpper;

            bbObj.trsignal = ConstantKey.S_NEUTRAL;
            if (perLower < 10) {
                if (rsiValue < 30) {
                    bbObj.trsignal = ConstantKey.S_BUY;

                    return bbObj;
                }
            } else if (perUpper < 10) {
                if (rsiValue > 70) {
                    bbObj.trsignal = ConstantKey.S_SELL;
                    return bbObj;
                }
            }

            j++;
        }
        bbObj = new BBObj();
        return bbObj;

    }

    //Bollinger-Bands
    //The default values are 20 for period, and 2 for standard deviations,
    public static BBObj BBandData(ArrayList StockArray, int DataOffset, int MAvg, int SD) {
        BBObj bbObj = new BBObj();
        try {
//            MAvg = 20;  // smooth moving average
//            SD = 2;  // standard deviation 

            int dataSize = MAvg + 20;
            if (StockArray.size() < DataOffset + dataSize) {
                logger.warning("> BBands incorrect StockRecArray size" + StockArray.size());
                return bbObj;
            }
            double[] close = new double[dataSize];

            for (int i = 0; i < dataSize; i++) {
                AFstockInfo stocktmp = (AFstockInfo) StockArray.get(i + DataOffset);
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

    //////////////
}
