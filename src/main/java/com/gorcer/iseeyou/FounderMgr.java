package com.gorcer.iseeyou;

import static org.bytedeco.javacpp.opencv_core.cvLoad;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Vector;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.opencv_core.CvSeq;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_objdetect.CvHaarClassifierCascade;
import org.bytedeco.javacpp.tesseract.TessBaseAPI;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.apache.commons.io.FileUtils;

/**
 * Клас для 
 * - хранения информации о полученном результате распознавания
 * - управления временными папками
 * - подсчета времени выполнения
 * @author gorcer
 *
 */
public class FounderMgr {

	//public static FounderMgr self=null;
	public Vector<PlateInfo> plates, rawPlates;
	public long startTime, endTime;
	
	public static String tmpPath = "./tmp";/*"/tmp/iSeeYouAnrp";*/
	private static String tmpPathPostfix;
	
	public IplImage sourceImage;
	public static TessBaseAPI api;
	public static CvHaarClassifierCascade haar;
	
	public static boolean verbose=false;
	
	/*
	public static FounderMgr getInstance() {
		if (self == null)
		{
			self = new FounderMgr();			
		}		
		return self;
	}	*/
	
	public FounderMgr() {
		plates = new Vector<PlateInfo>();
		rawPlates = new Vector<PlateInfo>();
	}
	
	public boolean prepareEnv() {
		
		// uncomment in prod
		tmpPathPostfix = "anrp-"+UUID.randomUUID().toString();
		
		//tmpPathPostfix = "local";
		
		//System.out.println("Prepare env " + tmpPathPostfix);
		// Создаем временное хранилище
		Path path = Paths.get(tmpPath + "/" + tmpPathPostfix);
		
		if (!Files.exists(path)) {
			try {
				Files.createDirectories(path);
			}
			catch (IOException e)
			{
				System.out.println(e.toString());
				return false;
			}
		} 
		
		//clear old tmp paths
		clearOldTmpPaths();
				
		// Отчищаем список
		//plates.clear();
		//rawPlates.clear();
				
		// Инициализируем тезеракт
		api = new TessBaseAPI();
		if (api.Init(null, "avt") != 0) {
		  	System.err.println("Could not initialize tesseract.");
            return false;
		  }
		
		// Инициализация классификатора Хаара
		String classifierName = null;
		File file=null;
		try {
			URL url = new URL("https://raw.githubusercontent.com/Itseez/opencv/master/data/haarcascades/haarcascade_licence_plate_rus_16stages.xml");	        
			file = Loader.extractResource(url, null, "classifier", ".xml");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.err.println("Error loading classifier from url");
            System.exit(1);
		}
        file.deleteOnExit();
        classifierName = file.getAbsolutePath();		
		haar = new CvHaarClassifierCascade(cvLoad(classifierName));
        if (haar.isNull()) {
            System.err.println("Error loading classifier file \"" + classifierName + "\".");
            System.exit(1);
        }
		
		return true;		
	}
	
	private void clearOldTmpPaths() {
		
		File folder = new File(tmpPath);
		File[] listOfFiles = folder.listFiles();

		    for (int i = 0; i < listOfFiles.length; i++) {
		      if (listOfFiles[i].isDirectory() && listOfFiles[i].getName().contains("anrp-")) {
		    	  String fn = tmpPath+"/"+listOfFiles[i].getName();
		    	  Path path = Paths.get(fn);
		    	    BasicFileAttributes attr;
		    	    try {
			    	    attr = Files.readAttributes(path, BasicFileAttributes.class);
			    	    long ageM = ((new Date().getTime()) - attr.creationTime().toMillis())/1000/60;
			    	    // Удаляем папки возрастом более часа
			    	    if (ageM > 60) {
			    	    	FileUtils.deleteDirectory(new File(fn));
			    	    	println("Remove old temp path " + fn);
			    	    }
		    	    } catch (IOException e) {
		    	    System.out.println("oops error! " + e.getMessage());
		    	    }
		      }
		    }
		
		
	}

	// Получение персональной папки для хранения временных файлов инстанса
	public static String getPersonalTmpPath() {
		return tmpPath + "/" + tmpPathPostfix;
	}
	
	public void addPlate(PlateInfo plate) {
		plates.add(plate);
	}
	
	public void start() {
		startTime = System.currentTimeMillis();
	}
	
	public long finish() {
		endTime = System.currentTimeMillis();
		
		try {
			api.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return endTime - startTime;
	}
	
	public long getWorkTime() {
		return (System.currentTimeMillis() - startTime)/1000;
	}
	
	public Vector<String> getNumbers() {
		
		Vector<String> result = new Vector<String>();
		
		for (int i=0;i<plates.size();i++) 
		for (int j=0;j<plates.get(i).numbers.size();j++)
		{
			if (!result.contains(plates.get(i).numbers.get(j)))
				result.add(plates.get(i).numbers.get(j));
		}
		
		return result;
	}
	
	public Map<String,Integer> getNumStat() {
		Map<String,Integer> map = new HashMap<String,Integer>();
		String number;
		int cnt;
		
		for (int i=0;i<plates.size();i++) 
			for (int j=0;j<plates.get(i).numbers.size();j++) {
				number = plates.get(i).numbers.get(j);
				
				if (!map.containsKey(number)) {
					map.put(number, 1);
				} else {
					cnt = map.get(number);
					map.put(number, cnt+1);
				}
			}
		return map;
	}
	
	public String getBestNum() {
		Map<String,Integer> map = getNumStat();
		int max=0;
		String best=null;
		
		for (Map.Entry<String,Integer> entry : map.entrySet()) {
			  
			  if (entry.getValue() > max) {
				  best = entry.getKey();
				  max = entry.getValue();
			  }
			  
			}
		
		return best;
	}

	public Vector<CvSeq> getSquares() {
		
		Vector<CvSeq> result =new Vector<CvSeq>();
		
		for (int i=0;i<plates.size();i++) {
			result.add(plates.get(i).plateCoords);
		}
		
		// TODO Auto-generated method stub
		return result;
	}

	public void destroy() {
		plates=null;
		rawPlates=null;
		api=null;
		//self = null;
		// TODO Auto-generated method stub
		
	}
	
	public static void println(String text) {
		if (verbose == true) {
			System.out.println(text);
		}
	}
	
	public String getJSON() {
		// Формируем JSON ответ
		 String num = getBestNum();
		 JSONObject resultJSON = new JSONObject();
		 if (num == null) {			 
			 resultJSON.put("result", "empty");						 			 
		 } else {			 
			// Лучший вариант
			JSONObject dataJSON = new JSONObject();
			dataJSON.put("popularNumber", num);
			
			// Все планки
			JSONArray platesJSON = new JSONArray();
			for (PlateInfo plate : plates) {
				platesJSON.add(plate.plateImagePath);
			}
			dataJSON.put("plates", platesJSON);
			
			// Лучшая планка - первая попавшаяся			
			dataJSON.put("bestPlate", plates.get(0).plateImagePath);
			
			// Все номера
			JSONArray numbersJSON = new JSONArray();					
			for (Map.Entry<String,Integer> entry : getNumStat().entrySet()) {
				JSONObject numberJSON = new JSONObject();
				numberJSON.put("number", entry.getKey());
				numberJSON.put("cnt", entry.getValue());
				numbersJSON.add(numberJSON);
			}
			dataJSON.put("numbers", numbersJSON);
	
			
			resultJSON.put("result", "success");
			resultJSON.put("data", dataJSON);
		 	
		 }
		 
		 return resultJSON.toString();
	}
}
