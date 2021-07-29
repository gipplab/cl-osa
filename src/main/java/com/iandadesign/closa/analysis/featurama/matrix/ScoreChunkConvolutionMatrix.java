package com.iandadesign.closa.analysis.featurama.matrix;
import com.iandadesign.closa.model.ScoringChunk;

import java.util.TreeMap;


/**
 * This is a class for computing a convolution matrix of the cosine scores
 * This follows a sparse matrix logic, where only index and value for all
 * non zero elements are stored
 *
 * @author Kay Herklotz
 *
 */
public class ScoreChunkConvolutionMatrix{

    private ScoringChunk[][] Scores;
    private int convolutionSize;
    private int convolutionWidth;
    private int rowsLength;
    private int colsLength;
    TreeMap<Integer, Double> data = new TreeMap<>();

/* ------------------------
   Constructor
 * ------------------------ */

    public ScoreChunkConvolutionMatrix(ScoringChunk[][] Scores, int convolutionSize)
    {
        this.rowsLength = Scores.length-1;
        this.colsLength = Scores[0].length-1;
        this.Scores = Scores;
        this.convolutionSize = convolutionSize;
        this.convolutionWidth = (convolutionSize-1)/2;
    }

/* ------------------------
   Public Methods
 * ------------------------ */

    public void computeConvolution()
    {
        double tmpConvolution = 0.0;
        for(int i = 0; i < this.rowsLength; i++)
        {
            for(int j = 0; j<this.colsLength;j++)
            {
                tmpConvolution = computeConvolution(i, j);
                if(tmpConvolution != 0.0)
                {
                    data.put(j+i*(this.colsLength+1), tmpConvolution);
                }
            }
        }
    }

    public double returnValue(int Index)
    {
        if(data.containsKey(Index))
        {
            return data.get(Index);
        }
        return 0.0;
    }

/* ------------------------
   Private Methods
 * ------------------------ */

    private double computeConvolution(int row, int column){
        double result = 0.0;
        for(int i = row-this.convolutionWidth; i < row+this.convolutionWidth; i++)
        {
            for(int j = column-this.convolutionWidth; j < column+this.convolutionWidth; j++)
            {
                if(notNullAndInRange(i,j)){
                    if(Scores[i][j].getComputedCosineSimilarity() >=0.0)
                        result += Scores[i][j].getComputedCosineSimilarity();
                }
            }
        }
        return result;
    }

    private Boolean notNullAndInRange(int row, int column)
    {
        if(row<0 || column<0)
            return false;
        if(row>this.rowsLength || column>this.colsLength)
            return false;
        if(Scores[row][column] == null)
            return false;

        return true;
    }


}
