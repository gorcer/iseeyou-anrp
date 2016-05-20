package com.gorcer.iseeyou;


import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*import net.sourceforge.tess4j.*;*/

import org.bytedeco.javacv.*;
import org.bytedeco.javacpp.*;
import org.bytedeco.javacpp.opencv_core.CvMemStorage;
import org.bytedeco.javacpp.opencv_core.CvRect;
import org.bytedeco.javacpp.opencv_core.CvSeq;
import org.bytedeco.javacpp.opencv_core.IplImage;

import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_imgproc.*;
import static org.bytedeco.javacpp.opencv_objdetect.*;
import static org.bytedeco.javacpp.opencv_imgcodecs.*;
import static org.bytedeco.javacpp.helper.opencv_objdetect.cvHaarDetectObjects;
import static org.bytedeco.javacpp.lept.*;

public class Recognizer {

	
	static double angle( CvPoint pt1, CvPoint pt2, CvPoint pt0 )
	{
		double dx1 = pt1.x() - pt0.x();
	    double dy1 = pt1.y() - pt0.y();
	    double dx2 = pt2.x() - pt0.x();
	    double dy2 = pt2.y() - pt0.y();
	    return (dx1*dx2 + dy1*dy2)/Math.sqrt((dx1*dx1 + dy1*dy1)*(dx2*dx2 + dy2*dy2) + 1e-10);
	}

	
	public static IplImage prepareImage( IplImage src,  CvMemStorage storage, RecognizeConfig config)
	{	
	    IplImage gray = cvCreateImage( cvGetSize(src), IPL_DEPTH_8U, 1 );
	    cvConvertImage(src, gray, 0);
	    //cvSaveImage("tmp/src.jpg", src);    
	    
	    // Уменьшает в два раза, затем увеличивает, в результате мелкие детали исчезают
	    if (config.doPyr)
	    {
	    	CvSize sz = cvSize(gray.width() & -2, gray.height() & -2);
	    	IplImage pyr = cvCreateImage(cvSize(sz.width()/2, sz.height()/2), gray.depth(), gray.nChannels());
	    	
	    	cvPyrDown(gray, pyr, CV_GAUSSIAN_5x5);
	    	// Fatal Error тут был
	    	cvReleaseImage(gray);
	    	gray = cvCreateImage(cvSize(2*pyr.width(), 2*pyr.height()), pyr.depth(), pyr.nChannels());          
	    	cvPyrUp(pyr, gray, CV_GAUSSIAN_5x5);
	    }
	    
	    if (config.doSmooth) {
	    	// сглаживаем исходную картинку
	        cvSmooth(gray, gray);
	     //   cvSaveImage("artifacts/smooth.jpg", gray); 
	    }

	    if (config.doDilate)
	    	cvDilate(gray, gray, null, 1);	    
	    
	    if (config.doCanny)	    
	    	 cvCanny(gray, gray, 0, config.Thresh, 3);
	 	
	    if (config.doThreshold) {
	    	cvThreshold(gray, gray, config.Thresh, 255, CV_THRESH_BINARY);
	    	cvAdaptiveThreshold(gray, gray, config.Thresh, CV_ADAPTIVE_THRESH_GAUSSIAN_C, CV_THRESH_BINARY, 7, 1);
	    }

		return gray;
	}
	
	public static IplImage Transform(CvRect rect, CvSeq poly, IplImage img, int n)
	{	
		IplImage tmp = cvCreateImage( cvSize(208, 48), img.depth(), img.nChannels() );		
		CvMat warp_mat = cvCreateMat(3, 3, CV_32FC1);
		Vector<CvPoint> pts;
		
		pts = getDirectPoints(poly, rect);
		if (pts==null) return(null);
		
		//cvCvtSeqToArray(poly, pts, CV_WHOLE_SEQ);
		
		double[] srcArr = new double[8];		
		// Переводим полигон в массив (необходимо унифицировать направление)
		for(int j = 0; j < 4 ; j ++  )
	    {
    		srcArr[j*2]=(int)pts.get(j).x()-rect.x();
    		srcArr[j*2+1]=(int)pts.get(j).y()-rect.y();	
    		
    		
    		//System.out.println("Transform src x="+srcArr[j*2]+" y="+srcArr[j*2+1]);    		 
	    }
		
		
		JavaCV.getPerspectiveTransform(srcArr, new double[]{0,0,tmp.width(),0,tmp.width(),tmp.height(),0,tmp.height()}, warp_mat);
		
		//cvGetAffineTransform(srcArr, new float[]{0,0,52,0,20,40}, warp_mat);
		
		cvSetImageROI(img,rect);
		cvWarpPerspective(img, tmp, warp_mat);		
		//cvSaveImage(tmpPath + "/afine"+n+".jpg",  tmp);
		cvResetImageROI(img);
		
		/*warp_mat=null;
		pts=null;*/
		
		return(tmp);
	}
	
