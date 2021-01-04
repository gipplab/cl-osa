package com.iandadesign.closa.evaluation.featurama.matrix;

public class CorrelationMatrix extends Matrix
{

/* ------------------------
   Constructor
 * ------------------------ */

    public CorrelationMatrix(int numObservations)
    {
        super(numObservations, numObservations);
    }

    public CorrelationMatrix(Matrix matrix)
    {
        super(matrix.getColumnDimension(), matrix.getColumnDimension());
        computeCorrelationMatrix(matrix);
    }

/* ------------------------
   Public Methods
 * ------------------------ */

    public void computeCorrelationMatrix(Matrix matrix)
    {
        int nVars = matrix.getColumnDimension();

        for (int i = 0; i < nVars; i++) {
            for (int j = 0; j < i; j++) {
                double corr = correlation(matrix.returnColumn(i), matrix.returnColumn(j));
                this.data[i][j] = corr;
                this.data[j][i] = corr;
            }
            this.data[i][i] = 1d;
        }
    }

    public double correlation(final double[] xArray, final double[] yArray) {
        // TODO: enforce xArray length == yArray length
        // TODO: enforce xArray length == num.observations && yArray length == num.observations

        double sx = 0.0, sy = 0.0, sxx = 0.0, syy = 0.0, sxy = 0.0;

        // TODO: eigentlich ist n = num_observations und n^2 könnte abgespeichert werden
        int n = xArray.length;
        int n_2 = n*n;

        // Namenskonvention:
        // sx = summe über alle x_i
        // sxy = summe über alle x_i*y_i
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

        if (sigma_x == 0 || sigma_y == 0)
        {
            return 0.0;
        }

        return kovarianz / (sigma_x * sigma_y);
    }

}
