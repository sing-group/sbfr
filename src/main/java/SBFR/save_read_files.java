package SBFR;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.BufferedWriter;
import java.util.*;

    



public class save_read_files {
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

    public static void hashMapToTxtFile(Map<String, List<String>> toWrite){
        String outputFilePath = "generalizedSynsets_" + App.maxDegree + ".txt"/*"cachedHypernyms.txt"*/;

        File file = new File(outputFilePath);
        BufferedWriter bf = null;

        try{
            bf = new BufferedWriter(new FileWriter(file));

            for(Map.Entry<String, List<String>> entry: toWrite.entrySet()){
                bf.write("Generalize to synset: "+ App.generalizeTo.get(entry.getKey()));
                bf.newLine();
                bf.write("List of generalized synsets: [" + entry.getKey() + ", ");
                for(int i = 0; i < entry.getValue().size(); i++){
                    if (i == entry.getValue().size() - 1){
                        bf.write(entry.getValue().get(i) + "]");
                    }else{
                        bf.write(entry.getValue().get(i) + ", ");
                    }
                }
                bf.newLine();
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

    public static void hashMapToTxtFile2(Map<String, String> toWrite){
        String outputFilePath = "generalizeTo_" + App.maxDegree + ".txt";

        File file = new File(outputFilePath);
        BufferedWriter bf = null;

        try{
            bf = new BufferedWriter(new FileWriter(file));

            for(Map.Entry<String, String> entry: toWrite.entrySet()){
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
}