	public static Vector<CvSeq> findHaarFiltered(IplImage img) {
		
		Vector<CvSeq> result = new Vector<CvSeq>();
		
		// Готовим изображение
		CvMemStorage storage3 = CvMemStorage.create();		
		RecognizeConfig config = new RecognizeConfig();
		config.doThreshold=false;
		config.doCanny=false;				
		config.Thresh=config.minThresh*4;
		config.doPyr=false; 
		config.doSmooth=false;
		config.doDilate=true;
		IplImage prepareImg = prepareImage(img, storage3, config);	
		cvClearMemStorage(storage3);
		
		// Применяем каскад
		CvMemStorage storage = CvMemStorage.create();	       
        CvSeq plates = cvHaarDetectObjects(prepareImg, FounderMgr.haar, storage,
                1.1, 0, CV_HAAR_DO_CANNY_PRUNING | CV_HAAR_SCALE_IMAGE);
		cvClearMemStorage(storage);
		
		
		// Собираем многоугольники
		CvRect r;
		CvMemStorage storage2 = CvMemStorage.create();  
		for(int i = 0; i < plates.total(); i++){
			r = new CvRect(cvGetSeqElem(plates, i));
			CvSeq approx =cvCreateSeq(CV_SEQ_ELTYPE_POINT,  Loader.sizeof(CvSeq.class), Loader.sizeof(CvPoint.class), storage2);		

			cvSeqPush(approx, new CvPoint(r.x(), r.y()));
			cvSeqPush(approx, new CvPoint(r.x()+r.width(), r.y()));
			cvSeqPush(approx, new CvPoint(r.x()+r.width(), r.y()+r.height()));
			cvSeqPush(approx, new CvPoint(r.x(), r.y()+r.height()));
			
			result.add(approx);
		}		
		cvClearMemStorage(storage2);
		
		
		
		return result;
		
	}
	
	public static Vector<CvSeq> findPolysFiltered(IplImage img, IplImage original, CvMemStorage storage, RecognizeConfig config)
	{
		Vector<CvSeq> squares = new Vector<CvSeq>();
		CvSeq contours = new CvSeq(null);
		CvSeq approx;
		double maxCosine;	
		double cosine;
		
		IplImage tmp = cvCloneImage(img);
		//cvSaveImage("tmp/ok-"+config.n+"-"+config.Thresh+".jpg",   img);
		cvFindContours(tmp, storage, contours, Loader.sizeof(CvContour.class), CV_RETR_LIST, CV_CHAIN_APPROX_SIMPLE);
		
		// Перебираем контуры
		while (contours != null && !contours.isNull()) 
		{
			
			//Если точек в контуре > 0
			if (contours.elem_size() > 0) 
			{
                approx = cvApproxPoly(contours, Loader.sizeof(CvContour.class),storage, CV_POLY_APPROX_DP, cvContourPerimeter(contours)*config.ApproxAccuracy, 0);
                
                /*if (approx.total() == 4 && Math.abs(cvContourArea(approx, CV_WHOLE_SEQ, 0)) > config.minContourArea)
                	System.out.println(cvCheckContourConvexity(approx));*/
                
                if (approx.total() == 4 // Четыре стороны
                    && Math.abs(cvContourArea(approx, CV_WHOLE_SEQ, 0)) > config.minContourArea
                    && cvCheckContourConvexity(approx) != 0 // контур замкнут
                    )
                {
                	
                	 maxCosine = 0;
                	 // Перебираем все углы, ищем максимальный
                	 for( int j = 2; j < 5; j++ )
                     {
                		// find the maximum cosine of the angle between joint edges
                        cosine = Math.abs(angle(new CvPoint(cvGetSeqElem(approx, j)), new CvPoint(cvGetSeqElem(approx, j-2)), new CvPoint(cvGetSeqElem(approx, j-1))));
                        maxCosine = Math.max(maxCosine, cosine);
                     }
                	 
                	 if( maxCosine < config.maxCosine)	{
                         CvRect rect=cvBoundingRect(approx, 1);
                         if (                      		 
                        		 //rect.width()*rect.height()<config.maxSquare && // Макс площадь
                        		 rect.width()>rect.height() &&  // Ширина больше высоты
                        		 //Math.abs(((float)x.height()/x.width())-config.maxAspectRatio)<0.1 && 
                        		  (rect.width()/(float)img.width()<0.9) && (rect.height()/(float)img.height()<0.9) // не более 90% размеров изображения
                        		 )	{  
                        	 			squares.add(approx);    
                         			}
                         //else System.out.println("False w : "+x.width()+" x h : "+x.height()+" = "+x.width()*x.height());
                	 }
                }
			}
			
			contours = contours.h_next();
		}
		approx=null;
		return squares; 
	}
	
