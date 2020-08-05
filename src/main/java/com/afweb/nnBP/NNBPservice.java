/*
 * AFneuralNet.java
 *
 * Created on March 7, 2007, 10:30 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package com.afweb.nnBP;

import com.afweb.service.ServiceAFweb;
import com.afweb.util.CKey;
import com.afweb.util.FileUtil;
import com.afweb.util.TimeConvertion;
import com.afweb.util.getEnv;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.logging.Logger;

/**
 *
 * @author vm1
 */
public class NNBPservice {

    /* random number generator */
    public static long randseed = 568731;
    protected static Logger logger = Logger.getLogger("NeuralUtil");

    public static double getRandom() {
        int randvalue;
        randseed = 15625L * randseed + 22221L;
        randvalue = (int) ((randseed >> 16) & 0x7FFF);
        return ((double) (randvalue / Math.pow(2.0, 15.0) - 0.5));
    }

    public NNBPservice() {
    }

    public double[][] getInputpattern() {
        if (BPnet != null) {
            return BPnet.nnBPobj.getInputpattern();
        }
        return null;
    }

    public double[][] getOutputpattern() {
        if (BPnet != null) {
            return BPnet.nnBPobj.getOutputpattern();
        }
        return null;
    }

    public void setOutputpattern(double[][] outputpattern) {
        if (BPnet != null) {
            BPnet.nnBPobj.setOutputpattern(outputpattern);
        }
    }

    public void setInputpattern(double[][] inputpattern) {
        if (BPnet != null) {
            BPnet.nnBPobj.setInputpattern(inputpattern);
        }
    }

    public void setAlphaEta(double Alpha, double Eta) {
        if (BPnet != null) {
            BPnet.alpha = Alpha;
            BPnet.eta = Eta;
        }
    }
    private Nnet BPnet = null;

    public void create(int inputSize, int middleSize, int outputSize) {
        int[] inLayer = new int[4];
        inLayer[0] = 3;
        inLayer[1] = inputSize;
        inLayer[2] = middleSize;
        inLayer[3] = outputSize;
        BPnet = new Nnet();
        BPnet.BPFnet(inLayer);

    }

    public boolean predict(double[] inputptr, double[] rspptr) {

        if (isEnable() == false) {
            return false;
        }
        try {
            return BPnet.OutPut(inputptr, rspptr);
        } catch (Exception ex) {
        }
        return false;
    }

    public double predictTest(double[][] inputptr, double[][] outputptr,
            double[][] rspptr, double errorIteration) {
        double totalError = 1;
        double[] input;
        double[] output;
        double[] rsp;

        if (isEnable() == false) {
            return totalError;
        }
        int i = 0;
        try {

            totalError = 0;
            int learnLenght = inputptr.length;

            for (int j = 0; j < inputptr.length; j++) {
                input = inputptr[j];
                output = outputptr[j];

//                if (output[0] == 0) {  // make sure set output not zero for predict
//                    learnLenght = learnLenght - 1;
//                    continue;
//                }
                rsp = rspptr[j];

                if (BPnet.OutPut(input, rsp) == false) {
                    return totalError;
                }

                float deltaError = 0;
                for (int k = 0; k < output.length; k++) {
                    deltaError += (float) Math.abs(output[k] - rsp[k]);
                }

                totalError += deltaError;
            }
            totalError = totalError / learnLenght;

//            System.out.println(i + " Predict Error =" + totalError);
        } catch (Exception ex) {
            logger.info("> learn  " + ex);
        }

        return totalError;

    }
    public static long delaySecond = 0;

