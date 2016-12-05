package org.mcdm;

import org.xmcda.*;

import java.io.File;
import java.util.*;

public class P2clust {

    protected XMCDA xmcda;
    protected final String randCoreName = "RANDOM";
    protected List<Alternative> currentAlternatives;
    protected AlternativesCriteriaValues currentCriteria;
    protected List<Alternative> centralProfiles;

    public P2clust()
    {
        xmcda = new XMCDA();
        currentCriteria = new AlternativesCriteriaValues();
        currentAlternatives = new ArrayList<>();
        centralProfiles = new ArrayList<>();
    }

    public boolean Calculate(String inputPath)
    {
        if (!Init(inputPath))
            return false;

        PrometheeII engine = new PrometheeII(xmcda);
        engine.SetCriteria(currentCriteria);

        LinkedHashMap<Alternative, List<Alternative>> lastOrder = new LinkedHashMap<>();
        LinkedHashMap<Alternative, List<Alternative>> profilesData = new LinkedHashMap<>();
        boolean flag = true;
        do {
            List<Alternative> tempAlternatives = new ArrayList<>(currentAlternatives);
            for(Alternative alt : centralProfiles)
            {
                profilesData.put(alt, new ArrayList<>());
                tempAlternatives.remove(alt);
            }

            for (Alternative alt : tempAlternatives)
            {
                Alternative prof = engine.FindClosest(centralProfiles, alt);
                profilesData.get(prof).add(alt);
            }

            UpdateCentralProfiles(profilesData);

            if (lastOrder == profilesData)
                flag = false;
            else
                lastOrder = profilesData;

        }while(flag);

        return true;
    }

    private void UpdateCentralProfiles(LinkedHashMap<Alternative, List<Alternative>> profilesData)
    {
        ArrayList<Alternative> list = new ArrayList<>();

        for (Map.Entry<Alternative, List<Alternative>> l : profilesData.entrySet())
        {
            Alternative key = l.getKey();
            List<Alternative> value = l.getValue();

            if(value.size() == 0)
                continue;


            UpdateCriteriaValue(value, key);

        }

    }

    private void UpdateCriteriaValue(List<Alternative> list, Alternative profile)
    {
        for (Criterion crt : xmcda.criteria)
        {
            Double partialRes = 0.0;
            for (Alternative alt : list)
            {
                CriteriaValues crVal = (CriteriaValues)currentCriteria.get(alt);
                LabelledQValues LabQVal = (LabelledQValues)crVal.get(crt);
                QualifiedValue QV = (QualifiedValue)LabQVal.get(0);
                Double value = (double)QV.getValue();
                partialRes += value;
            }
            partialRes /= (double)list.size();

            CriteriaValues crVal = (CriteriaValues)currentCriteria.get(profile);
            LabelledQValues LabQVal = (LabelledQValues)crVal.get(crt);
            LabQVal.set(0, new QualifiedValue<Double>(partialRes));
        }
    }

    private boolean Init(String inputPath)
    {
        String[] tags = new String[]{
                "alternatives",
                "alternativesCriteriaValues",
                "criteria",
                "criteriaScales",
                "criteriaThresholds",
                "criteriaValues"
        };

        for(String tag : tags)
            LoadData(inputPath.concat(tag).concat(".xml"), tag);

        if(!Validate())
            return false;

        CopyToCurrent();

        for (int i = 0; i < 3; i++)
            AddRandomAlternative();

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

    protected void AddRandomAlternative()
    {

        Alternative alternative = new Alternative(UUID.randomUUID().toString());
        currentAlternatives.add(alternative);

        org.xmcda.CriteriaValues criteria = new  org.xmcda.CriteriaValues<LabelledQValues<QualifiedValue<Double>>>();

        Random generator = new Random();
        for (Object c : xmcda.criteria.toArray())
            criteria.put(c, new LabelledQValues(new QualifiedValue<Double>(new Double(generator.nextInt()))));

        currentCriteria.put(alternative, criteria);

        centralProfiles.add(alternative);

    }

    protected void CopyToCurrent()
    {
        for(Alternative alt : xmcda.alternatives)
            currentAlternatives.add(alt);

        currentCriteria.putAll(xmcda.alternativesCriteriaValuesList.get(0));
    }
}
