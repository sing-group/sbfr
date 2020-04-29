package SBFR;

//import types.CachedBabelUtils;
//import java.util.concurrent.*;
import java.util.stream.Collectors;
//import javafx.util.*;

import org.bdp4j.dataset.CSVDatasetReader;
/*import org.bdp4j.pipe.AbstractPipe;
import org.bdp4j.pipe.SerialPipes;*/
import org.bdp4j.transformers.Enum2IntTransformer;
//import org.bdp4j.types.Instance;
import org.bdp4j.types.Transformer;
//import org.bdp4j.util.InstanceListUtils;
import org.bdp4j.types.Dataset;
import org.nlpa.util.BabelUtils;

import it.uniroma1.lcl.babelnet.BabelNet;
import it.uniroma1.lcl.babelnet.BabelSynset;
import it.uniroma1.lcl.babelnet.BabelSynsetID;
//import it.uniroma1.lcl.babelnet.BabelSynsetRelation;
import it.uniroma1.lcl.babelnet.data.BabelPointer;
/*import it.uniroma1.lcl.jlt.ling.*;
import it.uniroma1.lcl.jlt.util.Language;
import it.uniroma1.lcl.babelnet.*;*/


/*import org.bdp4j.*;

import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.core.Instances;
import weka.knowledgeflow.StepOutputListener;*/

import java.util.*;

/**
 * 
 * @author Javier Quintas Bergantiño
 */

public class App 
{

    static BabelNet bn = BabelNet.getInstance();
    //Map used to save the hypernyms to which synset pairs will generalize
    static Map<String, String> generalizeTo = new HashMap<String, String>(); 
    static Map<String, String> auxGeneralizeTo = new HashMap<String, String>();
    //Max relationship degree that we will admit 
    static int maxDegree = 2;
    static Map<String, List<String>> cachedHypernyms;
    static boolean keepGeneralizing;

    /**
     * 
     * ESP: Recibe una lista de synsets y comprueba si están guardados en disco, si no, los añade al mapa con una lista de sus respectivos hiperónimos
     * ENG: Receives a synset list and checks if they are already stored, if not, they get added to the map along with a list of its hypernyms
     * 
     * @param cachedHypernyms contains a map with <synsets, hypernyms> already saved in disk
     * @param synsetList list of all the synsets that we want to check if they are already in disk or not
     */
    public static void createCache(Map<String, List<String>> cachedHypernyms, List<String> synsetList){
        CachedBabelUtils cachedBabelUtils = new CachedBabelUtils(cachedHypernyms);

        for(String s: synsetList){
            if(!cachedBabelUtils.existsSynsetInMap(s)){
                cachedBabelUtils.addSynsetToCache(s, BabelUtils.getDefault().getAllHypernyms(s));
                System.out.println("Adding "+ s);

                for(String h: cachedBabelUtils.getCachedSynsetHypernymsList(s)){
                    if(!cachedBabelUtils.existsSynsetInMap(h)){
                        cachedBabelUtils.addSynsetToCache(h, BabelUtils.getDefault().getAllHypernyms(h));
                    }
                }
            }
        }

        save_read_files.saveMap(cachedBabelUtils.getMapOfHypernyms());
		//System.out.println("Saved elements: " + cachedBabelUtils.getMapOfHypernyms().size());
        
    }

    public static void addNewCachedSynset(Map<String, List<String>> cachedHypernyms, String synset){
        CachedBabelUtils cachedBabelUtils = new CachedBabelUtils(cachedHypernyms);

        if(!cachedBabelUtils.existsSynsetInMap(synset)){
            cachedBabelUtils.addSynsetToCache(synset, BabelUtils.getDefault().getAllHypernyms(synset));
            System.out.println("Adding "+ synset);

            for(String h: cachedBabelUtils.getCachedSynsetHypernymsList(synset)){
                if(!cachedBabelUtils.existsSynsetInMap(h)){
                    cachedBabelUtils.addSynsetToCache(h, BabelUtils.getDefault().getAllHypernyms(h));
                }
            }
        }

        save_read_files.saveMap(cachedBabelUtils.getMapOfHypernyms());
    }

    /**
     * 
     * @param arr the array to sort
     * @param begin the index from which we will be sorting
     * @param end the index of the final element we will be sorting
     * @return
     */
    public static String[] quickSort(String[] arr, int begin, int end){
        if(begin < end){
            int partitionIndex = partition(arr, begin, end);

            quickSort(arr, begin, partitionIndex - 1);
            quickSort(arr, partitionIndex + 1, end);
        }
        return arr;
    }

