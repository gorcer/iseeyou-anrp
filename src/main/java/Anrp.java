/*import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_highgui.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;
*/
import java.awt.Image;
import java.sql.Time;
import java.util.Date;
import java.util.Timer;
import java.util.Vector;

import org.bytedeco.javacv.*;
import org.bytedeco.javacv.FrameGrabber.Exception;

import com.gorcer.iseeyou.FoundedMgr;
import com.gorcer.iseeyou.Recognizer;

import org.bytedeco.javacpp.*;
import org.bytedeco.javacpp.opencv_videoio.CvCapture;

import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_imgproc.*;
import static org.bytedeco.javacpp.opencv_imgcodecs.*;
/*import org.bytedeco.javacv.cpp.CanvasFrame;
import org.bytedeco.javacv.cpp.opencv_core.*;
import org.bytedeco.javacv.cpp.opencv_core.CvSeq;
import org.bytedeco.javacv.cpp.opencv_legacy.CvImageDrawer;
*/

public class Anrp {

		static double angle( CvPoint pt1, CvPoint pt2, CvPoint pt0 )
		{
			double dx1 = pt1.x() - pt0.x();
		    double dy1 = pt1.y() - pt0.y();
		    double dx2 = pt2.x() - pt0.x();
		    double dy2 = pt2.y() - pt0.y();
		    return (dx1*dx2 + dy1*dy2)/Math.sqrt((dx1*dx1 + dy1*dy1)*(dx2*dx2 + dy2*dy2) + 1e-10);
		}
		
	
	public static Vector<CvSeq> findSquares( IplImage src,  CvMemStorage storage)
	{
	int N=10;
	int thresh=100;
	
	float val_thresh=0;
	
	Vector<CvSeq> squares = new Vector<CvSeq>();
	//squares = cvCreateSeq(0, Loader.sizeof(CvContour.class), Loader.sizeof(CvSeq.class), storage);
	
	
	IplImage pyr = null, timg = null;
	timg = cvCloneImage(src);

	CvSize sz = cvSize(src.width() & -2, src.height() & -2);
	
    IplImage tgray = cvCreateImage( sz, IPL_DEPTH_8U, 1 );
    IplImage gray = cvCreateImage( sz, IPL_DEPTH_8U, 1 );
	
	//tgray = cvCreateImage(sz, src.depth(), 1);
	//gray = cvCreateImage(sz, src.depth(), 1);
	pyr = cvCreateImage(cvSize(sz.width()/2, sz.height()/2), src.depth(), src.nChannels());
	
	// down-scale and upscale the image to filter out the noise
	cvPyrDown(timg, pyr, CV_GAUSSIAN_5x5);
	cvPyrUp(pyr, timg, CV_GAUSSIAN_5x5);	
	
	CvSeq contours = new CvSeq(null);
	CvSeq approx;
	
	// request closing of the application when the image window is closed
	// show image on window
	// find squares in every color plane of the image
	// IplImage channels[] = {cvCreateImage(sz, 8, 1), cvCreateImage(sz, 8, 1), cvCreateImage(sz, 8, 1)};	 
	 
	
	   tgray = cvCloneImage(timg);
	   gray = cvCloneImage(timg);	 
	    
	 //  cvSaveImage("tmp/th.jpg",   tgray);
	   //cvDilate(tgray, tgray, null, 3);
	   //cvSaveImage("tmp/tg.jpg",   tgray);
	    
	    // try several threshold levels
	    for( int l = 0; l < N; l++ )
	    {
	    //             hack: use Canny instead of zero threshold level.
	    //             Canny helps to catch squares with gradient shading
	      
	    	if( l == 0 )
	        {
	    //                apply Canny. Take the upper threshold from slider
	    //                and set the lower to 0 (which forces edges merging)
	                      cvCanny(tgray, gray, 0, thresh, 3);
	    //                 dilate canny output to remove potential
	    //                // holes between edge segments
	                      cvDilate(gray, gray, null, 1);
	                      
	               	   
	                 }
	          else
	        {
	        	  
	        	  val_thresh = (35+l*5);
	    //                apply threshold if l!=0:	        	  
	        	  cvThreshold(tgray, gray, val_thresh, 255, CV_THRESH_BINARY);
	        	  cvSaveImage("tmp/g-"+l+".jpg",   gray);
	          }
	    	
	    	
	        //            find contours and store them all as a list	    				 
	    				 cvFindContours(gray, storage, contours, Loader.sizeof(CvContour.class), CV_RETR_LIST, CV_CHAIN_APPROX_SIMPLE);
	    				  
	                      while (contours != null && !contours.isNull()) {
	                    	  
	                      if (contours.elem_size() > 0) {
	                           approx = cvApproxPoly(contours, Loader.sizeof(CvContour.class),storage, CV_POLY_APPROX_DP, cvContourPerimeter(contours)*0.02, 0);
	                     
	                           
	                      if( approx.total() == 4
	                            &&
	                            Math.abs(cvContourArea(approx, CV_WHOLE_SEQ, 0)) > 50 &&
	                        cvCheckContourConvexity(approx) != 0
	                        ){
	                    	  
	                    //	  cvSaveImage("tmp/ok-"+l+".jpg",   gray);
	                    	  
	                        double maxCosine = 0;
	                        //
	                        for( int j = 2; j < 5; j++ )
	                        {
	            // find the maximum cosine of the angle between joint edges
	                                                double cosine = Math.abs(angle(new CvPoint(cvGetSeqElem(approx, j)), new CvPoint(cvGetSeqElem(approx, j-2)), new CvPoint(cvGetSeqElem(approx, j-1))));
	                                                maxCosine = Math.max(maxCosine, cosine);
	                         }
	                         if( maxCosine < 0.4 ){
	                                 CvRect x=cvBoundingRect(approx, 1);
	                                 
	                                 if((x.width()*x.height())<500000 && x.width()>x.height() && Math.abs(((float)x.height()/x.width())-0.2)<0.1 ){
	                                     //System.out.println("("+x.x()+" , "+x.y()+") -> Width : "+x.width()+" Height : "+x.height()+" div="+(Math.abs(((float)x.height()/x.width()))));
	                                     //System.out.println("Params = iteration:"+l+" thresh:"+val_thresh);
	                                     //cvSeqPush(squares, approx);
	                                     squares.add(approx);
	                                
	                                 }
	                                 
	                                 
	                                 /*else
	                                 System.out.println("False w : "+x.width()+" x h : "+x.height()+" = "+x.width()*x.height());*/
	                         }
	                         /*else
	                        	 System.out.println("False Cosine : "+maxCosine);*/
	                    }
	                    /*  else
	                    	  if( approx.total() == 4 && cvCheckContourConvexity(approx)!=0 && Math.abs(cvContourArea(approx, CV_WHOLE_SEQ, 0))>50)	                    	  
	    	                      System.out.println("False to Small : "+Math.abs(cvContourArea(approx, CV_WHOLE_SEQ, 0)));*/  
	                }
	                contours = contours.h_next();
	            }
	        contours = new CvSeq(null);
	    
	}
	    cvReleaseImage(timg);
	    cvReleaseImage(tgray);
	    cvReleaseImage(gray);
	    cvReleaseImage(pyr); 
	    
	    
	return squares;
	}
	
	
	
