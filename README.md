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

Прописываем путь
```
export TESSDATA_PREFIX=$PWD
```
В IDE должен быть так же прописана глобальная переменная указывающая на папку проекта

Собираем проект
```
mvn package
```

Пробуем распознать номер по URL
```
java -jar target/iSeeYouAnrp-1.0-jar-with-dependencies.jar http://s.auto.drom.ru/i24195/s/photos/21465/21464270/167091099.jpg
```

