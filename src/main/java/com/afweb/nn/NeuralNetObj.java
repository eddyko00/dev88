/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.nn;

/**
 *
 * @author eddy
 */
public class NeuralNetObj {
    
    private int patterndimension;  //input size
    private int numberofhiddenneuron;  // hidden size
    private int targetdimension;   //output size
    
    private int activationtype;
    private double learningrate;
    private double momentum;    
    private double[][][] weights;
    private double[][] input;
    private double[] target;
    /**
     * @return the weights
     */
    public double[][][] getWeights() {
        return weights;
    }



    /**
     * @return the patterndimension
     */
    public int getPatterndimension() {
        return patterndimension;
    }

    /**
     * @param patterndimension the patterndimension to set
     */
    public void setPatterndimension(int patterndimension) {
        this.patterndimension = patterndimension;
    }

    /**
     * @return the numberofhiddenneuron
     */
    public int getNumberofhiddenneuron() {
        return numberofhiddenneuron;
    }

    /**
     * @param numberofhiddenneuron the numberofhiddenneuron to set
     */
    public void setNumberofhiddenneuron(int numberofhiddenneuron) {
        this.numberofhiddenneuron = numberofhiddenneuron;
    }

    /**
     * @return the targetdimension
     */
    public int getTargetdimension() {
        return targetdimension;
    }

    /**
     * @param targetdimension the targetdimension to set
     */
    public void setTargetdimension(int targetdimension) {
        this.targetdimension = targetdimension;
    }

    /**
     * @return the activationtype
     */
    public int getActivationtype() {
        return activationtype;
    }

    /**
     * @param activationtype the activationtype to set
     */
    public void setActivationtype(int activationtype) {
        this.activationtype = activationtype;
    }

    /**
     * @return the learningrate
     */
    public double getLearningrate() {
        return learningrate;
    }

    /**
     * @param learningrate the learningrate to set
     */
    public void setLearningrate(double learningrate) {
        this.learningrate = learningrate;
    }

    /**
     * @return the momentum
     */
    public double getMomentum() {
        return momentum;
    }

    /**
     * @param momentum the momentum to set
     */
    public void setMomentum(double momentum) {
        this.momentum = momentum;
    }

    /**
     * @param weights the weights to set
     */
    public void setWeights(double[][][] weights) {
        this.weights = weights;
    }

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
