package com.gorcer.iseeyou;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Vector;

import org.bytedeco.javacpp.opencv_core.CvSeq;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.tesseract.TessBaseAPI;

/**
 * Клас для 
 * - хранения информации о полученном результате распознавания
 * - управления временными папками
 * - подсчета времени выполнения
 * @author gorcer
 *
 */
public class FounderMgr {

	public static FounderMgr self=null;
	public Vector<PlateInfo> plates, rawPlates;
	public long startTime, endTime;
	
	public String tmpPath = "/tmp/iSeeYouAnrp";
	private String tmpPathPostfix;
	
	public IplImage sourceImage;
	public TessBaseAPI api;
	
	public boolean verbose=false;
	
	public static FounderMgr getInstance() {
		if (self == null)
		{
			//System.out.println("Create founder");
			self = new FounderMgr();
			self.plates = new Vector<PlateInfo>();
			self.rawPlates = new Vector<PlateInfo>();
		}		
		return self;
	}	
	
	public boolean prepareEnv() {
		
		// uncomment in prod
		//tmpPathPostfix = UUID.randomUUID().toString();
		
		tmpPathPostfix = "local";
		
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
		
		// Отчищаем список
		self.plates.clear();
		self.rawPlates.clear();
				
		// Инициализируем тезеракт
		self.api = new TessBaseAPI();
		if (self.api.Init(null, "avt") != 0) {
		  	System.err.println("Could not initialize tesseract.");
            return false;
		  }
		
		return true;		
	}
	
	// Получение персональной папки для хранения временных файлов инстанса
	public String getPersonalTmpPath() {
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
		return (endTime - startTime)/1000;
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
		self.plates=null;
		self.rawPlates=null;
		self = null;
		// TODO Auto-generated method stub
		
	}
	
	public void println(String text) {
		if (verbose == true) {
			System.out.println(text);
		}
	}
}
