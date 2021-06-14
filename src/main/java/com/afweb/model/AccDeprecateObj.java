/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.model;

/**
 *
 * @author eddyko
 */
public class AccDeprecateObj {

    private float monCost = 0;
    private float rate = 0;
    private String data = "";
    /**
     * @return the rate
     */
    public float getRate() {
        return rate;
    }

    /**
     * @param rate the rate to set
     */
    public void setRate(float rate) {
        this.rate = rate;
    }

    /**
     * @return the data
     */
    public String getData() {
        return data;
    }

    /**
     * @param data the data to set
     */
    public void setData(String data) {
        this.data = data;
    }

    /**
     * @return the monCost
     */
    public float getMonCost() {
        return monCost;
    }

    /**
     * @param monCost the monCost to set
     */
    public void setMonCost(float monCost) {
        this.monCost = monCost;
    }
}
