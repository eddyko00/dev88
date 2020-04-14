/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.nn;

/**
 *
 * @author eddyko
 */
public class NNInputDataObj {
    private long updatedatel;
    private NNInputOutObj obj;

    /**
     * @return the updatedatel
     */
    public long getUpdatedatel() {
        return updatedatel;
    }

    /**
     * @param updatedatel the updatedatel to set
     */
    public void setUpdatedatel(long updatedatel) {
        this.updatedatel = updatedatel;
    }

    /**
     * @return the obj
     */
    public NNInputOutObj getObj() {
        return obj;
    }

    /**
     * @param obj the obj to set
     */
    public void setObj(NNInputOutObj obj) {
        this.obj = obj;
    }
}
