package org.OC;

import jdk.internal.util.xml.impl.Pair;
import org.jgrapht.alg.CycleDetector;
import org.jgrapht.alg.cycle.TarjanSimpleCycles;
import org.omg.CORBA.INTERNAL;
import org.xmcda.Alternative;
import org.xmcda.QualifiedValue;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.jgrapht.*;
import org.jgrapht.graph.*;


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

        OrderedClutering();


        DefaultDirectedWeightedGraph<String, DefaultWeightedEdge> graph = new DefaultDirectedWeightedGraph<String, DefaultWeightedEdge>(DefaultWeightedEdge.class);

        graph.addVertex("A");
        graph.addVertex("B");
        graph.addVertex("C");

        DefaultWeightedEdge edge;

        edge = graph.addEdge("A", "B");
        graph.setEdgeWeight(edge, -1.0);
        edge = graph.addEdge("B", "C");
        graph.setEdgeWeight(edge, -1.0);
        edge = graph.addEdge("C", "A");
        graph.setEdgeWeight(edge, -1.0);


        //graph.addEdge(alternatives.indexOf("C"), alternatives.indexOf("A"), 1);

        //isCyclic = cycles.detectCycles();


        //graph.addVertex(alternatives.indexOf("D"));

    }


    private void OrderedClutering()
    {
//        ArrayList<ArrayList<Integer>> Marray = new ArrayList<ArrayList<Integer>>();
//        ArrayList<ArrayList<Double>> Iarray = new ArrayList<ArrayList<Double>>();
//
//        for(int i =0; i < alternatives.size(); i++)
//        {
//            Marray.add(i, new ArrayList<Integer>());
//            Iarray.add(i, new ArrayList<Double>());
//
//            for(int j =0; j < alternatives.size(); j++)
//            {
//                Marray.get(i).add(j, 0);
//                Iarray.get(i).add(j, 0.0);
//            }
//        }

        ArrayList<ArrayList<Double>> matrixC = cloneList(matrix);


        Tuple<Integer, Integer> coord = getMaxFrom(matrixC);
//        while(coord.getFirst() >= 0 && coord.getSecond() >= 0)
//        {
//
//
//        }


    }


    private static ArrayList<ArrayList<Double>> cloneList(ArrayList<ArrayList<Double>> list)
    {
        ArrayList<ArrayList<Double>> result = new ArrayList<ArrayList<Double>>(list.size());
        for(int i = 0; i < list.size(); i++)
        {
            result.add(new ArrayList<Double>());
            for(Double x : list.get(i))
            {
                result.get(i).add(x);
            }
        }
        return result;
    }

    private Tuple<Integer, Integer> getMaxFrom(ArrayList<ArrayList<Double>> table)
    {
        Tuple<Integer, Integer> result = new Tuple<Integer, Integer>(-1, -1);

        Double max = 0.0;

        for(int i = 0; i < table.size(); i++)
        {
            for(int j = 0; j < table.size(); j++) {
                if (table.get(i).get(j) > max) {
                    max = table.get(i).get(j);
                    result.set(i, j);
                }
            }
        }

        return result;
    }


}