	public static Vector<String> RecognizeNumber(IplImage src) {
		
		
		String outText = null;
		BytePointer recText = null;
		CvMemStorage storage = CvMemStorage.create();
		PIX pixImage;
		Matcher m;
		Vector<String> result = new Vector<String>();

		  Pattern p = Pattern.compile("^[ABCEHKMOPTXY]\\d{3}[ABCEHKMOPTXY]{2}\\d{2,3}$");
		  /*outText="K095CX77";	 
		  m = p.matcher(outText);
		  System.out.println(" m:"+m.matches());
		  return "";*/
		  
		  String tmpPath = FounderMgr.getPersonalTmpPath();
		  
		  RecognizeConfig config = new RecognizeConfig();
		  
		  for (int i=0;i<50;i++) {
		  
			config.doThreshold=true;
			config.doDilate=false;
			config.doCanny=false;				
			config.Thresh=0+i*10;
			config.doPyr=false;
			config.n=i;
				
				IplImage prepareImg = prepareImage(src, storage, config);
				
				// Перевод изоражения в PIX
				cvSaveImage(tmpPath + "/plate.jpg", prepareImg);				
				pixImage = pixRead(tmpPath + "/plate.jpg");
				
				// Распознаем
				FounderMgr.api.SetImage(pixImage);
				recText = FounderMgr.api.GetUTF8Text();
				
				// Для отладки распознавалки, потом убрать
				/*PlateInfo rawPlate = new PlateInfo();
    	 		rawPlate.plateImage = prepareImg;
    	 		rawPlate.numbers = new Vector<String>();
    	 		*/
    	 		// Если найден текст
				if (recText != null) {
									
					outText = recText.getString();
					// Убираем все лишнее
					outText = outText.replaceAll("[^ABCEHKMOPTXY0-9]", "");
					//rawPlate.numbers.add(outText);
					m = p.matcher(outText);  
					//System.out.println(outText);
					// Если текст соответствует маске номера
					if (m.matches() == true) {
						
	        	 		if (!result.contains(outText))
	        	 			result.add(outText); 
						//System.out.println("["+i+","+j+"]"+" num:["+outText+"] m:"+m.matches());
					}
				}
				
				//mgr.rawPlates.add(rawPlate);
				//System.out.println("["+i+","+j+"]"+" num:["+outText+"] m:"+m.matches());
		  }
		  cvClearMemStorage(storage);		  
		  outText=null;
		  recText=null;
		  pixImage=null;
		  m=null;
		  
          return result;
	}


