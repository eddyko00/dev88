package com.afweb.nn;

/* Backpropagation for emulating XOR
	 * Copyright: Denny Hermawanto
	 * Mail: denny.hermawanto@gmail.com
 */

public class Backpropagation {

    private void DefineInput() {
        inputpattern = new double[numberofpattern][patterndimension];

        inputpattern[0][0] = 0;
        inputpattern[0][1] = 0;

        inputpattern[1][0] = 0;
        inputpattern[1][1] = 1;

        inputpattern[2][0] = 1;
        inputpattern[2][1] = 0;

        inputpattern[3][0] = 1;
        inputpattern[3][1] = 1;
    }

    private void DefineTarget() {
        targetpattern = new double[numberofpattern];

        targetpattern[0] = 0;
        targetpattern[1] = 1;
        targetpattern[2] = 1;
        targetpattern[3] = 0;
    }

    private double RandomNumberGenerator() {
        java.util.Random rnd = new java.util.Random();
        return rnd.nextDouble();
    }

    private void CreateNetworks() {
        numberofinputneuron = patterndimension;
        numberofhiddenneuron = 10;  //2;
        numberofoutputneuron = targetdimension;

        inputlayer = new double[numberofinputneuron];
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

        SSE = new double[maxiteration];

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

    private void FeedForward(int iteration) {
        System.out.println("Learning...");
        for (int iter = 0; iter < iteration; iter++) {
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
                    SSE[iter] = totalerror / numberofpattern;
                    System.out.println("SSE at iteration[" + (iter + 1) + "]= " + SSE[iter]);
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
        }
    }

    public void PatternMapping() {
        double[] result = new double[numberofpattern];
        System.out.println("");
        System.out.println("Mapping Pattern");

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
        System.out.println("0 XOR 0 = " + result[0]);
        System.out.println("0 XOR 1 = " + result[1]);
        System.out.println("1 XOR 0 = " + result[2]);
        System.out.println("1 XOR 1 = " + result[3]);
    }

    public void DefineParameters() {
        numberofpattern = 4;
        patterndimension = 2;
        targetdimension = 1;
        numberofhiddenlayer = 1;
        activationtype = BIPOLAR_SIGMOID;
        learningrate = 0.5;
        momentum = 0.8;
        maxiteration = 10000;
    }

    public void Run() {
        DefineParameters();
        DefineInput();
        DefineTarget();
        CreateNetworks();
        FeedForward(maxiteration);
        PatternMapping();
    }

    //variable definition
    private double[][] inputpattern;
    private int numberofpattern;
    private int patterndimension;
    private double[] targetpattern;
    private int targetdimension;

    private double[] inputlayer;
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
    private double[] SSE;
    private double learningrate;
    private double momentum;

    private int numberofhiddenneuron;
    private int numberofinputneuron;
    private int numberofoutputneuron;
    private int numberofhiddenlayer;
    private int activationtype;
    private int maxiteration;

    private static final int BINARY_SIGMOID = 1;
    private static final int BIPOLAR_SIGMOID = 2;
    private static final int HYPERBOLIC_TANGENT = 3;

//    public static void main(String[] args) {
//        Backpropagation backpropagation = new Backpropagation();
//        backpropagation.Run();
//    }
}
