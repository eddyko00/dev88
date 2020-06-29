/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.nnprocess;

import com.afweb.util.CKey;
import com.afweb.model.*;
import com.afweb.model.stock.*;
import com.afweb.nn.*;

import com.afweb.service.*;

import com.afweb.signal.*;
import com.afweb.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 *
 * @author koed
 */
public class NNProcessStock {

    public static Logger logger = Logger.getLogger("NNProcessStock");

    private void NeuralNetInputStTesting(ServiceAFweb serviceAFWeb) {
        TradingNNprocess NNProcessImp = new TradingNNprocess();

        boolean createTrain = true;
        if (createTrain == true) {
            int sizeYr = 3;
            for (int j = 0; j < sizeYr; j++) { //4; j++) {
                int size = 20 * CKey.MONTH_SIZE * j;

            }
        }
        // create neural net input data
    }

    
}
