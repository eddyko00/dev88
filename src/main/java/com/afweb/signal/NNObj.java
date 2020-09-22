/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.signal;

/**
 *
 * @author eddy
 */
public class NNObj {

    /**
     * @return the output1
     */
    public float getOutput1() {
        return output1;
    }

    /**
     * @param output1 the output1 to set
     */
    public void setOutput1(float output1) {
        this.output1 = output1;
    }

    /**
     * @return the output2
     */
    public float getOutput2() {
        return output2;
    }

    /**
     * @param output2 the output2 to set
     */
    public void setOutput2(float output2) {
        this.output2 = output2;
    }
    
    private int trsignal;
    private String trNameLink;
    private float rating;
    private float grossprofit;
    private float prediction;
    private float output1;
    private float output2;    
    private String comment="";

    /**
     * @return the trsignal
     */
    public int getTrsignal() {
        return trsignal;
    }

    /**
     * @param trsignal the trsignal to set
     */
    public void setTrsignal(int trsignal) {
        this.trsignal = trsignal;
    }


    /**
     * @return the rating
     */
    public float getRating() {
        return rating;
    }

    /**
     * @param rating the rating to set
     */
    public void setRating(float rating) {
        this.rating = rating;
    }

    /**
     * @return the grossprofit
     */
    public float getGrossprofit() {
        return grossprofit;
    }

    /**
     * @param grossprofit the grossprofit to set
     */
    public void setGrossprofit(float grossprofit) {
        this.grossprofit = grossprofit;
    }

    /**
     * @return the trNameLink
     */
    public String getTrNameLink() {
        return trNameLink;
    }

    /**
     * @param trNameLink the trNameLink to set
     */
    public void setTrNameLink(String trNameLink) {
        this.trNameLink = trNameLink;
    }

    /**
     * @return the prediction
     */
    public float getPrediction() {
        return prediction;
    }

    /**
     * @param prediction the prediction to set
     */
    public void setPrediction(float prediction) {
        this.prediction = prediction;
    }

    /**
     * @return the comment
     */
    public String getComment() {
        return comment;
    }

    /**
     * @param comment the comment to set
     */
    public void setComment(String comment) {
        this.comment = comment;
    }
}
