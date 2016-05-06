package com.gorcer.iseeyou;

import java.util.Vector;

import org.bytedeco.javacpp.opencv_core.CvSeq;
import org.bytedeco.javacpp.opencv_core.IplImage;

public class PlateInfo {

	public IplImage plateImage;
	public CvSeq plateCoords;
	public Vector<String> numbers;
	
}
