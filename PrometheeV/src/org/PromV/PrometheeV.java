package org.PromV;


import java.io.File;
import java.sql.Array;
import java.util.*;


import org.xmcda.*;


import org.xmcda.LinearConstraint;
import org.xmcda.parsers.xml.xmcda_3_0.XMCDAParser;
import scpsolver.constraints.*;
import scpsolver.lpsolver.LinearProgramSolver;
import scpsolver.lpsolver.SolverFactory;
import scpsolver.problems.LinearProgram;


import org.xmcda.utils.ValueConverters.*;

public class PrometheeV extends Core{

    public PrometheeV(){}

    protected void LoadData(String input)
    {
        LoadFile(input.concat("/positive.xml"), "alternativesValues");
        LoadFile(input.concat("/test.xml"), "alternativesLinearConstraints");
        LoadFile(input.concat("/alternatives.xml"), "alternatives");
    }

    public void Compute(String input, String output)
    {
        LoadData(input);
        GetAlternatives();
        LinearProgram lp = MakeLP();
        MakeConstraints(lp);
        SolveAndSave(lp, output);
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

    private void MakeConstraints(LinearProgram lp)
    {
        int constraintsNum = xmcda.alternativesLinearConstraintsList.size();
        try
        {
            org.xmcda.AlternativesLinearConstraints constraints = xmcda.alternativesLinearConstraintsList.get(0);

            ListIterator<LinearConstraint> var =  constraints.listIterator();

            while(var.hasNext())
            {
                LinearConstraint ccc = var.next();

                String name = ccc.id();
                double rhs = (double)ccc.getRhs().getValue();
                double[] leftSide = new double[alternatives.size()];
                Arrays.fill(leftSide, 0.0);

                ArrayList<LinearConstraint.Element> elements = ccc.getElements();
                for(LinearConstraint.Element ele : elements)
                {
                    Alternative owner = (Alternative) ele.getUnknown();
                    leftSide[ alternatives.indexOf(owner.id()) ] = (double)ele.getCoefficient().getValue();
                }

                LinearConstraint.Operator op = ccc.getOperator();
                ConstraintFactory(leftSide, op.name(), rhs, name, lp);

            }
        }
        catch(Throwable thr)
        {
            errors.add(thr);
        }
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
        }
    }

    private void SolveAndSave(LinearProgram lp, String out)
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
            errors.add(thr);
        }


        System.out.print("asd");
    }
}
