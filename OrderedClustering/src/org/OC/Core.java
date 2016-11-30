package org.OC;


import org.xmcda.Alternative;
import org.xmcda.LabelledQValues;
import org.xmcda.QualifiedValue;
import org.xmcda.XMCDA;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public abstract class Core {

    protected HashMap<String, Double> flows;
    protected ArrayList<String> alternatives;
    public XMCDA xmcda;

    public List<Throwable> errors = new ArrayList<Throwable>();

    public class InvalidInputData extends Exception{}

    public Core()
    {
        xmcda = new XMCDA();
        errors.clear();
    }


    public boolean LoadFile(String path, String ... typeTag)
    {
        final org.xmcda.parsers.xml.xmcda_3_0.XMCDAParser parser = new org.xmcda.parsers.xml.xmcda_3_0.XMCDAParser();
        File file = new File(path);

        if(!file.exists())
        {
            return false;
        }

        try
        {
            parser.readXMCDA(xmcda, file, typeTag);
            return true;
        }
        catch (Throwable throwable)
        {
            errors.add(throwable);
            return false;
        }
    }

    protected abstract void LoadData(String input);
    public  abstract void Compute(String in, String out);

    protected HashMap<String, Double>  GetFlows()
    {
        org.xmcda.AlternativesValues flows;

        HashMap<String, Double> result = new HashMap<String, Double>();

        try
        {
            flows = xmcda.alternativesValuesList.get(0).asDouble();

        }
        catch (Throwable ce)
        {
            return null;
        }

        Set<Alternative> asd = flows.getAlternatives();
        for( org.xmcda.Alternative alt : asd)
        {
            try {
                LabelledQValues<QualifiedValue<Double>> something = (LabelledQValues<QualifiedValue<Double>>) flows.get(alt);
                Object obj = something.get(0).getValue();
                result.put(alt.id(), new Double((double)obj));
                //result.add(new Double((double)obj));
            }
            catch (Throwable thr) {
                return null;
            }
        }


        return result;
    }

    protected void GetAlternatives()
    {
        try
        {
            alternatives = xmcda.alternatives.getIDs();
        }
        catch(Throwable thr)
        {
            errors.add(thr);
        }
    }
}