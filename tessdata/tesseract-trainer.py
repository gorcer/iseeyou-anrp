# Tesseract 3.02 Font Trainer
# V0.01 - 3/04/2013

# Complete the documentation 
import time
import os
import sys

fontname = 'numbers'
language = 'avt'
directory = "./"

print 'Tesseract Font Builder - assumes training TIFFs and boxfiles already created'
print 'Note: Only up to 32 .tiff files are supported for training purposes'
print 'TESSDATA_PREFIX='+os.environ['TESSDATA_PREFIX']

count = 0
for files in os.listdir(directory):
    
    if files.endswith(".tiff"):
        #Train the boxfiles
        rename = 'mv '+files+' '+language+'.'+fontname+'.exp'+str(count)+'.tiff'
        os.system(rename)
        command='tesseract avt.'+fontname+'.exp'+str(count)+'.tiff avt.'+fontname+'.exp'+str(count)+' nobatch box.train.stderr'
        print command   
        os.system(command)
        count = count + 1

trfilelist = ''
boxfilelist = ''
font_properties = ''

for files in os.listdir(directory):
    if files.endswith(".tr"):
        trfilelist = trfilelist + ' ' + files    
        font_properties = fontname+' 0 0 0 0 0'    
    if files.endswith(".box"):
        boxfilelist = boxfilelist + ' ' + files

#Build the Unicharset File
command2 = 'unicharset_extractor ' + boxfilelist
print command2
os.system(command2)

#Build the font properties file
fontpropertiesfile = open('font_properties', 'a+') # saving into a file        
fontpropertiesfile.write(font_properties)
print 'Wrote font_properties file'
fontpropertiesfile.close()

#Clustering
command3 = 'shapeclustering -F font_properties -U unicharset' + trfilelist
print command3
os.system(command3)

#MFTraining
mftraining = 'mftraining -F font_properties -U unicharset -O '+fontname+'.charset '+trfilelist
print mftraining
os.system(mftraining)

#CNTraining
command4 = 'cntraining ' + trfilelist
print command4
os.system(command4)

#Rename necessary files
os.system('mv unicharset '+language+'.unicharset')
os.system('mv shapetable '+language+'.shapetable')
os.system('mv normproto '+language+'.normproto')
os.system('mv pffmtable '+language+'.pffmtable')
os.system('mv inttemp '+language+'.inttemp')
##Put it all together
command5 = 'combine_tessdata '+language+'.'
os.system(command5)

#Copy it over
command6 = 'cp -f '+language+'.* /usr/local/share/tessdata'
os.system(command6)