	public static void testImage()
	{
		 Vector<CvSeq> squares;
		 final IplImage image = cvLoadImage("Images/Test2.jpg");
		 IplImage dst;
		 
		 final CanvasFrame original = new CanvasFrame("Ori");		 
		 dst = cvCloneImage(image);

		 squares = Recognizer.findNumbers(dst);		
		 Recognizer.drawSquares(dst, dst, squares);
		 
		 OpenCVFrameConverter converter = new OpenCVFrameConverter.ToIplImage();
		 
		 FoundedMgr mgr = FoundedMgr.getInstance();		 
		 System.out.println("Обработка заняла " + mgr.getWorkTime() + " сек.");
		 
		 original.showImage(converter.convert(dst));			
		 original.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
	}
	
	
	
	public static void testVideo() throws Exception
	{
			
		Frame frm;
		IplImage dst;
		IplImage ifrm;
		Vector<CvSeq> squares;
		OpenCVFrameConverter converter = new OpenCVFrameConverter.ToIplImage();
		
		
		final CanvasFrame original = new CanvasFrame("Ori");
				
		FFmpegFrameGrabber g = new FFmpegFrameGrabber("test.avi");
		g.start();
		 
		while(true){
			frm = g.grabImage();
			if(frm == null) {
	            break;
			}
			
			ifrm = converter.convertToIplImage(frm);
			
			dst = cvCloneImage(ifrm);

		     dst = cvCreateImage( cvSize(ifrm.width()/1, ifrm.height()/1), ifrm.depth(), ifrm.nChannels() );
            cvResize(ifrm, dst, ifrm.nChannels());
            
            squares = Recognizer.findNumbers(dst);
			
            Recognizer.drawSquares(dst, dst, squares);
			
			
			
			original.showImage(converter.convert(dst));
			//smooth.showImage(gray);
			original.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
			
		}
		
		g.stop();
	
	}
	
	public static void testImageOCR()
	{
		 Vector<CvSeq> squares;
		 final IplImage image = cvLoadImage("tmp/afine1.jpg");
		 Vector<String> numbers;
		 
		 final CanvasFrame original = new CanvasFrame("Ori");		 
          
		 numbers = Recognizer.RecognizeNumber(image);		
		 System.out.println("num:"+numbers);
		 OpenCVFrameConverter converter = new OpenCVFrameConverter.ToIplImage();
						
		 original.showImage(converter.convert(image));			
		 original.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
	}
	
	public static void main(String[] args) {
		testImage();
	}
	
