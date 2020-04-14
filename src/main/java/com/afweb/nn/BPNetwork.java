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
public class BPNetwork {

    protected static Logger logger = Logger.getLogger("BPNetwork");
    //variable definition
    private int numberofpattern;
    private int patterndimension;
    private int targetdimension;
    private int numberofhiddenlayer;
    private int activationtype;
    public double learningrate;
    public double momentum;
    private boolean BFnetInit = false;

    /////////////////////
    private double[][] inputpattern;
    private double[] targetpattern;

//    private double[] inputlayer;
    private double[][] hiddenlayer;
    private double[][] backpropagationhiddenlayer;
    private double[][] errorhidden;
    private double[] outputlayer;
    private double[][] activatedhiddenlayer;
    private double[] activatedoutputlayer;
    private double[] outputerror;
    private double[][][] weights;
    private double[][][] deltaweightsbuffer;
    private double[][][] deltaweights;
    private double[] patternerror;

    private int numberofhiddenneuron;
    private int numberofinputneuron;
    private int numberofoutputneuron;

    public static final int BINARY_SIGMOID = 1;
    public static final int BIPOLAR_SIGMOID = 2;
    public static final int HYPERBOLIC_TANGENT = 3;

    public double netError = 0;

    public BPNetwork() {
        numberofpattern = 1;
        patterndimension = 2;
        targetdimension = 1;
        numberofhiddenlayer = 1;
        activationtype = BIPOLAR_SIGMOID;
        learningrate = 0.5;
        momentum = 0.8;
    }

    public BPNetwork(String nnString) {
    }

    public boolean TestNet() {
        if (BFnetInit == false) {
            return false;
        }

        return true;
    }

    private double RandomNumberGenerator() {
        java.util.Random rnd = new java.util.Random();
        return rnd.nextDouble();
    }

    public boolean BPCreate(NeuralNetObj weightObj) {
        if (weightObj == null) {
            return false;
        }
        patterndimension = weightObj.getPatterndimension();
        numberofhiddenneuron = weightObj.getNumberofhiddenneuron();
        targetdimension = weightObj.getTargetdimension();   // only support 1 node for output
        activationtype = weightObj.getActivationtype();
        learningrate = weightObj.getLearningrate();
        momentum = weightObj.getMomentum();

        if (targetdimension != 1) {
            return false;
        }

        numberofhiddenlayer = 1;
        numberofpattern = 1;

        numberofinputneuron = patterndimension;
        numberofoutputneuron = targetdimension;

//        inputlayer = new double[numberofinputneuron];
        hiddenlayer = new double[numberofhiddenlayer][numberofhiddenneuron];
        backpropagationhiddenlayer = new double[numberofhiddenlayer][numberofhiddenneuron];
        errorhidden = new double[numberofhiddenlayer][numberofhiddenneuron];
        outputlayer = new double[numberofoutputneuron];
        outputerror = new double[numberofoutputneuron];
        activatedhiddenlayer = new double[numberofhiddenlayer][numberofhiddenneuron];
        activatedoutputlayer = new double[numberofoutputneuron];
        patternerror = new double[numberofpattern];

        weights = new double[2][][];
        weights[0] = new double[numberofinputneuron + 1][numberofhiddenneuron];
        weights[1] = new double[numberofhiddenneuron + 1][numberofoutputneuron];
        deltaweights = new double[2][][];
        deltaweights[0] = new double[numberofinputneuron + 1][numberofhiddenneuron];
        deltaweights[1] = new double[numberofhiddenneuron + 1][numberofoutputneuron];
        deltaweightsbuffer = new double[2][][];
        deltaweightsbuffer[0] = new double[numberofinputneuron + 1][numberofhiddenneuron];
        deltaweightsbuffer[1] = new double[numberofhiddenneuron + 1][numberofoutputneuron];

        //System.out.println("InputToHiddenWeigth:");
        for (int i = 0; i <= numberofinputneuron; i++) {
            for (int j = 0; j < numberofhiddenneuron; j++) {
                weights[0][i][j] = RandomNumberGenerator();
                //System.out.println(weights[0][i][j]);
            }
        }
        //System.out.println("HiddenToOutputWeigth:");
        for (int i = 0; i <= numberofhiddenneuron; i++) {
            for (int j = 0; j < numberofoutputneuron; j++) {
                weights[1][i][j] = RandomNumberGenerator();
                //System.out.println(weights[1][i][j]);
            }
        }

        if (weightObj.getWeights() != null) {
            weights = weightObj.getWeights();
        }

        BFnetInit = true;
        return true;

    }

