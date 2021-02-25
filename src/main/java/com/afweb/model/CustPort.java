/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.model;

import java.util.ArrayList;

/**
 *
 * @author koed
 */
public class CustPort {

    /**
     * @return the featL
     */
    public ArrayList getFeatL() {
        return featL;
    }

    /**
     * @param featL the featL to set
     */
    public void setFeatL(ArrayList featL) {
        this.featL = featL;
    }

    /**
     * @return the serv
     */
    public int getServ() {
        return serv;
    }

    /**
     * @param serv the serv to set
     */
    public void setServ(int serv) {
        this.serv = serv;
    }

    /**
     * @return the nPlan
     */
    public int getnPlan() {
        return nPlan;
    }

    /**
     * @param nPlan the nPlan to set
     */
    public void setnPlan(int nPlan) {
        this.nPlan = nPlan;
    }
    
    private int nPlan = -1;
    private int serv = 0;
    private ArrayList featL = new ArrayList();    
}
