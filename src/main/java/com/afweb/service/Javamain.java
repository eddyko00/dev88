/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.service;


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
   
        while (true) {
            srv.timerHandler("");
            ServiceAFweb.AFSleep1Sec(1);
        }

    }
}