	/**
	 * Разворот многоугольника
	 * @param poly
	 * @param rect
	 * @return
	 */
	private static Vector<CvPoint> getDirectPoints(CvSeq poly, CvRect rect) {
		// TODO Auto-generated method stub
		CvPoint pts = new CvPoint(4);
		CvPoint tmpPoint = new CvPoint();
		
		//CvPoint ptsNew = new CvPoint(4);
		cvCvtSeqToArray(poly, pts, CV_WHOLE_SEQ);
		
		Vector<CvPoint> ptsNew = new Vector<CvPoint>();
		
		int x,y, minJ=0;
		double min=0, s;
		
		tmpPoint = new CvPoint();
		ptsNew.add(tmpPoint.x(rect.x()).y(rect.y()));
		
		tmpPoint = new CvPoint();
		ptsNew.add(tmpPoint.x(rect.x()+rect.width()).y(rect.y()));
		
		tmpPoint = new CvPoint();
		ptsNew.add(tmpPoint.x(rect.x()+rect.width()).y(rect.y()+rect.height()));
		
		tmpPoint = new CvPoint();
		ptsNew.add(tmpPoint.x(rect.x()).y(rect.y()+rect.height()));
		
		
		
		// Разворачиваем к квадрату
		for(int i = 0; i < 4 ; i ++  )
		{			
		for(int j = 0; j < 4 ; j ++  )
	    {
    		x=Math.abs((int)pts.position(j).x()-ptsNew.get(i).x());
    		y=Math.abs((int)pts.position(j).y()-ptsNew.get(i).y());	
    		s=Math.sqrt(x*x+y*y);
    		
    		if (j==0)
    			{min=s;minJ=j;}
    		else
    		 if (min>s) 
    		 	{min=s;minJ=j;}   
    		
    		//System.out.println("Find x="+(int)pts.position(j).x()+" y="+(int)pts.position(j).y()+" s="+s+" min="+min+" minJ="+minJ);
	    }
		
		ptsNew.get(i).x(pts.position(minJ).x());
		ptsNew.get(i).y(pts.position(minJ).y());		
		}		
		
		return ptsNew;
	}


