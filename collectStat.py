import os, sys
import Image
import urllib2, json
import shutil, uuid, hashlib
import csv, time

'''
Script to analyze recognizer quality, use drom image base
'''

# Variables
scriptName = "gorcerAnrp"
dataFileName = "imagesDataWithPlates.csv"
scriptCommand = "java -jar iSeeYouAnrp.jar %s"
rowTemplate="<div class='row'><div class='col-xs-5'><img width='450px' src='%s'/></div><div class='col-xs-4'><br/><img src='%s'/><br/><br/>bullId:<a href='http://%s.drom.ru' target='_blank'>%s</a><br/>photoId:%s</div></div><hr size='1'/>"

#init
total=0
plates=0
errors=0
resultPath = "./resultData/"+scriptName+"/"
reportHtml=""
reportFile = resultPath + "result.html"
startTime=time.time()


#initReport
with open(reportFile, "w") as report:
    report.write("<html><head>")
    report.write("<link rel='stylesheet' href='https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.min.css' integrity='sha384-1q8mTJOASx8j1Au+a5WDVnPi2lkFfwwEAa8hDDdjZlpLegxhjVME1fgjWPGmkzs7' crossorigin='anonymous'>")
    report.write("</head><body>")
    report.write("<h1>Script name '"+scriptName+"'</h1>")
    report.write("\n")


#logFile = "drom-"+scriptName+".log";

#clear old logs
#if (os.path.exists(logFile)):
#    os.remove(logFile)

with open(dataFileName, 'rb') as csvfile:
        reader = csv.reader(csvfile, delimiter=',', quotechar='|')
        for row in reader:
	    total=total+1
	    photoId=row[0]
	    bullId=row[1]
	    url=row[2]

	    print url
	    status="empty"

	    # execute command
	    command = scriptCommand % url
	    result = os.popen(command).read()

	    # try to identify error
            if (result != ""):
		try:
	    	    result= json.loads(result)
		except ValueError, e:
	            result=""
            if (("error" in result) or (result == "")):
                print "result: error"
		errors=errors+1
		continue

	    # process result if success
	    if (result["result"] == "success" and len(result["data"]["bestPlate"])>0):
                
		imagePath = "%s_%s/" % (bullId, photoId)
		platePath = resultPath + imagePath
		if (os.path.exists(platePath) is False):
                    os.makedirs( platePath, 0775 )

		#save plate
		srcFile = result["data"]["bestPlate"]
		imagePath = imagePath + "%s.jpg"
		imagePath = imagePath % str(uuid.uuid4())

		dstFile = resultPath + imagePath
		os.rename(srcFile, dstFile)
		
		#inc stat
		plates=plates+1
		status="found"

		#save report
		#reportHtml = reportHtml + (rowTemplate % (url, imagePath))
		with open(reportFile, "a") as report:
                    report.write(rowTemplate % (url, imagePath,bullId, bullId, photoId))
		    report.write("\n")

            elif (result["result"] != "empty"):
                errors=errors+1
	    print "result: " + status
endTime=time.time()
elapsed = (endTime-starTtime)/60
with open(reportFile, "a") as report:
        report.write("<br/><p>Total images processed: %d<br/>\n Total plates found: %d <br/>\n Errors count: %d <br/>\n Elapsed time (m.): %d</p></body></html>" % (total, plates, errors, elapsed))
        report.write("\n")

sys.exit();