    // exit after 3 minutes
    public double learn(String name, double[][] inputptr, double[][] outputptr,
            double[][] rspptr, int numberIteration, double errorIteration) {
        double totalError = 1;
        double[] input;
        double[] output;
        double[] rsp;

        if (isEnable() == false) {
            return totalError;
        }

        long currentTime = System.currentTimeMillis();
        int Min4 = 4;
        if (getEnv.checkLocalPC() == true) {
            Min4 = 10;
        }
        if (CKey.NN_DEBUG == true) {
            Min4 = 100;
            numberIteration = 9000000;
        }

        long lockDate4Min = TimeConvertion.addMinutes(currentTime, Min4);

        int i = 0;
        try {

            for (i = 0; i < numberIteration; i++) {
                totalError = 0;
                int learnLenght = inputptr.length;

                for (int j = 0; j < inputptr.length; j++) {
                    input = inputptr[j];
                    output = outputptr[j];

                    if (output[0] == 0) {
                        learnLenght = learnLenght - 1;
                        continue;
                    }
                    if (output[0] < 0) {
                        learnLenght = learnLenght - 1;
                        continue;
                    }
                    rsp = rspptr[j];
                    if (BPnet.Learn(input, output, rsp) == false) {
                        return totalError;
                    }
                    totalError += BPnet.netError;
                }
                totalError = totalError / learnLenght;

                if (i % 4000 == 0) { //4000 == 0) {
                    if (CKey.NN_DEBUG == true) {
                        System.out.println(i + " " + name + "  threshold=" + errorIteration + "  Error=" + totalError);
                    }
                }
                if (delaySecond != 0) {
                    if (i % 50 == 0) {
                        ServiceAFweb.AFSleep();
                    }
                }
                if (i % 1000 == 0) {
                    // 0.001 limit
                    if (totalError < errorIteration) {
                        break;
                    }
                    currentTime = System.currentTimeMillis();
                    if (lockDate4Min < currentTime) {
                        break;
                    }
                }

            }
        } catch (Exception ex) {
            logger.info("> learn  " + ex);
        }
        if (numberIteration > 1000) {
            logger.info("> learn  " + i + " " + name + "  threshold=" + errorIteration + "  Error=" + totalError);
        }
        return totalError;
    }

    public String getNetObjSt() {
        if (isEnable() == false) {
            return null;
        }
        String retObjSt = BPnet.toString();
        if (retObjSt == null) {
            return null;
        }
        retObjSt = retObjSt.replaceAll("\"", "#");
        return retObjSt;

    }

    public void createNet(String retObjSt) {
        if (retObjSt == null) {
            return;
        }
        retObjSt = retObjSt.replaceAll("#", "\"");
        BPnet = new Nnet(retObjSt);
        if (BPnet.TestNet() == false) {
            BPnet = null;
        }
    }

    public void writeNet(String FileName) {
        if (isEnable() == false) {
            return;
        }
        StringBuffer sb = new StringBuffer(BPnet.toString());
        FileUtil.FileWriteText(FileName, sb);
    }

    public void readNet(String FileName) {
        StringBuffer sb = new StringBuffer();
        if (FileUtil.FileTest(FileName) == true) {
            sb = FileUtil.FileReadText(FileName);
        }
        BPnet = new Nnet(sb.toString());
        if (BPnet.TestNet() == false) {
            BPnet = null;
        }
    }

    public boolean isEnable() {
        if (BPnet == null) {
            return false;
        }
        return BPnet.TestNet();
    }

    public class doubleArray {

        public double[] doublePtr;

        public doubleArray(int size) {
            doublePtr = new double[size];
        }
    }

    public class neuronArray {

        public Neuron[] NeuronPtr;

        public neuronArray(int size) {
            NeuronPtr = new Neuron[size];
        }
    }

    public class Neuron {

        public double[] WeightPtr;
        public double[] DelWeight;

        public Neuron(int size) {
            WeightPtr = new double[size];
            DelWeight = new double[size];
        }
    }

    public class Nnet {

        private int nLayer = 0;
        /* total number of layer */

        private int[] layer;
        /* # of neuron on each layer */

        private doubleArray[] outPtr;
        /* # of weight for each node */

        private doubleArray[] outError;
        private neuronArray[] neu;
        /* # of neuron for each layer */

        private double inNormalize = 1;
        private double outNormalize = 1;
        public double alpha = 0.6;
        /* learning rate */

        public double eta = 0.7;
        /* depend on how much memory the system have */
        public final int maxHLayers = 5;
        public double netError = 0;

        public NNBPobj nnBPobj = new NNBPobj();

        public Nnet() {

        }

        public double getInNormalize() {
            return inNormalize;
        }

        public void setInNormalize(double value) {
            inNormalize = value;
        }

        public double getOutNormalize() {
            return outNormalize;
        }

        public void setOutNormalize(double value) {
            outNormalize = value;
        }

        public boolean TestNet() {
            if (nLayer == 0) {
                return false;
            }
            return true;

        }

