/*import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_highgui.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;
*/

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Vector;

import org.bytedeco.javacv.FrameGrabber.Exception;

import com.gorcer.iseeyou.FounderMgr;

import com.gorcer.iseeyou.Recognizer;

import org.bytedeco.javacpp.*;
import org.bytedeco.javacpp.opencv_core.IplImage;

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
		 /*Vector<CvSeq> squares;
		 final IplImage image = cvLoadImage("Images/Test2.jpg");
		 IplImage dst;
		 FounderMgr mgr = FounderMgr.getInstance();
		 mgr.prepareEnv();
		 
		 final CanvasFrame original = new CanvasFrame("Ori");		 
		 dst = cvCloneImage(image);

		 squares = Recognizer.findNumbers(dst);		
		 Recognizer.drawSquares(dst, squares);
		 
		 OpenCVFrameConverter converter = new OpenCVFrameConverter.ToIplImage();
		 
		 		 
		 System.out.println("Processing took " + mgr.getWorkTime() + " sec.");
		 
		 original.showImage(converter.convert(dst));			
		 original.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);*/
	}
	
	
	
	public static void testVideo() throws Exception
	{
			
		/*Frame frm;
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
			
            Recognizer.drawSquares(dst, squares);
			
			
			
			original.showImage(converter.convert(dst));
			//smooth.showImage(gray);
			original.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
			
		}
		
		g.stop();*/
	
	}
	
	public static void testImageOCR()
	{
		/* Vector<CvSeq> squares;
		 final IplImage image = cvLoadImage("tmp/afine1.jpg");
		 Vector<String> numbers;
		 
		 final CanvasFrame original = new CanvasFrame("Ori");		 
          
		 numbers = Recognizer.RecognizeNumber(image);		
		 System.out.println("num:"+numbers.toString());
		 OpenCVFrameConverter converter = new OpenCVFrameConverter.ToIplImage();
						
		 original.showImage(converter.convert(image));			
		 original.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);*/
	}
	
	public static void main_haar(String[] args) throws IOException {
		/*String fn = "./Images/Test7.jpg"; 
		IplImage src = cvLoadImage(fn);
		IplImage grayImage    = IplImage.create(src.width(), src.height(), IPL_DEPTH_8U, 1);
		
		Loader.load(opencv_objdetect.class);
		
		String classifierName = null;
		URL url = new URL("https://raw.githubusercontent.com/Itseez/opencv/master/data/haarcascades/haarcascade_licence_plate_rus_16stages.xml");
        File file = Loader.extractResource(url, null, "classifier", ".xml");
        file.deleteOnExit();
        classifierName = file.getAbsolutePath();

        CvHaarClassifierCascade classifier = new CvHaarClassifierCascade(cvLoad(classifierName));
        if (classifier.isNull()) {
            System.err.println("Error loading classifier file \"" + classifierName + "\".");
            System.exit(1);
        }
        CvMemStorage storage = CvMemStorage.create();
        cvCvtColor(src, grayImage, CV_BGR2GRAY);
        
        CvSeq plates = cvHaarDetectObjects(grayImage, classifier, storage,
                1.1, 3, CV_HAAR_FIND_BIGGEST_OBJECT | CV_HAAR_DO_ROUGH_SEARCH);
        
        System.out.println( plates.total());
        
		cvClearMemStorage(storage);

		for(int i = 0; i < plates.total(); i++){
			CvRect r = new CvRect(cvGetSeqElem(plates, i));
			System.out.println(r);
			cvRectangle (
					src,
					cvPoint(r.x(), r.y()),
					cvPoint(r.width() + r.x(), r.height() + r.y()),
					CvScalar.RED,
					2,
					CV_AA,
					0);

		}
		
		final CanvasFrame original = new CanvasFrame("Ori");
		OpenCVFrameConverter converter = new OpenCVFrameConverter.ToIplImage();
		
		original.showImage(converter.convert(src));
		//smooth.showImage(gray);
		original.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
		*/
		
	}
	
	public static void main(String[] args) throws IOException {		
		 
		//for(int i=0;i<10;i++) {
				
		
		//testImage();		
		if (args.length == 0) {
			System.out.println("Error: Undefined image to recognize, type -h to help");
			System.exit(0);
		}		 
		else if (args.length > 0) {
			if (args[0] == "-h") {
				System.out.println("Use java -jar iSeeYouAnrp.jar path_to_file.jpg|http://.../.jpg [-v]");
				System.exit(0);
			}
			else {
				String fn = args[0]; 
				FounderMgr mgr = new FounderMgr();											
				
				if (args.length == 2 && args[1].equals("-v")) {
					mgr.verbose = true;
				}
				
				mgr.prepareEnv();	
				// System.out.println("Used Memory:" + (runtime.totalMemory() - runtime.freeMemory()) / 1024/1024);
				
				// Если url
				if (fn.toLowerCase().contains("http") && (fn.toLowerCase().contains("jpg") || fn.toLowerCase().contains("jpeg"))) {
					FounderMgr.println("Try to download file " + fn);
					URL website = new URL(fn);
					ReadableByteChannel rbc = Channels.newChannel(website.openStream());
					fn = FounderMgr.getPersonalTmpPath() + "/downloadedvc.jpg";
					FileOutputStream fos = new FileOutputStream(fn);
					fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
					fos.close();					
					FounderMgr.println("Download and save to " + fn);
				}
				
				// Если файл на диске
				 if (!Files.exists(Paths.get(fn))) {
					System.out.println("Error: File not found " + fn);
					System.exit(0); 
				 }
					 
				 // Распознаем
				 Recognizer.process(fn, mgr);				 
				 
				 mgr.println("Processing finished, " + mgr.getWorkTime() + " sec. remained");				 
				
				// System.out.println("Used Memory:" + (runtime.totalMemory() - runtime.freeMemory()) / 1024/1024);
				 
				 System.out.println(mgr.getJSON());
				 mgr.destroy();
			}
		}
		
		/*try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		}*/
		
	}

	
}