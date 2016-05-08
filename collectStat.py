import os, sys

'''
Script to analyze recognizer quality, use autoimho.com image base
sudo sshfs -o allow_other gorcer@www:/var/www/autoimho.com/content/numbers/ ./aimnumbers
'''

empty=0;
plates=0;
founded=0;

path = "./tmp/aim_numbers";
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
				command = "java -jar iSeeYouAnrp.jar " + path3 + "/" +image.replace("(", "\(").replace(")", "\)")
				print command
				result= os.popen(command).read()
				if (result == "empty"):
					empty=empty+1
				else:
					plates=plates+1
					if (result.lower() == number.lower()):
						founded=founded+1
					else:
					    print "diferent " + number + " - " + result

				print number + ": e/p: %d/%d f:%d" % (empty,plates,founded,)
				#sys.exit()

total = (plates + empty)
print "Stat is:"
print "Total images processed: %d" % total
print "Plates detected: %d (%d%%)" % (plates, (100*founded/total),)
print "Correct recognized numbers: %d (%d%%)" % (founded, (100*founded/total),)