	public static void main2(String[] args) {
		
		 CvSeq contours = new CvSeq(null);
		 CvMemStorage storage = CvMemStorage.create();
		 Vector<CvSeq> squares;// = new Vector();
		
		//Load image img1 as IplImage
		final IplImage image = cvLoadImage("Images/Test2.jpg");
		
		  // ��������� �������� 
        //final IplImage dst = cvCreateImage(cvSize(image.width(), image.height()), image.depth(), image.nChannels());
        
        //final IplImage gray = cvCreateImage( cvGetSize(image), IPL_DEPTH_8U, 1 );
        IplImage dst;


		
		//create canvas frame named 'Demo'
		final CanvasFrame original = new CanvasFrame("Ori");
		final CanvasFrame smooth = new CanvasFrame("Smo");
/*		
		CvCapture capture = cvCreateFileCapture("test.avi");
		IplImage frame;
		
		cvSetCaptureProperty(capture, CV_CAP_PROP_POS_FRAMES, 4700);

		while(true){
			frame = cvQueryFrame( capture ); 
			if(frame == null) {
                break;
			}
			
		     dst = cvCloneImage(frame);

		     dst = cvCreateImage( cvSize(frame.width()/1, frame.height()/1), frame.depth(), frame.nChannels() );
             cvResize(frame, dst, frame.nChannels());
               
              */
			dst = cvCloneImage(image);
		     
			
			
			
		    // cvCvtColor(dst, gray, CV_RGB2GRAY);
            // cvConvertImage(dst, gray,0);
			// squares = findSquares(gray, storage);  
             
     	    
             
             squares = Recognizer.findNumbers(dst);
			
             Recognizer.drawSquares(dst, dst, squares);
			
			/*CvSize sz = cvSize(gray.width() & -2, gray.height() & -2);			
			IplImage pyr = cvCreateImage(cvSize(sz.width()/2, sz.height()/2), gray.depth(), gray.nChannels());
//			cvPyrDown(gray, pyr, CV_GAUSSIAN_5x5);
//			cvPyrUp(pyr, gray, CV_GAUSSIAN_5x5);
			cvDilate(gray, gray, null, 1);
			cvThreshold(gray, gray, 85, 255, CV_THRESH_BINARY);
			 */
			 //cvCanny(gray, gray, 0, 100, 3);
			 
//			 cvAdaptiveThreshold(gray, gray, 255, CV_ADAPTIVE_THRESH_GAUSSIAN_C, CV_THRESH_BINARY, 7, 1);

			OpenCVFrameConverter converter = new OpenCVFrameConverter.ToIplImage();
			
			original.showImage(converter.convert(dst));
			//smooth.showImage(gray);
			original.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
			
			
			
		//} //--video

		//create canvas frame named 'Demo'
		//final CanvasFrame smooth = new CanvasFrame("Smo");
		
		//cvCvtColor(image, gray, CV_RGB2GRAY);
		//squares = findSquares(gray, storage);      
		
		
		//drawSquares(image, dst, squares);
		
		
		//CvRect Rect = new CvRect(10, 10, 10, 10);  //cvBoundingRect(tRect, 1);
		//cvSetImageROI(image,Rect);
		
		
		//cvResize(image, dst, 4);
		//cvSmooth(dst, dst, CV_GAUSSIAN, 3);
		
		
	//	cvDilate(gray, gray, null, 1);
		//cvPyrDown(image, gray, CV_GAUSSIAN_5x5);
		
	//cvThreshold(gray, gray, 150, 250, CV_THRESH_OTSU);		
	//	cvCanny(gray, gray, 50, 100, 3);
		
	//	final IplImage dst = cvCloneImage(gray);
		
	/*	
		 // ���������� �����
        lines = cvHoughLines2( gray, storage, CV_HOUGH_PROBABILISTIC, 1, Math.PI/180, 10, 10, 10 );       
        
       for(int i=0;i<lines.total();i++)
       {
    	   Pointer line = cvGetSeqElem(lines, i);
           CvPoint pt1  = new CvPoint(line).position(0);
           CvPoint pt2  = new CvPoint(line).position(1);
           
            cvLine(image, pt1, pt2, CV_RGB(255,0,0), 1, CV_AA, 0 );           
        	
        }*/       
       
        
	//	cvFindContours(gray, storage, contours, Loader.sizeof(CvContour.class), CV_RETR_LIST, CV_CHAIN_APPROX_TC89_L1);
		
		//cvDrawContours(image, contours, CvScalar.BLUE, CvScalar.BLUE, -1, 1, CV_AA);
		
		/*
		 while (contours != null && !contours.isNull()) {
             if (contours.elem_size() > 0) {
                 CvSeq points = cvApproxPoly(contours, Loader.sizeof(CvContour.class),
                         storage, CV_POLY_APPROX_DP, cvContourPerimeter(contours)*0.02, 0);
                 cvDrawContours(image, points, CvScalar.BLUE, CvScalar.BLUE, -1, 1, CV_AA);
             }
             contours = contours.h_next();
         }*/
		 

		
		//Show image in canvas frame
		//original.showImage(image);
		//smooth.showImage(dst);
		
		//This will close canvas frame on exit
		
	}
	
}