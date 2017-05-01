package put.poznan.pl;

import org.xmcda.*;
import org.xmcda.converters.v2_v3.XMCDAConverter;
import org.xmcda.parsers.xml.xmcda_v3.XMCDAParser;

import java.io.File;
import java.util.*;

public class P3clust {

    protected XMCDA xmcda;
    protected final String randCoreName = "RANDOM";
    private List<Alternative> currentAlternatives;
    private PerformanceTable currentCriteria;
    private List<Alternative> centralProfiles;
    private int NumberOfClustersParam;
    private int DistanceParam;
    private String prefix;
    private ProgramExecutionResult execResult = new ProgramExecutionResult();
    private Integer randomSeed = null;

    public enum xmcdaVersion {V2, V3};

    public P3clust()
    {
        xmcda = new XMCDA();
        currentCriteria = new PerformanceTable();
        currentAlternatives = new ArrayList<>();
        centralProfiles = new ArrayList<>();
        prefix = UUID.randomUUID().toString();
    }

    public boolean Calculate(xmcdaVersion version, String inputPath, String outputPath)
    {
        if (!Init(inputPath, version)) {
            if (!execResult.isError())
                execResult.addError("[InitData] Error while initialize data");
            return false;
        }

        PrometheeTri engine = new PrometheeTri(xmcda, DistanceParam);
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
                catch(Exception exc)
                {
                    execResult.addError("[PrometheeTri] Engine error");
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
        for(int i = 0; i < NumberOfClustersParam; i++)
        {
            AlternativesSet set = new AlternativesSet<String>();
            List<Alternative> temp = data.get(centralProfiles.get(i));

            for (Alternative item : temp) {
                set.put(item, null);
            }
            set.setId(String.valueOf(NumberOfClustersParam -i));
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
            execResult.addError("[SaveData] Save data error");
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


            UpdateCriteriaValueMedian(value, key);

        }

    }

    private void UpdateCriteriaValueMedian(List<Alternative> list, Alternative profile)
    {
        for (Criterion crt : xmcda.criteria)
        {
            ArrayList<Double> tempList = new ArrayList<Double>();

            double partialRes = 0;
            Double tempVal = 0.0;

            for (Alternative alt : list)
            {
                QualifiedValues qvals = (QualifiedValues)currentCriteria.get(alt, crt);
                QualifiedValue qv = (QualifiedValue)qvals.get(0);
                tempVal += (double)qv.getValue();
                tempList.add(tempVal);
            }

            Collections.sort(tempList, new Comparator<Double>() {
                @Override
                public int compare(Double o1, Double o2) {
                    return o1.compareTo(o2);
                }
            });

            int altNum = list.size()/2;
            partialRes = (double)((QualifiedValue)currentCriteria.get(list.get(altNum), crt).get(0)).getValue();

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
                    "criteriaScales.xml",
                    "criteriaThresholds.xml",
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
            if (!GetParameters())
                return false;
            if (!Validate())
                return false;
        }
        catch (Throwable thr)
        {
            execResult.addError("[InitData] Data are invalid");
            return false;
        }

        CopyToCurrent();
        GetRandomAlternative(NumberOfClustersParam);


        return true;
    }

    private org.xmcda.v2.XMCDA ObsoleteLoadData(org.xmcda.v2.XMCDA xmcdaV2, String path, String tag)
    {
        File file = new File(path);

        if (file.exists()) {
            try {
                xmcdaV2.getProjectReferenceOrMethodMessagesOrMethodParameters().addAll(org.xmcda.parsers.xml.xmcda_v2.XMCDAParser.readXMCDA(file).getProjectReferenceOrMethodMessagesOrMethodParameters());
            } catch (Throwable thr) {
                execResult.addError("[LoadData] Parse data error");
            }
        }
        else
        {
            execResult.addError("[LoadData] File doesn't exists");
        }
        return xmcdaV2;
    }

    private boolean GetParameters()
    {
        // parameter: random seed, if supplied
        ProgramParameter<?> param = xmcda.programParametersList.get(0).getParameter("NumberOfClusters");
        if (param==null)
            return false;
        if (param.getValues().size()<1)
            return false;
        try
        {
            NumberOfClustersParam = (Integer) (param.getValues().get(0)).getValue();
        }
        catch (ClassCastException e)
        {
            return false;
        }


        // parameter: random seed, if supplied
        param = xmcda.programParametersList.get(0).getParameter("distance");
        if (param==null)
            return false;
        if (param.getValues().size()<1)
            return false;
        try
        {
            DistanceParam = (Integer) (param.getValues().get(0)).getValue();
        }
        catch (ClassCastException e)
        {
            return false;
        }

        // parameter: random seed, if supplied
        param = xmcda.programParametersList.get(0).getParameter("randomSeed");
        if (param==null)
            return true;  // it is optional
        if (param.getValues().size()<1)
            return false;
        try
        {
            randomSeed = (Integer) (param.getValues().get(0)).getValue();
        }
        catch (ClassCastException e)
        {
            return false;
        }
        return true;
    }

    protected boolean LoadData(String path, String tag)
    {
        final org.xmcda.parsers.xml.xmcda_v3.XMCDAParser parser = new org.xmcda.parsers.xml.xmcda_v3.XMCDAParser();
        File file = new File(path);

        if(!file.exists()) {
            execResult.addError("[LoadData] File doesn't exists");
            return false;
        }

        try
        {
            parser.readXMCDA(xmcda, file, tag);
            return true;
        }
        catch (Throwable thr)
        {
            execResult.addError("[LoadData] Parse data error");
            return false;
        }
    }

    protected boolean Validate()
    {
        int critNum = xmcda.criteria.size();
        int critThrNum = xmcda.criteriaThresholdsList.size();
        if (critThrNum > 0)
            critThrNum = xmcda.criteriaThresholdsList.get(0).size();
        if (critNum != critThrNum) {
            execResult.addError("[InitData] Number of criteria is different with number of criteria thresholds.");
            return false;
        }

        int altNum = xmcda.alternatives.size();
        int altCritValNum = xmcda.performanceTablesList.size();
        if (altCritValNum > 0)
            altCritValNum = xmcda.performanceTablesList.get(0).size();
        if (altCritValNum != altNum*critNum) {
            execResult.addError("[InitData] Number of alternatives is different with number of alternatives in performance table.");
            return false;
        }

        int critScalNum = xmcda.criteriaScalesList.size();
        if (critScalNum > 0)
            critScalNum = xmcda.criteriaScalesList.get(0).size();
        if (critNum != critScalNum) {
            execResult.addError("[InitData] Number of criteria is different with number of criteria scales.");
            return false;
        }

        if (NumberOfClustersParam >= altNum || NumberOfClustersParam <= 1){
            execResult.addError("[InitData] Invalid number of clusters.");
            return false;
        }

        return true;
    }

    protected void GetRandomAlternative(int num)
    {

        int currentNum = 0;
        Random rnd = new Random();
        if (randomSeed != null)
            rnd.setSeed(randomSeed);
        List<Alternative> chosenOnes = new ArrayList<>();
        while (currentNum < num)
        {
            int chosen = rnd.nextInt(num);
            if (!chosenOnes.contains(currentAlternatives.get(chosen)))
            {
                currentNum++;
                chosenOnes.add(currentAlternatives.get(chosen));
            }
        }

        SetAlternativesWithCriteria(chosenOnes);

    }

    protected void SetAlternativesWithCriteria(List<Alternative> from)
    {
        for (int i = 0; i < NumberOfClustersParam; i++)
        {
            Alternative alt = new Alternative(prefix.concat(Integer.toString(i)));
            centralProfiles.add(alt);
        }

        for (int i = 0; i < centralProfiles.size(); i++)
        {
            Alternative altDst = centralProfiles.get(i);
            Alternative altSrc = from.get(i);
            for (Criterion crt : xmcda.criteria)
            {
                QualifiedValues qvals = (QualifiedValues)currentCriteria.get(altSrc, crt);
                QualifiedValue qv = (QualifiedValue)qvals.get(0);
                QualifiedValue<Double> insert = new QualifiedValue<Double>((double)qv.getValue());
                currentCriteria.put(altDst, crt, insert);
            }

        }
    }

    protected void CopyToCurrent()
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
