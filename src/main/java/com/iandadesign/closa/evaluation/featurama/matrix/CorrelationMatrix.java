package com.iandadesign.closa.evaluation.featurama.matrix;

public class CorrelationMatrix {

    public final Matrix correlationMatrix;
    private final int num_observations;

    public CorrelationMatrix() {
        this.correlationMatrix = null;
        this.num_observations = 0;
    }

    public CorrelationMatrix(Matrix matrix) {
        this.num_observations = matrix.getRowDimension();
        this.correlationMatrix = computeCorrelationMatrix(matrix);
    }

    public Matrix computeCorrelationMatrix(Matrix matrix) {
        int nVars = matrix.getColumnDimension();
        Matrix outMatrix = new Matrix(nVars, nVars);
        for (int i = 0; i < nVars; i++) {
            for (int j = 0; j < i; j++) {
                double corr = correlation(matrix.returnColumn(i), matrix.returnColumn(j));
                outMatrix.writeValue(i, j, corr);
                outMatrix.writeValue(j, i, corr);
            }
            outMatrix.writeValue(i, i, 1d);
        }
        return outMatrix;
    }

    public double correlation(final double[] xArray, final double[] yArray) {
        // TODO: enforce xArray length == yArray length

        double xMean = 0.0, yMean = 0.0, s_x = 0.0, s_y = 0.0, tmpCorrelation = 0.0;

        for(int i=0; i<xArray.length; i++) {
            xMean += xArray[i];
            yMean += yArray[i];
        }
        xMean = xMean/xArray.length;
        yMean = yMean/yArray.length;

        for(int i=0; i<xArray.length; i++) {
            s_x += Math.pow((xArray[i]-xMean), 2);
            s_y += Math.pow((yArray[i]-yMean), 2);
        }

        s_x = Math.sqrt(s_x/(xArray.length-1));
        s_y = Math.sqrt(s_y/(yArray.length-1));

        for(int i=0; i<xArray.length; i++) {
            for(int j=0; j<xArray.length; j++) {
                tmpCorrelation = (xArray[i]-xMean)*(yArray[j]-yMean);
            }
        }
        return (1.0/(this.num_observations-1))*(tmpCorrelation/s_x*s_y);
    }

}
