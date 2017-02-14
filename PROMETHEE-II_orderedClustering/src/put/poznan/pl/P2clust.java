package put.poznan.pl;

import javafx.util.Pair;
import org.xmcda.*;
import org.xmcda.Alternative;
import org.xmcda.AlternativesSet;
import org.xmcda.Criterion;
import org.xmcda.PerformanceTable;
import org.xmcda.Scale;
import org.xmcda.XMCDA;
import org.xmcda.converters.v2_v3.XMCDAConverter;
import org.xmcda.parsers.xml.xmcda_v3.XMCDAParser;

import java.io.File;
import java.util.*;

public class P2clust {

    protected XMCDA xmcda;
    protected final String randCoreName = "RANDOM";
    private List<Alternative> currentAlternatives;
    private PerformanceTable currentCriteria;
    private List<Alternative> centralProfiles;
    private int K;
    private String prefix;
    private ProgramExecutionResult execResult = new ProgramExecutionResult();

    public enum xmcdaVersion {V2, V3};

    public P2clust()
    {
        xmcda = new XMCDA();
        currentCriteria = new PerformanceTable();
        currentAlternatives = new ArrayList<>();
        centralProfiles = new ArrayList<>();
        prefix = UUID.randomUUID().toString();
    }

    public boolean Calculate(xmcdaVersion version, String inputPath, String outputPath)
    {
        if (!Init(inputPath, version))
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
                try {
                    Alternative prof = engine.FindClosest(centralProfiles, alt);
                    profilesData.get(prof).add(alt);
                }
                catch (Exception exc)
                {
                    execResult.addError("[PrometheeII] Promethee II engine error");
                    return false;
                }
            }

            UpdateCentralProfiles(profilesData);

            if (!isValid(profilesData))
                profilesData = RepairData(profilesData);

