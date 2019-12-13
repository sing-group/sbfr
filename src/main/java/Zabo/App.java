package Zabo;


import org.bdp4j.dataset.CSVDatasetReader;
import org.bdp4j.pipe.AbstractPipe;
import org.bdp4j.pipe.SerialPipes;
import org.bdp4j.transformers.Enum2IntTransformer;
import org.bdp4j.types.Instance;
import org.bdp4j.types.Transformer;
import org.bdp4j.util.InstanceListUtils;
import org.bdp4j.types.Dataset;

import org.bdp4j.*;

import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.core.Instances;
import weka.knowledgeflow.StepOutputListener;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class App 
{
    public static void main( String[] args )
    {
       
        //generateInstances("/Users/zabo/Documents/Proyectos Visual Studio Code/firstMVNProject/src/main/java/Zabo/outputsyns_sms_last.csv");

        //Then load the dataset to use it with Weka TM
        Map<String, Integer> targetValues = new HashMap<>();
        targetValues.put("ham", 0);
        targetValues.put("spam", 1);

        //Lets define transformers for the dataset
        Map<String, Transformer> transformersList = new HashMap<>();
        transformersList.put("target", new Enum2IntTransformer(targetValues));

        String filePath = "ultimo_outputsyns_100.csv";
        CSVDatasetReader fileDataSet = new CSVDatasetReader(filePath, transformersList);
        Dataset originalDataset = fileDataSet.loadFile();        
    }

}
