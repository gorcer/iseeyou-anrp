package com.gorcer.iseeyou;

public class RecognizeConfig {
	
	public int Thresh;
	public int minThresh=30;
	public boolean doThreshold=true;
	public boolean doCanny=false;
	public boolean doDilate=false;
	public boolean doPyr=true;
	public int minContourArea=50;
	public double maxCosine=0.4;
	public double maxSquare=500000;
	public double maxAspectRatio=0.5;
	public double ApproxAccuracy=0.02;
	public int n=0;
	

}