    private double ActivationFunction(double value) {
        double result = 0;
        switch (activationtype) {
            case 1://sigmoid
                result = 1 / (1 + Math.exp(-value));
                break;
            case 2://bipolar sigmoid
                result = (2 / (1 + Math.exp(-value))) - 1;
                break;
            case 3:
                result = (Math.exp(value) - Math.exp(-value)) / ((Math.exp(value) + Math.exp(-value)));
            default:
                result = 1 / (1 + Math.exp(-value));
                break;
        }
        return result;
    }

    private double DerivationOfActivationFunction(double value) {
        double result = 0;
        switch (activationtype) {
            case 1:
                result = ActivationFunction(value) * (1 - ActivationFunction(value));
                break;
            case 2:
                result = 0.5 * (1 + ActivationFunction(value)) * (1 - ActivationFunction(value));
                break;
            case 3:
                result = (1 + ActivationFunction(value)) * (1 - ActivationFunction(value));
                break;
            default:
                result = ActivationFunction(value) * (1 - ActivationFunction(value));
                break;
        }
        return result;
    }

    public boolean Learn(double[] input, double outputptr, double rspptr) {
        netError = 0.0;
        inputpattern = new double[numberofpattern][patterndimension];
        for (int i = 0; i < patterndimension; i++) {
            inputpattern[0][i] = input[i];
        }
        targetpattern = new double[numberofpattern];
        targetpattern[0] = outputptr;

//        System.out.println("Learning...");
        // numberofpattern = 1 all the time
        for (int k = 0; k < numberofpattern; k++) {
            //Forward
            for (int i = 0; i < numberofhiddenneuron; i++) {
                hiddenlayer[0][i] = 0;
            }
            for (int i = 0; i < numberofoutputneuron; i++) {
                outputlayer[i] = 0;
            }
            for (int i = 0; i < numberofhiddenneuron; i++) {
                for (int j = 0; j <= numberofinputneuron; j++) {
                    if (j != numberofinputneuron) {
                        hiddenlayer[0][i] += weights[0][j][i] * inputpattern[k][j];
                    } else {
                        hiddenlayer[0][i] += weights[0][j][i];
                    }
                }
                //System.out.println("hiddenout["+i+"]:"+hiddenlayer[0][i]);
            }
            for (int i = 0; i < numberofhiddenneuron; i++) {
                activatedhiddenlayer[0][i] = ActivationFunction(hiddenlayer[0][i]);
                //System.out.println("activatedhiddenout["+i+"]:"+activatedhiddenlayer[0][i]);
            }
            for (int i = 0; i < numberofoutputneuron; i++) {
                for (int j = 0; j <= numberofhiddenneuron; j++) {
                    if (j != numberofhiddenneuron) {
                        outputlayer[i] += weights[1][j][i] * activatedhiddenlayer[0][j];
                    } else {
                        outputlayer[i] += weights[1][j][i];
                    }
                }
                //System.out.println("Outputout["+i+"]:"+outputlayer[i]);
            }
            for (int i = 0; i < numberofoutputneuron; i++) {
                activatedoutputlayer[i] = ActivationFunction(outputlayer[i]);
                rspptr = activatedoutputlayer[i];
                //System.out.println("activatedhiddenout["+i+"]:"+activatedoutputlayer[i]);
            }

            for (int i = 0; i < numberofoutputneuron; i++) {
                outputerror[i] = (targetpattern[k] - activatedoutputlayer[i]) * (DerivationOfActivationFunction(outputlayer[i]));
                //System.out.println("outputerror["+i+"]:"+outputerror[i]);
            }
            patternerror[k] = 0;
            for (int i = 0; i < numberofoutputneuron; i++) {
                patternerror[k] += Math.pow((targetpattern[k] - activatedoutputlayer[i]), 2);
                //System.out.println("patternerror["+k+"]:"+patternerror[k]);
            }
            patternerror[k] = 0.5 * patternerror[k];
            if (k == numberofpattern - 1) {
                double totalerror = 0;
                for (int i = 0; i < numberofpattern; i++) {
                    totalerror += patternerror[i];
                }
                netError = totalerror;
//                System.out.println("totalerror " + totalerror);
            }
            for (int i = 0; i < numberofoutputneuron; i++) {
                for (int j = 0; j <= numberofhiddenneuron; j++) {
                    if (j != numberofhiddenneuron) {
                        deltaweights[1][j][i] = (learningrate * outputerror[i] * activatedhiddenlayer[0][j]) + (momentum * deltaweightsbuffer[1][j][i]);
                    } else {
                        deltaweights[1][j][i] = (learningrate * outputerror[i]) + (momentum * deltaweightsbuffer[1][j][i]);
                    }
                }
            }

            for (int i = 0; i < numberofoutputneuron; i++) {
                for (int j = 0; j <= numberofhiddenneuron; j++) {
                    deltaweightsbuffer[1][j][i] = deltaweights[1][j][i];
                }
            }
            //backpropagation
            for (int i = 0; i < numberofhiddenneuron; i++) {
                backpropagationhiddenlayer[0][i] = 0;
            }
            for (int i = 0; i < numberofhiddenneuron; i++) {
                for (int j = 0; j < numberofoutputneuron; j++) {
                    backpropagationhiddenlayer[0][i] += outputerror[j] * weights[1][i][j];
                }
            }
            for (int i = 0; i < numberofhiddenneuron; i++) {
                errorhidden[0][i] = backpropagationhiddenlayer[0][i] * DerivationOfActivationFunction(hiddenlayer[0][i]);
            }
            for (int i = 0; i < numberofhiddenneuron; i++) {
                for (int j = 0; j <= numberofinputneuron; j++) {
                    if (j != numberofinputneuron) {
                        deltaweights[0][j][i] = (learningrate * errorhidden[0][i] * inputpattern[k][j]) + (momentum * deltaweightsbuffer[0][j][i]);
                    } else {
                        deltaweights[0][j][i] = (learningrate * errorhidden[0][i]) + (momentum * deltaweightsbuffer[0][j][i]);
                    }
                }
            }
            for (int i = 0; i < numberofhiddenneuron; i++) {
                for (int j = 0; j <= numberofinputneuron; j++) {
                    deltaweightsbuffer[0][j][i] = deltaweights[0][j][i];
                }
            }
            for (int i = 0; i < numberofoutputneuron; i++) {
                for (int j = 0; j <= numberofhiddenneuron; j++) {
                    weights[1][j][i] += deltaweights[1][j][i];
                }
            }
            for (int i = 0; i < numberofhiddenneuron; i++) {
                for (int j = 0; j <= numberofinputneuron; j++) {
                    weights[0][j][i] += deltaweights[0][j][i];
                }
            }
        }

        return true;
    }

