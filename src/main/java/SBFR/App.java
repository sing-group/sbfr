package SBFR;

//import types.CachedBabelUtils;
import java.util.concurrent.*;
import org.bdp4j.dataset.CSVDatasetReader;
import org.bdp4j.pipe.AbstractPipe;
import org.bdp4j.pipe.SerialPipes;
import org.bdp4j.transformers.Enum2IntTransformer;
import org.bdp4j.types.Instance;
import org.bdp4j.types.Transformer;
import org.bdp4j.util.InstanceListUtils;
import org.bdp4j.types.Dataset;
import org.nlpa.util.BabelUtils;

import it.uniroma1.lcl.babelnet.BabelNet;
import it.uniroma1.lcl.babelnet.BabelNetUtils;
import it.uniroma1.lcl.babelnet.BabelSynsetRelation;
import it.uniroma1.lcl.babelnet.BabelSynset;

import org.bdp4j.*;

import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.core.Instances;
import weka.knowledgeflow.StepOutputListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * 
 * @author Javier Quintas Bergantiño
 */

public class App 
{
    /**
     * 
     * ESP: Para guardar el mapa de los synsets y sus hiperónimos en el disco
     * ENG: Saves a map containing synsets and their hypernyms in a file
     * 
     * @param mapOfHypernyms Map containing every synset (key) and a list of its hypernyms (values)
    */
	public static void save(Map<String, List<String>> mapOfHypernyms){
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

        save(cachedBabelUtils.getMapOfHypernyms());
		System.out.println("Saved elements: " + cachedBabelUtils.getMapOfHypernyms().size());
        
    }

    public static int parentingDegree(String synset1, String synset2, Map<String, List<String>> cachedHypernyms){
        return 0;
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
    public static void evaluate(Dataset originalDataset, List<String> synsetList, Map<String, List<String>> cachedHypernyms){
        for(String s1: synsetList){
            List<String> s1Hypernyms = cachedHypernyms.get(s1);
 
            //If the synset to evaluate has no fathers, we use it, else, we get its first hypernym
            String s1Father = s1Hypernyms.get(0);
            if(s1Hypernyms.size() > 1){
                s1Father = s1Hypernyms.get(1);
            }
            //We get the index of the synset
            int index = synsetList.indexOf(s1);

            //Iterate through a sublist of the original synset that contains only from the next synset to s1 onwards
            for (String s2: synsetList.subList(index + 1, synsetList.size())){
                List<String> s2Hypernyms = cachedHypernyms.get(s2);
                String s2Father = s2Hypernyms.get(0);
                if(s2Hypernyms.size() > 1){
                    s2Father = s2Hypernyms.get(1);
                }

                if (s1Father == s2Father) {
                    String expressionS1 = "(" + s1 + " >= 1) ? 1 : 0";
                    String expressionS2 = "(" + s2 + " >= 1) ? 1 : 0";

                    Map<String, Integer> result1 = originalDataset.evaluateColumns(expressionS1,
                        int.class, 
                        new String[]{s1}, 
                        new Class[]{double.class}, 
                        "target");
    
                    Map<String, Integer> result2 = originalDataset.evaluateColumns(expressionS2,
                        int.class, 
                        new String[]{s2}, 
                        new Class[]{double.class}, 
                        "target");
                         
                    //Results from evaluating these synsets
                    System.out.print("Synset 1: " + s1 + " -> " + result1);
                    System.out.println(" Synset 2: "+ s2 + " -> " + result2);

                }
            }
        }
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
        

        evaluate(originalDataset, synsetList, cachedHypernyms);

        long endTime = System.currentTimeMillis();
        System.out.println("Execution time in milliseconds: " + (endTime - startTime));
    }

}