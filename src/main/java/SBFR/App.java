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

import java.io.BufferedWriter;

/*import org.bdp4j.*;

import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.core.Instances;
import weka.knowledgeflow.StepOutputListener;*/

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
//import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
/*import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;*/
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
    //Max relationship degree that we will admit 
    static int maxDegree = 2;

    /**
     * 
     * ESP: Para guardar el mapa de los synsets y sus hiperónimos en el disco
     * ENG: Saves a map containing synsets and their hypernyms in a file
     * 
     * @param mapOfHypernyms Map containing every synset (key) and a list of its hypernyms (values)
    */
	public static void saveMap(Map<String, List<String>> mapOfHypernyms){
		try{
			File fileOne=new File("outputsyns_youtube_old.map");
			FileOutputStream fos=new FileOutputStream(fileOne);
			ObjectOutputStream oos=new ObjectOutputStream(fos);

			oos.writeObject(mapOfHypernyms);
			oos.flush();
			oos.close();
			fos.close();
		}catch(Exception e){}
    }

    /** 
     * 
     * ESP: Lee desde disco un archivo ".map" de tipo <String, List<String>>
     * ENG: Reads a ".map" file from disk with type <String, List<String>> 
     *
     * @return A map containing every synset (key) and a list of its hypernyms (values)
     */
	public static Map<String , List<String>> read() {
        try{
            //Poner aquí el nombre del fichero a cargar. Extensión ".map"
             File toRead=new File("outputsyns_youtube_old.map");
             if (!toRead.exists()){
                return new HashMap<>();
             }
             FileInputStream fis=new FileInputStream(toRead);
             ObjectInputStream ois=new ObjectInputStream(fis);
 
             HashMap<String,List<String>> mapInFile=(HashMap<String,List<String>>)ois.readObject();
 
             ois.close();
             fis.close();
             //print All data in MAP
             return mapInFile;
        }catch(Exception e){}
            return null;
    }

    /**
     * ESP: Guarda un mapa del tipo <String, String> en formato txt
     * ENG: Saves a <String, String>-type map to txt file
     * 
     * @param toWrite the map to save as txt file
     */
    public static void hashMapToTxtFile(Map<String, List<String>> toWrite){
        String outputFilePath = "generalizedSynsets_" + maxDegree + ".txt";

        File file = new File(outputFilePath);
        BufferedWriter bf = null;

        try{
            bf = new BufferedWriter(new FileWriter(file));

            for(Map.Entry<String, List<String>> entry: toWrite.entrySet()){
                bf.write(entry.getKey() + " : " + entry.getValue());
                bf.newLine();
            }

            bf.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try{
                bf.close();
            } catch (Exception e){}
        }
    }

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

        saveMap(cachedBabelUtils.getMapOfHypernyms());
		System.out.println("Saved elements: " + cachedBabelUtils.getMapOfHypernyms().size());
        
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
            
            generalizeTo.put(synset1, s1);

            return 1;
        } else if (s2Hypernyms.contains(s1)) {

            generalizeTo.put(synset1, s1);

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
            List<String> s1Hypernyms = cachedHypernyms.get(s1);
 
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
                    List<String> s2Hypernyms = cachedHypernyms.get(s2);
                    
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
                            
                            usedSynsets.add(s2);
                            s2List.add(s2);
                        }
                    }
                }
                usedSynsets.add(s1);
            }
            if (s2List.size() > 0){
                finalResult.put(s1, s2List);
            }
        }

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
        for (String s1 : toGeneralize.keySet()){
            List<String> listAttributeNameToJoin = toGeneralize.get(s1);
            String newAttributeName = generalizeTo.get(s1);

            listAttributeNameToJoin.add(s1);
            originalDataset.joinAttributeColumns(listAttributeNameToJoin, newAttributeName, Dataset.COMBINE_SUM);
        }

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
        String filePath = "ultimo_outputsyns_100.csv";
        CSVDatasetReader fileDataSet = new CSVDatasetReader(filePath, transformersList);
        Dataset originalDataset = fileDataSet.loadFile();        
        System.out.println("Dataset cargado");

        //Filter Dataset columns
        List<String> synsetList = originalDataset.filterColumnNames("^bn:");

        //System.out.println("Synset original: "+ synsetList);

        //Create a file that stores all the hypernyms on a map
        Map<String, List<String>> cachedHypernyms = read(); 
        createCache(cachedHypernyms, synsetList);  
        cachedHypernyms = read();
        

        Map<String, List<String>> toGeneralize = evaluate(originalDataset, synsetList, cachedHypernyms);
        int numberOfGeneralizedSynsets = 0;
        for (String s1: toGeneralize.keySet()){
            numberOfGeneralizedSynsets = numberOfGeneralizedSynsets + 1 + toGeneralize.get(s1).size();
        }
        System.out.println(numberOfGeneralizedSynsets);
        hashMapToTxtFile(toGeneralize);

        originalDataset = generalize(originalDataset, toGeneralize);
        originalDataset.setOutputFile("relationshipDegree_" + maxDegree + ".csv");
        originalDataset.generateCSV();
        String arff = originalDataset.generateARFFWithComments(null, "relationshipDegree_" + maxDegree + ".arff");

        long endTime = System.currentTimeMillis();
        System.out.println("Execution time in milliseconds: " + (endTime - startTime));
    }

}