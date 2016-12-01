package org.mcdm;
import org.xmcda.*;
import org.xmcda.v2_2_1.CriteriaValues;

import java.io.File;
import java.util.UUID;
import java.util.Random;

public class P2clust {

    protected XMCDA xmcda;
    protected final String randCoreName = "RANDOM";

    public P2clust()
    {
        xmcda = new XMCDA();
    }

    public boolean Calculate(String inputPath)
    {
        String[] tags = new String[]{
                "alternatives",
                "alternativesCriteriaValues",
                "criteria",
                "criteriaScales",
                "criteriaThresholds"
        };

        for(String tag : tags)
            LoadData(inputPath.concat(tag).concat(".xml"), tag);

        if(!Validate())
            return false;

        for (int i = 0; i < 4; i++)
            RandomAlternative();

        return true;
    }

    protected boolean LoadData(String path, String tag)
    {
        final org.xmcda.parsers.xml.xmcda_3_0.XMCDAParser parser = new org.xmcda.parsers.xml.xmcda_3_0.XMCDAParser();
        File file = new File(path);

        if(!file.exists())
            return false;

        try
        {
            parser.readXMCDA(xmcda, file, tag);
            return true;
        }
        catch (Throwable thr)
        {
            return false;
        }
    }

    protected boolean Validate()
    {
        int critNum = xmcda.criteria.size();
        int critThrNum = xmcda.criteriaThresholdsList.size();
        if (critThrNum > 0)
            critThrNum = xmcda.criteriaThresholdsList.get(0).size();
        if (critNum != critThrNum)
            return false;

        int altNum = xmcda.alternatives.size();
        int altCritValNum = xmcda.alternativesCriteriaValuesList.size();
        if (altCritValNum > 0)
            altCritValNum = xmcda.alternativesCriteriaValuesList.get(0).size();
        if (altCritValNum != altNum)
            return false;

        int critScalNum = xmcda.criteriaScalesList.size();
        if (critScalNum > 0)
            critScalNum = xmcda.criteriaScalesList.get(0).size();
        if (critNum != critScalNum)
            return false;

        return true;
    }

    protected void PrepareIteration()
    {

    }

    protected boolean RandomAlternative()
    {
        if (!xmcda.alternativesSets.contains(randCoreName))
            xmcda.alternativesSets.add(new AlternativesSet(randCoreName));
        if (xmcda.alternativesSets.size() < 1)
            return false;
        Alternative alt = new Alternative(UUID.randomUUID().toString());
        xmcda.alternativesSets.get(randCoreName).put(alt, null);

        if(xmcda.alternativesCriteriaValuesList.size() < 2)
            xmcda.alternativesCriteriaValuesList.add(new AlternativesCriteriaValues());

        org.xmcda.CriteriaValues crit = new  org.xmcda.CriteriaValues<Double>();
        Random generator = new Random();
        for (Object c : xmcda.criteria.toArray())
            crit.put(c, (double)generator.nextInt() );
        xmcda.alternativesCriteriaValuesList.get(1).put(alt, crit);

        return true;
    }
}
