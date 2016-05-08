import os, sys
import Image

'''
Script to analyze recognizer quality, use autoimho.com image base
sudo sshfs -o allow_other gorcer@www:/var/www/autoimho.com/content/numbers/ ./aimnumbers
'''

empty=0
plates=0
founded=0
errors=0

if (os.path.exists("recognized.log")):
    os.remove("recognized.log")

path = "./tmp/aimnumbers";
regions = os.listdir(path)
for region in regions:
    path2 = path + "/" + region;
    if (os.path.isdir(path2)):
	numbersPath = os.listdir(path2);
	for numberPath in numbersPath:
	    number = numberPath.replace("rus", "");
	    path3 = path2 + "/" + numberPath+"/rate_files";
	    if (os.path.exists(path3)):
		imagePath = os.listdir(path3);
		for image in imagePath:
			if ("jpg" in image.lower() or "jpeg" in image.lower()):
				fullImagePath = path3 + "/" +image
				size = os.path.getsize(fullImagePath)
				if (size/1024 > 500):
					print "Found big file"
					im = Image.open(fullImagePath)
					thumbSize = 1024, 768
					im.thumbnail(thumbSize, Image.ANTIALIAS)
					fullImagePath = "tmp/thumb.jpg"
					im.save(fullImagePath, "JPEG")

				command = "java -jar iSeeYouAnrp.jar " + fullImagePath.replace("(", "\(").replace(")", "\)")
				print command
				result= os.popen(command).read()

				if (("error" in result) or (result == "")):
				    errors=errors+1
				    continue
				if (result == "empty"):
					empty=empty+1
				else:
					plates=plates+1
					if (result.lower() == number.lower()):
						founded=founded+1
						with open("recognized.log", "a") as logfile:
						    logfile.write("Found number " + result + " on the image "+ fullImagePath + " and it's correct\n")
					else:
						with open("recognized.log", "a") as logfile:
						    logfile.write("Found number " + result + " on the image "+ fullImagePath + "\n")
						print "diferent " + number + " - " + result

				print number + ": e/p: %d/%d f:%d err:%d" % (empty,plates,founded,errors,)
				#sys.exit()

total = (plates + empty - errors)
print "Stat is:"
print "Total images processed: %d" % total
print "Plates detected: %d" % (plates,)
print "Correct recognized numbers: %d" % (founded,)
print "Errors: %d" % (errors,)