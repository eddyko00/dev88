/*
 * NeuNet.java
 *
 * Created on May 21, 2007, 6:58 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package com.afweb.nnBP;

import com.afweb.util.CKey;
import com.afweb.util.FileUtil;
import java.util.ArrayList;

/**
 *
 * @author vm1
 */
public class NeuNet {

    public static final String NET_Path = "T:/Netbean/debug/";
    public static final String RELEASE_Path = "T:/Netbean/debug";
    public static int INNET_SIZE = 8;
    public static int MIDDLE_SIZE = 10; // must be the same as the input
    public static int OUTNET_SIZE = 1;
    public static String NNfile = "NN";
    public static int NumTrainingStep = 30000;
    public double CmpError = 0.0025;
    private double totalError = 0;

    public double getTotalError() {
        return totalError;
    }

    /**
     * Creates a new instance of NeuNet
     */
    public NeuNet() {
    }
////////////////////////////////////Training ///////////////////////
    public double[][] input = null;
    public double[][] output = null;
    public double[][] rspptr = null;
    private double[] inputMaxNormal = new double[INNET_SIZE];
    private double[] outputMaxNormal = new double[OUTNET_SIZE];
    private double[] inputMinNormal = new double[INNET_SIZE];
    private double[] outputMinNormal = new double[OUTNET_SIZE];

    public NNBPservice GetNnet(String path) {
        NNBPservice afneural = new NNBPservice();
        afneural.readNet(path);
        if (afneural.isEnable() == false) {
            afneural.create(INNET_SIZE, MIDDLE_SIZE, OUTNET_SIZE);
            afneural.writeNet(path);
        }
        return afneural;
    }

    public NNBPservice Training(String netPath, String filename, int TrainingStep) {

        String netWeightpath = netPath + filename + "_net.txt";

        NNBPservice BPnet = GetNnet(netWeightpath);  // weight file

        String inputOutputpath = netPath + filename + "_inout.csv"; // input file

        boolean ret = readNetInOut(inputOutputpath);
        if (ret == false) {
            return null;
        }
        totalError = BPnet.learn("",input, output, rspptr, TrainingStep, CmpError);


        if (netPath.equals(RELEASE_Path)) {
            return BPnet; // no need to save if calling from release path
        }

        BPnet.writeNet(netWeightpath);

        if (CmpError > totalError) { //0.025;
            // save release path
            String release = RELEASE_Path + filename + "_net.txt";
            BPnet.writeNet(release);
        }
        return BPnet;
    }

    public void setNetInOut(int size) {
        input = new double[size][INNET_SIZE];
        output = new double[size][OUTNET_SIZE];
        rspptr = new double[size][OUTNET_SIZE];
    }

    public boolean readNetInOut(String FileName) {

        if (FileUtil.FileTest(FileName) == false) {
            return false;
        }

        ArrayList retArray = new ArrayList();
        // ignore # - comment line
        FileUtil.FileReadTextArray(FileName, retArray);

        String inOutArray[];
        String inputOutput = "";

        int i = 0;
        String value;
        try {
            if (retArray.size() > 0) {
                int size = retArray.size();
                setNetInOut(size);

                for (i = 0; i < size; i++) {

                    inputOutput = (String) retArray.get(i);

                    inOutArray = inputOutput.split(CKey.COMMA);
                    int index = 0;

                    for (int j = 0; j < OUTNET_SIZE; j++) {
                        value = inOutArray[index++];
                        output[i][j] = Double.parseDouble(value);
                    }
                    for (int j = 0; j < INNET_SIZE; j++) {
                        value = inOutArray[index++];
                        input[i][j] = Double.parseDouble(value);
                    }
                }
                return true;

            }
        } catch (NumberFormatException ex) {
            System.out.println(ex.getMessage());
        }
        return false;
    }

    public String predictTest(String netPath, String filename) {
        double[] respPredict = new double[OUTNET_SIZE];
        String predicReturn = "";
        int numberMatch = 0;
        float percentError = 0;

        String inputOutputpath = netPath + filename + ".csv"; // input file

        boolean ret = readNetInOut(inputOutputpath);
        if (ret == false) {
            return predicReturn;
        }
        ArrayList retArray = new ArrayList();

        predicReturn += "len=" + input.length;
        for (int j = 0; j < input.length; j++) {
            double[] inputPredict = input[j];
            double[] outputPredict = output[j];
            String result = "";

            respPredict = predict(netPath, filename, inputPredict);
            for (int k = 0; k < respPredict.length; k++) {
                result += respPredict[k] + ",";
            }
            for (int k = 0; k < outputPredict.length; k++) {
                result += outputPredict[k] + ",";
                if (k == 0) {
                    if ((respPredict[k] > 0.5) && (outputPredict[k] > 0.5)) {
                        numberMatch++;
                    } else if ((respPredict[k] < 0.5) && (outputPredict[k] < 0.5)) {
                        numberMatch++;
                    }
                }
            }
            for (int k = 0; k < inputPredict.length; k++) {
                result += inputPredict[k] + ",";
            }
            retArray.add(result);
        }
        predicReturn += ", match %=" + numberMatch;
        percentError = (float) (100.0 * ((1.0 * input.length - numberMatch) / input.length));
        predicReturn += ", Error %=" + percentError;

        String FileName = netPath + filename + "_test.csv"; // input file
        FileUtil.FileWriteTextArray(FileName, retArray);

        return predicReturn;
    }

    public double[] predict(String netPath, String filename, double[] inputPredict) {

        double[] respPredict = new double[OUTNET_SIZE];
        String netWeightpath = netPath + filename + "_net.txt";
        if (FileUtil.FileTest(netWeightpath) == false) {
            return null;
        }
        NNBPservice BPnet = GetNnet(netWeightpath);
        if (BPnet.isEnable() == false) {
            return null;
        }

        boolean ret = BPnet.predict(inputPredict, respPredict);
        if (ret == false) {
            return null;
        }
        return respPredict;

    }
}
