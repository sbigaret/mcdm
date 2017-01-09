package org.mcdm;


public class p3clustUI {
    public static void main(String[] args)
    {
        P3clust.xmcdaVersion version = null;
        String input = "";
        String output = "";

        if (args.length == 5)
        {
            for(int i = 0; i < 5; i++)
            {
                switch (args[i])
                {
                    case "--v2":
                        version = P3clust.xmcdaVersion.V2;
                        break;
                    case "--v3":
                        version = P3clust.xmcdaVersion.V3;
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

            P3clust pr = new P3clust();
            pr.Calculate(version, input, output);
        }
    }
}
