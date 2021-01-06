package com.iandadesign.closa.analysis.featurama.PCA;

import com.iandadesign.closa.analysis.featurama.matrix.CovarianceMatrix;
import com.iandadesign.closa.analysis.featurama.matrix.Matrix;

import java.util.Arrays;
import java.util.Locale;


/**
 *
 * @author thomas diewald
 *
 * date: 21.06.2013
 *
 *
 * PCA - Principal Component Analysis
 *
 * works for any number of dimension
 * using: "Jama" for eigenvector calculation
 *
 */

public class PCA {
    int num_data = 0;
    int dim_data = 0;

    Matrix   data;  // input data-vectors
    DwVector mean;  // translation vector
    Matrix   cvmat; // (variance)-covariance-matrix

    EigenvalueDecomposition edec; // eigenvalue decomposition (by Jama)
    DwEigenVector[] evec;         // list of eigenvectors and eigenvalues
    Matrix emat;                  // eigenvector matrix (sorted colums by eigenvalues)

/* ------------------------
   Constructor
 * ------------------------ */

    public PCA(Matrix data) {
        this.num_data = data.getRowDimension();
        this.dim_data = data.getColumnDimension();
        this.data = data;
    }

    public PCA compute() {
        centerData();
        computeCovarianceMatrix();
        updatePCAMatrix();
        setTransformDimension(2);
        return this;
    }

/* ------------------------
   Public Methods
 * ------------------------ */

    DwVector transformVector(DwVector vec) {
        int cols = emat.getColumnDimension();
        int rows = emat.getRowDimension();

        if (cols != vec.v.length) {
            System.out.println("error, cant transform vector");
        }
        double[][] emat_dd = emat.getArray();
        float[] vec_new = new float[rows];

        for (int r = 0; r < rows; r++) {
            float val = 0;
            for (int c = 0; c < cols; c++) {
                val += emat.returnValue(r, c) * vec.v[c];
            }
            vec_new[r] = val;
        }
        return new DwVector(vec_new);
    }

    DwVector[] transformData(DwVector[] data, boolean transpose) {
        Matrix mat = emat;
        if (transpose) {
            mat = emat.transpose();
        }

        final int cols = mat.getColumnDimension();
        final int rows = mat.getRowDimension();

        final double[][] emat_dd = mat.getArray();
        final int num_data = data.length;

        DwVector[] data_new = new DwVector[num_data];
        for (int i = 0; i < num_data; i++) {
            DwVector vec = data[i];

            float[] vec_new = new float[rows];
            for (int r = 0; r < rows; r++) {
                float val = 0;
                for (int c = 0; c < cols; c++) {
                    val += emat_dd[r][c] * vec.v[c];
                }
                vec_new[r] = val;
            }
            data_new[i] = new DwVector(vec_new);
        }

        if (transpose) {
            mat = emat.transpose();
        }

        return data_new;
    }

    void printEigenValuesSorted(){
        System.out.println("sorted eigenvalues: "+evec.length);
        for(int i = 0; i < evec.length; i++) System.out.println("["+i+"] "+evec[i].eval);
    }
    void printEigenVectorsSorted(){
        System.out.println("sorted eigenvectors: "+evec.length);
        for(int i = 0; i < evec.length; i++) evec[i].print();
    }

    // can be used to reduce dimensions.
    // e.g. dimensions with very low eigenvalues can be removed
    // = same, as setting the dimension of the vector to 0, after the
    // transformation
    public void setTransformDimension(int dim_new) {

        // compose new transform matrix of sorted eigenvectors
        // if dim_new is smaller than the original size, than dimensions are reduced!
        double[][] emat_dd = new double[dim_new][dim_data];
        for (int i = 0; i < dim_new; i++) {
            emat_dd[i] = evec[i].evec;
        }
        emat = new Matrix(emat_dd); // to keep edec object untouched
    }

/* ------------------------
   Private Methods
 * ------------------------ */

    private void centerData() {

        // compute mean
        float[] mean_tmp = new float[dim_data];
        for (int i = 0; i < num_data; i++) {
            for (int j = 0; j < dim_data; j++) {
                mean_tmp[j] += data.returnValue(i,j);
            }
        }

        for (int j = 0; j < dim_data; j++) {
            mean_tmp[j] /= (float) num_data;
        }

        // center data (subtract mean) -> mean is at origin now
        for (int i = 0; i < num_data; i++) {
            for (int j = 0; j < dim_data; j++) {
                data.writeValue(i, j, data.returnValue(i,j) - mean_tmp[j]);
            }
        }
        mean = new DwVector(mean_tmp);
    }

    private void computeCovarianceMatrix() {
        cvmat = new CovarianceMatrix(this.data);
    }

    private void updatePCAMatrix() {
        edec = cvmat.eig(); // get eigenvalue decomposition
        emat = edec.getV(); // get eigenvector matrix

        // transpose it, to get eigenvectors from columns
        emat = emat.transpose();

        double[][] emat_dd = emat.getArray();
        double[]   eval = edec.getRealEigenvalues();

        // create objects for sorting
        // columns are eigenvectors ... principal components
        evec = new DwEigenVector[eval.length];
        for (int i = 0; i < evec.length; i++) {
            evec[i] = new DwEigenVector(emat_dd[i], eval[i]);
        }
        Arrays.sort(evec);

        // put the sorted vectors back into the matrix ... use all eigenvectors
        setTransformDimension(dim_data);
    }
}

class DwEigenVector implements Comparable<DwEigenVector>{
    double   eval;
    double[] evec;

    DwEigenVector(double[] evec, double eval){
        this.evec = evec;
        this.eval = eval;
    }
    void print(){
        System.out.printf(Locale.ENGLISH, "%+8.5f,     ", eval);
        for(int i = 0; i < evec.length; i++){
            System.out.printf(Locale.ENGLISH, "%+8.5f, ", evec[i]);
        }
        System.out.printf(Locale.ENGLISH, "\n");
    }

    public int compareTo(DwEigenVector o) {
        if( eval < o.eval ) return +1;
        if( eval > o.eval ) return -1;
        return 0;
    }
}


class DwVector{
    float[] v;

    DwVector(double[] dataVector){
        this.v = new float[dataVector.length];
        for(int i = 0; i < dataVector.length; i++){
            this.v[i] = (float) dataVector[i];
        }
    }

    DwVector(float ... values){
        this.v = new float[values.length];
        for(int i = 0; i < values.length; i++){
            this.v[i] = values[i];
        }
    }

    void print(){
        for(int i = 0; i < v.length; i++){
            System.out.printf(Locale.ENGLISH, "%+8.5f, ", v[i]);
        }
        System.out.printf(Locale.ENGLISH, "\n");
    }

}