        // ---------------------------------------------------------------
        public boolean BPFnet(int[] inptr) {

            if (NnetSet(inptr) == true) {
                BPFSetNeuron();
                return true;
            }
            return false;
        }

        private boolean BPFSetNeuron() {
            for (int i = 1; i < nLayer; i++) {
                for (int j = 0; j < layer[i]; j++) {
                    neu[i].NeuronPtr[j] = new Neuron(layer[i - 1] + 1);
                    if (NnetSetNeuron(neu[i].NeuronPtr[j], layer[i - 1] + 1) == false) {
                        nLayer = 0;
                        return false;
                    }
                }
            }
            return true;
        }

        private boolean NnetSetNeuron(Neuron neuron, int inlayer) {
            if (inlayer > 0) {

                for (int i = 0; i < inlayer; i++) {
                    neuron.WeightPtr[i] = getRandom();
                    neuron.DelWeight[i] = 0.0;
                }
            }
            return true;
        }

        public void ForNeuron(Neuron[] neuptr, int inlayer, double[] inptr,
                int outlayer, double[] outptr) {
            int i, j;
            double[] weight;

            for (i = 0; i < outlayer; i++) /* output layer */ {
                outptr[i] = 0.0;
                weight = neuptr[i].WeightPtr;
                for (j = 0; j < inlayer; j++) /* inpput layer */ {
                    outptr[i] += inptr[j] * weight[j];
                }
            }
            outptr[i] = 1.0;
            /* offset */
        }

        void WeightNeuron(Neuron[] neuptr, int inlayer, int outlayer) {
            int i, j;
            double[] weight;
            double[] delweight;

            /* weight change */
            for (i = 0; i < outlayer; i++) {
                weight = neuptr[i].WeightPtr;
                delweight = neuptr[i].DelWeight;
                for (j = 0; j < inlayer + 1; j++) {
                    weight[j] += delweight[j];
                }
            }
        }

        // ---------------------------------------
        // ---------------------------------------
        private boolean NnetSet(int[] inpLayer) {

            nLayer = inpLayer[0];
            if ((nLayer > maxHLayers + 2) || (nLayer <= 0)) {
                nLayer = 0;
                return false;
            }

            layer = new int[nLayer];
            int i;
            for (i = 0; i < nLayer; i++) {
                layer[i] = inpLayer[i + 1];
            }

            outPtr = new doubleArray[layer[0] + 1];
            outError = new doubleArray[layer[0] + 1];
            neu = new neuronArray[layer[0] + 1];

            outPtr[0] = new doubleArray(layer[1] + 1);
            outError[0] = new doubleArray(layer[1] + 1);

            for (i = 1; i < nLayer; i++) {
                outPtr[i] = new doubleArray(layer[i] + 1);
                outError[i] = new doubleArray(layer[i] + 1);
                neu[i] = new neuronArray(layer[i]);
            }
            return true;
        }

        private void NnetInput(double[] inptr) {
            int i;
            double[] ptrOutPtr;

            ptrOutPtr = outPtr[0].doublePtr;

            for (i = 0; i < layer[0]; i++) {
                ptrOutPtr[i] = inptr[i];
            }
            ptrOutPtr[i] = 1.0;
        }

        private void NnetFunction(int outlayer, double[] outptr) {
            int i;

            for (i = 0; i < outlayer; i++) /* output layer */ {
                outptr[i] = (double) 1 / (1 + Math.exp(-outptr[i]));
            }
        }

        private void NnetOutput(double[] rspptr) {
            int i;
            double[] ptrOutPtr;

            ptrOutPtr = outPtr[nLayer - 1].doublePtr;
            for (i = 0; i < layer[nLayer - 1]; i++) {
                rspptr[i] = ptrOutPtr[i];
            }
        }

        private double NnetDelta(double[] outputptr) {
            double ep;

            ep = NnetError(layer[nLayer - 1], outPtr[nLayer - 1].doublePtr,
                    outError[nLayer - 1].doublePtr, outputptr);
            Backward();
            return (ep);
        }

        private double NnetError(int outlayer, double[] outptr,
                double[] outerror, double[] rspptr) {
            int i;
            double outp, ep;

            ep = 0.0;
            for (i = 0; i < outlayer; i++) {
                outp = outptr[i];
                outerror[i] = (rspptr[i] - outp) * (1 - outp) * outp;
                ep += Math.abs(rspptr[i] - outp);
            }
            return (ep);
        }

