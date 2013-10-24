import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_highgui.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;




import java.util.Vector;


import com.googlecode.javacpp.*;
import com.googlecode.javacv.JavaCV;
import com.googlecode.javacv.cpp.opencv_core.CvMat;
import com.googlecode.javacv.cpp.opencv_core.CvPoint;
import com.googlecode.javacv.cpp.opencv_core.CvRect;
import com.googlecode.javacv.cpp.opencv_core.CvSeq;
import com.googlecode.javacv.cpp.opencv_core.CvSize;
import com.googlecode.javacv.cpp.opencv_core.IplImage;


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
	   // cvSaveImage("tmp/src.jpg", src);    
	    
	    
	    if (config.doPyr)
	    {
	    	CvSize sz = cvSize(gray.width() & -2, gray.height() & -2);
	    	IplImage pyr = cvCreateImage(cvSize(sz.width()/2, sz.height()/2), gray.depth(), gray.nChannels());	    	
	    	cvPyrDown(gray, pyr, CV_GAUSSIAN_5x5);
	    	cvPyrUp(pyr, gray, CV_GAUSSIAN_5x5);	 
	    	//cvSaveImage("tmp/src.jpg", gray);
	    	cvReleaseImage(pyr);
	    }
	    
	    if (config.doDilate)
	    	cvDilate(gray, gray, null, 1);	    
	    
	    if (config.doCanny)	    
	    	 cvCanny(gray, gray, 0, config.Thresh, 3);
	 	
	    
	    
	    
	    if (config.doThreshold)
	    	cvThreshold(gray, gray, config.Thresh, 255, CV_THRESH_BINARY);
	    
	    
		
	    
		return gray;
	}
	
	public static IplImage Transform(CvRect rect, CvSeq poly, IplImage img, int n)
	{
		
		
		IplImage tmp = cvCreateImage( cvSize(52*4, 12*4), img.depth(), img.nChannels() );		
		CvMat warp_mat = cvCreateMat(3, 3, CV_32FC1);
		Vector<CvPoint> pts;
		 
		pts = getDirectPoints(poly, rect);
		 
		
		//cvCvtSeqToArray(poly, pts, CV_WHOLE_SEQ);
		
		
		
		double[] srcArr = new double[8];		
		// Переводим полигон в массив (необходимо унифицировать направление)
		for(int j = 0; j < 4 ; j ++  )
	    {
    		srcArr[j*2]=(int)pts.get(j).x()-rect.x();
    		srcArr[j*2+1]=(int)pts.get(j).y()-rect.y();	
    		
    		System.out.println("Transform src x="+srcArr[j*2]+" y="+srcArr[j*2+1]);    		 
	    }
		
	
		System.out.println(srcArr);
		
		JavaCV.getPerspectiveTransform(srcArr, new double[]{0,0,tmp.width(),0,tmp.width(),tmp.height(),0,tmp.height()}, warp_mat);
		
		//cvGetAffineTransform(srcArr, new float[]{0,0,52,0,20,40}, warp_mat);
		cvSetImageROI(img,rect);
		cvWarpPerspective(img, tmp, warp_mat);		
		cvSaveImage("tmp/afine"+n+".jpg",  tmp);
		cvResetImageROI(img);
		
		return(tmp);
	}
	
	public static Vector<CvSeq> findNumber(IplImage img, IplImage original, CvMemStorage storage, RecognizeConfig config)
	{
		Vector<CvSeq> squares = new Vector<CvSeq>();
		CvSeq contours = new CvSeq(null);
		CvSeq approx;
		double maxCosine;	
		
		
		IplImage tmp = cvCloneImage(img);
		//cvSaveImage("tmp/ok-"+config.n+"-"+config.Thresh+".jpg",   img);
		cvFindContours(tmp, storage, contours, Loader.sizeof(CvContour.class), CV_RETR_LIST, CV_CHAIN_APPROX_SIMPLE);
		
		
		int cn=config.n;
		// Перебираем контуры
		while (contours != null && !contours.isNull()) 
		{
			
			//Если точек в контуре > 0
			if (contours.elem_size() > 0) 
			{
                approx = cvApproxPoly(contours, Loader.sizeof(CvContour.class),storage, CV_POLY_APPROX_DP, cvContourPerimeter(contours)*config.ApproxAccuracy, 0);
               
                if( approx.total() == 4 // Четыре стороны
                        &&
                        Math.abs(cvContourArea(approx, CV_WHOLE_SEQ, 0)) > config.minContourArea &&
                    cvCheckContourConvexity(approx) != 0 // контур замкнут
                    )
                {
                	 maxCosine = 0;
                	 // Перебираем все углы
                	 for( int j = 2; j < 5; j++ )
                     {
                		// find the maximum cosine of the angle between joint edges
                        double cosine = Math.abs(angle(new CvPoint(cvGetSeqElem(approx, j)), new CvPoint(cvGetSeqElem(approx, j-2)), new CvPoint(cvGetSeqElem(approx, j-1))));
                        maxCosine = Math.max(maxCosine, cosine);
                     }
                	 if( maxCosine < config.maxCosine ){
                         CvRect rect=cvBoundingRect(approx, 1);
                         
                         if((
                        		 rect.width()*rect.height())<config.maxSquare // Макс площадь
                        		 && rect.width()>rect.height()  // Ширина больше высоты
                        		 /*&& Math.abs(((float)x.height()/x.width())-config.maxAspectRatio)<0.1*/ 
                        		 && (rect.width()/(float)img.width()<0.9) && (rect.height()/(float)img.height()<0.9) // не более 90% размеров изображения
                        		 ){
                        	 		cn++;
                        	 		
                        	 		 RecognizeNumber(Transform(rect, approx, original, cn));
		                        	 
		                        	 
		                        //	 System.out.println("("+rect.x()+" , "+rect.y()+") -> Width : "+rect.width()+" Height : "+rect.height()+" div="+(Math.abs(((float)rect.height()/rect.width()))));
		                       //      System.out.println("Params = thresh:"+config.Thresh);
		
		                             //cvSaveImage("tmp/ok-"+config.thresh+".jpg",   img);
		                             //cvSeqPush(squares, approx);
		                             squares.add(approx);
		                             
		                             //System.out.println(x);
		                             //System.out.println(x);
                         }
                         //else System.out.println("False w : "+x.width()+" x h : "+x.height()+" = "+x.width()*x.height());
                 }
                	 
                }
			}
			contours = contours.h_next();
		}
		
		
		return squares; 
	}
	
	private static void RecognizeNumber(IplImage transform) {
		// TODO Auto-generated method stub
		
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
		//CvPoint ptsNew = new CvPoint(4);
		cvCvtSeqToArray(poly, pts, CV_WHOLE_SEQ);
		
		Vector<CvPoint> ptsNew = new Vector<CvPoint>();
		
		int x,y, minJ=0;
		double min=0, s;
		
		ptsNew.add(new CvPoint().x(rect.x()).y(rect.y()));
		ptsNew.add(new CvPoint().x(rect.x()+rect.width()).y(rect.y()));
		ptsNew.add(new CvPoint().x(rect.x()+rect.width()).y(rect.y()+rect.height()));
		ptsNew.add(new CvPoint().x(rect.x()).y(rect.y()+rect.height()));
		
		
		
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
    		
    		System.out.println("Find x="+(int)pts.position(j).x()+" y="+(int)pts.position(j).y()+" s="+s+" min="+min+" minJ="+minJ);
	    }
		
		ptsNew.get(i).x(pts.position(minJ).x());
		ptsNew.get(i).y(pts.position(minJ).y());		
		}
		
		
		
		return ptsNew;
	}


	public static Vector<CvSeq> findNumbers( IplImage src)
	{
		Vector<CvSeq> squares = new Vector<CvSeq>();
		CvMemStorage storage = CvMemStorage.create();
		
		
		RecognizeConfig config = new RecognizeConfig();
		for (int j=0;j<2;j++)
		for (int i=0;i<20;i++)
		{
			if (j == 0)
			{
				config.doThreshold=false;
				//config.doDilate=true;
				config.doCanny=true;				
				config.Thresh=config.minThresh*1+i*15;
				config.doPyr=true;
			}
			else
			{
			config.doThreshold=true;
			config.Thresh = config.minThresh+i*5;
			config.doCanny=false;
			config.doDilate=false;
			config.doPyr=false;
			}
			config.n=j*100+i;
			
			IplImage prepareImg = prepareImage(src, storage, config);		
			squares.addAll(findNumber(prepareImg, src, storage, config));
		}
		
		//cvReleaseImage(prepareImg);
		System.out.println("Total found "+squares.size()+" squares");
		
		return squares;
		
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
