package org.OC;


public class OrderedClustering extends Core{

    @Override
    protected void LoadData(String input) {
        LoadFile(input.concat("preference_matrix.xml"), "performanceTableList");
    }

    @Override
    public void Compute(String in, String out) {
        LoadData(in);
        System.out.print("asd");
    }
}
