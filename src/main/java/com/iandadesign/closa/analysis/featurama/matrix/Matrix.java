package com.iandadesign.closa.analysis.featurama.matrix;

import com.iandadesign.closa.analysis.featurama.PCA.EigenvalueDecomposition;
import com.iandadesign.closa.analysis.featurama.observation.ObservationHolder;
import com.iandadesign.closa.util.CSVUtil;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Matrix {

    protected double[][] data = null;
    private int rowsLength = 0;
    private int colsLength = 0;
    private Boolean empty;
    public ArrayList<String> columnNames = new ArrayList<>();

/* ------------------------
   Constructor
 * ------------------------ */

    public Matrix(int rows, int cols)
    {
        this.data = new double[rows][cols];
        this.rowsLength = rows;
        this.colsLength = cols;
        this.empty = true;
    }

    public Matrix(double[][] data)
    {
        this.data = data.clone();
        rowsLength = this.data.length;
        colsLength = this.data[0].length;
        this.empty = false;
    }

    public Matrix(ObservationHolder observations) {
        this.rowsLength = observations.size();
        this.colsLength = observations.observations.get(0).returnObservationDim();
        this.data = new double[rowsLength][colsLength];
        for(int i = 0; i < this.rowsLength; i++)
        {
            this.data[i] = observations.observations.get(i).returnObservationData(observations.dataNames);
        }
        setColumnNames(observations.dataNames);
        this.empty = false;
    }

/* ------------------------
   Public Methods
 * ------------------------ */

    public int getRowDimension()
    {
        return this.rowsLength;
    }

    public int getColumnDimension()
    {
        return this.colsLength;
    }

    public double returnValue(int row_num, int column_num)
    {
        return data[row_num][column_num];
    }

    public double[][] getArray() {
        return data;
    }

    public double[] returnColumn(int column_num)
    {
        double[] column = new double[this.rowsLength];
        for(int i=0; i<this.rowsLength; i++){
            column[i] = this.data[i][column_num];
        }
        return column;
    }

    public double[] returnRow(int row_num)
    {
        double[] row = new double[this.colsLength];
        for(int i=0; i<this.rowsLength; i++){
            row[i] = this.data[row_num][i];
        }
        return row;
    }

    public Boolean writeValue(int row, int col, Double value)
    {
        if(row > this.rowsLength || col > this.colsLength)
        {
            return false;
        }
        this.data[row][col] = value;
        this.empty = false;
        return true;
    }

    public void setColumnNames(ArrayList<String> columnNames)
    {
        this.columnNames = columnNames;
    }

    public Matrix transpose()
    {
        Matrix transposed = new Matrix(this.rowsLength, this.colsLength);
        for(int row = 0; row<this.rowsLength; row++)
        {
            for(int col = 0; col<this.colsLength; col++)
            {
                transposed.writeValue(row, col, this.data[col][row]);
            }
        }
        return transposed;
    }

    public static Matrix identity(int N) {
        Matrix I = new Matrix(N, N);
        for (int i = 0; i < N; i++)
            I.data[i][i] = 1;
        return I;
    }

    public void display() {
        if(this.empty)
        {
            System.out.println("Matrix has no elements to display");
        }
        System.out.print("[");
        for (int row = 0; row < rowsLength; ++row) {
            if (row != 0) {
                System.out.print(" ");
            }

            System.out.print("[");

            for (int col = 0; col < colsLength; ++col) {
                System.out.printf("%8.3f", data[row][col]);

                if (col != colsLength - 1) {
                    System.out.print(" ");
                }
            }

            System.out.print("]");

            if (row == rowsLength - 1) {
                System.out.print("]");
            }

            System.out.println();
        }
    }

    public void saveMatrixToFile(String dir,String filename) throws IOException
    {
        String final_filename = dir + '/' + filename + ".csv";

        String[] file_content = writeMatrixToStringArray();

        BufferedWriter writer = new BufferedWriter(new FileWriter(final_filename));

        for (String line : file_content)
        {
            writer.write(line);
            writer.write("\n");
        }

        writer.flush();
        writer.close();
    }

    public EigenvalueDecomposition eig()
    {
        return new EigenvalueDecomposition(this);
    }

/* ------------------------
   Private Methods
 * ------------------------ */

    private static String[] getStrings(double[] a) {
        String[] output = new String[a.length];
        int i = 0;
        for (double d : a)
        {
            output[i++] = Double.toString(d);
        }
        return output;
    }

    private String[] writeMatrixToStringArray()
    {
        // TODO throw exception if matrix empty

        String[] matrix_string_values = new String[this.rowsLength]; // TODO Array -> ArrayList :)
        int offset = 0;

        if ((this.columnNames != null) && !this.columnNames.isEmpty())
        {
            matrix_string_values = new String[this.rowsLength + 1];
            matrix_string_values[0] = CSVUtil.convertToCSV(this.columnNames.toArray(new String[this.columnNames.size()]));
            offset = 1;
        }

        String[] string_array;
        for(int i = 0; i < this.rowsLength; i++)
        {
            string_array = getStrings(returnRow(i));
            matrix_string_values[i+offset] = CSVUtil.convertToCSV(string_array);
        }

        return matrix_string_values;
    }
}
