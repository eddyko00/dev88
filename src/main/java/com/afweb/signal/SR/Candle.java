/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.signal.SR;

import java.util.Date;

/**
 *
 * @author koed
 */
class Candle {

    /**
     * @return the high
     */
    public Double getHigh() {
        return high;
    }

    /**
     * @param high the high to set
     */
    public void setHigh(Double high) {
        this.high = high;
    }

    private Double high;
    private Double low;
    private Double open;
    private Double close;
    private Double ltp;  //Last Traded Price stands for the price of a stock on which the last transaction

    private Date timestamp;

    /**
     * @return the low
     */
    public Double getLow() {
        return low;
    }

    /**
     * @param low the low to set
     */
    public void setLow(Double low) {
        this.low = low;
    }

    /**
     * @return the open
     */
    public Double getOpen() {
        return open;
    }

    /**
     * @param open the open to set
     */
    public void setOpen(Double open) {
        this.open = open;
    }

    /**
     * @return the close
     */
    public Double getClose() {
        return close;
    }

    /**
     * @param close the close to set
     */
    public void setClose(Double close) {
        this.close = close;
    }

    /**
     * @return the ltp
     */
    public Double getLtp() {
        return ltp;
    }

    /**
     * @param ltp the ltp to set
     */
    public void setLtp(Double ltp) {
        this.ltp = ltp;
    }

    /**
     * @return the timestamp
     */
    public Date getTimestamp() {
        return timestamp;
    }

    /**
     * @param timestamp the timestamp to set
     */
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

}
