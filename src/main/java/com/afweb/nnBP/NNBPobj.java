/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.nnBP;

/**
 *
 * @author eddy
 */
public class NNBPobj {

    private double[][] inputpattern;
    private double[][] outputpattern;

    /**
     * @return the inputpattern
     */
    public double[][] getInputpattern() {
        return inputpattern;
    }

    /**
     * @param inputpattern the inputpattern to set
     */
    public void setInputpattern(double[][] inputpattern) {
        this.inputpattern = inputpattern;
    }

    /**
     * @return the outputpattern
     */
    public double[][] getOutputpattern() {
        return outputpattern;
    }

    /**
     * @param outputpattern the outputpattern to set
     */
    public void setOutputpattern(double[][] outputpattern) {
        this.outputpattern = outputpattern;
    }
}