        private boolean Backward() {
            int i;

            if (nLayer == 0) {
                return false;
            }
            for (i = nLayer - 1; i > 0; i--) {
                BackProp(neu[i].NeuronPtr, layer[i - 1],
                        outPtr[i - 1].doublePtr, outError[i - 1].doublePtr,
                        layer[i], outError[i].doublePtr);
                BackError(layer[i - 1], outPtr[i - 1].doublePtr,
                        outError[i - 1].doublePtr);
                WeightNeuron(neu[i].NeuronPtr, layer[i - 1], layer[i]);
            }
            return true;
        }

        private void BackProp(Neuron[] neuptr, int inlayer, double[] inptr,
                double[] inerror, int outlayer, double[] outerror) {
            int i, j;
            double[] weight;
            double[] delweight;

            for (i = 0; i < inlayer; i++) {
                inerror[i] = 0.0;
            }
            for (i = 0; i < outlayer; i++) {
                weight = neuptr[i].WeightPtr;
                delweight = neuptr[i].DelWeight;
                for (j = 0; j < inlayer + 1; j++) {
                    inerror[j] += outerror[i] * weight[j];
                    delweight[j] = eta * outerror[i] * inptr[j]
                            + alpha * delweight[j];
                }
            }

        }

        private void BackError(int inlayer, double[] inptr, double[] inerror) {
            int i;

            for (i = 0; i < inlayer; i++) {
                inerror[i] = inptr[i] * (1 - inptr[i]) * inerror[i];
            }
        }

        // ---------------------------------------
        public boolean OutPut(double[] inptr, double[] rspptr) {
            int i;

            if ((inptr.length == 0) || (rspptr.length == 0)) {
                return false;
            }

            if (nLayer == 0) {
                return false;
            }

            if (inptr.length != layer[0]) {
                return false;
            }

            NnetInput(inptr);
            for (i = 0; i < nLayer - 1; i++) {
                ForNeuron(neu[i + 1].NeuronPtr, layer[i] + 1,
                        outPtr[i].doublePtr, layer[i + 1],
                        outPtr[i + 1].doublePtr);
                NnetFunction(layer[i + 1], outPtr[i + 1].doublePtr);
            }
            NnetOutput(rspptr);
            return true;
        }

        public boolean Learn(double[] input, double[] outputptr,
                double[] rspptr) {
            netError = 0.0;
            if (nLayer == 0) {
                return false;
            }

            if (OutPut(input, rspptr)) {
                netError = NnetDelta(outputptr);
                return true;
            }
            return false;
        }

        private String ToStringNeuron(Neuron[] neuptr, int outlayer,
                double[] outptr, int inlayer) {
            int i, j;
            double[] weight;

            StringBuffer retStr = new StringBuffer();
            for (i = 0; i < outlayer; i++) {
                weight = neuptr[i].WeightPtr;
                for (j = 0; j < inlayer + 1; j++) {
                    retStr.append(";" + weight[j]);
                }
            }
            return retStr.toString();
        }

        public String toString() {
            int i;
            StringBuffer retStr = new StringBuffer();

            String nnBPobjSt = "";
            try {
                nnBPobjSt = new ObjectMapper().writeValueAsString(nnBPobj);
                nnBPobjSt = nnBPobjSt.replaceAll("\"", "#");
            } catch (JsonProcessingException ex) {
            }

            retStr.append(CKey.version);
            retStr.append(";" + nnBPobjSt);
            retStr.append(";" + nLayer);
            for (i = 0; i < nLayer; i++) {
                retStr.append(";" + layer[i]);
            }
            retStr.append(";" + alpha);
            retStr.append(";" + eta);
            retStr.append(";" + inNormalize);
            retStr.append(";" + outNormalize);

            for (i = 1; i < nLayer; i++) {
                String NeuronStr = ToStringNeuron(neu[i].NeuronPtr, layer[i],
                        outPtr[i].doublePtr,
                        layer[i - 1]);
                retStr.append(NeuronStr);
            }

            return retStr.toString();
        }

