package put.poznan.pl;


public class p2clustUI {
    public static void main(String[] args)
    {
        P2clust.xmcdaVersion version = null;
        String input = "";
        String output = "";

        if (args.length == 5) {
            for (int i = 0; i < 5; i++) {
                switch (args[i]) {
                    case "--v2":
                        version = P2clust.xmcdaVersion.V2;
                        break;
                    case "--v3":
                        version = P2clust.xmcdaVersion.V3;
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

            P2clust pr = new P2clust();
            pr.Calculate(version, input, output);
            pr.GetStatus(version, output);
        }
    }
}
