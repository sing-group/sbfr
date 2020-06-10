package SBFR;

import java.util.stream.Collectors;

import org.bdp4j.dataset.CSVDatasetReader;

import org.bdp4j.transformers.attribute.Enum2IntTransformer;
import org.bdp4j.types.Transformer;

import org.bdp4j.types.Dataset;

import java.util.*;



/**
 * 
 * @author Javier Quintas Bergantiño
 */

public class App 
{


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
        String filePath = "outputsyns_testJavier_85227.csv"/*"ultimo_outputsyns_100.csv"*/;
        CSVDatasetReader fileDataSet = new CSVDatasetReader(filePath, transformersList);
        Dataset originalDataset = fileDataSet.loadFile();        
        System.out.println("Dataset cargado");

        GeneralizationTransformation gt = new GeneralizationTransformation(2, Dataset.COMBINE_SUM, true);

        Dataset resultDataset = gt.transformTemplate(originalDataset);

        long endTime = System.currentTimeMillis();
        System.out.println("Execution time in milliseconds: " + (endTime - startTime));
    }

}