        public Nnet(String NetString) {
            String[] strNetArray = NetString.split(";");
            if (strNetArray.length == 0) {
                return;
            }

            int icnt = 0;
            if (strNetArray[icnt++].toString().equals(CKey.version) == false) {
                // disable because inputStockNeuralNetData need different version so reload the base network
//                return;
            }

            try {
                String nnBPobjSt = strNetArray[icnt++].toString();

                try {
                    nnBPobjSt = nnBPobjSt.replaceAll("#", "\"");
                    NNBPobj nnObj = new ObjectMapper().readValue(nnBPobjSt, NNBPobj.class);
                    nnBPobj = nnObj;
                } catch (IOException ex) {
                    return;
                }

                int iNLayer = Integer.parseInt(strNetArray[icnt++]);
                int[] inptr = new int[iNLayer + 1];

                inptr[0] = iNLayer;
                for (int j = 1; j <= iNLayer; j++) {
                    inptr[j] = Integer.parseInt(strNetArray[icnt++]);
                }

                if (BPFnet(inptr) == false) {
                    nLayer = 0;
                    return;
                }

                alpha = Double.parseDouble(strNetArray[icnt++]);
                eta = Double.parseDouble(strNetArray[icnt++]);

                inNormalize = Double.parseDouble(strNetArray[icnt++]);
                outNormalize = Double.parseDouble(strNetArray[icnt++]);

                double[] weight;
                for (int i = 1; i < nLayer; i++) {
                    for (int k = 0; k < layer[i]; k++) {
                        weight = neu[i].NeuronPtr[k].WeightPtr;
                        for (int j = 0; j < layer[i - 1] + 1; j++) {
                            weight[j] = Double.parseDouble(strNetArray[icnt++]);
                        }
                    }
                }
            } catch (Exception e) {
                nLayer = 0;
            }
        }
    }

//------------------------------------------------------------------------------
//------------------------------------------------------------------------------
    public static void mainTest(String[] args) {
        NNBPservice afneural = new NNBPservice();
        afneural.create(3, 3, 1);
        double[][] inData = new double[8][];
        double[][] outData = new double[8][];
        double[][] rspptr = new double[8][];
        inData[0] = new double[3];
        inData[0][0] = 0.1;
        inData[0][1] = 0.1;
        inData[0][2] = 0.1;

        inData[1] = new double[3];
        inData[1][0] = 0.1;
        inData[1][1] = 0.1;
        inData[1][2] = 0.9;

        inData[2] = new double[3];
        inData[2][0] = 0.1;
        inData[2][1] = 0.9;
        inData[2][2] = 0.1;

        inData[3] = new double[3];
        inData[3][0] = 0.1;
        inData[3][1] = 0.9;
        inData[3][2] = 0.9;

        inData[4] = new double[3];
        inData[4][0] = 0.9;
        inData[4][1] = 0.1;
        inData[4][2] = 0.1;

        inData[5] = new double[3];
        inData[5][0] = 0.9;
        inData[5][1] = 0.1;
        inData[5][2] = 0.9;

        inData[6] = new double[3];
        inData[6][0] = 0.9;
        inData[6][1] = 0.9;
        inData[6][2] = 0.1;

        inData[7] = new double[3];
        inData[6][0] = 0.9;
        inData[6][1] = 0.9;
        inData[6][2] = 0.9;

        outData[0] = new double[1];
        outData[0][0] = 0.1;
        outData[1] = new double[1];
        outData[1][0] = 0.9;
        outData[2] = new double[1];
        outData[2][0] = 0.9;
        outData[3] = new double[1];
        outData[3][0] = 0.1;
        outData[4] = new double[1];
        outData[4][0] = 0.9;
        outData[5] = new double[1];
        outData[5][0] = 0.1;
        outData[6] = new double[1];
        outData[6][0] = 0.1;
        outData[7] = new double[1];
        outData[7][0] = 0.9;

        for (int i = 0; i < outData.length; i++) {
            rspptr[i] = new double[1];
        }
        double error = afneural.learn("XOR", inData, outData, rspptr, 1000000, 0.001);
        System.out.println("Error is " + error);
        afneural.writeNet("T:/Netbean/debug/netFile.net");
        afneural = new NNBPservice();
        afneural.readNet("T:/Netbean/debug/netFile.net");
        error = afneural.learn("XOR", inData, outData, rspptr, 1, 0.001);

    }
}