    /**
     * 
     * @param arr
     * @param begin
     * @param end
     * @return
     */
    private static int partition(String[] arr, int begin, int end){
        String pivot = arr[end];
        int i = begin - 1;

        for(int j = begin; j < end; j++){
            if(cachedHypernyms.get(arr[j]).size() >= cachedHypernyms.get(pivot).size()){
                i++;

                String swapTemp = arr[i];
                arr[i] = arr[j];
                arr[j] = swapTemp;
            }
        }

        String swapTemp = arr[i+1];
        arr[i+1] = arr[end];
        arr[end] = swapTemp;
        
        return i+1;
    }

    /**
     * 
     * @param synset the synset from which we want to get its hypernyms
     * @return list of its hypernyms
     */
    private static List<String> getHypernyms(String synset){
        List<String> toRet;
        
        if(cachedHypernyms.keySet().contains(synset)){    
            toRet = cachedHypernyms.get(synset);
        }else{
            addNewCachedSynset(cachedHypernyms, synset);
            cachedHypernyms = save_read_files.read();
            toRet = cachedHypernyms.get(synset);
        }

        return toRet;
    }

    /**
     * 
     * @param synsetList
     * @param originalDataset
     */
    public static Dataset generalizeDirectly(List<String> synsetList, Dataset originalDataset){
        List<String> usedSynsets = new ArrayList<String>();
        for(String s1: synsetList){
            int index = synsetList.indexOf(s1);

            //Evaluate ham/spam %
            String expressionS1 = "(" + s1 + " >= 1) ? 1 : 0";
            Map<String, Integer> result1 = originalDataset.evaluateColumns(expressionS1,
                int.class, 
                new String[]{s1}, 
                new Class[]{double.class}, 
                "target");
            float ham1 = (float) result1.get("0");
            float spam1 = (float) result1.get("1");
            float percentage1 = (spam1/(ham1 + spam1));

            if(percentage1 >= 0.99 || percentage1 <= 0.01){
                for(String s2: synsetList.subList(index + 1, synsetList.size())){
                    //Get hypernym list of both synsets
                    List<String> s1Hypernyms = getHypernyms(s1);
                    List<String> s2Hypernyms = getHypernyms(s2);

                    if(s2Hypernyms.contains(s1) || s1Hypernyms.contains(s2)){
                        String expressionS2 = "(" + s2 + " >= 1) ? 1 : 0";
                
                        Map<String, Integer> result2 = originalDataset.evaluateColumns(expressionS2,
                            int.class, 
                            new String[]{s2}, 
                            new Class[]{double.class}, 
                            "target");
                        
                        float ham2 = (float) result2.get("0");
                        float spam2 = (float) result2.get("1");
                        float percentage2 = (spam2/(ham2 + spam2));

                        int degree = relationshipDegree(s1, s2, s1Hypernyms, s2Hypernyms);

                        if((percentage2 >= 0.99 && percentage1 >= 0.99) || (percentage2 <= 0.01 && percentage1 <= 0.01)){
                            if(s1Hypernyms.contains(s2) && degree <= maxDegree && degree >= 0){
                                List<String> listAttributeNameToJoin = new ArrayList<String>();
                                Boolean aux = usedSynsets.contains(s2);

                                generalizeTo.put(s1, s2);
                                
                                listAttributeNameToJoin.add(s1);
                                listAttributeNameToJoin.add(s2);
                                originalDataset.joinAttributes(listAttributeNameToJoin, s2, Dataset.COMBINE_SUM, !aux);

                                if(!usedSynsets.contains(s1))
                                    usedSynsets.add(s1);

                                break; 
                            }
                            else if(s2Hypernyms.contains(s1) && degree <= maxDegree && degree >= 0){
                                List<String> listAttributeNameToJoin = new ArrayList<String>();
                                Boolean aux = usedSynsets.contains(s1);
                                
                                generalizeTo.put(s2, s1);
                                listAttributeNameToJoin.add(s2);                                
                                listAttributeNameToJoin.add(s1);
                                originalDataset.joinAttributes(listAttributeNameToJoin, s1, Dataset.COMBINE_SUM, !aux);
                                
                                keepGeneralizing = true;

                                if(!usedSynsets.contains(s2))
                                    usedSynsets.add(s2);
                                
                                break;
                            }
                        }
                    }
                        
                }
            }
        }
        System.out.println("");
        return originalDataset;
    }

