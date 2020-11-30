package com.iandadesign.closa.evaluation.featurama.matrix;

import com.iandadesign.closa.evaluation.featurama.observation.Observation;
import com.iandadesign.closa.evaluation.featurama.observation.ObservationHolder;

import java.util.*;
import java.util.stream.IntStream;

public class Matrix {
    private double[][] data = null;
    private int rows = 0;
    private int cols = 0;
    private Boolean empty;

    public Matrix(int rows, int cols) {
        this.data = new double[rows][cols];
        this.rows = rows;
        this.cols = cols;
        this.empty = true;
    }

    public Matrix(double[][] data) {
        this.data = data.clone();
        rows = this.data.length;
        cols = this.data[0].length;
        this.empty = false;
    }

    public Matrix(ObservationHolder observations) {
        this.rows = observations.size();
        this.cols = observations.observations.get(0).returnObservationDim();
        this.data = new double[rows][cols];
        for(int i = 0; i < this.rows; i++)
        {
            this.data[i] = observations.observations.get(i).returnObservationData();
        }
        this.empty = false;
    }

    public int getRowDimension()
    {
        return this.rows;
    }

    public int getColumnDimension()
    {
        return this.cols;
    }

    public double[] returnColumn(int column_num)
    {
        double[] column = new double[this.rows];
        for(int i=0; i<this.rows; i++){
            column[i] = this.data[i][column_num];
        }
        return column;
    }

    public static Matrix identity(int N) {
        Matrix I = new Matrix(N, N);
        for (int i = 0; i < N; i++)
            I.data[i][i] = 1;
        return I;
    }

    public Boolean writeValue(int row, int col, Double value)
    {
        if(row > this.rows || col > this.cols)
        {
            return false;
        }
        this.data[row][col] = value;
        this.empty = false;
        return true;
    }

    public void display() {
        if(this.empty)
        {
            System.out.println("Matrix has no elements to display");
        }
        System.out.print("[");
        for (int row = 0; row < rows; ++row) {
            if (row != 0) {
                System.out.print(" ");
            }

            System.out.print("[");

            for (int col = 0; col < cols; ++col) {
                System.out.printf("%8.3f", data[row][col]);

                if (col != cols - 1) {
                    System.out.print(" ");
                }
            }

            System.out.print("]");

            if (row == rows - 1) {
                System.out.print("]");
            }

            System.out.println();
        }
    }
}
