package com.gorcer.iseeyou;

import java.util.UUID;
import java.util.Vector;

import org.bytedeco.javacpp.opencv_core.CvSeq;
import org.bytedeco.javacpp.opencv_core.IplImage;
import static org.bytedeco.javacpp.opencv_imgcodecs.*;

public class PlateInfo {

	public String plateImagePath;
	public CvSeq plateCoords;
	public Vector<String> numbers;
	
	/**
	 * Сохраняем изображение и заполняем путь
	 * @param img
	 * @return
	 */
	public String addPlateImage(IplImage img) {
		
		plateImagePath = FounderMgr.getPersonalTmpPath() + "/" + UUID.randomUUID()+".jpg";
		cvSaveImage(plateImagePath, img);  
		
		return plateImagePath;
		
	}
}
