package org.OC;


import org.xmcda.Alternative;
import org.xmcda.Alternatives;
import org.xmcda.AlternativesSetsMatrix;
import org.xmcda.QualifiedValue;
import org.xmcda.parsers.xml.xmcda_3_0.AlternativesMatrixParser;
import org.xmcda.utils.Coord;
import org.xmcda.v2_2_1.AlternativesMatrix;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

public class OrderedClustering extends Core{

    protected ArrayList<ArrayList<Double>> matrix;
    protected int clustersNum = 0;

    @Override
    protected void LoadData(String input) {
        LoadFile(input.concat("preferences_matrix.xml"), "alternativesMatrix");
        LoadFile(input.concat("alternatives.xml"), "alternatives");
        LoadFile(input.concat("program_config.xml"), "programParameters");
    }

    protected void PrepareData()
    {
        GetAlternatives();
        GetMatrix();
        GetParameters();
    }

    protected void GetParameters()
    {
        try
        {
            clustersNum= (Integer) xmcda.programParametersList.get(0).get(0).getValues().get(0).getValue();
        }
        catch (Throwable thr)
        {

        }
    }

    protected void GetMatrix()
    {
        matrix = new ArrayList<ArrayList<Double>>();

        for(int i =0; i < alternatives.size(); i++)
        {
            matrix.add(i, new ArrayList<Double>());  //= ;

            for(int j =0; j < alternatives.size(); j++)
            {
                matrix.get(i).add(j, new Double(0.0));
            }
        }

        List type = xmcda.alternativesMatricesList;
        org.xmcda.AlternativesMatrix<LinkedHashMap<Object, Object>> alt;

        try
        {
            alt = (org.xmcda.AlternativesMatrix<LinkedHashMap<Object, Object>>) type.get(0);

            for(String row : alternatives)
            {
                for(String col : alternatives) {
                    Alternative arow = new Alternative(row);
                    Alternative acol = new Alternative(col);
                    QualifiedValue asd = alt.get(arow, acol).get(0);
                    Double value = (Double) asd.getValue();
                    matrix.get(alternatives.indexOf(row)).set(alternatives.indexOf(col), value);
                }
            }

        }
        catch (Throwable thr)
        {
            return;
        }



        //LinkedHashMap<Coord<Alternative, Alternative>, QualifiedValue<Double>>>
    }

    @Override
    public void Compute(String in, String out) {
        LoadData(in);
        PrepareData();
        System.out.print("asd");
    }
}
