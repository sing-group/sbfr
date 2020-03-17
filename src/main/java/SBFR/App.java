package SBFR;

//import types.CachedBabelUtils;
import org.bdp4j.dataset.CSVDatasetReader;
import org.bdp4j.pipe.AbstractPipe;
import org.bdp4j.pipe.SerialPipes;
import org.bdp4j.transformers.Enum2IntTransformer;
import org.bdp4j.types.Instance;
import org.bdp4j.types.Transformer;
import org.bdp4j.util.InstanceListUtils;
import org.bdp4j.types.Dataset;
import org.nlpa.util.BabelUtils;

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

public class App 
{
    //Para guardar el mapa de los synsets y sus hiperónimos en el disco
    //Saves a map containing synsets and their hypernyms in a file
	public static void guardar(Map<String, List<String>> mapOfHypernyms){
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
    
    //Para leer el fichero que contiene el mapa de hiperónimos y esta en el disco
    //Reads the file that contains the hypernym map
	public static Map<String , List<String>> leer() {
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

    public static void evaluate(Dataset originalDataset, List<String> synsetList, Map<String, List<String>> cachedHypernyms){
        for (String s1: synsetList){
            synsetList.remove(s1);
            List<String> s1Hypernyms = cachedHypernyms.get(s1);
            
            for (String s2: synsetList){
                List<String> s2Hypernyms = cachedHypernyms.get(s2);

                if (s1Hypernyms == s2Hypernyms){

                }
            }
        }

    }

    public static void main( String[] args )
    {
        List<String> result;
        Map<String, Integer> targetValues = new HashMap<>();
        targetValues.put("ham", 0);
        targetValues.put("spam", 1);

        //Lets define transformers for the dataset
        Map<String, Transformer> transformersList = new HashMap<>();
        transformersList.put("target", new Enum2IntTransformer(targetValues));

        String filePath = "ultimo_outputsyns_100.csv";
        CSVDatasetReader fileDataSet = new CSVDatasetReader(filePath, transformersList);
        Dataset originalDataset = fileDataSet.loadFile();
        
        System.out.println("Dataset cargado");

        List<String> synsetList = originalDataset.filterColumnNames("^bn:|^target");
        result = synsetList;

        System.out.println("Synset original: "+ synsetList);

        Map<String, List<String>> cachedHypernyms = leer();
        CachedBabelUtils cachedBabelUtils = new CachedBabelUtils(cachedHypernyms);

        for(String s: synsetList){
            if(!cachedBabelUtils.existsSynsetInMap(s)){
                cachedBabelUtils.addSynsetToCache(s, BabelUtils.getDefault().getAllHypernyms(s));
                System.out.println("Añadiendo "+ s);

                for(String h: cachedBabelUtils.getCachedSynsetHypernymsList(s)){
                    if(!cachedBabelUtils.existsSynsetInMap(h)){
                        cachedBabelUtils.addSynsetToCache(h, BabelUtils.getDefault().getAllHypernyms(h));
                    }
                }
            }
        }

        guardar(cachedBabelUtils.getMapOfHypernyms());
		System.out.println("Elementos guardados: " + cachedBabelUtils.getMapOfHypernyms().size());
        
        /*Map<String, List<String>> */
        cachedHypernyms = leer();
        

        for(String s1: synsetList){
            synsetList.remove(s1);
            List<String> s1Hypernyms = cachedHypernyms.get(s1);
            for (String s2: synsetList){
                List<String> s2Hypernyms = cachedHypernyms.get(s2);
                if (s1Hypernyms == s2Hypernyms) {

                }
            }
        }

        
        //Map<String, Integer> result = originalDataset.evaluateColumns(expression, expressionType, parameterNames, parameterTypes, resultColumn);
        



    }

}