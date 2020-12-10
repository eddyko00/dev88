/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.service;

import com.afweb.nnprocess.NNProcessBySignal;
import com.afweb.util.CKey;

/**
 *
 * @author eddyko
 */
public class Javamain {

    /**
     * @param args the command line arguments
     */
    public static void javamain(String[] args) {
        // TODO code application logic here
        ServiceAFweb srv = new ServiceAFweb();

        if (args != null) {
            if (args.length > 0) {
                for (int i = 0; i < args.length; i++) {
                    String cmd = args[i];
                    if (cmd.indexOf("flagNNLearningSignal") != -1) {
                        NNProcessBySignal.flagNNLearningSignal = true;
                    } else if (cmd.indexOf("flagNN3LearningTrend") != -1) {
                        NNProcessBySignal.flagNN3LearningTrend = true;
                    } else if (cmd.indexOf("flagNNReLearning") != -1) {
                        NNProcessBySignal.flagNNReLearning = true;
                    } else if (cmd.indexOf("processNNSignalAdmin") != -1) {
                        NNProcessBySignal.processNNSignalAdmin = true;
                    } else if (cmd.indexOf("processRestinputflag") != -1) {
                        NNProcessBySignal.processRestinputflag = true;
                    } else if (cmd.indexOf("processRestAllStockflag") != -1) {
                        NNProcessBySignal.processRestAllStockflag = true;

                    } else if (cmd.indexOf("proxyflag") != -1) {
                        CKey.PROXY = true;
                    } else if (cmd.indexOf("nndebugflag") != -1) {
                        CKey.NN_DEBUG = true;
                        CKey.UI_ONLY = true;
                    }
                } // loop
            }
        }

        while (true) {
            srv.timerHandler("");
            ServiceAFweb.AFSleep1Sec(1);
        }

    }
}
