/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.service;

import static com.afweb.util.CKey.LOCAL_MYSQL;

/**
 *
 * @author eddyko
 */
public class javamain {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        ServiceAFweb srv = new ServiceAFweb();
   
        while (true) {
            srv.timerHandler("");
            ServiceAFweb.AFSleepSec(1);
        }

    }
}
