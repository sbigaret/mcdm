package org.OC;



public class OrderedClusteringUI {

    public static void main(String[] args)
    {
        OrderedClustering.xmcdaVersion version = null;
        String input = "";
        String output = "";

        if (args.length == 5) {
            for (int i = 0; i < 5; i++) {
                switch (args[i]) {
                    case "--v2":
                        version = OrderedClustering.xmcdaVersion.V2;
                        break;
                    case "--v3":
                        version = OrderedClustering.xmcdaVersion.V3;
                        break;
                    case "-i":
                        i++;
                        if (i < 5)
                            input = args[i];
                        break;
                    case "-o":
                        i++;
                        if (i < 5)
                            output = args[i];
                        break;
                }
            }

            if (input == "" || output == "" || version == null)
                return;

            OrderedClustering pr = new OrderedClustering();
            pr.Compute(version, input, output);
        }
    }
}
