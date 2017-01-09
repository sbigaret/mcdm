package org.PromV;

import java.io.File;
import java.util.*;

import org.xmcda.*;
import org.xmcda.Alternative;
import org.xmcda.AlternativesLinearConstraints;
import org.xmcda.AlternativesValues;
import org.xmcda.LinearConstraint;
import org.xmcda.XMCDA;
import org.xmcda.converters.v2_v3.XMCDAConverter;
import org.xmcda.parsers.xml.xmcda_v3.XMCDAParser;
import scpsolver.constraints.*;
import scpsolver.lpsolver.LinearProgramSolver;
import scpsolver.lpsolver.SolverFactory;
import scpsolver.problems.LinearProgram;

import javax.xml.bind.JAXBElement;


public class PrometheeV {

    private HashMap<String, Double> flows;
    private ArrayList<String> alternatives;
    private XMCDA xmcda;

    public enum xmcdaVersion {V2, V3}

    ;

    public PrometheeV() {
        xmcda = new XMCDA();
    }

    public boolean LoadFile(String path, String... typeTag) {
        final org.xmcda.parsers.xml.xmcda_v3.XMCDAParser parser = new org.xmcda.parsers.xml.xmcda_v3.XMCDAParser();
        File file = new File(path);

        if (!file.exists()) {
            return false;
        }

        try {
            parser.readXMCDA(xmcda, file, typeTag);
            return true;
        } catch (Throwable throwable) {
            return false;
        }
    }

    public org.xmcda.v2.XMCDA LoadFileObsolete(org.xmcda.v2.XMCDA xmcdaV2, String path, String... typeTag) {
        File file = new File(path);

        if (file.exists()) {
            try {
                xmcdaV2.getProjectReferenceOrMethodMessagesOrMethodParameters().addAll(org.xmcda.parsers.xml.xmcda_v2.XMCDAParser.readXMCDA(file).getProjectReferenceOrMethodMessagesOrMethodParameters());
            } catch (Throwable thr) {
                System.out.print(thr.getMessage());
            }
        }
        return xmcdaV2;
    }

    protected HashMap<String, Double> GetFlows() {
        org.xmcda.AlternativesValues flows;

        HashMap<String, Double> result = new HashMap<String, Double>();

        try {
            flows = xmcda.alternativesValuesList.get(0).asDouble();

        } catch (Throwable ce) {
            return null;
        }

        Set<Alternative> asd = flows.getAlternatives();
        for (org.xmcda.Alternative alt : asd) {
            try {
                LabelledQValues<QualifiedValue<Double>> something = (LabelledQValues<QualifiedValue<Double>>) flows.get(alt);
                Object obj = something.get(0).getValue();
                result.put(alt.id(), (double) obj);
            } catch (Throwable thr) {
                return null;
            }
        }


        return result;
    }

    protected boolean GetAlternatives() {
        try {
            alternatives = xmcda.alternatives.getIDs();
            return true;
        } catch (Throwable thr) {
            return false;
        }
    }

    protected void LoadData(xmcdaVersion version, String input) {
        if (version == xmcdaVersion.V3) {
            LoadFile(input.concat("/flows.xml"), "alternativesValues");
            LoadFile(input.concat("/constraints.xml"), "alternativesLinearConstraints");
            LoadFile(input.concat("/alternatives.xml"), "alternatives");
        } else {
            org.xmcda.v2.XMCDA xmcdaV2 = new org.xmcda.v2.XMCDA();
            xmcdaV2 = LoadFileObsolete(xmcdaV2, input.concat("/flows.xml"), "alternativesValues");
            xmcdaV2 = LoadFileObsolete(xmcdaV2, input.concat("/alternatives.xml"), "alternatives");
            xmcdaV2 = LoadFileObsolete(xmcdaV2, input.concat("/constraints.xml"), "alternativesLinearConstraints");
            xmcda = XMCDAConverter.convertTo_v3(xmcdaV2);
            xmcda.alternativesLinearConstraintsList.add(convertLC(xmcdaV2));
        }
    }

