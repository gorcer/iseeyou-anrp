package com.gorcer.iseeyou;

import java.util.Vector;

public class FoundedMgr {

	public static FoundedMgr self=null;
	public Vector<PlateInfo> plates;
	 
	
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
}
