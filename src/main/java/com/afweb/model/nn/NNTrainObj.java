/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.model.nn;

import com.afweb.model.nn.NNInputOutObj;
import static com.afweb.service.ServiceAFweb.FileLocalDebugPath;
import com.afweb.util.FileUtil;
import com.afweb.util.getEnv;
import java.util.ArrayList;

/**
 *
 * @author koed
 */
public class NNTrainObj {

    /**
     * @return the targetpattern
     */
    public double[][] getTargetpattern() {
        return targetpattern;
    }

    /**
     * @param targetpattern the targetpattern to set
     */
    public void setTargetpattern(double[][] targetpattern) {
        this.targetpattern = targetpattern;
    }

    private String nameNN;
    private String symbol;
    private String trname;

    private ArrayList<NNInputOutObj> nnInputList;

    private double[][] inputpattern;
    private double[][] outputpattern;
    private double[][] response;
    private double[][] targetpattern;

    public void printInputFile() {
        if (getEnv.checkLocalPC() == true) {
            ArrayList writeArray = new ArrayList();
            double[] input;
            double[] output;
            double[] target;
            int testSampleSize = inputpattern.length;
            for (int j = 0; j < testSampleSize; j++) {
                input = inputpattern[j];
                output = outputpattern[j];
                target = getTargetpattern()[j];
                String st = "";
                for (int k = 0; k < input.length; k++) {
                    st += "\"" + input[k] + "\",";
                }
                st += "\"" + output[0] + "\",\"" + output[1] + "\",\"" + target[0] + "\",\"" + target[1] + "\" ";
                writeArray.add(st);
            }
            FileUtil.FileWriteTextArray(FileLocalDebugPath + nameNN + ".csv", writeArray);
        }
    }

    /**
     * @return the symbol
     */
    public String getSymbol() {
        return symbol;
    }

    /**
     * @param symbol the symbol to set
     */
    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    /**
     * @return the trname
     */
    public String getTrname() {
        return trname;
    }

    /**
     * @param trname the trname to set
     */
    public void setTrname(String trname) {
        this.trname = trname;
    }

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
     * @return the nnInputList
     */
    public ArrayList<NNInputOutObj> getNnInputList() {
        return nnInputList;
    }

    /**
     * @param nnInputList the nnInputList to set
     */
    public void setNnInputList(ArrayList<NNInputOutObj> nnInputList) {
        this.nnInputList = nnInputList;
    }

    /**
     * @return the nameNN
     */
    public String getNameNN() {
        return nameNN;
    }

    /**
     * @param nameNN the nameNN to set
     */
    public void setNameNN(String nameNN) {
        this.nameNN = nameNN;
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

    /**
     * @return the response
     */
    public double[][] getResponse() {
        return response;
    }

    /**
     * @param response the response to set
     */
    public void setResponse(double[][] response) {
        this.response = response;
    }

}
