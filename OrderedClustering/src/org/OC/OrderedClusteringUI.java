package org.OC;



public class OrderedClusteringUI {

    public static void main(String[] args)
    {
        OrderedClustering oc = new OrderedClustering();
        oc.Compute(OrderedClustering.xmcdaVersion.V3, "/home/mateusz/Development/mcdm/OrderedClustering/test/v3/", "/home/mateusz/Development/mcdm/OrderedClustering/test/v3out/");
    }
}
