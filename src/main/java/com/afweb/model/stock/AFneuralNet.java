/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.model.stock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vpumlmodel.afweb.neuralNet;

/**
 *
 * @author eddy
 */
public class AFneuralNet extends neuralNet {

    /////helper function
    public ReferNameData getReferNameData() {
        ReferNameData refData = new ReferNameData();
        String refName = this.getRefname();
        try {
            if ((refName != null) && (refName.length() > 0)) {
                refName = refName.replaceAll("#", "\"");
                refData = new ObjectMapper().readValue(refName, ReferNameData.class);
                return refData;
            }
        } catch (Exception ex) {
        }
        return refData;
    }
}