    /**
     * 
     * ESP: Determina el grado de parentesco entre dos synsets
     * ENG: Determines the relationship degree between two synsets
     * 
     * @param synset1 synset that we want to evaluate
     * @param synset2 synset that we want to evaluate
     * @param s1Hypernyms list containing all the hypernyms of synset1
     * @param s2Hypernyms list containing all the hypernyms of synset2
     * 
     * @return degree of relationship between both synsets
     * 
     */
    public static int relationshipDegree(String synset1, String synset2, 
                                        List<String> s1Hypernyms, List<String> s2Hypernyms)
    {
        if (s1Hypernyms.size() == 0)
            return Integer.MIN_VALUE;

        String s1 = s1Hypernyms.get(0);

        if (s1 == synset2){
            //if(generalizeTo.get(synset1) == null)
                auxGeneralizeTo.put(synset1, s1);

            return 1;
        } else if (s2Hypernyms.contains(s1)) {
            //if(generalizeTo.get(synset1) == null)
                auxGeneralizeTo.put(synset1, s1);

            return s2Hypernyms.indexOf(s1); 
        } else {

            return 1 + relationshipDegree(synset1, synset2, 
                                        s1Hypernyms.subList(1, s1Hypernyms.size()),
                                        s2Hypernyms);
        }
    }

    /**
     * 
     * ESP: Función que evalúa todos los synset y decide cuáles deben generalizarse
     * ENG: Evaluates every synset and decides if they should be generalized
     * 
     * @param originalDataset the original dataset that we are working with
     * @param synsetList list of all the synsets in the dataset
     * @param cachedHypernyms map containing every synset and its hypernyms (previously read from disk)
     * 
     * Note: not yet finished! may change in future versions
     */
    public static Map<String, List<String>> evaluate(Dataset originalDataset, List<String> synsetList, Map<String, List<String>> cachedHypernyms){
        Map<String, List<String>> finalResult = new HashMap<String, List<String>>();
        List<String> usedSynsets = new ArrayList<String>();
        for(String s1: synsetList){
 
            //We get the index of the synset
            int index = synsetList.indexOf(s1);

            String expressionS1 = "(" + s1 + " >= 1) ? 1 : 0";
            Map<String, Integer> result1 = originalDataset.evaluateColumns(expressionS1,
                int.class, 
                new String[]{s1}, 
                new Class[]{double.class}, 
                "target");
            float ham1 = (float) result1.get("0");
            float spam1 = (float) result1.get("1");
            float percentage1 = (spam1/(ham1 + spam1));

            List<String> s2List = new ArrayList<String>();
            
            if((percentage1 >= 0.99 || percentage1 <= 0.01) && !usedSynsets.contains(s1)){
                System.out.println("Synset 1: " + s1 + " -> " + result1 + " -> " + percentage1);
                //Iterate through a sublist of the original synset that contains only from the next synset to s1 onwards
                for (String s2: synsetList.subList(index + 1, synsetList.size())){
                    
                    List<String> s1Hypernyms = getHypernyms(s1);
                    List<String> s2Hypernyms = getHypernyms(s2);

                    int degree = relationshipDegree(s1, s2, s1Hypernyms, s2Hypernyms);

                    if (degree >= 0 && degree <= maxDegree && !usedSynsets.contains(s2) && !finalResult.containsKey(s1)) {
                        String expressionS2 = "(" + s2 + " >= 1) ? 1 : 0";
        
                        Map<String, Integer> result2 = originalDataset.evaluateColumns(expressionS2,
                            int.class, 
                            new String[]{s2}, 
                            new Class[]{double.class}, 
                            "target");
                        
                        float ham2 = (float) result2.get("0");
                        float spam2 = (float) result2.get("1");
                        float percentage2 = (spam2/(ham2 + spam2));  
                        if((percentage2 >= 0.99 && percentage1 >= 0.99) || (percentage2 <= 0.01 && percentage1 <= 0.01)){
                            //Results from evaluating these synsets
                            System.out.print("Synset 1: " + s1 + " -> " + result1);
                            System.out.println(" Synset 2: "+ s2 + " -> " + result2);

                            generalizeTo.put(s1, auxGeneralizeTo.get(s1)); 
                            usedSynsets.add(s2);
                            s2List.add(s2);

                            keepGeneralizing = true;
                        }
                    }
                }
                usedSynsets.add(s1);
            }
            if (s2List.size() > 0){
                finalResult.put(s1, s2List);
            }
        }
        System.out.println("");
        return finalResult;
    }

