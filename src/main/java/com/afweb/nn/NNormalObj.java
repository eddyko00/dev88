/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.nn;

import java.util.ArrayList;
import java.util.Date;

/**
 *
 * @author eddy
 */
public class NNormalObj {

    private float high;
    private float low;
    private float midpoint;
    private float normalHigh;
    private float factor;
    private float offset;

    private boolean initflag = false;

    public float getNormalizeValue(float value) {
        if (initflag == false) {
            return -1;
        }
        float old_value = value;
        float old_min = low;
        float old_max = high;
        float new_max = 100;
        float new_min = 0;
        //https://stackoverflow.com/questions/929103/convert-a-number-range-to-another-range-maintaining-ratio
        float new_value = ((old_value - old_min) / (old_max - old_min)) * (new_max - new_min) + new_min;

        float normalizeValue = new_value;

        if (normalizeValue > 100) {
            normalizeValue = 100;
        }
        if (normalizeValue < 0.1) {
            normalizeValue = 0;
        }
        return normalizeValue;
    }

    public int initHighLow(ArrayList<Float> dataList) {
        high = -9999999;
        low = 9999999;

        for (int i = 0; i < dataList.size(); i++) {
            float close = dataList.get(i);
            if (close >= high) {
                high = close;
            }
            if (close <= low) {
                low = close;
            }
        }

        initflag = true;
        return 1;
    }
    
    
    public int initHighLowOld(ArrayList<Float> dataList) {
        high = -9999999;
        low = 9999999;

        for (int i = 0; i < dataList.size(); i++) {
            float close = dataList.get(i);
            if (close >= high) {
                high = close;
            }
            if (close <= low) {
                low = close;
            }
        }

        offset = high;
        if (high < 0) {
            offset = -offset;
        }
        if (low < 0) {
            offset += -low;
        }

        high = high + offset;
        low = low + offset;

        high = (float) (high + high * 0.1);
        low = (float) (low - low * 0.1);

        midpoint = low + ((high - low) / 2);
        normalHigh = high - midpoint;
        factor = 100 / normalHigh;

        initflag = true;
        return 1;
    }

    // map the range between 0 to 100
    public float getNormalizeValueOld(float value) {
        if (initflag == false) {
            return -1;
        }

        float normalizeValue = 0;
        value = value + offset;

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

    /// end
}
