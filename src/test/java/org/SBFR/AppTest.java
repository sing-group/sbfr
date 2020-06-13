package org.SBFR;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.nlpa.util.BabelUtils;

import it.uniroma1.lcl.babelnet.BabelNet;
import it.uniroma1.lcl.babelnet.BabelSynset;
import it.uniroma1.lcl.babelnet.BabelSynsetID;
import it.uniroma1.lcl.babelnet.data.BabelPointer;


/**
 * Unit test for simple App.
 */
public class AppTest 
{
    static BabelNet bn = BabelNet.getInstance();
    /**
     * Rigorous Test :-)
     */
    @Test
    public void shouldAnswerWithTrue()
    {
        assertTrue( true );
    }

    @Test
    public void testGetChildNodes() {
        BabelNet bn = BabelNet.getInstance();
        BabelSynset by = bn.getSynset(new BabelSynsetID("bn:00083081v"));

        List<String> childNodes = new ArrayList<String>();
        
        by.getOutgoingEdges(BabelPointer.HYPONYM)
                .stream()
                .collect(Collectors.toList())
                .forEach(babelSynsetRelation -> childNodes.add(babelSynsetRelation.getBabelSynsetIDTarget().toString()));

        System.out.println(childNodes);
        System.out.println("");
    }

    public static int relationshipDegree(String synset1, String synset2, 
                                        List<String> s1Hypernyms, List<String> s2Hypernyms)
    {
        if (s1Hypernyms.size() == 0)
            return Integer.MIN_VALUE;

        String s1 = s1Hypernyms.get(0);
        
        if (s1 == synset2)
            return 1;
        else if (s2Hypernyms.contains(s1))
                return s2Hypernyms.indexOf(s1); 
        else
            return 1 + relationshipDegree(synset1, synset2, 
                                        s1Hypernyms.subList(1, s1Hypernyms.size()),
                                        s2Hypernyms);
    }

    @Test
    public void testRelationshipDegree() {
        String poesia = "bn:00063195n";
        String eufuismo = "bn:00305669n";
        List<String> poesiaHypernyms = BabelUtils.getDefault().getAllHypernyms(poesia);
        List<String> cuentoHypernyms = BabelUtils.getDefault().getAllHypernyms(eufuismo);

        int result = relationshipDegree(poesia, eufuismo, poesiaHypernyms, cuentoHypernyms);
        System.out.println();
        
    }

    public static List<String> getChildNodes(String synset) {
        BabelSynset by = bn.getSynset(new BabelSynsetID(synset));

        List<String> childNodes = new ArrayList<String>();
        
        by.getOutgoingEdges(BabelPointer.HYPONYM)
                .stream()
                .collect(Collectors.toList())
                .forEach(babelSynsetRelation -> childNodes.add(babelSynsetRelation.getBabelSynsetIDTarget().toString()));
        return childNodes;
    }

    @Test
    public void testFatherNodes() {
        List<String> fatherNodes = getChildNodes("bn:00071261n");
        System.out.println(fatherNodes);
    }

    public static List<String> getFatherNodes(String synset) {
        BabelSynset by = bn.getSynset(new BabelSynsetID(synset));

        List<String> fatherNodes = new ArrayList<String>();
        
        by.getOutgoingEdges(BabelPointer.HYPERNYM)
                .stream()
                .collect(Collectors.toList())
                .forEach(babelSynsetRelation -> fatherNodes.add(babelSynsetRelation.getBabelSynsetIDTarget().toString()));
        return fatherNodes;
    }


}
