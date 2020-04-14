/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.model.stock;

import com.vpumlmodel.stock.stock;

/**
 *
 * @author eddy
 */
public class AFstockObj extends stock {

    private AFstockInfo afstockInfo=null;
    private float prevClose;
    private int TRsignal=0;
    private String updateDateD = "";

    /**
     * @return the updateDateD
     */
    public String getUpdateDateD() {
        return updateDateD;
    }

    /**
     * @param updateDateD the updateDateD to set
     */
    public void setUpdateDateD(String updateDateD) {
        this.updateDateD = updateDateD;
    }

    /**
     * @return the TRsignal
     */
    public int getTRsignal() {
        return TRsignal;
    }

    /**
     * @param TRsignal the TRsignal to set
     */
    public void setTRsignal(int TRsignal) {
        this.TRsignal = TRsignal;
    }

    /**
     * @return the prevClose
     */
    public float getPrevClose() {
        return prevClose;
    }

    /**
     * @param prevClose the prevClose to set
     */
    public void setPrevClose(float prevClose) {
        this.prevClose = prevClose;
    }

    /**
     * @return the afstockInfo
     */
    public AFstockInfo getAfstockInfo() {
        return afstockInfo;
    }

    /**
     * @param afstockInfo the afstockInfo to set
     */
    public void setAfstockInfo(AFstockInfo afstockInfo) {
        this.afstockInfo = afstockInfo;
    }

}
