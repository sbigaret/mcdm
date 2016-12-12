package org.mcdm;
import javafx.util.Pair;
import org.xmcda.*;

import java.util.*;

public class PrometheeTri {

    protected XMCDA xmcda;
    protected AlternativesCriteriaValues currentCriteria;
    protected double P = 1.0;

    public PrometheeTri(XMCDA core, double P)
    {
        xmcda = core;
        this.P = P;
    }


    public void SetCriteria(AlternativesCriteriaValues criteria)
    {
        currentCriteria = criteria;
    }

    private double getIndifference(Criterion crit)
    {
        CriteriaThresholds ctr = xmcda.criteriaThresholdsList.get(0);

        CriterionThresholds criterionT = ctr.get(crit);

        if (criterionT.get(0).id().equals("indifference"))
            return (double)criterionT.get(0).getConstant().getValue();
        else
            return (double)criterionT.get(1).getConstant().getValue();
    }

    private double getPreference(Criterion crit)
    {
        CriteriaThresholds ctr = xmcda.criteriaThresholdsList.get(0);

        CriterionThresholds criterionT = ctr.get(crit);

        if (criterionT.get(0).id().equals("preference"))
            return (double)criterionT.get(0).getConstant().getValue();
        else
            return (double)criterionT.get(1).getConstant().getValue();
    }

    private double functionPMax(double indf, double pref, double x)
    {
        if (x < pref)
            return 1.0;
        else if (x < indf)
            return (indf - x)/(indf - pref);
        else
            return 0.0;
    }

    private double functionPMin(double indf, double pref, double x)
    {
        if (x < indf)
            return 0.0;
        else if (x < pref)
            return (x - indf)/(pref - indf);
        else
            return 1.0;
    }

    private double getWeigth(Criterion criterion)
    {
        double result = 0.0;

        LabelledQValues var = xmcda.criteriaValuesList.get(0).get(criterion);
        QualifiedValue val = (QualifiedValue)var.get(0);
        result = (double)val.getValue();

        return result;
    }

    private double functionP(Alternative A, Alternative B, Criterion crit)
    {
        CriteriaValues firstAlter = (CriteriaValues)currentCriteria.get(A);
        CriteriaValues secondAlter = (CriteriaValues)currentCriteria.get(B);

        LabelledQValues secLQV = (LabelledQValues)secondAlter.get(crit);
        QualifiedValue secQV = (QualifiedValue)secLQV.get(0);
        Double bVal = (double)secQV.getValue();


        LabelledQValues fiLQV = (LabelledQValues)firstAlter.get(crit);
        QualifiedValue fiQV = (QualifiedValue)fiLQV.get(0);
        Double aVal = (double)fiQV.getValue();

        double difference = aVal - bVal;

        Double indif  = getIndifference(crit);
        Double pref  = getPreference(crit);

        double critResult = 0.0;

        QuantitativeScale scale = (QuantitativeScale)xmcda.criteriaScalesList.get(0).get(crit).get(0);
        if (scale.getPreferenceDirection() == Scale.PreferenceDirection.MIN)
            critResult = functionPMax(indif, pref, difference);
        else
            critResult = functionPMin(indif, pref, difference);

       return critResult;
    }

    private double fi(Alternative arument, List<Alternative> space, Criterion crit)
    {
        double result = 0.0;

        double spaceSizeR = space.size() - 1;

        for(Alternative element : space)
        {
            result += functionP(arument, element, crit) - functionP(element, arument, crit);
        }

        result = result / spaceSizeR;

        return result;
    }

    private double deviation(Alternative A, Alternative Rh, List<Alternative> R)
    {
        double result = 0.0;

        for(Criterion crit : xmcda.criteria)
        {
            double temp = fi(A, R, crit) - fi(Rh, R, crit);
            temp = Math.abs(temp);
            temp = Math.pow(temp, P);
            temp *= getWeigth(crit);
            result += temp;
        }

        result = Math.pow(result, 1.0/P);
        return result;
    }


    public Alternative FindClosest(List<Alternative> centralProfiles, Alternative alt)
    {
        double min = Double.MAX_VALUE;
        Alternative chosen = null;

        for (Alternative element : centralProfiles)
        {
            double current = deviation(alt, element, centralProfiles);
            if (current < min)
            {
                min = current;
                chosen = element;
            }
        }
        return chosen;
    }

}
