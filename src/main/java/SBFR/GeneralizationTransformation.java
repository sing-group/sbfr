package SBFR;

    /*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import org.bdp4j.types.Dataset;
import org.bdp4j.types.DatasetTransformer;


import org.nlpa.util.BabelUtils;

import it.uniroma1.lcl.babelnet.BabelNet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.bdp4j.util.Pair;
import org.nlpa.util.*;

/**
 * This class contains all the methods necessary for the 
 * reduction of dimensionality of a dataset based on the synsets it contains. 
 * This is achieved through the generalization of similar synsets.
 * Two synsets are considered similar if they are not too far away from each other
 * and they both have a similar ham/spam rate (greater or equal than 90% or lower or equal than 10%)
 * 
 * @author Javier Quintas Bergantiño
 */
public class GeneralizationTransformation extends DatasetTransformer {

    private static final File file = new File("outputsyns_file.map");
    private static final Dataset.CombineOperator DEFAULT_OPERATOR = Dataset.COMBINE_SUM;
    private static final int DEFAULT_DEGREE = 2;

    private static BabelNet bn = BabelNet.getInstance();

    /**
     * Map used to save the hypernyms to which synset pairs will generalize
     */
    private static Map<String, String> generalizeTo = new HashMap<>();
    private static Map<String, String> auxGeneralizeTo = new HashMap<>();

    /**
     * Max relationship degree that we will admit
     */
    static int maxDegree;

    /**
     * Whether ".csv" and ".arff" files should be generated or not
     */
    static boolean generateFiles;

    /**
     * Combine operator to use when joining attributes
     */
    static Dataset.CombineOperator combineOperator;

    /**
     * Map containing synsets as keys and their list of hypernyms as values
     */
    private static Map<String, List<String>> cachedHypernyms;

    /**
     * Boolean variable needed to determine if the algorithm should keep
     * generalizing
     */
    private static boolean keepGeneralizing;

    /**
     * Auxiliar map used to save the relationship degree between two synsets
     */
    private static Map<Pair<String, String>, Integer> degreeMap = new HashMap<>();
    private static Map<String, List<String>> toGeneralizePrint = new HashMap<>();

    /**
     * For logging purposes
     */
    private static final Logger logger = LogManager.getLogger(GeneralizationTransformation.class);

    /**
     * 
     * Constructor that lets you customize execution options
     * 
     * @param degree the max degree that will be allowed
     * @param operator the operator used to combine attributes
     * @param generate decides whether output files will be generated or not
     */
    public GeneralizationTransformation(int degree, Dataset.CombineOperator operator, boolean generate){
        this.maxDegree = degree;
        this.combineOperator = operator;
        this.generateFiles = generate;
    }

    /**
     * 
     * Constructor that lets you customize execution options
     * 
     * @param degree the max degree that will be allowed
     * @param operator the operator used to combine attributes
     */
    public GeneralizationTransformation(int degree, Dataset.CombineOperator operator){
        this.maxDegree = degree;
        this.combineOperator = operator;
        this.generateFiles = true;
    }

    /**
     * Default constructor.
     */
    public GeneralizationTransformation() {
        this.maxDegree = DEFAULT_DEGREE;
        this.combineOperator = DEFAULT_OPERATOR;
        this.generateFiles = true;
    }