    public boolean OutPut(double[] input, double[] result) {

        if ((input.length == 0) || (result.length == 0)) {
            return false;
        }
        inputpattern = new double[numberofpattern][patterndimension];
        for (int i = 0; i < patterndimension; i++) {
            inputpattern[0][i] = input[i];
        }
//        double[] result = new double[numberofpattern];

        // numberofpattern = 1 all the time
        for (int k = 0; k < numberofpattern; k++) {
            for (int i = 0; i < numberofhiddenneuron; i++) {
                hiddenlayer[0][i] = 0;
            }
            for (int i = 0; i < numberofoutputneuron; i++) {
                outputlayer[i] = 0;
            }
            for (int i = 0; i < numberofhiddenneuron; i++) {
                for (int j = 0; j <= numberofinputneuron; j++) {
                    if (j != numberofinputneuron) {
                        hiddenlayer[0][i] += weights[0][j][i] * inputpattern[k][j];
                    } else {
                        hiddenlayer[0][i] += weights[0][j][i];
                    }
                }
                //System.out.println("hiddenout["+i+"]:"+hiddenlayer[0][i]);
            }
            for (int i = 0; i < numberofhiddenneuron; i++) {
                activatedhiddenlayer[0][i] = ActivationFunction(hiddenlayer[0][i]);
                //System.out.println("activatedhiddenout["+i+"]:"+activatedhiddenlayer[0][i]);
            }
            for (int i = 0; i < numberofoutputneuron; i++) {
                for (int j = 0; j <= numberofhiddenneuron; j++) {
                    if (j != numberofhiddenneuron) {
                        outputlayer[i] += weights[1][j][i] * activatedhiddenlayer[0][j];
                    } else {
                        outputlayer[i] += weights[1][j][i];
                    }
                }
                //System.out.println("Outputout["+i+"]:"+outputlayer[i]);
            }
            for (int i = 0; i < numberofoutputneuron; i++) {
                activatedoutputlayer[i] = ActivationFunction(outputlayer[i]);
                result[k] = activatedoutputlayer[i];
                //System.out.println("Mapping["+k+"]:"+activatedoutputlayer[i]);
            }
        }

        return true;
    }


    public NeuralNetObj getNeuralNetObj() {
        NeuralNetObj weightObj = new NeuralNetObj();
        weightObj.setActivationtype(activationtype);
        weightObj.setLearningrate(learningrate);
        weightObj.setMomentum(momentum);
        weightObj.setNumberofhiddenneuron(numberofhiddenneuron);
        weightObj.setPatterndimension(patterndimension);
        weightObj.setTargetdimension(targetdimension);
        weightObj.setWeights(weights);
        return weightObj;

    }
}
