package com.gorcer.iseeyou;

import java.util.Vector;

public class FoundedMgr {

	public static FoundedMgr self=null;
	public Vector<PlateInfo> plates;
	public long startTime, endTime;
	 
	
	public static FoundedMgr getInstance() {
		if (self == null)
		{
			self = new FoundedMgr();
			self.plates = new Vector<PlateInfo>();
		}
		
		return self;
	}
	
	public void addPlate(PlateInfo plate) {
		plates.add(plate);
	}
	
	public void start() {
		startTime = System.currentTimeMillis();
	}
	
	public long finish() {
		endTime = System.currentTimeMillis();
		
		return endTime - startTime;
	}
	
	public long getWorkTime() {
		return (endTime - startTime)/1000;
	}
}
