package org.mcdm;

import javafx.util.Pair;
import org.xmcda.*;
import org.xmcda.parsers.xml.xmcda_3_0.XMCDAParser;

import java.io.File;
import java.util.*;

public class P3clust {

    protected XMCDA xmcda;
    protected final String randCoreName = "RANDOM";
    protected List<Alternative> currentAlternatives;
    protected AlternativesCriteriaValues currentCriteria;
    protected List<Alternative> centralProfiles;
    protected int K;
    protected String prefix;

    public P3clust()
    {
        xmcda = new XMCDA();
        currentCriteria = new AlternativesCriteriaValues();
        currentAlternatives = new ArrayList<>();
        centralProfiles = new ArrayList<>();
        prefix = UUID.randomUUID().toString();
    }

    public boolean Calculate(String inputPath)
    {
        if (!Init(inputPath))
            return false;

        PrometheeTri engine = new PrometheeTri(xmcda, 2.0);
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

            if (!isValid(profilesData))
                profilesData = RepairData(profilesData);

            if (lastOrder == profilesData)
                flag = false;
            else
                lastOrder = profilesData;

        }while(flag);

        SaveData(profilesData);

        return true;
    }

    private void SaveData(LinkedHashMap<Alternative, List<Alternative>> data)
    {
        for(int i = 0; i < K; i++)
        {
            AlternativesSet set = new AlternativesSet<String>();
            List<Alternative> temp = data.get(centralProfiles.get(i));

            for (Alternative item : temp) {
//                QualifiedValues<String> val = new QualifiedValues<>();
//                val.add(new QualifiedValue<>(String.valueOf(i)));
                set.put(item, null);
            }
            set.setId(String.valueOf(K-i));
            xmcda.alternativesSets.add(set);
        }

        final File plik = new File("result.xml");
        final XMCDAParser parser = new XMCDAParser();
        try {
            parser.writeXMCDA(xmcda, plik.getAbsolutePath(), "alternativesSets");
        }
        catch(Throwable thr)
        {
            return;
        }
    }

    private LinkedHashMap<Alternative, List<Alternative>> RepairData(LinkedHashMap<Alternative, List<Alternative>> data)
    {
        List<Alternative> notEnougth = new ArrayList<>();
        List<Alternative> moreThanRequired = new ArrayList<>();

        for (Map.Entry<Alternative, List<Alternative>> item : data.entrySet())
        {
            if (item.getValue().size() == 0)
                notEnougth.add(item.getKey());
            else if (item.getValue().size() > 1)
                moreThanRequired.add(item.getKey());
        }

        for (Alternative item : notEnougth)
        {
            for (Alternative temp : moreThanRequired)
            {
                if (data.get(temp).size() > 1)
                {
                    data.get(item).add(data.get(temp).get(0));
                    data.get(temp).remove(0);
                    break;
                }
            }
        }

        return data;
    }

    private boolean isValid(LinkedHashMap<Alternative, List<Alternative>> data)
    {
        for (Map.Entry<Alternative, List<Alternative>> item : data.entrySet())
        {
            if (item.getValue().size() == 0)
                return false;
        }
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
                "criteriaValues",
                "programParameters"
        };

        for(String tag : tags)
            LoadData(inputPath.concat(tag).concat(".xml"), tag);

        try
        {
            if (!Validate())
                return false;
        }
        catch (Throwable thr)
        {
            return false;
        }

        CopyToCurrent();

        K = (int)((QualifiedValue)xmcda.programParametersList.get(0).get(0).getValues().get(0)).getValue();
        AddRandomAlternative(K);


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

        int cNum = (int)((QualifiedValue)xmcda.programParametersList.get(0).get(0).getValues().get(0)).getValue();
        if (cNum >= altNum)
            return false;

        return true;
    }

    protected void AddRandomAlternative(int num)
    {
        HashMap<Criterion, Pair<Double, Double>> bounds = GetBounds();

        SetAlternativesWithEmptyCriteria();

        HashMap<Criterion, List<Double>> tempList = getRandomInRange(bounds);

        for(Map.Entry<Criterion, List<Double>> val : tempList.entrySet())
        {
            List<Double> vars = val.getValue();
            Criterion crt = val.getKey();
            Collections.sort(vars);

            QuantitativeScale direction =  (QuantitativeScale)xmcda.criteriaScalesList.get(0).get(crt).get(0);
            if (direction.getPreferenceDirection() == Scale.PreferenceDirection.MAX)
                Collections.reverse(vars);


            for(int i = 0; i < K; i++)
            {
                int index = currentAlternatives.size() - K;
                Alternative alt = currentAlternatives.get(index + i);
                ((QualifiedValue)((LabelledQValues)((CriteriaValues)currentCriteria.get(alt)).get(crt)).get(0)).setValue(vars.get(i));
            }

        }

    }

    protected HashMap<Criterion, Pair<Double, Double>> GetBounds()
    {
        HashMap<Criterion, Pair<Double, Double>> bounds = new HashMap<>();
        for(Criterion crt : xmcda.criteria)
            bounds.put(crt, new Pair<>(Double.MIN_VALUE, Double.MAX_VALUE));

        for (Criterion crt : xmcda.criteria)
        {
            double max = Double.MIN_VALUE;
            double min = Double.MAX_VALUE;
            for (Alternative alt : currentAlternatives)
            {
                LabelledQValues temp = xmcda.alternativesCriteriaValuesList.get(0).get(alt).get(crt);
                double tempVal = (double)((QualifiedValue)temp.get(0)).getValue();

                if(tempVal < min)
                    min = tempVal;
                if(tempVal > max)
                    max = tempVal;
            }

            bounds.put(crt, new Pair<>(min, max));
        }

        return bounds;
    }

    protected void SetAlternativesWithEmptyCriteria()
    {
        for (int i = 0; i < K; i++)
        {
            Alternative alt = new Alternative(prefix.concat(Integer.toString(i)));
            centralProfiles.add(alt);
            currentAlternatives.add(alt);
        }

        for (Alternative alt : centralProfiles)
        {
            org.xmcda.CriteriaValues criteria = new  org.xmcda.CriteriaValues<LabelledQValues<QualifiedValue<Double>>>();
            for (Criterion crt : xmcda.criteria)
                criteria.put(crt,new QualifiedValue(0.0));
            currentCriteria.put(alt, criteria);
        }
    }

    protected HashMap<Criterion, List<Double>> getRandomInRange(HashMap<Criterion, Pair<Double, Double>>  bounds)
    {
        Random engine = new Random();

        HashMap<Criterion, List<Double>> tempList = new HashMap<>();
        for (Criterion crt : xmcda.criteria)
            tempList.put(crt, new ArrayList<>());

        for (int i = 0; i < K; i++)
        {
            for (Criterion crt : xmcda.criteria)
            {
                double randVal = bounds.get(crt).getValue() - bounds.get(crt).getKey();
                randVal = engine.nextInt((int)randVal);
                randVal += bounds.get(crt).getKey();
                tempList.get(crt).add(randVal);
            }
        }
        return tempList;
    }

    protected void CopyToCurrent()
    {
        for(Alternative alt : xmcda.alternatives)
            currentAlternatives.add(alt);

        currentCriteria.putAll(xmcda.alternativesCriteriaValuesList.get(0));
    }
}
