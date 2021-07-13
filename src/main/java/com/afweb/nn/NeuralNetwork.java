/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.nn;


import java.util.logging.Logger;

/**
 *
 * @author eddy
 */
public class NeuralNetwork {

    protected static Logger logger = Logger.getLogger("NeuralNetwork");
    private BPNetwork BPnet = null;
    private double[][] input;
    private double[] target;

    public static double getRandom() {
        java.util.Random rnd = new java.util.Random();
        return rnd.nextDouble();
    }

    public void setAlphaEta(double Alpha, double Eta) {
        if (BPnet != null) {
            BPnet.learningrate = Alpha;
            BPnet.momentum = Eta;
        }
    }

//    public void create(int inputSize, int middleSize, int outputSize) {
//        BPnet = new BPNetwork();
//
//        NeuralNetObj weightObj = new NeuralNetObj();
//        weightObj.setActivationtype(BPNetwork.BIPOLAR_SIGMOID);
//        weightObj.setLearningrate(0.5);
//        weightObj.setMomentum(0.8);
//        weightObj.setNumberofhiddenneuron(middleSize);
//        weightObj.setPatterndimension(inputSize);
//        weightObj.setTargetdimension(outputSize);
//        weightObj.setWeights(null);
//        BPnet.BPCreate(weightObj);
//
//    }

//    public boolean predict(double[] inputptr, double[] rspptr) {
//
//        if (testNeuralNet() == false) {
//            return false;
//        }
//        try {
//            return BPnet.OutPut(inputptr, rspptr);
//        } catch (Exception ex) {
//            logger.info("> predict  " + ex);
//        }
//        return false;
//    }

    ///
    public static long delaySecond = 0;
//
//    public double learn(double[][] inputptr, double[] outputptr, double[] rspptr, int numberIteration, double errorIteration) {
//        double totalError = 0;
//        double[] input;
//        double output;
//        double rsp;
//
//        if (testNeuralNet() == false) {
//            return totalError;
//        }
//        int i = 0;
//        try {
//            int testSampleSize = inputptr.length;
//            int iteration = numberIteration * testSampleSize;
//            for (i = 0; i < iteration; i++) {
//                double netError = 0;
//
//                for (int j = 0; j < testSampleSize; j++) {
//                    input = inputptr[j];
//                    output = outputptr[j];
//                    rsp = rspptr[j];
//
//                    if (BPnet.Learn(input, output, rsp) == false) {
//                        return totalError;
//                    }
//                    netError += BPnet.netError;
//                }
//                totalError = netError / testSampleSize;
//
//                if (i % 400 == 0) {
//                    System.out.println(i + " Error =" + totalError);
//                }
//                if (delaySecond != 0) {
//                    if (i % 50 == 0) {
//                        ServiceAFweb.AFSleep();
//                    }
//                }
//                if (i % 500 == 0) {
//                    // 0.001 limit
//                    if (totalError < errorIteration) {
//                        break;
//                    }
//                }
//            }
//        } catch (Exception ex) {
//            logger.info("> learn  " + ex);
//        }
//        if (numberIteration > 1000) {
//            logger.info("> learn  " + i + " " + totalError);
//        }
//        return totalError;
//    }

//    public boolean writeNeuralNet(String FileName) {
//        if (testNeuralNet() == false) {
//            return false;
//        }
//        String weightSt = getNeuralNetObjSt();
//
//        if (weightSt == null) {
//            return false;
//        }
//        if (FileName != null) {
//            if (FileName.length() > 0) {
//                StringBuffer sb = new StringBuffer(weightSt);
//                FileUtil.FileWriteText(FileName, sb);
//            }
//        }
//        return true;
//    }
//
//    public boolean readNeuralNet(String FileName) {
//        StringBuffer sb = new StringBuffer();
//        if (FileUtil.FileTest(FileName) == true) {
//            sb = FileUtil.FileReadText(FileName);
//
//        }
//        String weightSt = sb.toString();
//        return createNeuralNetbyWeight(weightSt);
//
//    }

//    public boolean createNeuralNetbyWeight(String weightSt) {
//        NeuralNetObj weightObj = null;
//
//        try {
//            weightSt = weightSt.replaceAll("#", "\"");
//            weightObj = new ObjectMapper().readValue(weightSt, NeuralNetObj.class);
//        } catch (IOException ex) {
//            logger.info("> createNeuralNetbyWeight exception " + ex.getMessage());
//        }
//
//        BPnet = new BPNetwork();
//        BPnet.BPCreate(weightObj);
//
//        this.setInput(weightObj.getInput());
//        this.setTarget(weightObj.getTarget());
//
//        if (BPnet.TestNet() == false) {
//            BPnet = null;
//            return false;
//        }
//        return true;
//    }

//    public String getNeuralNetObjSt() {
//
//        String weightSt = null;
//        try {
//            NeuralNetObj weightObj = getNeuralNetObj();
//            weightSt = new ObjectMapper().writeValueAsString(weightObj);
//            weightSt = weightSt.replaceAll("\"", "#");
//        } catch (JsonProcessingException ex) {
//            return null;
//        }
//        return weightSt;
//    }
//
//    public NeuralNetObj getNeuralNetObj() {
//        NeuralNetObj weightObj = BPnet.getNeuralNetObj();
//        weightObj.setInput(input);
//        weightObj.setTarget(target);
//        return weightObj;
//    }
//
//    public boolean testNeuralNet() {
//        if (BPnet == null) {
//            return false;
//        }
//        return BPnet.TestNet();
//    }

    /**
     * @return the input
     */
    public double[][] getInput() {
        return input;
    }

    /**
     * @param input the input to set
     */
    public void setInput(double[][] input) {
        this.input = input;
    }

    /**
     * @return the target
     */
    public double[] getTarget() {
        return target;
    }

    /**
     * @param target the target to set
     */
    public void setTarget(double[] target) {
        this.target = target;
    }

}
