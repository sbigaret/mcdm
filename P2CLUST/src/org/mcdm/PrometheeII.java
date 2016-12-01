package org.mcdm;
import org.xmcda.*;

import java.util.ArrayList;
import java.util.List;

public class PrometheeII {

    protected XMCDA xmcda;
    protected AlternativesCriteriaValues currentCriteria;

    public PrometheeII(XMCDA core)
    {
        xmcda = core;
    }

    public Alternative FindClosest(List<Alternative> centralProfiles, Alternative tested)
    {
        CalculatePI(centralProfiles.get(0), tested);
        return null;
    }

    public void SetCriteria(AlternativesCriteriaValues criteria)
    {
        currentCriteria = criteria;
    }

    private double CalculatePI(Alternative first, Alternative second)
    {
        //for (Object qwe : xmcda.criteria)
        CriteriaValues asd = (CriteriaValues)currentCriteria.get(first);
        for (Object qwe : xmcda.criteria)
        {
            Object aa = asd.get(qwe);
            aa.equals(qwe);
        }


//        //int firstIndex = currentCriteria.get(first);
//        int secondIndex = currentCriteria.getAlternativeCriteriaValue().lastIndexOf(second);
//
//        for (Criterion crt : xmcda.criteria)
//        {
//            double dk = (double)currentCriteria.getAlternativeCriteriaValue().get(firstIndex).getCriterionValue().get(0).getValueOrValues().get(0);
//
//
//
//        }
//

        return 0;
    }
}
