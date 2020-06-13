package org.SBFR;

import org.bdp4j.types.Dataset;
import org.junit.Before;
import org.junit.Test;
import weka.core.Attribute;
import weka.core.Instance;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class GeneralizationTransformationTest {

	Dataset cleanDataset;
	Dataset dataset;
	ArrayList<Attribute> attributes = new ArrayList<>();
	List<String> target_values;
	int maxDegree = 2;
	Dataset.CombineOperator combineOperator = Dataset.COMBINE_SUM;
	List<String> orderedSynsetList;
	GeneralizationTransformation gt;
	
	
	@Before
	public void setUp() {
		target_values = new ArrayList<>();
		target_values.add("0");
		target_values.add("1");
		
		attributes.add(new Attribute("id", true)); //0
		attributes.add(new Attribute("length")); //1
		attributes.add(new Attribute("length_after_drop")); //2
		attributes.add(new Attribute("bn:00033982n")); //3 Felino
		attributes.add(new Attribute("bn:00010309n")); //4 Grandes Felinos
		attributes.add(new Attribute("bn:00049156n")); //5 León
		attributes.add(new Attribute("bn:01610183n")); //6 León Blanco
		attributes.add(new Attribute("bn:00050713n")); //7 Leopardo
		attributes.add(new Attribute("bn:00050720n")); //8 Leopardo Hembra
		attributes.add(new Attribute("bn:00060435n")); //9 Pantera Negra
		attributes.add(new Attribute("target", target_values)); //10
		
		dataset = new Dataset("test", attributes, 0);

		gt = new GeneralizationTransformation(maxDegree, combineOperator, true);
		
	}

	@Test
	public void testGenerateCachedHypernymsMap(){
		Instance instance = dataset.createDenseInstance();
		instance.setValue(0, "1");
		instance.setValue(1, 18d);
		instance.setValue(2, 12d);
		instance.setValue(3, 0);
		instance.setValue(4, 0);
		instance.setValue(5, 0);
		instance.setValue(6, 0);
		instance.setValue(7, 0);
		instance.setValue(8, 0);
		instance.setValue(9, 0);
		instance.setValue(10, 1);

		Dataset resultDataset;

		resultDataset = gt.transformTemplate(dataset);

		File file = new File("outputsyns_file.map");
		assertTrue(file.exists());
	}

	@Test
	public void testGenerateCSVFile(){
		Instance instance = dataset.createDenseInstance();
		instance.setValue(0, "1");
		instance.setValue(1, 18d);
		instance.setValue(2, 12d);
		instance.setValue(3, 0);
		instance.setValue(4, 0);
		instance.setValue(5, 0);
		instance.setValue(6, 0);
		instance.setValue(7, 0);
		instance.setValue(8, 0);
		instance.setValue(9, 0);
		instance.setValue(10, 1);

		Dataset resultDataset;

		resultDataset = gt.transformTemplate(dataset);

		File file = new File("relationshipDegree_" + maxDegree + ".csv");
		assertTrue(file.exists());
	}

	@Test
	public void testGenerateARFFFile(){
		Instance instance = dataset.createDenseInstance();
		instance.setValue(0, "1");
		instance.setValue(1, 18d);
		instance.setValue(2, 12d);
		instance.setValue(3, 0);
		instance.setValue(4, 0);
		instance.setValue(5, 0);
		instance.setValue(6, 0);
		instance.setValue(7, 0);
		instance.setValue(8, 0);
		instance.setValue(9, 0);
		instance.setValue(10, 1);

		List<String> synsetList = dataset.filterColumnNames("^bn:");

		Dataset resultDataset;
		resultDataset = gt.transformTemplate(dataset);

		List<String> resultSynsetList = resultDataset.filterColumnNames("^bn:");

		File file = new File("relationshipDegree_" + maxDegree + ".arff");
		assertTrue(file.exists());
	}
	
	@Test
	public void testGeneralizeVertically() {
		Instance instance = dataset.createDenseInstance();
		instance.setValue(0, "1");
		instance.setValue(1, 18d);
		instance.setValue(2, 12d);
		instance.setValue(3, 1);
		instance.setValue(4, 1);
		instance.setValue(5, 1);
		instance.setValue(6, 1);
		instance.setValue(7, 1);
		instance.setValue(8, 1);
		instance.setValue(9, 1);
		instance.setValue(10, 1);
		
		instance = dataset.createDenseInstance();
		instance.setValue(0, "2");
		instance.setValue(1, 19d);
		instance.setValue(2, 13d);
		instance.setValue(3, 0);
		instance.setValue(4, 1);
		instance.setValue(5, 0);
		instance.setValue(6, 1);
		instance.setValue(7, 0);
		instance.setValue(8, 0);
		instance.setValue(9, 0);
		instance.setValue(10, 1);
		
		instance = dataset.createDenseInstance();
		instance.setValue(0, "3");
		instance.setValue(1, 20d);
		instance.setValue(2, 14d);
		instance.setValue(3, 1);
		instance.setValue(4, 0);
		instance.setValue(5, 1);
		instance.setValue(6, 0);
		instance.setValue(7, 1);
		instance.setValue(8, 1);
		instance.setValue(9, 1);
		instance.setValue(10, 0);
		
		List<String> synsetList = dataset.filterColumnNames("^bn:");
		
		Dataset resultDataset;

		resultDataset = gt.transformTemplate(dataset);
		
		List<String> resultSynsetList = resultDataset.filterColumnNames("^bn:");
		
		assertEquals(synsetList.size() - 1, resultSynsetList.size());
		assertTrue(!resultSynsetList.contains("bn:01610183n"));
		assertTrue(resultSynsetList.contains("bn:00010309n"));
	}

	@Test
	public void testGeneralizeHorizontallyToExistingSynset(){
		Instance instance = dataset.createDenseInstance();
		instance.setValue(0, "1");
		instance.setValue(1, 18d);
		instance.setValue(2, 12d);
		instance.setValue(3, 1);
		instance.setValue(4, 1);
		instance.setValue(5, 1);
		instance.setValue(6, 1);
		instance.setValue(7, 1);
		instance.setValue(8, 1);
		instance.setValue(9, 1);
		instance.setValue(10, 1);

		instance = dataset.createDenseInstance();
		instance.setValue(0, "2");
		instance.setValue(1, 19d);
		instance.setValue(2, 13d);
		instance.setValue(3, 0);
		instance.setValue(4, 0);
		instance.setValue(5, 1);
		instance.setValue(6, 0);
		instance.setValue(7, 1);
		instance.setValue(8, 0);
		instance.setValue(9, 0);
		instance.setValue(10, 1);

		instance = dataset.createDenseInstance();
		instance.setValue(0, "3");
		instance.setValue(1, 20d);
		instance.setValue(2, 14d);
		instance.setValue(3, 1);
		instance.setValue(4, 1);
		instance.setValue(5, 0);
		instance.setValue(6, 1);
		instance.setValue(7, 0);
		instance.setValue(8, 1);
		instance.setValue(9, 1);
		instance.setValue(10, 0);

		List<String> synsetList = dataset.filterColumnNames("^bn:");

		Dataset resultDataset;

		resultDataset = gt.transformTemplate(dataset);

		List<String> resultSynsetList = resultDataset.filterColumnNames("^bn:");

		assertEquals(synsetList.size() - 2, resultSynsetList.size());
		assertFalse(resultSynsetList.contains("bn:00049156n") && resultSynsetList.contains("bn:00050713n"));
		assertTrue(resultSynsetList.contains("bn:00010309n"));
	}

	@Test
	public void testGeneralizeHorizontallyToNonExistingSynset(){

		Instance instance = dataset.createDenseInstance();
		instance.setValue(0, "1");
		instance.setValue(1, 18d);
		instance.setValue(2, 12d);
		instance.setValue(3, 1);
		instance.setValue(4, 1);
		instance.setValue(5, 1);
		instance.setValue(6, 1);
		instance.setValue(7, 1);
		instance.setValue(8, 1);
		instance.setValue(9, 1);
		instance.setValue(10, 1);

		instance = dataset.createDenseInstance();
		instance.setValue(0, "2");
		instance.setValue(1, 19d);
		instance.setValue(2, 13d);
		instance.setValue(3, 0);
		instance.setValue(4, 0);
		instance.setValue(5, 0);
		instance.setValue(6, 0);
		instance.setValue(7, 0);
		instance.setValue(8, 1);
		instance.setValue(9, 1);
		instance.setValue(10, 1);

		instance = dataset.createDenseInstance();
		instance.setValue(0, "3");
		instance.setValue(1, 20d);
		instance.setValue(2, 14d);
		instance.setValue(3, 1);
		instance.setValue(4, 1);
		instance.setValue(5, 1);
		instance.setValue(6, 1);
		instance.setValue(7, 1);
		instance.setValue(8, 0);
		instance.setValue(9, 0);
		instance.setValue(10, 0);

		List<String> toDelete = new ArrayList<>();
		toDelete.add("bn:00050713n");

		dataset.deleteAttributeColumns(toDelete);

		List<String> synsetList = dataset.filterColumnNames("^bn:");

		Dataset resultDataset;

		resultDataset = gt.transformTemplate(dataset);

		List<String> resultSynsetList = resultDataset.filterColumnNames("^bn:");

		assertEquals(synsetList.size() - 1, resultSynsetList.size());
		assertFalse(resultSynsetList.contains("bn:00050720n") && resultSynsetList.contains("bn:00060435n"));
		assertTrue(resultSynsetList.contains("bn:00050713n"));
	}

	@Test
	public void testNoGeneralizationHamSpam(){
		Instance instance = dataset.createDenseInstance();
		instance.setValue(0, "1");
		instance.setValue(1, 18d);
		instance.setValue(2, 12d);
		instance.setValue(3, 1);
		instance.setValue(4, 1);
		instance.setValue(5, 1);
		instance.setValue(6, 1);
		instance.setValue(7, 1);
		instance.setValue(8, 1);
		instance.setValue(9, 1);
		instance.setValue(10, 1);

		instance = dataset.createDenseInstance();
		instance.setValue(0, "1");
		instance.setValue(1, 18d);
		instance.setValue(2, 12d);
		instance.setValue(3, 1);
		instance.setValue(4, 1);
		instance.setValue(5, 1);
		instance.setValue(6, 1);
		instance.setValue(7, 1);
		instance.setValue(8, 1);
		instance.setValue(9, 1);
		instance.setValue(10, 0);

		List<String> synsetList = dataset.filterColumnNames("^bn:");

		Dataset resultDataset;
		resultDataset = gt.transformTemplate(dataset);

		List<String> resultSynsetList = resultDataset.filterColumnNames("^bn:");

		assertEquals(synsetList.size(), resultSynsetList.size());
	}

	@Test
	public void testNoGeneralizationNoVerticalRelation(){
		Instance instance = dataset.createDenseInstance();
		instance.setValue(0, "1");
		instance.setValue(1, 18d);
		instance.setValue(2, 12d);
		instance.setValue(3, 1);
		instance.setValue(4, 1);
		instance.setValue(5, 1);
		instance.setValue(6, 1);
		instance.setValue(7, 1);
		instance.setValue(8, 1);
		instance.setValue(9, 1);
		instance.setValue(10, 1);

		instance = dataset.createDenseInstance();
		instance.setValue(0, "1");
		instance.setValue(1, 18d);
		instance.setValue(2, 12d);
		instance.setValue(3, 1);
		instance.setValue(4, 0);
		instance.setValue(5, 0);
		instance.setValue(6, 1);
		instance.setValue(7, 0);
		instance.setValue(8, 0);
		instance.setValue(9, 0);
		instance.setValue(10, 1);

		instance = dataset.createDenseInstance();
		instance.setValue(0, "1");
		instance.setValue(1, 18d);
		instance.setValue(2, 12d);
		instance.setValue(3, 0);
		instance.setValue(4, 1);
		instance.setValue(5, 1);
		instance.setValue(6, 0);
		instance.setValue(7, 1);
		instance.setValue(8, 1);
		instance.setValue(9, 1);
		instance.setValue(10, 0);

		List<String> synsetList = dataset.filterColumnNames("^bn:");

		Dataset resultDataset;
		resultDataset = gt.transformTemplate(dataset);

		List<String> resultSynsetList = resultDataset.filterColumnNames("^bn:");

		assertEquals(synsetList.size(), resultSynsetList.size());
	}

	@Test
	public void testNoGeneralizationNoHorizontalRelation(){
		Instance instance = dataset.createDenseInstance();
		instance.setValue(0, "1");
		instance.setValue(1, 18d);
		instance.setValue(2, 12d);
		instance.setValue(3, 1);
		instance.setValue(4, 1);
		instance.setValue(5, 1);
		instance.setValue(6, 1);
		instance.setValue(7, 1);
		instance.setValue(8, 1);
		instance.setValue(9, 1);
		instance.setValue(10, 1);

		instance = dataset.createDenseInstance();
		instance.setValue(0, "1");
		instance.setValue(1, 18d);
		instance.setValue(2, 12d);
		instance.setValue(3, 0);
		instance.setValue(4, 0);
		instance.setValue(5, 0);
		instance.setValue(6, 1);
		instance.setValue(7, 1);
		instance.setValue(8, 0);
		instance.setValue(9, 0);
		instance.setValue(10, 1);

		instance = dataset.createDenseInstance();
		instance.setValue(0, "1");
		instance.setValue(1, 18d);
		instance.setValue(2, 12d);
		instance.setValue(3, 1);
		instance.setValue(4, 1);
		instance.setValue(5, 1);
		instance.setValue(6, 0);
		instance.setValue(7, 0);
		instance.setValue(8, 1);
		instance.setValue(9, 1);
		instance.setValue(10, 0);

		List<String> synsetList = dataset.filterColumnNames("^bn:");

		Dataset resultDataset;
		resultDataset = gt.transformTemplate(dataset);

		List<String> resultSynsetList = resultDataset.filterColumnNames("^bn:");

		assertEquals(synsetList.size(), resultSynsetList.size());
	}
	
}

