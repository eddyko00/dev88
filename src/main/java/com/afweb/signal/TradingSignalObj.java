/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.signal;

/**
 *
 * @author eddy
 */
public class TradingSignalObj {

    private int signal = 0;
    private float signalValue = 0;
    private long dateValue = 0;

    public int getSignal() {
        return signal;
    }

    public void setSignal(int signal) {
        this.signal = signal;
    }

    public float getSignalValue() {
        return signalValue;
    }

    public void setSignalValue(float signalValue) {
        this.signalValue = signalValue;
    }

    public long getDateValue() {
        return dateValue;
    }

    public void setDateValue(long dateValue) {
        this.dateValue = dateValue;
    }
}