	public static Vector<CvSeq> findPolys( IplImage src)
	{
		Vector<CvSeq> squares = new Vector<CvSeq>();
		Vector<CvSeq> tmpSquares = new Vector<CvSeq>();
		CvMemStorage storage = CvMemStorage.create();
		IplImage prepareImg = null;
		
		RecognizeConfig config = new RecognizeConfig();
		for (int j=0;j<2;j++)
		for (int i=0;i<100;i++)
		{
			if (j == 0)
			{
				config.doThreshold=false;
				//config.doDilate=true;
				config.doCanny=true;				
				config.Thresh=config.minThresh*1+i*10;
				config.doPyr=true; //true
				config.doSmooth=false;
			}
			else
			{
				config.doThreshold=true;
				config.Thresh = config.minThresh+i*5;
				config.doCanny=false;
				config.doDilate=false;
				config.doPyr=false;
				config.doSmooth=true;
			}
			config.n=j*100+i;
			
			prepareImg = prepareImage(src, storage, config);
						
			//cvSaveImage(FounderMgr.getPersonalTmpPath()+"/filtered"+config.n+".jpg", prepareImg);
			
			tmpSquares = findPolysFiltered(prepareImg, src, storage, config);			
			
			squares.addAll(tmpSquares);
		}
		cvReleaseImage(prepareImg);
		//System.out.println("Total found "+squares.size()+" squares");	
		
		cvClearMemStorage(storage);
		tmpSquares=null;
		prepareImg=null;
		
		return squares;
		
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
	
	/**
	 * Обработка изображения
	 * @param filename
	 * @param mgr
	 */
	public static void process(String filename, FounderMgr mgr)
	{
		mgr.start();
		
		final IplImage image = cvLoadImage(filename);
		
		mgr.sourceImage = image;	
		Vector<CvSeq> polys = new Vector<CvSeq>(); 
		polys = findPolys( image);
		mgr.println("Found " + polys.size() + " polygons via FindPoly");
		
		Vector<CvSeq> haarPolys = findHaarFiltered(image);
		mgr.println("Found " + haarPolys.size() + " polygons via Haar Cascade");
		polys.addAll(haarPolys);
		
		 //для отладки
		/*CvSeq approx;
		for (int i=0; i<haarPolys.size(); i++) {
			approx = haarPolys.get(i);
			CvPoint pts = new CvPoint(4);
			cvCvtSeqToArray(approx, pts, CV_WHOLE_SEQ);
			System.out.println(pts.toString());
		}*/		
		
		
		
		optimizeSquares(polys);
		// есть проблема, не все вычищает с первого раза, поэтому пока так. Исследовать на примере https://s.auto.drom.ru/5/sales/photos/19233/19232478/151658366.jpg
		optimizeSquares(polys);
		
		mgr.println("Polygons after optimization " + polys.size() + " polygons");
		mgr.plates = findNumbers(polys, image);
		mgr.println("Found " + mgr.plates.size() + " plates");
		mgr.println("Found numbers: " + mgr.getNumStat());		
		
		mgr.finish();
		mgr=null;
	}
	
	/**
	 * Перебираем найденные многоугольники и пытаемся найти в них номерные знаки
	 * @param poly
	 * @param original
	 * @return
	 */
	private static Vector<PlateInfo> findNumbers(Vector<CvSeq> poly, IplImage original) {
		
		CvRect rect;
		IplImage tmpImage;
		Vector<String> recognized;
		Vector<PlateInfo> result = new Vector<PlateInfo>();
		
		CvSeq approx;
		for (int i=0; i<poly.size(); i++) {
			approx = poly.get(i);
			CvPoint pts = new CvPoint(4);
			cvCvtSeqToArray(approx, pts, CV_WHOLE_SEQ);
			
			rect=cvBoundingRect(approx, 1);
			
			tmpImage = Transform(rect, approx, original, i);
	 		recognized = RecognizeNumber(tmpImage); 		
	 		
	 		// Сохраняем информацию о найденном номере
	 		if (recognized.size() > 0) {
		 		PlateInfo plate = new PlateInfo();	
		 		plate.addPlateImage(tmpImage);
		 		plate.plateCoords = approx;
		 		plate.numbers = recognized;  
		 		result.add(plate);
	 		}
		}
		
		return result;
	}


	private static void optimizeSquares(Vector<CvSeq> plates) {
		
		CvSeq plateI, plateJ;
		CvPoint pI, pJ;
		int equalPoints;
		
		// TODO Auto-generated method stub
		if (plates.size() > 0)
		for(int i=0; i<plates.size();i++) 
			for(int j=0; j<plates.size();j++) {
				
				if (i == j) continue;
				
				if (i>=plates.size() || j>=plates.size())
					continue;
				
				plateI = plates.get(i);
				plateJ = plates.get(j);
				
				
				equalPoints=0;
				for (int n=0;n<4;n++) {
					pI = new CvPoint(cvGetSeqElem(plateI, n));
					pJ = new CvPoint(cvGetSeqElem(plateJ, n));

					if ( Math.abs(pI.x() - pJ.x()) < 10 && Math.abs(pI.y() - pJ.y()) < 10) {
						equalPoints++;
					}
				}
				
				if (equalPoints == 4) {
					plates.remove(j);
				}
		}
	}


	public static void drawSquares( IplImage image, final Vector<CvSeq> squares )
	{
		CvFont font = new CvFont();
		CvSeq seq;
		
	    if((squares.size() >0) ){    
	            
	        cvInitFont( font, CV_FONT_HERSHEY_COMPLEX_SMALL, .6, .6, 0, 1, 6);

	     //   CvPoint2D32f srcTri=new CvPoint2D32f(3);
	     //   CvPoint2D32f dstTri=new CvPoint2D32f(3);
	        
	        
	        
	        //CvMat warp_mat = cvCreateMat(2, 3, CV_32FC1);

	        CvPoint pts = new CvPoint(4);
	        for(int i = 0; i < squares.size(); i ++  )
	        { 	
	        	
	        	
	        	
	        	//System.out.println("i="+i+"; t="+pts.capacity());
	        	seq = (CvSeq)squares.get(i);
	        	cvCvtSeqToArray(seq, pts, CV_WHOLE_SEQ);
	                    
	            
	        	//CvRect Rect = new CvRect(10, 10, 100, 100);  //cvBoundingRect(tRect, 1);
	        	//System.out.println(Rect);        	
	        	
	        	// cvSetImageROI(image,Rect);
	        	 
	        	 //System.out.println(image);
	        	 
	        	//int step=150;
	        	
	        	//float[] dstArr = {i*step,0,i*step,Math.round(step*0.2),step+i*step,Math.round(step*0.2)};
	        	//float[] srcArr = new float[6];
	                
		        
		        /*
	        	for(int j = 0; j < 3 ; j ++  )
			    {
		        	//srcTri.position(j).x(pts.position(j).x());
		        	//srcTri.position(j).y(pts.position(j).y());
	        		srcArr[j*2]=(float)pts.position(j).x();
	        		srcArr[j*2+1]=(float)pts.position(j).y();	        		
	        		 
			    }
	        	
	        	for(int j = 1; j < 3 ; j ++  )
			    {     		
	        		//cvDrawLine(image, new CvPoint((int)srcArr[j*2],(int)srcArr[j*2+1]), new CvPoint((int)srcArr[(j-1)*2],(int)srcArr[(j-1)*2+1]), CvScalar.RED, 1, CV_AA, 0);
	        		//cvDrawLine(image, new CvPoint((int)dstArr[j*2],(int)dstArr[j*2+1]), new CvPoint((int)dstArr[(j-1)*2],(int)dstArr[(j-1)*2+1]), CvScalar.BLUE, 1, CV_AA, 0);
	        		//System.out.println("src "+j+" x="+srcTri.position(j).x()+"; y="+srcTri.position(j).y());
	        		//System.out.println("dst "+j+" x="+dstTri.position(j).x()+"; y="+dstTri.position(j).y());
			    }
	        	*/
	        	
	        	/*
	        	 * 
	        	 * Affine Transform
	        	 * 
	        	 * 
	        	CvRect x = cvBoundingRect(seq, 1);   		
	     		
	     		cvSetImageROI(image,x);
	     		
	        	//cvGetAffineTransform(srcTri,dstTri,warp_mat);
	        	cvGetAffineTransform(srcArr, dstArr, warp_mat);
	        	//cvGetAffineTransform(new float[]{0,0,20,0,20,20}, new float[]{0,0,40,0,20,40}, warp_mat);
	        	cvWarpAffine(dst, tmpImg, warp_mat);

	        	cvSaveImage("tmp/dst"+i+".jpg",   tmpImg);
	        	cvResetImageROI(image);
	             */
	        	
	        	
//	                 //cvBoundingRect(image, i);
//	            	int npt[] = {4, 4};
//	                //DrawLine() reference http://opencv.willowgarage.com/documentation/cpp/drawing_functions.html#cv-line
	            cvDrawLine(image, new CvPoint(pts.position(0).x(),pts.position(0).y()), new CvPoint(pts.position(1).x(),pts.position(1).y()), CvScalar.GREEN, 1, CV_AA, 0);
	            cvDrawLine(image, new CvPoint(pts.position(1).x(),pts.position(1).y()), new CvPoint(pts.position(2).x(),pts.position(2).y()), CvScalar.GREEN, 1, CV_AA, 0);
	            cvDrawLine(image, new CvPoint(pts.position(2).x(),pts.position(2).y()), new CvPoint(pts.position(3).x(),pts.position(3).y()), CvScalar.GREEN, 1, CV_AA, 0);
	            cvDrawLine(image, new CvPoint(pts.position(3).x(),pts.position(3).y()), new CvPoint(pts.position(0).x(),pts.position(0).y()), CvScalar.GREEN, 1, CV_AA, 0);
	            
	            //cvPutText( image, "i="+i, new CvPoint(pts.position(0).x()-10,pts.position(0).y()-10), font, CV_RGB(255,255,0) );
	            
	            
	            
	            //if ( i == 3)
	           /* for(j = 0; j < pts.capacity() ; j ++  )
		        {
	            	System.out.println("x="+pts.position(j).x()+"; y="+pts.position(j).y());
	            	
	            	cvPutText( image, ""+j, new CvPoint(pts.position(j).x(),pts.position(j).y()), font, CV_RGB(255,255,0) );
		        }*/
	        }
	    }
	//    else
	  //   System.out.println("No squares");
	}

}
