package org.PromV;


import java.util.ArrayList;
import java.util.Arrays;



public class PromVUI {

    public static void main(String[] args)
    {
        PrometheeV pr = new PrometheeV();
        pr.Compute(PrometheeV.xmcdaVersion.V3, "/home/mateusz/Development/mcdm/PrometheeV/tests/v3_1", "/home/mateusz/Development/mcdm/PrometheeV/tests/v3_1out");
    }
}
