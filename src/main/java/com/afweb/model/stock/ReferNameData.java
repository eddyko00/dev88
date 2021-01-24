/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.model.stock;

/**
 *
 * @author eddyko
 */
public class ReferNameData {
    private int numReLearn = -1;
    private double minError = 0;
  

    /**
     * @return the numReLearn
     */
    public int getNumReLearn() {
        return numReLearn;
    }

    /**
     * @param numReLearn the numReLearn to set
     */
    public void setNumReLearn(int numReLearn) {
        this.numReLearn = numReLearn;
    }

    /**
     * @return the minError
     */
    public double getMinError() {
        return minError;
    }

    /**
     * @param minError the minError to set
     */
    public void setMinError(double minError) {
        this.minError = minError;
    }

}
