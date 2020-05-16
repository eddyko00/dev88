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
public class FundM {

    private ArrayList accL = new ArrayList();
    private ArrayList funL = new ArrayList();
    private float bal=0;
    private float inv=0;

    /**
     * @return the accL
     */
    public ArrayList getAccL() {
        return accL;
    }

    /**
     * @param accL the accL to set
     */
    public void setAccL(ArrayList accL) {
        this.accL = accL;
    }

    /**
     * @return the funL
     */
    public ArrayList getFunL() {
        return funL;
    }

    /**
     * @param funL the funL to set
     */
    public void setFunL(ArrayList funL) {
        this.funL = funL;
    }

    /**
     * @return the bal
     */
    public float getBal() {
        return bal;
    }

    /**
     * @param bal the bal to set
     */
    public void setBal(float bal) {
        this.bal = bal;
    }

    /**
     * @return the inv
     */
    public float getInv() {
        return inv;
    }

    /**
     * @param inv the inv to set
     */
    public void setInv(float inv) {
        this.inv = inv;
    }
}
