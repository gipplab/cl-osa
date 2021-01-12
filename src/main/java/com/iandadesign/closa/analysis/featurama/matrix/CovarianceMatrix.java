package com.iandadesign.closa.analysis.featurama.matrix;

public class CovarianceMatrix extends Matrix
{

    public CovarianceMatrix(int numObservations)
    {
        super(numObservations, numObservations);
    }

    public CovarianceMatrix(Matrix matrix)
    {
        super(matrix.getColumnDimension(), matrix.getColumnDimension());
        computeCovarianceMatrix(matrix);
    }

    public void computeCovarianceMatrix(Matrix matrix) {
        int nVars = matrix.getColumnDimension();

        for (int i = 0; i < nVars; i++) {
            for (int j = 0; j < i; j++) {
                double corr = covariance(matrix.returnColumn(i), matrix.returnColumn(j));
                this.data[i][j] = corr;
                this.data[j][i] = corr;
            }
            this.data[i][i] = 1d;
        }
    }

    private double covariance(final double[] xArray, final double[] yArray) {
        // TODO: enforce xArray length == yArray length
        // TODO: enforce xArray length == num.observations && yArray length == num.observations

        double sx = 0.0, sy = 0.0, sxy = 0.0;

        // TODO: eigentlich ist n = num_observations und n^2 könnte abgespeichert werden
        int n = xArray.length;
        int n_2 = n*n;

        for(int i = 0; i < n; i++)
        {
            double x = xArray[i];
            double y = yArray[i];

            // sx = sum over all x_i
            // sxy = sum over all x_i*y_i
            sx += x;
            sy += y;
            sxy += x * y;
        }

        double kovarianz = sxy / n - sx * sy /(n_2);

        return kovarianz;
    }

}