    /**
     * ESP: función utilizada para reducir los synsets del dataset original
     * ENG: used to reduce the number of synsets on the original dataset
     * 
     * @param originalDataset the original dataset that we want to reduce
     * @param toGeneralize map of synsets that we will be generalizing
     * 
     * @return the originalDataset modified and reduced according to the generalizable synsets
     */
    public static Dataset generalize(Dataset originalDataset, Map<String, List<String>> toGeneralize){
        System.out.println("Entro");
        List<String> synsetList = originalDataset.filterColumnNames("^bn:");
        for (String s1 : toGeneralize.keySet()){
            System.out.println("Esta en el for. Synset: "+ s1);
            List<String> listAttributeNameToJoin = toGeneralize.get(s1);
            String newAttributeName = generalizeTo.get(s1);
            System.out.println("Nuevo nombre de atributo: "+ newAttributeName);
            listAttributeNameToJoin.add(s1);
            System.out.println("Lista a reducir: "+ listAttributeNameToJoin);
            originalDataset.joinAttributes(listAttributeNameToJoin, newAttributeName, Dataset.COMBINE_SUM, synsetList.contains(newAttributeName));
            System.out.println("");
        }

        System.out.println("Número de atributos: "+ originalDataset.numAttributes());
        return originalDataset;
    }

    /* NOTE! The first time you run this with a dataset should be slower than subsequent ones.
        This is due to the creation of the cached hypernyms map
    */
    public static void main( String[] args )
    {
        long startTime = System.currentTimeMillis();

        Map<String, Integer> targetValues = new HashMap<>();
        targetValues.put("ham", 0);
        targetValues.put("spam", 1);

        //Define transformers for the dataset
        Map<String, Transformer> transformersList = new HashMap<>();
        transformersList.put("target", new Enum2IntTransformer(targetValues));

        //Create Dataset
        String filePath = "outputsyns_testJavier_85227.csv";
        CSVDatasetReader fileDataSet = new CSVDatasetReader(filePath, transformersList);
        Dataset originalDataset = fileDataSet.loadFile();        
        System.out.println("Dataset cargado");

        //Filter Dataset columns
        List<String> synsetList = originalDataset.filterColumnNames("^bn:");

        System.out.println("Synset original: "+ synsetList.size());

        //Create a file that stores all the hypernyms on a map
        cachedHypernyms = save_read_files.read(); 
        createCache(cachedHypernyms, synsetList);  
        cachedHypernyms = save_read_files.read();

        Map<String, List<String>> toGeneralize = new HashMap<String, List<String>>();

        do{
            keepGeneralizing = false;

            String[] arr = quickSort(synsetList.toArray(new String[0]), 0, synsetList.toArray().length - 1);
            synsetList = Arrays.asList(arr);

            originalDataset = generalizeDirectly(synsetList, originalDataset);
            synsetList = originalDataset.filterColumnNames("^bn:");

            /*Map<String, List<String>> evaluateResult = evaluate(originalDataset, synsetList, cachedHypernyms);
            for (String s: evaluateResult.keySet()){
                toGeneralize.put(s, evaluateResult.get(s));
            }*/
            
            toGeneralize.putAll(evaluate(originalDataset, synsetList, cachedHypernyms));
            /*int numberOfGeneralizedSynsets = 0;
            for (String s1: toGeneralize.keySet()){
                numberOfGeneralizedSynsets = numberOfGeneralizedSynsets + 1 + toGeneralize.get(s1).size();
            }*/
            //System.out.println(numberOfGeneralizedSynsets);
            //System.out.println(generalizeTo);

            originalDataset = generalize(originalDataset, toGeneralize);
            synsetList = originalDataset.filterColumnNames("^bn:");

        }while(keepGeneralizing);

        originalDataset.setOutputFile("relationshipDegree_" + maxDegree + ".csv");
        originalDataset.generateCSV();
        String arff = originalDataset.generateARFFWithComments(null, "relationshipDegree_" + maxDegree + ".arff");

        save_read_files.hashMapToTxtFile(toGeneralize /*cachedHypernyms*/);
        save_read_files.hashMapToTxtFile2(generalizeTo);
        /*for(String s: cachedHypernyms.keySet()){
            System.out.println("Synset: " + s);
            System.out.println("Number of hypernyms: "+ cachedHypernyms.get(s).size() +"\n");
        }*/

        /*String[] arr = quickSort(synsetList.toArray(new String[0]), 0, synsetList.toArray().length - 1);
        for(int i = 0; i < arr.length; i++){
            System.out.println("Synset: " + arr[i]);
            System.out.println("Number of hypernyms: "+ cachedHypernyms.get(arr[i]).size() +"\n");
        }*/

        long endTime = System.currentTimeMillis();
        System.out.println("Execution time in milliseconds: " + (endTime - startTime));
    }

}