            if (lastOrder == profilesData)
                flag = false;
            else
                lastOrder = profilesData;

        }while(flag);

        SaveData(profilesData, outputPath, version);

        return true;
    }

    private void SaveData(LinkedHashMap<Alternative, List<Alternative>> data, String path, xmcdaVersion version)
    {
        for(int i = 0; i < K; i++)
        {
            AlternativesSet set = new AlternativesSet<String>();
            List<Alternative> temp = data.get(centralProfiles.get(i));

            for (Alternative item : temp) {
                set.put(item, null);
            }
            set.setId(String.valueOf(i));
            xmcda.alternativesSets.add(set);
        }

        final File plik = new File(path, "result.xml");
        try {
            if (version == xmcdaVersion.V3) {
                final XMCDAParser parser = new XMCDAParser();
                parser.writeXMCDA(xmcda, plik.getAbsolutePath(), "alternativesSets");
            }
            else
            {
                org.xmcda.v2.XMCDA oldXmcda = new org.xmcda.v2.XMCDA();
                oldXmcda = XMCDAConverter.convertTo_v2(xmcda);
                org.xmcda.parsers.xml.xmcda_v2.XMCDAParser.writeXMCDA(oldXmcda, plik.getAbsoluteFile(), "alternativesSets");
            }
        }
        catch(Throwable thr)
        {
            execResult.addError("[Save] Save output data error.");
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
                QualifiedValues qvals = (QualifiedValues)currentCriteria.get(alt, crt);
                QualifiedValue qv = (QualifiedValue)qvals.get(0);
                partialRes += (double)qv.getValue();
            }
            partialRes /= (double)list.size();



            QualifiedValues vals = currentCriteria.get(profile, crt);
            QualifiedValue crtVal = (QualifiedValue)vals.get(0);
            crtVal.setValue(partialRes);
        }
    }

    private boolean Init(String inputPath, xmcdaVersion version)
    {
        if (version == xmcdaVersion.V3) {
            String[] tags = new String[]{
                    "alternatives",
                    "criteria",
                    "criteriaScales",
                    "criteriaThresholds",
                    "criteriaValues",
                    "programParameters",
                    "performanceTable"
            };

            String[] filenames = new String[]{
                    "alternatives.xml",
                    "criteria.xml",
                    "criteria.xml",
                    "criteria.xml",
                    "criteriaValues.xml",
                    "programParameters.xml",
                    "performanceTable.xml"
            };
            for (int i = 0; i < filenames.length; i++)
                LoadData(inputPath.concat("/").concat(filenames[i]), tags[i]);
        }
        else {
            String[] tagsV2 = new String[]{
                    "alternatives",
                    "criteria",
                    "criteriaValues",
                    "methodParameters",
                    "performanceTable"
            };
            String[] filenamesV2 = new String[]{
                    "alternatives.xml",
                    "criteria.xml",
                    "criteriaValues.xml",
                    "programParameters.xml",
                    "performanceTable.xml"
            };

            org.xmcda.v2.XMCDA oldXMCDA = new org.xmcda.v2.XMCDA();
            for (int i = 0; i < filenamesV2.length; i++)
                oldXMCDA = ObsoleteLoadData(oldXMCDA, inputPath.concat("/").concat(filenamesV2[i]), tagsV2[i]);
            xmcda = XMCDAConverter.convertTo_v3(oldXMCDA);
        }

        try
        {
            if (!Validate())
                return false;
        }
        catch (Throwable thr)
        {
            execResult.addError("[Validate] Input data validation error");
            return false;
        }

        CopyToCurrent();

        K = (int)((QualifiedValue)xmcda.programParametersList.get(0).get(0).getValues().get(0)).getValue();
        AddRandomAlternative(K);


        return true;
    }

    private org.xmcda.v2.XMCDA ObsoleteLoadData(org.xmcda.v2.XMCDA xmcdaV2, String path, String tag)
    {
        File file = new File(path);

        if (file.exists()) {
            try {
                xmcdaV2.getProjectReferenceOrMethodMessagesOrMethodParameters().addAll(org.xmcda.parsers.xml.xmcda_v2.XMCDAParser.readXMCDA(file).getProjectReferenceOrMethodMessagesOrMethodParameters());
            } catch (Throwable thr) {
                execResult.addError("[LoadV2] Load data error.");
            }
        }
        return xmcdaV2;
    }

    private boolean LoadData(String path, String tag)
    {
        final org.xmcda.parsers.xml.xmcda_v3.XMCDAParser parser = new org.xmcda.parsers.xml.xmcda_v3.XMCDAParser();
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
            execResult.addError("[LoadV3] Load data error");
            return false;
        }
    }

    private boolean Validate()
    {
        int critNum = xmcda.criteria.size();
        int critThrNum = xmcda.criteriaThresholdsList.size();
        if (critThrNum > 0)
            critThrNum = xmcda.criteriaThresholdsList.get(0).size();
        if (critNum != critThrNum)
            return false;

        int altNum = xmcda.alternatives.size();
        int altCritValNum = xmcda.performanceTablesList.size();
        if (altCritValNum > 0)
            altCritValNum = xmcda.performanceTablesList.get(0).size();
        if (altCritValNum != altNum*critNum)
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

    private void AddRandomAlternative(int num)
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
                QualifiedValue<Double> result = new QualifiedValue<Double>();
                result.setValue(vars.get(i));
                currentCriteria.get(alt, crt).add(result);

            }

        }

    }

    private HashMap<Criterion, Pair<Double, Double>> GetBounds()
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

                try {
                    QualifiedValue<Double> temp = xmcda.performanceTablesList.get(0).get(alt, crt).get(0).convertToDouble();
                    double tempVal = (double)temp.getValue();

                    if(tempVal < min)
                        min = tempVal;
                    if(tempVal > max)
                        max = tempVal;
                }
                catch(Throwable thr){
                    execResult.addError("[Compute] Computation error");
                    System.out.print(thr.getMessage());
                }
            }

            bounds.put(crt, new Pair<>(min, max));
        }

        return bounds;
    }

    private void SetAlternativesWithEmptyCriteria()
    {
        for (int i = 0; i < K; i++)
        {
            Alternative alt = new Alternative(prefix.concat(Integer.toString(i)));
            centralProfiles.add(alt);
            currentAlternatives.add(alt);
        }

        for (Alternative alt : centralProfiles)
        {
            for (Criterion crt : xmcda.criteria)
            {
                currentCriteria.put(alt, crt, new QualifiedValues<>());
            }
        }
    }

    private HashMap<Criterion, List<Double>> getRandomInRange(HashMap<Criterion, Pair<Double, Double>>  bounds)
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

    private void CopyToCurrent()
    {
        for(Alternative alt : xmcda.alternatives)
            currentAlternatives.add(alt);

        currentCriteria.putAll(xmcda.performanceTablesList.get(0));
    }

    public void GetStatus(xmcdaVersion version, String outputPath)
    {
        org.xmcda.parsers.xml.xmcda_v3.XMCDAParser parser = new org.xmcda.parsers.xml.xmcda_v3.XMCDAParser();


        if (execResult.getStatus() == ProgramExecutionResult.Status.OK || execResult.getStatus() == ProgramExecutionResult.Status.WARNING)
        {
            execResult.addInfo("Success");
        }

        XMCDA prgExecResults = new XMCDA();
        prgExecResults.programExecutionResultsList.add(execResult);

        try {
            if(version == xmcdaVersion.V3)
            {
                parser.writeXMCDA(prgExecResults, outputPath.concat("/messages.xml"), "programExecutionResult");
            }
            else
            {
                org.xmcda.v2.XMCDA xmcda_v2 = XMCDAConverter.convertTo_v2(prgExecResults);
                org.xmcda.parsers.xml.xmcda_v2.XMCDAParser.writeXMCDA(xmcda_v2, outputPath.concat("/messages.xml"), "methodMessages");
            }
        }
        catch (Exception ex)
        {

        }
    }
}
