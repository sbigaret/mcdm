package org.PromV;


import java.util.ArrayList;
import java.util.Arrays;



public class PromVUI {

    public static void main(String[] args)
    {
        PrometheeV pr = new PrometheeV();
        pr.Compute(PrometheeV.xmcdaVersion.V2, "/home/mateusz/Development/mcdm/PrometheeV/tests/v2", "/home/mateusz/Development/mcdm/PrometheeV/tests/v2out");
    }
}
