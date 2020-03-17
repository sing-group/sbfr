package SBFR;

import java.io.Serializable;
import java.util.ArrayList;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nlpa.util.BabelUtils;

/**
 *
 * @author María Novo
 */
public class CachedBabelUtils implements Serializable {

    /**
     * A Map to save information of synsets and hypernyms
     */
    private Map<String, List<String>> mapOfHypernyms;

    /**
     * The default constructor for this class
     */
    public CachedBabelUtils() {
        mapOfHypernyms = new HashMap<>();
    }

    public CachedBabelUtils(Map<String, List<String>> map) {
        this.mapOfHypernyms = map;
    }

    /**
     * Add a new element to the cachedBabelUtils Map structure. Only a synset is
     * needed the hypernyms are calculated automatically
     *
     * @param synset The synset to be added
     */
    public void addSynsetToCacheAutomatic(String synset) {
        if (!this.mapOfHypernyms.containsKey(synset)) {
            mapOfHypernyms.put(synset, BabelUtils.getDefault().getAllHypernyms(synset));
        }
    }

    /**
     * Indicates if synset1 is included in Hypernyms list of synset 2 (synset1
     * is father of synset2)
     *
     * @param synset1 Synset1 to check if is included in hypernyms list of
     * synset2
     * @param synset2 Synset2 synset to get hypernyms list to compare
     * @return return True or False
     */
    public boolean isSynsetFatherOf(String synset1, String synset2) {
        if (synset1 != null && synset2 != null && mapOfHypernyms.get(synset1)!=null) {
            return mapOfHypernyms.get(synset1).contains(synset2);
        }
        return false;
    }

    /**
     * Indicates if a register of synset is included in the cachedBabelUtils Map
     * structure
     *
     * @param synset Synset to check
     * @return return True or False
     */
    public boolean existsSynsetInMap(String synset) {
        return this.mapOfHypernyms.containsKey(synset);
    }

    /**
     * Add a new element to the cachedBabelUtils Map structure.
     *
     * @param synset Synset to be the key
     * @param hypernymsList hypernym list of the synset
     */
    public void addSynsetToCache(String synset, List<String> hypernymsList) {
        this.mapOfHypernyms.put(synset, hypernymsList);
    }

    /**
     * gets the hypernym of the synset n-leves abode. If the level value
     * exceeded the number of items in the list, it returns the last available
     * item.
     *
     * @param synset The Synset to search its hypernym.
     * @param level The number of levels to be scaled.
     * @return a string with the hypernym synset ID
     */
    public String getCachedHypernym(String synset, int level) {
        if (mapOfHypernyms.get(synset) != null) {
            int elements = (mapOfHypernyms.get(synset).size());
            if (level >= elements) {
                return mapOfHypernyms.get(synset).get(elements - 1);
            } else {
                return mapOfHypernyms.get(synset).get(level);
            }
        }
        return null;
    }

    /**
     * gets the hypernyms list stored in CachedBabelUtils for the given synset.
     *
     * @param synset The Synset to get its hypernyms.
     * @return a List with the hypernyms
     */
    public List<String> getCachedSynsetHypernymsList(String synset) {

        if (existsSynsetInMap(synset)) {
            return mapOfHypernyms.get(synset);
        } else {
            return null;
        }
    }

//    public void cachedScalateSynset (String synset, int levels) {
//    	if (!mapOfHypernyms.containsKey(synset)) {
//    		List<String> tmpList = new ArrayList<String>(mapOfHypernyms.get(synset));
//    		for (int i=1; i<=levels; i++) {
//    			tmpList.remove(0);
//    		}
//    		mapOfHypernyms.put(tmpList.get(0), tmpList);		
//    	}
//    }
    /**
     * to get the Map of the class.
     *
     * @return a Map
     */
    public Map<String, List<String>> getMapOfHypernyms() {
        return mapOfHypernyms;
    }

    public void setMapOfHypernyms(Map<String, List<String>> mapOfHypernyms) {
        this.mapOfHypernyms = mapOfHypernyms;
    }

    public void clearCache() {
        mapOfHypernyms.clear();
    }

    /**
     * To print the content of the Map inside CachedBabelUtils.
     *
     * @return toString() output
     */
    @Override
    public String toString() {
        return mapOfHypernyms.toString();
    }

}
