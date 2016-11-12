package org.OC;


import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.AllDirectedPaths;
import org.jgrapht.alg.CycleDetector;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.alg.FloydWarshallShortestPaths;
import org.xmcda.Alternative;
import org.xmcda.QualifiedValue;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import org.jgrapht.graph.*;
import java.util.Set;
import java.util.HashSet;


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
        OrderedClutering();
    }


    private void OrderedClutering()
    {

        DefaultDirectedWeightedGraph<String, DefaultWeightedEdge> graph = new DefaultDirectedWeightedGraph<String, DefaultWeightedEdge>(DefaultWeightedEdge.class);
        CycleDetector detector = new CycleDetector(graph);
        AllDirectedPaths path = new AllDirectedPaths(graph);

        Iterator<String> iter = alternatives.iterator();
        while (iter.hasNext())
            graph.addVertex(iter.next());


        ArrayList<ArrayList<Double>> matrixC = cloneList(matrix);
        Tuple<Integer, Integer> coord = getMaxFrom(matrixC);

        while (coord.getFirst() >= 0 && coord.getSecond() >= 0)
        {
            String from = alternatives.get(coord.getFirst());
            String to = alternatives.get(coord.getSecond());

            DefaultWeightedEdge edge;

            edge = graph.addEdge(from, to);
            if (edge == null)
                return;
            graph.setEdgeWeight(edge, -1.0);

            if (detector.findCycles().size() == 0)
            {
                FloydWarshallShortestPaths<String, DefaultWeightedEdge> alg = new FloydWarshallShortestPaths<>(graph);
                List<GraphPath<String, DefaultWeightedEdge>> res2 = alg.getShortestPaths();

                Iterator<GraphPath<String, DefaultWeightedEdge>> findM = res2.iterator();
                Integer max = 0;
                while(findM.hasNext())
                {
                    List<String> temp = findM.next().getVertexList();
                    if (temp.size() > max)
                        max = temp.size();
                }

                if (max > clustersNum)
                {
                    graph.removeEdge(edge);
                }

                System.out.print("asd");
            }
            else
            {
                graph.removeEdge(edge);
            }

            matrixC.get(coord.getFirst()).set(coord.getSecond(), 0.0);
            coord = getMaxFrom(matrixC);
        }

        System.out.print("asd");

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
