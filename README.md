iseeyou-anrp
============

Система распознавания русских автомобильных номеров.
Работает не точно.

Установка
---------

Клонируем репозиторий
```
git clone https://github.com/gorcer/iseeyou-anrp.git
cd ./iseeyou-anrp
```

Ставим tesserat
```
sudo apt-get install tesseract-ocr
```

Копируем данные для tesseract
```
cp ./train/avt.traineddata /usr/share/tesseract-ocr/tessdata/
```

Прописываем путь (в IDE это тоже нужно сделать)
```
export TESSDATA_PREFIX="/usr/share/tesseract-ocr/tessdata/"
```

Собираем проект
```
mvn package
```

Пробуем распознать номер по URL
```
java -jar target/iSeeYouAnrp-1.0-jar-with-dependencies.jar http://s.auto.drom.ru/i24195/s/photos/21465/21464270/167091099.jpg
```

