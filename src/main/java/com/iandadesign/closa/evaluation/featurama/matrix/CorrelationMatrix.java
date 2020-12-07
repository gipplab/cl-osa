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
        // TODO: enforce xArray length == num.observations && yArray length == num.observations

        double sx = 0.0, sy = 0.0, sxx = 0.0, syy = 0.0, sxy = 0.0;

        // TODO: eigentlich ist n = num_observations und n^2 könnte abgespeichert werden
        int n = xArray.length;
        int n_2 = n*n;

        for(int i = 0; i < n; i++)
        {
            double x = xArray[i];
            double y = yArray[i];

            sx += x;
            sy += y;
            sxx += x * x;
            syy += y * y;
            sxy += x * y;
        }



        // Todo hier könnte angeknüpft werden für eine Kovarianzmatrix (evtl. Correlationsklasse)
        double kovarianz = sxy / n - sx * sy /(n_2);
        if(Math.abs(kovarianz) < 2 * Double.MIN_VALUE)
        {
            return 0.0;
        }
        double sigma_x = Math.sqrt(sxx / n -  sx * sx /(n_2));
        double sigma_y = Math.sqrt(syy / n -  sy * sy /(n_2));
        return kovarianz / (sigma_x * sigma_y);
    }

}
