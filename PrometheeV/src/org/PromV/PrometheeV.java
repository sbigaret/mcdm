package org.PromV;

import java.io.File;
import java.util.*;

import org.xmcda.*;
import org.xmcda.LinearConstraint;
import org.xmcda.parsers.xml.xmcda_3_0.XMCDAParser;
import scpsolver.constraints.*;
import scpsolver.lpsolver.LinearProgramSolver;
import scpsolver.lpsolver.SolverFactory;
import scpsolver.problems.LinearProgram;


public class PrometheeV {

    private HashMap<String, Double> flows;
    private ArrayList<String> alternatives;
    private XMCDA xmcda;

    public PrometheeV()
    {
        xmcda = new XMCDA();
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
            return false;
        }
    }

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
                result.put(alt.id(), (double)obj);
            }
            catch (Throwable thr) {
                return null;
            }
        }


        return result;
    }

    protected boolean GetAlternatives()
    {
        try
        {
            alternatives = xmcda.alternatives.getIDs();
            return true;
        }
        catch(Throwable thr)
        {
            return false;
        }
    }

    protected void LoadData(String input)
    {
        LoadFile(input.concat("/positive.xml"), "alternativesValues");
        LoadFile(input.concat("/test.xml"), "alternativesLinearConstraints");
        LoadFile(input.concat("/alternatives.xml"), "alternatives");
    }

    public boolean Compute(String input, String output)
    {
        LoadData(input);
        if (!GetAlternatives())
            return false;
        LinearProgram lp = MakeLP();
        if (!MakeConstraints(lp))
            return false;
        if (!SolveAndSave(lp, output))
            return false;
        return true;
    }


    private LinearProgram MakeLP()
    {
        flows = GetFlows();
        double [] problem = new double[flows.size()];
        Arrays.fill(problem, 1.0);
        LinearProgram lp = new LinearProgram(problem);
        lp.setMinProblem(false);

        for(int i = 0; i < alternatives.size(); i++)
            lp.setBinary(i);

        return lp;
    }

    private boolean MakeConstraints(LinearProgram lp)
    {
        try
        {
            org.xmcda.AlternativesLinearConstraints constraints = xmcda.alternativesLinearConstraintsList.get(0);

            ListIterator<LinearConstraint> var =  constraints.listIterator();

            while(var.hasNext())
            {
                LinearConstraint linearConstr = var.next();

                String name = linearConstr.id();
                double rhs = (double)linearConstr.getRhs().getValue();
                double[] leftSide = new double[alternatives.size()];
                Arrays.fill(leftSide, 0.0);

                ArrayList<LinearConstraint.Element> elements = linearConstr.getElements();
                for(LinearConstraint.Element ele : elements)
                {
                    Alternative owner = (Alternative) ele.getUnknown();
                    leftSide[ alternatives.indexOf(owner.id()) ] = (double)ele.getCoefficient().getValue();
                }

                LinearConstraint.Operator op = linearConstr.getOperator();
                ConstraintFactory(leftSide, op.name(), rhs, name, lp);

            }
        }
        catch(Throwable thr)
        {
            return false;
        }
        return true;
    }

    private void ConstraintFactory(double[] leftSide, String operator, double rhs, String name, LinearProgram lp)
    {
        switch (operator)
        {
            case "GEQ":
                lp.addConstraint(new LinearBiggerThanEqualsConstraint(leftSide, rhs, name));
                break;
            case "LEQ":
                lp.addConstraint(new LinearSmallerThanEqualsConstraint(leftSide, rhs, name));
                break;
            case "EQ":
                lp.addConstraint(new LinearEqualsConstraint(leftSide, rhs, name));
                break;
            default:
                lp.addConstraint(new LinearEqualsConstraint(leftSide, rhs, name));
                break;
        }
    }

    private boolean SolveAndSave(LinearProgram lp, String out)
    {
        LinearProgramSolver solver = SolverFactory.newDefault();
        double[] sol = solver.solve(lp);
        xmcda.alternativesValuesList.clear();

        for (int i = 0; i < alternatives.size(); i++)
        {
            Alternative alt = new Alternative(alternatives.get(i));
            AlternativesValues<Double> altV = new AlternativesValues<Double>();
            altV.setDefault(alt, sol[i]);
            xmcda.alternativesValuesList.add(altV);
        }

        final XMCDAParser parser = new XMCDAParser();
        final File plik = new File(out, "result.xml");
        try
        {
            parser.writeXMCDA(xmcda, plik.getAbsolutePath(), "alternativesValues");
        }
        catch (Throwable thr)
        {
            return false;
        }

        return true;
    }
}
