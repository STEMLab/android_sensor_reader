package io.github.stemlab.androidsensorreader;

import android.content.Context;

import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.complex.ComplexUtils;
import org.jtransforms.fft.DoubleFFT_1D;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import io.github.stemlab.androidsensorreader.pojo.Location;
import io.github.stemlab.androidsensorreader.pojo.Signal;

import static java.lang.Math.abs;


public class TensorFlowClassifier {
    private static final String MODEL_FILE = "file:///android_asset/frozen_tfdroid_deepSenseNoConv3D.pb";
    //    private static final String MODEL_FILE = "file:///android_asset/frozen_har.pb";
    private static final String INPUT_NODE = "I";
    private static final String[] OUTPUT_NODES = {"O"};
    private static final String OUTPUT_NODE = "O";
    private static final long[] INPUT_SIZE = {1, 2400};
    private static final int OUTPUT_SIZE = 6;
    private static long timeScale = 100000000;
    private static int spectralSamples = 10;

    static {
        System.loadLibrary("tensorflow_inference");
    }

    private TensorFlowInferenceInterface inferenceInterface;

    public TensorFlowClassifier(final Context context) {
        inferenceInterface = new TensorFlowInferenceInterface(context.getAssets(), MODEL_FILE);
    }

    public static double[] linspace(double min, double max, int points) {
        double[] d = new double[points];
        for (int i = 0; i < points; i++) {
            d[i] = min + i * (max - min) / (points - 1);
        }
        return d;
    }

    public static Complex[] fft1D(Complex[] signal) {

        int n = signal.length;
        Complex[] fourier = new Complex[n];

        double[] coeff = new double[2 * n];
        int i = 0;
        for (Complex c : signal) {
            coeff[i++] = c.getReal();
            coeff[i++] = c.getImaginary();
        }

        DoubleFFT_1D fft = new DoubleFFT_1D(n);
        fft.complexForward(coeff);

        for (i = 0; i < 2 * n; i += 2) {
            Complex c = new Complex(coeff[i], coeff[i + 1]);
            fourier[i / 2] = c;
        }
        return fourier;
    }

    public float[] predictProbabilities(float[] data) {
        float[] result = new float[OUTPUT_SIZE];
        inferenceInterface.feed(INPUT_NODE, data, INPUT_SIZE);
        inferenceInterface.run(OUTPUT_NODES);
        inferenceInterface.fetch(OUTPUT_NODE, result);

        //["bike", "sit", "stand", "walk", "stairsup", "stairsdown"]
        return result;
    }

    public float[] prepareDataForClassifier(List<Signal> accelerometerSamples, List<Signal> gyroscopeSamples) {

/*        ArrayList<Double> data = new ArrayList<>();
        List<HashMap> pairedSignal = pairSignalsByTime(accelerometerSamples, gyroscopeSamples);

        Collections.sort(pairedSignal, (o1, o2) -> {

            double t1 = (double) o1.get("Time");
            double t2 = (double) o2.get("Time");

            if (t1 < t2) return -1;
            if (t1 > t2) return 1;
            return 0;
        });

        double window = Math.abs((double) pairedSignal.get(pairedSignal.size() - 1).get("Time") - (double) pairedSignal.get(0).get("Time")) / 20;

        int id1 = 0;
        int id2 = 1;
        int c = 0;
        while (id1 < pairedSignal.size() && id2 < pairedSignal.size()) {
            if (abs((double) pairedSignal.get(id1).get("Time") - (double) pairedSignal.get(id2).get("Time")) < window) {
                c++;
                id2++;
                if (id2 == pairedSignal.size()) {
                    System.out.println("From  id1 = " + id1 + " to id2 = " + ((id2) - 1) + " is " + (id2 - id1));
                    data.addAll(getFFTValues(pairedSignal.subList(id1, (id2) - 1)));
                }
            } else {
                System.out.println("From  id1 = " + id1 + " to id2 = " + id2 + " is " + c);
                data.addAll(getFFTValues(pairedSignal.subList(id1, (id2) - 1)));
                c = 0;
                id1 = id2;
                id2++;
            }
        }

        float[] r = new float[data.size()];
        for (int i = 0; i < data.size(); i++) {
            r[i] = data.get(i).floatValue();
        }

        if (r.length == 2400)
            return predictProbabilities(r);*/
        /*else*/ return new float[6];
    }