    private AlternativesLinearConstraints convertLC(org.xmcda.v2.XMCDA oldXmcda)
    {
        AlternativesLinearConstraints<org.xmcda.v2.AlternativesLinearConstraints.Constraint> alc = new AlternativesLinearConstraints();

        Iterator var2 = oldXmcda.getProjectReferenceOrMethodMessagesOrMethodParameters().iterator();
        AlternativesLinearConstraints alt = new AlternativesLinearConstraints();

        while(var2.hasNext())
        {
            JAXBElement element = (JAXBElement)var2.next();
            if(element.getValue() instanceof org.xmcda.v2.AlternativesLinearConstraints) {
                org.xmcda.v2.AlternativesLinearConstraints oldLC = (org.xmcda.v2.AlternativesLinearConstraints)element.getValue();

                alt.setId(oldLC.getId());
                alt.setMcdaConcept(oldLC.getMcdaConcept());
                alt.setName(oldLC.getName());

                List<org.xmcda.v2.AlternativesLinearConstraints.Constraint> cList = oldLC.getConstraint();

                for (org.xmcda.v2.AlternativesLinearConstraints.Constraint cons : cList)
                {
                    String id = cons.getId();
                    String operator = cons.getOperator();
                    Double rhsNV = cons.getRhs().getReal();
                    List<org.xmcda.v2.AlternativesLinearConstraints.Constraint.Element> elements = cons.getElement();

                    LinearConstraint linC = new LinearConstraint();
                    linC.setId(id);
                    if (operator.equals("eq"))
                        linC.setOperator(LinearConstraint.Operator.EQ);
                    else if (operator.equals("geq"))
                        linC.setOperator(LinearConstraint.Operator.GEQ);
                    else
                        linC.setOperator(LinearConstraint.Operator.LEQ);
                    linC.setRhs(new QualifiedValue(rhsNV));

                    ArrayList<LinearConstraint.Element> newEleList = new ArrayList<>();
                    for (org.xmcda.v2.AlternativesLinearConstraints.Constraint.Element ele : elements)
                    {
                        LinearConstraint.Element newEle = new LinearConstraint.Element();
                        newEle.setCoefficient(new QualifiedValue(ele.getCoefficient().getReal()));
                        if (ele.getAlternativeID() != null)
                            newEle.setUnknown(new Alternative(ele.getAlternativeID()));
                        newEleList.add(newEle);

                    }

                    linC.setElements(newEleList);

                    alc.add(linC);
                }
            }
        }

        return alc;
    }


    public boolean Compute(xmcdaVersion version, String input, String output) {
        LoadData(version, input);

        if (!GetAlternatives())
            return false;

        LinearProgram lp = MakeLP();
        if (lp == null)
            return false;

        if (!MakeConstraints(lp))
            return false;

        if (!SolveAndSave(version, lp, output))
            return false;

        return true;
    }


    private LinearProgram MakeLP() {
        flows = GetFlows();
        if (flows == null)
            return null;
        double[] problem = new double[flows.size()];
        Arrays.fill(problem, 1.0);

        for (Map.Entry<String, Double> element : flows.entrySet())
        {
            String strIndex = element.getKey();
            Double value = element.getValue();
            int index  = alternatives.indexOf(strIndex);

            try
            {
                problem[index] = value;
            }
            catch (Throwable thr)
            {
                System.out.print(thr.getMessage());
            }
        }

        LinearProgram lp = new LinearProgram(problem);
        lp.setMinProblem(false);

        for (int i = 0; i < alternatives.size(); i++)
            lp.setBinary(i);

        return lp;
    }

    private boolean MakeConstraints(LinearProgram lp) {
        try {
            org.xmcda.AlternativesLinearConstraints constraints = xmcda.alternativesLinearConstraintsList.get(0);

            ListIterator<LinearConstraint> var = constraints.listIterator();

            while (var.hasNext()) {
                LinearConstraint linearConstr = var.next();

                String name = linearConstr.id();
                double rhs = (double) linearConstr.getRhs().getValue();
                double[] leftSide = new double[alternatives.size()];
                Arrays.fill(leftSide, 0.0);

                ArrayList<LinearConstraint.Element> elements = linearConstr.getElements();
                for (LinearConstraint.Element ele : elements) {
                    Alternative owner = (Alternative) ele.getUnknown();
                    leftSide[alternatives.indexOf(owner.id())] = (double) ele.getCoefficient().getValue();
                }

                LinearConstraint.Operator op = linearConstr.getOperator();
                ConstraintFactory(leftSide, op.name(), rhs, name, lp);

            }
        } catch (Throwable thr) {
            return false;
        }
        return true;
    }

    private void ConstraintFactory(double[] leftSide, String operator, double rhs, String name, LinearProgram lp) {
        switch (operator) {
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

    private boolean SolveAndSave(xmcdaVersion version, LinearProgram lp, String out) {
        LinearProgramSolver solver = SolverFactory.newDefault();
        double[] sol = solver.solve(lp);
        xmcda.alternativesValuesList.clear();

        for (int i = 0; i < alternatives.size(); i++) {
            Alternative alt = new Alternative(alternatives.get(i));
            AlternativesValues<Double> altV = new AlternativesValues<Double>();
            altV.setDefault(alt, sol[i]);
            xmcda.alternativesValuesList.add(altV);
        }

        if (version == xmcdaVersion.V3) {
            final XMCDAParser parser = new XMCDAParser();
            final File plik = new File(out, "result.xml");
            try {
                parser.writeXMCDA(xmcda, plik.getAbsolutePath(), "alternativesValues");
            } catch (Throwable thr) {
                return false;
            }
        }
        else {
            org.xmcda.v2.XMCDA resXmcda = XMCDAConverter.convertTo_v2(xmcda);
            final File plik = new File(out, "result.xml");
            try {
                org.xmcda.parsers.xml.xmcda_v2.XMCDAParser.writeXMCDA(resXmcda, plik.getAbsolutePath(), "alternativesValues");
            } catch (Throwable thr) {
                return false;
            }
        }

        return true;
    }
}
