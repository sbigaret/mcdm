package org.OC;

import org.xmcda.Alternative;
import org.xmcda.AlternativesMatrix;
import org.xmcda.AlternativesSet;
import org.xmcda.AlternativesValues;
import org.xmcda.XMCDA;
import org.xmcda.converters.v2_v3.XMCDAConverter;
import org.xmcda.parsers.xml.xmcda_v3.XMCDAParser;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.AllDirectedPaths;
import org.jgrapht.alg.CycleDetector;
import org.jgrapht.alg.FloydWarshallShortestPaths;
import org.xmcda.*;
import java.io.File;
import java.util.*;

import org.jgrapht.graph.*;
import org.xmcda.v2.*;


public class OrderedClustering {

    private HashMap<String, Double> flows;
    private ArrayList<String> alternatives;
    private XMCDA xmcda;
    private ArrayList<ArrayList<Double>> matrix;
    private int clustersNum = 0;
    public enum xmcdaVersion {V2, V3}

    public ArrayList<ArrayList<Alternative>> mainResult;

    public OrderedClustering()
    {
        xmcda = new XMCDA();
    }

    private boolean LoadFile(String path, String ... typeTag)
    {
        final org.xmcda.parsers.xml.xmcda_v3.XMCDAParser parser = new org.xmcda.parsers.xml.xmcda_v3.XMCDAParser();
        File file = new File(path);

        if(!file.exists())
        {
            return false;
        }

        try
        {
            parser.readXMCDA(xmcda, file, typeTag);
            return true;
        }
        catch (Throwable throwable)
        {
            return false;
        }
    }

    private void LoadData(xmcdaVersion version, String input) {
        if (version == xmcdaVersion.V3) {
            LoadFile(input.concat("preferences_matrix.xml"), "alternativesMatrix");
            LoadFile(input.concat("alternatives.xml"), "alternatives");
            LoadFile(input.concat("program_config.xml"), "programParameters");
        }
        else
        {
            org.xmcda.v2.XMCDA tempXmcda = new org.xmcda.v2.XMCDA();
            tempXmcda = LoadFileObsolete(tempXmcda, input.concat("preferences_matrix.xml"), "alternativesMatrix");
            tempXmcda = LoadFileObsolete(tempXmcda, input.concat("alternatives.xml"), "alternatives");
            tempXmcda = LoadFileObsolete(tempXmcda, input.concat("program_config.xml"), "programParameters");
            xmcda = XMCDAConverter.convertTo_v3(tempXmcda);
        }
    }

    public org.xmcda.v2.XMCDA LoadFileObsolete(org.xmcda.v2.XMCDA xmcdaV2, String path, String... typeTag) {
        File file = new File(path);

        if (file.exists()) {
            try {
                xmcdaV2.getProjectReferenceOrMethodMessagesOrMethodParameters().addAll(org.xmcda.parsers.xml.xmcda_v2.XMCDAParser.readXMCDA(file).getProjectReferenceOrMethodMessagesOrMethodParameters());
            } catch (Throwable thr) {
                System.out.print(thr.getMessage());
            }
        }
        return xmcdaV2;
    }

    private void PrepareData()
    {
        GetAlternatives();
        GetMatrix();
        GetParameters();
    }

    protected HashMap<String, Double>  GetFlows()
    {
        org.xmcda.AlternativesValues flows;

        HashMap<String, Double> result = new HashMap<String, Double>();

        try
        {
            flows = xmcda.alternativesValuesList.get(0).asDouble();

        }
        catch (Throwable ce)
        {
            return null;
        }

        Set<Alternative> asd = flows.getAlternatives();
        for( org.xmcda.Alternative alt : asd)
        {
            try {
                LabelledQValues<QualifiedValue<Double>> something = (LabelledQValues<QualifiedValue<Double>>) flows.get(alt);
                Object obj = something.get(0).getValue();
                result.put(alt.id(), new Double((double)obj));
                //result.add(new Double((double)obj));
            }
            catch (Throwable thr) {
                return null;
            }
        }


        return result;
    }

    private void GetAlternatives()
    {
        try
        {
            alternatives = xmcda.alternatives.getIDs();
        }
        catch(Throwable thr)
        {
        }
    }

    private void GetParameters()
    {
        try
        {
            clustersNum= (Integer) xmcda.programParametersList.get(0).get(0).getValues().get(0).getValue();
        }
        catch (Throwable thr)
        {

        }
    }

    private void GetMatrix()
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


    public void Compute(xmcdaVersion version, String in, String out) {
        LoadData(version, in);
        PrepareData();
        OrderedClutering(version, out);
    }


    private void OrderedClutering(xmcdaVersion version, String out)
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


        xmcda.alternativesSets.clear();
        for (int i = 0; i < clustersNum; i++)
        {
            List<Alternative> partial = getNextResultAlt(graph);
            AlternativesSet set = new AlternativesSet<String>();

            for (Alternative alt : partial)
                set.put(alt, null);

            set.setId(String.valueOf(i));
            xmcda.alternativesSets.add(set);
        }

        if (version == xmcdaVersion.V3) {

            final XMCDAParser parser = new XMCDAParser();
            final File plik = new File(out, "result.xml");
            try {
                parser.writeXMCDA(xmcda, plik.getAbsolutePath(), "alternativesSets");
            } catch (Throwable thr) {
            }
        }
        else
        {
            org.xmcda.v2.XMCDA resXmcda = XMCDAConverter.convertTo_v2(xmcda);
            final File plik = new File(out, "result.xml");
            try {
                org.xmcda.parsers.xml.xmcda_v2.XMCDAParser.writeXMCDA(resXmcda, plik.getAbsolutePath(), "alternativesSets");
            } catch (Throwable thr) {
            }
        }


    }


    private List<Alternative> getNextResultAlt(DefaultDirectedWeightedGraph<String, DefaultWeightedEdge> graph)
    {
        List<Alternative> result = new ArrayList<Alternative>();

        for (String node : graph.vertexSet())
        {
            int in = graph.inDegreeOf(node);
            if (in == 0)
                result.add(new Alternative(node));
        }

        for(Alternative alt : result)
            graph.removeVertex(alt.id());


        return result;
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