    private ArrayList<Double> getFFTValues(List<HashMap> window) {
        System.out.println("Windows size:" + window.size());

        double curTimeList[] = new double[window.size()];
        double curAccList[][] = new double[3][window.size()];
        double curGyrList[][] = new double[3][window.size()];

        for (int i = 0; i < window.size(); i++) {
            curTimeList[i] = (double) window.get(i).get("Time");
            Signal temp = (Signal) window.get(i).get("Accelerometer");
            curAccList[0][i] = temp.getX();
            curAccList[1][i] = temp.getY();
            curAccList[2][i] = temp.getZ();
            temp = (Signal) window.get(i).get("Gyroscope");
            curGyrList[0][i] = temp.getX();
            curGyrList[1][i] = temp.getY();
            curGyrList[2][i] = temp.getZ();
        }

        ArrayList<Double> accvalues = getSignalFFT(curAccList, curTimeList);
        ArrayList<Double> gyrvalues = getSignalFFT(curGyrList, curTimeList);
        accvalues.addAll(gyrvalues);
        return accvalues;
    }

    public ArrayList<Double> getSignalFFT(double curAccList[][], double curTimeList[]) {

        double accInterpTime[] = linspace(curTimeList[0], curTimeList[curTimeList.length - 1], spectralSamples);

        double accInterpVal[][] = new double[curAccList.length][accInterpTime.length];

        for (int i = 0; i < curAccList.length; i++) {
            LinearInterpolator interp = new LinearInterpolator();
            PolynomialSplineFunction f = interp.interpolate(curTimeList, curAccList[i]);

            for (int j = 0; j < accInterpTime.length; j++) {
                accInterpVal[i][j] = f.value(accInterpTime[j]);
            }
        }

        ArrayList<Complex[]> whole = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Complex[] c = ComplexUtils.convertToComplex(accInterpVal[i]);
            Complex[] test = fft1D(c);
            whole.add(test);
        }

        Complex[][] right = new Complex[10][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 10; j++) {
                right[j][i] = whole.get(i)[j];
            }
        }

        ArrayList<Double> result = new ArrayList<>();
        for (Complex[] comp : right) {
            for (Complex c : comp) {
                result.add(c.getReal());
                result.add(c.getImaginary());
            }
        }
        return result;
    }

    public List<HashMap> pairSignalsByTime(List<Signal> accelerometerSamples, List<Signal> gyroscopeSamples/*, HashMap<Long, Location> locSamples*/) {
        int idx1 = 0;
        int idx2 = 0;
        List<HashMap> whole = new ArrayList<>();
        while (idx1 < accelerometerSamples.size() && idx2 < gyroscopeSamples.size()) {
            Signal curItem1 = accelerometerSamples.get(idx1);
            Signal curItem2 = gyroscopeSamples.get(idx2);

            long curTime1 = curItem1.getTimestamp() / timeScale;
            long curTime2 = curItem2.getTimestamp() / timeScale;
            //Location current = new Location();
            //if (locSamples.containsKey(curItem1.getTimestamp())) current = locSamples.get(curItem1.getTimestamp());
            if (abs(curTime1 - curTime2) < 0.1) {
                HashMap curSaveElem = new HashMap();
                curSaveElem.put("Time", (long) (0.5 * (curItem1.getTimestamp() + curItem2.getTimestamp())));
                curSaveElem.put("Accelerometer", curItem1);
                curSaveElem.put("Gyroscope", curItem2);
                //curSaveElem.put("Location", current);
                whole.add(curSaveElem);
                idx1 += 1;
                idx2 += 1;
                //count += 1;
            } else if (curTime1 - curTime2 >= 0.1) {
                idx2 += 1;
            } else {
                idx1 += 1;
            }

        }
        return whole;
    }


}
