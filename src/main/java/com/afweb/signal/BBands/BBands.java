/**
 * Author: Ali Ahad Mukhida
 */

package com.afweb.signal.BBands;

import java.util.ArrayList;

//# Simple-Moving-Average-and-Bollinger-Bands
//
//This project is used to calculate M-day Simple Moving Average and Bollinger Bands of Stock Data 
//given stock prices over a period of time. The particular stock price we have used to caclulate 
//the Simple Moving average is the "Closing Stock Price"
//Bollinger Bands use 2 parameters, Period and Standard Deviations, StdDev. 
//The default values are 20 for period, and 2 for standard deviations, 
//Short term: 10 day moving average, bands at 1.5 standard deviations. 
//(1.5 times the standard dev. +/- the SMA)
//Medium term: 20 day moving average, bands at 2 standard deviations.

public class BBands 
{
    // Method to calculate the standard deviation of M-day stock price
    private double[] standard_deviation(int m, double[] close_rate, double[] sma)
    {
        double difference = 0;
        double squared_sum;
        
        ArrayList<Double> closing_rate = new ArrayList<Double>();
        for (int i = 0; i < close_rate.length; i++)
            closing_rate.add(close_rate[i]);
        
        ArrayList<Double> average = new ArrayList<Double>();
        for (int i = 0; i < sma.length; i++)
            average.add(sma[i]);
        
        ArrayList<Double> dev = new ArrayList<Double>();
        
        // The loop that calculates the standard deviation
        // Removes the first element from arraylists to keep a moving flow
        while (closing_rate.size() >= m)
        {
            squared_sum = 0;
            for (int i = 0; i < m; i++)
            {
                difference = closing_rate.get(i) - average.get(0);
                squared_sum += (difference * difference);
            }
            
           dev.add(Math.pow(squared_sum / m, 0.5));
           average.remove(0);
           closing_rate.remove(0);
        }
        
        // Converts the arraylist to array and returns that array
        double sd[];
        sd = new double[dev.size()];
        
        for (int i = 0; i < dev.size(); i++)
            sd[i] = dev.get(i);
    
        return sd;
    }
    
    // Method that calcualtes the lower band for each SMA
    public double[] lower_band(int d, double[] sma, double[] close_rate, int m)
    {   
        ArrayList<Double> lb = new ArrayList<Double>();
        
        double[] sd = standard_deviation(m, close_rate, sma);
        
        for (int i = 0; i < sma.length; i++)
            lb.add(sma[i] - (sd[i] * d));
        
        // Converts the arraylist to array and returns it
        double lband[];
        lband = new double[lb.size()];
        
        for (int i = 0; i < lb.size(); i++)
            lband[i] = lb.get(i);
        
        return lband;
    }
    
    // Method that calcualtes the upper band for each SMA
    public double[] upper_band(int d, double[] sma, double[] close_rate, int m)
    {   
        ArrayList<Double> ub = new ArrayList<Double>();
        
        double[] sd = standard_deviation(m, close_rate, sma);
        
        for (int i = 0; i < sma.length; i++)
            ub.add(sma[i] + (sd[i] * d));
        
        // Converts the arraylist to array and returns it
        double uband[];
        uband = new double[ub.size()];
        
        for (int i = 0; i < ub.size(); i++)
            uband[i] = ub.get(i);
        
        return uband;
    }    
}
