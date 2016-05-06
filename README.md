iseeyou-anrp
============

Система распознавания автомобильного номера отечественных авто.
Работает не точно.

Установка
---------
Клонируем репозиторий
git clone https://github.com/gorcer/iseeyou-anrp.git
cd iseeyou-anrp

Копируем данные для tesseract
cp ./train/avt.traineddata /usr/share/tesseract-ocr/tessdata/

Прописываем путь (в IDE это тоже нужно сделать)
export TESSDATA_PREFIX="/usr/share/tesseract-ocr/tessdata/"

Пробуем распознать номер
java -jar iSeeYouAnrp.jar http://s.auto.drom.ru/i24195/s/photos/21465/21464270/167091099.jpg
   
P.S: Работает пока только на линуксе, т.к. временные файлы хранит в /tmp/iSeeYouAnrp, но это можно легко исправить.

