/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.model;

import com.afweb.util.CKey;

/**
 *
 * @author eddy
 */
public class WebStatus {

    /**
     * @return the ver
     */
    public float getVer() {
        return ver;
    }

    /**
     * @param ver the ver to set
     */
    public void setVer(float ver) {
        this.ver = ver;
    }
    private boolean result;
    private int resultID;
    private String response; 
    private float ver = CKey.iis_ver;

    /**
     * @return the result
     */
    public boolean isResult() {
        return result;
    }

    /**
     * @param result the result to set
     */
    public void setResult(boolean result) {
        this.result = result;
    }

    /**
     * @return the resultID
     */
    public int getResultID() {
        return resultID;
    }

    /**
     * @param resultID the resultID to set
     */
    public void setResultID(int resultID) {
        this.resultID = resultID;
    }

    /**
     * @return the response
     */
    public String getResponse() {
        return response;
    }

    /**
     * @param response the response to set
     */
    public void setResponse(String response) {
        this.response = response;
    }
}