    @Override
    protected Dataset transformTemplate(Dataset dataset) {


        Dataset originalDataset = dataset;
        logger.info("Dataset loaded");

        //Filter Dataset columns
        List<String> synsetList = originalDataset.filterColumnNames("^bn:");

        System.out.println("Original synset: "+ synsetList.size());

        //Create a file that stores all the hypernyms on a map
        cachedHypernyms = readMap();
        createCache(cachedHypernyms, synsetList);
        cachedHypernyms = readMap();

        System.out.println(originalDataset.filterColumnNames("^bn:").size());

        Map<String, List<String>> toGeneralize = new HashMap<>();

        //Loop that keeps generalizing while possible
        do {
            keepGeneralizing = false;

            //The synsetlist gets sorted by hypernym list size
            String[] arr = quickSort(synsetList.toArray(new String[0]), 0, synsetList.toArray().length - 1);
            synsetList = Arrays.asList(arr);
            logger.info("Synsets sorted");

            //Generalize synsets with those that appear on its hypernym list and distance <= maxDegree
            originalDataset = generalizeVertically(synsetList, originalDataset);
            synsetList = originalDataset.filterColumnNames("^bn:");

            toGeneralize.putAll(evaluate(originalDataset, synsetList, cachedHypernyms));
            for (String s : toGeneralize.keySet()) {
                toGeneralizePrint.put(s, toGeneralize.get(s));
            }
            logger.info("");

            originalDataset = generalize(originalDataset, toGeneralize);
            logger.info("Synsets generalized");

            synsetList = originalDataset.filterColumnNames("^bn:");

            toGeneralize.clear();

        } while (keepGeneralizing);

        if(generateFiles){
            originalDataset.setOutputFile("relationshipDegree_" + maxDegree + ".csv");
            originalDataset.generateCSV();
            String arff = originalDataset.generateARFFWithComments(null, "relationshipDegree_" + maxDegree + ".arff");
        }


        System.out.println(originalDataset.filterColumnNames("^bn:").size());
        return originalDataset;
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * 
     * Receives a synset list and checks if they are already stored, if
     * not, they get added to the map along with a list of its hypernyms
     *
     * @param cachedHypernyms contains a map with <synsets, hypernyms> already
     * saved in disk
     * @param synsetList list of all the synsets that we want to check if they
     * are already in disk or not
     */
    private static void createCache(Map<String, List<String>> cachedHypernyms, List<String> synsetList) {
        CachedBabelUtils cachedBabelUtils = new CachedBabelUtils(cachedHypernyms);

        for (String s : synsetList) {
            if (!cachedBabelUtils.existsSynsetInMap(s)) {
                cachedBabelUtils.addSynsetToCache(s, BabelUtils.getDefault().getAllHypernyms(s));
                System.out.println("Adding {0}"+ s);

                for (String h : cachedBabelUtils.getCachedSynsetHypernymsList(s)) {
                    if (!cachedBabelUtils.existsSynsetInMap(h)) {
                        cachedBabelUtils.addSynsetToCache(h, BabelUtils.getDefault().getAllHypernyms(h));
                    }
                }
            }
        }

        saveMap(cachedBabelUtils.getMapOfHypernyms());

    }

    /**
     *
     * Adds a new synset to the map saved in disk
     *
     * @param cachedHypernyms contains a map with <synsets, hypernyms> already
     * saved in disk
     * @param synset synset that we want to add to disk
     */
    private static void addNewCachedSynset(Map<String, List<String>> cachedHypernyms, String synset) {
        CachedBabelUtils cachedBabelUtils = new CachedBabelUtils(cachedHypernyms);

        if (!cachedBabelUtils.existsSynsetInMap(synset)) {
            cachedBabelUtils.addSynsetToCache(synset, BabelUtils.getDefault().getAllHypernyms(synset));
            System.out.println("Adding "+ synset);

            for (String h : cachedBabelUtils.getCachedSynsetHypernymsList(synset)) {
                if (!cachedBabelUtils.existsSynsetInMap(h)) {
                    cachedBabelUtils.addSynsetToCache(h, BabelUtils.getDefault().getAllHypernyms(h));
                }
            }
        }

        saveMap(cachedBabelUtils.getMapOfHypernyms());
    }

    /**
     *
     * Sorting algorithm
     *
     * @param arr the array to sort
     * @param begin the index from which we will be sorting
     * @param end the index of the final element we will be sorting
     * @return the sorted array
     */
    private static String[] quickSort(String[] arr, int begin, int end) {
        if (begin < end) {
            int partitionIndex = partition(arr, begin, end);

            quickSort(arr, begin, partitionIndex - 1);
            quickSort(arr, partitionIndex + 1, end);
        }
        return arr;
    }

    /**
     *
     * Essential function needed for quicksort algorithm
     */
    private static int partition(String[] arr, int begin, int end) {
        String pivot = arr[end];
        int i = begin - 1;

        for (int j = begin; j < end; j++) {
            if (cachedHypernyms.get(arr[j]).size() >= cachedHypernyms.get(pivot).size()) {
                i++;

                String swapTemp = arr[i];
                arr[i] = arr[j];
                arr[j] = swapTemp;
            }
        }

        String swapTemp = arr[i + 1];
        arr[i + 1] = arr[end];
        arr[end] = swapTemp;

        return i + 1;
    }

    /**
     *
     * Obtains the hypernyms of a synset
     *
     * @param synset the synset from which we want to get its hypernyms
     * @return list of its hypernyms
     */
    private static List<String> getHypernyms(String synset) {
        List<String> toRet;

        if (cachedHypernyms.keySet().contains(synset)) {
            toRet = cachedHypernyms.get(synset);
        } else {
            addNewCachedSynset(cachedHypernyms, synset);
            cachedHypernyms = readMap();
            toRet = cachedHypernyms.get(synset);
        }

        return toRet;
    }

    /**
     *
     * Generalizes synsets with those that appear in its hypernyms list with a
     * distance less or equal to maxDegree
     *
     * @param synsetList the list of synsets in the dataset
     * @param originalDataset the original dataset
     * @return dataset that had its vertical relationships generalized
     */
    private static Dataset generalizeVertically(List<String> synsetList, Dataset originalDataset) {
        List<String> usedSynsets = new ArrayList<>();
        for (String s1 : synsetList) {
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
            float percentage1 = (spam1 / (ham1 + spam1));
            System.out.println("Synset 1: "+ s1 + " -> " + result1 + " -> " +percentage1);

            if (percentage1 >= 0.99 || percentage1 <= 0.01) {
                for (String s2 : synsetList.subList(index + 1, synsetList.size())) {
                    //Get hypernym list of both synsets
                    List<String> s1Hypernyms = getHypernyms(s1);
                    List<String> s2Hypernyms = getHypernyms(s2);

                    if (s2Hypernyms.contains(s1) || s1Hypernyms.contains(s2)) {
                        String expressionS2 = "(" + s2 + " >= 1) ? 1 : 0";

                        Map<String, Integer> result2 = originalDataset.evaluateColumns(expressionS2,
                                int.class,
                                new String[]{s2},
                                new Class[]{double.class},
                                "target");

                        float ham2 = (float) result2.get("0");
                        float spam2 = (float) result2.get("1");
                        float percentage2 = (spam2 / (ham2 + spam2));

                        Pair<String, String> pair = new Pair<>(s1, s2);

                        if (!degreeMap.containsKey(pair)) {
                            int degree = relationshipDegree(s1, s2, s1Hypernyms, s2Hypernyms);
                            degreeMap.put(pair, degree);
                        }

                        if ((percentage2 >= 0.99 && percentage1 >= 0.99) || (percentage2 <= 0.01 && percentage1 <= 0.01)) {
                            if (s1Hypernyms.contains(s2) && degreeMap.get(pair) <= maxDegree && degreeMap.get(pair) >= 0) {
                                List<String> listAttributeNameToJoin = new ArrayList<>();
                                Boolean aux = usedSynsets.contains(s2);
                                System.out.println("Synset 1: " + s1 + " -> "+ result1 +" Synset 2: "+s2+" -> "+result2);
                                generalizeTo.put(s1, s2);

                                List<String> auxPrint = new ArrayList<>();
                                auxPrint.add(s2);
                                toGeneralizePrint.put(s1, auxPrint);

                                listAttributeNameToJoin.add(s1);
                                listAttributeNameToJoin.add(s2);

                                originalDataset.joinAttributes(listAttributeNameToJoin, s2, combineOperator, !aux);

                                if (!usedSynsets.contains(s1)) {
                                    usedSynsets.add(s1);
                                }

                                break;
                            } else if (s2Hypernyms.contains(s1) && degreeMap.get(pair) <= maxDegree && degreeMap.get(pair) >= 0) {
                                List<String> listAttributeNameToJoin = new ArrayList<>();
                                Boolean aux = usedSynsets.contains(s1);
                                System.out.println("Synset 1: " + s1 + " -> "+ result1 +" Synset 2: "+s2+" -> "+result2);
                                generalizeTo.put(s2, s1);

                                List<String> auxPrint = new ArrayList<>();
                                auxPrint.add(s1);
                                toGeneralizePrint.put(s2, auxPrint);

                                listAttributeNameToJoin.add(s2);
                                listAttributeNameToJoin.add(s1);

                                originalDataset.joinAttributes(listAttributeNameToJoin, s1, combineOperator, !aux);
                                

                                keepGeneralizing = true;

                                if (!usedSynsets.contains(s2)) {
                                    usedSynsets.add(s2);
                                }

                                break;
                            }
                        }
                    }

                }
            }
        }
        logger.info("");
        return originalDataset;
    }

    /**
     *
     * Determines the relationship degree between two synsets
     *
     * @param synset1 synset that we want to evaluate
     * @param synset2 synset that we want to evaluate
     * @param s1Hypernyms list containing all the hypernyms of synset1
     * @param s2Hypernyms list containing all the hypernyms of synset2
     *
     * @return degree of relationship between both synsets
     *
     */
    private static int relationshipDegree(String synset1, String synset2,
            List<String> s1Hypernyms, List<String> s2Hypernyms) {
        if (s1Hypernyms.size() == 0) {
            return Integer.MIN_VALUE;
        }

        String s1 = s1Hypernyms.get(0);

        if (s1.equals(synset2)) {
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
     * Function that evaluates more
     * complex relations between two synsets and decides if they should be generalized
     *
     * @param originalDataset the original dataset that we are working with
     * @param synsetList list of all the synsets in the dataset
     * @param cachedHypernyms map containing every synset and its hypernyms
     * (previously read from disk)
     * @return map containing a synset as key and a list of synsets that should be generalized with it as value
     *
     */
    private static Map<String, List<String>> evaluate(Dataset originalDataset, List<String> synsetList, Map<String, List<String>> cachedHypernyms) {
        Map<String, List<String>> finalResult = new HashMap<>();
        List<String> usedSynsets = new ArrayList<>();
        for (String s1 : synsetList) {

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
            float percentage1 = (spam1 / (ham1 + spam1));

            List<String> s2List = new ArrayList<>();

            if ((percentage1 >= 0.99 || percentage1 <= 0.01) && !usedSynsets.contains(s1)) {
                System.out.println("Synset 1: "+ s1 + " -> " + result1 + " -> " +percentage1);
                //Iterate through a sublist of the original synset that contains only from the next synset to s1 onwards
                for (String s2 : synsetList.subList(index + 1, synsetList.size())) {

                    List<String> s1Hypernyms = getHypernyms(s1);
                    List<String> s2Hypernyms = getHypernyms(s2);

                    Pair<String, String> pair = new Pair<>(s1, s2);

                    if (!degreeMap.containsKey(pair)) {
                        int degree = relationshipDegree(s1, s2, s1Hypernyms, s2Hypernyms);
                        degreeMap.put(pair, degree);
                    }

                    if (degreeMap.get(pair) >= 0 && degreeMap.get(pair) <= maxDegree && !usedSynsets.contains(s2) && !finalResult.containsKey(s1)) {
                        String expressionS2 = "(" + s2 + " >= 1) ? 1 : 0";

                        Map<String, Integer> result2 = originalDataset.evaluateColumns(expressionS2,
                                int.class,
                                new String[]{s2},
                                new Class[]{double.class},
                                "target");

                        float ham2 = (float) result2.get("0");
                        float spam2 = (float) result2.get("1");
                        float percentage2 = (spam2 / (ham2 + spam2));
                        if ((percentage2 >= 0.99 && percentage1 >= 0.99) || (percentage2 <= 0.01 && percentage1 <= 0.01)) {
                            //Results from evaluating these synsets
                            System.out.println("Synset 1: " + s1 + " -> "+ result1 +" Synset 2: "+s2+" -> "+result2);

                            if (s1Hypernyms.indexOf(generalizeTo.get(s1)) <= s1Hypernyms.indexOf(auxGeneralizeTo.get(s1))) {
                                generalizeTo.put(s1, auxGeneralizeTo.get(s1));
                            }

                            usedSynsets.add(s2);
                            s2List.add(s2);

                            keepGeneralizing = true;
                        }
                    }
                }
                usedSynsets.add(s1);
            }
            if (s2List.size() > 0) {
                finalResult.put(s1, s2List);
            }
        }
        logger.info("\n");
        return finalResult;
    }

    /**
     * 
     * Function used to reduce the number of synsets on the original dataset
     *
     * @param originalDataset the original dataset that we want to reduce
     * @param toGeneralize map of synsets that we will be generalizing
     *
     * @return the originalDataset modified and reduced according to the
     * generalizable synsets
     */
    private static Dataset generalize(Dataset originalDataset, Map<String, List<String>> toGeneralize) {

        for (String s1 : toGeneralize.keySet()) {

            List<String> listAttributeNameToJoin = new ArrayList<>();
            listAttributeNameToJoin.addAll(toGeneralize.get(s1));
            String newAttributeName = generalizeTo.get(s1);

            System.out.println("New attribute name: "+ newAttributeName);

            listAttributeNameToJoin.add(s1);

            System.out.println("List to reduce: "+ listAttributeNameToJoin);

            List<String> synsetList = originalDataset.filterColumnNames("^bn:");
            
            originalDataset.joinAttributes(listAttributeNameToJoin, newAttributeName, combineOperator, synsetList.contains(newAttributeName));

            logger.info("\n");
        }

        System.out.println("Number of features after reducing: "+ originalDataset.filterColumnNames("^bn:").size());

        return originalDataset;
    }

    /**
     *
     * Reads a ".map" file from disk with type <String, List<String>>
     *
     * @return A map containing every synset (key) and a list of its hypernyms
     * (values)
     */
    private static Map<String, List<String>> readMap() {
        try {
            //Poner aquí el nombre del fichero a cargar. Extensión ".map"
            if (!file.exists()) {
                return new HashMap<>();
            }
            HashMap<String, List<String>> mapInFile;
            try (FileInputStream fis = new FileInputStream(file);
                    ObjectInputStream ois = new ObjectInputStream(fis)) {
                mapInFile = (HashMap<String, List<String>>) ois.readObject();
            }
            //print All data in MAP
            return mapInFile;
        } catch (Exception e) {
            System.err.println("[READ]" + e.getMessage());
        }
        return null;
    }

    /**
     *
     * Saves a map containing synsets and their hypernyms in a file
     *
     * @param mapOfHypernyms Map containing every synset (key) and a list of its
     * hypernyms (values)
     */
    private static void saveMap(Map<String, List<String>> mapOfHypernyms) {
        try {
            try (FileOutputStream fos = new FileOutputStream(file);
                    ObjectOutputStream oos = new ObjectOutputStream(fos);) {

                oos.writeObject(mapOfHypernyms);
            }
        } catch (Exception e) {
            System.err.println("[SAVE MAP]" + e.getMessage());
        }
    }

    /**
     *
     * @return maxDegree variable value
     */
    public static int getMaxDegree() {
        return maxDegree;
    }

    /**
     *
     * @param maxDegree the new value that maxDegree will have
     */
    public static void setMaxDegree(int maxDegree) {
        GeneralizationTransformation.maxDegree = maxDegree;
    }

    /**
     *
     * @return generateFiles variable value
     */
    public static boolean isGenerateFiles() {
        return generateFiles;
    }

    /**
     *
     * @param generateFiles the new value that generateFiles will have
     */
    public static void setGenerateFiles(boolean generateFiles) {
        GeneralizationTransformation.generateFiles = generateFiles;
    }

    /**
     *
     * @return combineOperator variable value
     */
    public static Dataset.CombineOperator getCombineOperator() {
        return combineOperator;
    }

    /**
     *
     * @param combineOperator the new value that combineOperator will have
     */
    public static void setCombineOperator(Dataset.CombineOperator combineOperator) {
        GeneralizationTransformation.combineOperator = combineOperator;
    }
}

