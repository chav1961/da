## Data Acquisition tools

### Общее описание

Этот репозитарий содержит набор полезных утилит для организации процесса краулинга данных, заливаемых в базы знаний. Все утилиты являются консольными приложениями, из 
которых стандартными средствами OC можно собрать конвейер обработки данных, например:

> java -jar da.source.jar параметры... | java -jar da.xmlcrowler.jar параметры... | java -jar da.infer.jar параметры... | java -jar da.sender.jar параметры... >/dev/null

Как показал собственный опыт автора, это наиболее оптимальный вариант организации краулинга данных, поскольку, во-первых, он легко встраивается в любой сценарий (например,
запуск процесса краулинга с помощью cron), и, во-вторых, его можно легко разорвать в любом месте, чтобы посмотреть состав данных, представленных на том или ином этапе конвейера
(весьма полезно для отладки). Кроме того, у пользователя при таком построении конвейера есть возможность вклиниться в процесс обработки данных со своими специфическими обработчиками,
не погружаясь глубоко в данный проект (вплоть до того, что консольное приложение может быть написано на любом языке программирования, отличном от Java)

Поток данных, передаваемых по конвейеру, представляет собой обычный ZIP-архив, в котором есть два обязательных раздела:

- раздел **ticket.txt** - должен быть самым первым разделом архива
- раздел **log.txt** - должен быть самым последним разделом архива

Раздел **ticket.txt** содержит данные в формате класса *java.util.Properties*. Обязательной парой ключ-значение в этом разделе является ключ **contentType**, в котором задается формат
данных, представленных в разделах ZIP-архива. Формат данных является одним из известных RDF-форматов. Допустимые для него значения см. нумерацию *chav1961.da.util.interfaces.ContentFormat* в данном проекте. Желательно также задавать ключ **baseUri**, в котором указывается базовый URI для RDF-данных. На набор остальных ключей в данном разделе никаких ограничений нет.

Раздел **log.txt** содержит лог обработки содержимого данного архива очередной утилитой. При переходе с одного этапа конвейера на другой новый лог дописывается в конец уже существующего.
Как показала практика, это весьма удобно при поиске ошибок, поскольку все проблемы оказываются привязаны к самим данным. Что именно будет записано в логе - зависит от конкретной утилиты и конкретных проблем, возникших при обработке содержимого архива.

Остальные разделы архива представляют собой либо данные в том или ином RDF-формате, либо вообще произвольные данные (например, изображения). Какие именно разделы из этого потока подлежат
обработке, определяется параметрами командной строки при запуске той или иной утилиты. Параметры командной строки, поддерживаемые большинством утилит, следующие:

- параметр **-process PATTERN** - указывает разделы архива, которые должны быть обработаны данной утилитой
- параметр **-pass PATTERN** - указывает разделы архива, которые должны быть переданы на выход утилиты без обработки
- параметр **-remove PATTERN** - указывает разделы архива, которые должны быть удалены из входного архива и не должны передаваться на выход утилиты
- параметр **-rename PATTERN->FORMAT** - указывает разделы архива, которые должны быть переименованы в выходном архиве
- параметр **-d** - включает вывод отладочной трассы на консоль

Параметры **-process** и **-pass** являются взаимоисключающими. Если ни тот, ни другой параметр не заданы, обработке будут подлежать все разделы архива. Обратите внимание также на порядок
обработки параметров - *вначале* параметры **-process** и **-pass** "решают", обрабатывать или нет тот или иной раздел архива, а *затем* параметры **-remove** и **-rename** "решают", включать ли данный раздел в выходной архив, и под каким именем. Параметр **PATTERN** во всех случаях задает маску имен разделов, подлежащих обработке, в формате класса *java.util.regex.Pattern*. Параметр **FORMAT** задает маску переименования в формате метода **java.lang.String.replaceAll(...)**.

### Утилита da.source

Эта утилита может употребляться в качестве первого элемента конвейера обработки, поскольку она "умеет" готовить пустой ZIP-архив в ранее описанном формате. Также она может употребляться в
качестве промежуточного элемента конвейера, если в обрабатываемый ZIP-архив необходимо по ходу дела добавлять дополнительные разделы.

Параметры запуска утилиты следующие:

> java -jar da.source.jar <стандартные аргументы> \[-startPipe] \[-join PART] \[-append URI URI URI ...]

Утилита поддерживает стандартные аргументы **-remove**, **-rename** и **-d**. Аргументы **-process** и **-pass** не поддерживаются, поскольку никакой обработки содержимого разделов в данной утилите не предусмотрено. Утилита ожидает в качестве входного потока ZIP-архив ранее описанного формата, и выдает ZIP-архив в качестве выходного потока. Если при запуске утилиты задан параметр **-startPipe**, утилита ожидает во входном потоке вместо ZIP-архива содержимое будущего раздела **ticket.txt** в формате *java.util.Properties* (не забываем про ключ
**contentType**!). Кодировка входного потока в этом случае - "UTF-8". Если при запуске утилиты задан параметр **-append** со списком URI, содержимое указанных URI будет включено в выходной архив в качестве новых разделов:

- если заданные URI являются относительными, они рассматриваются, как ссылки на файлы и/или директории файловой системы данного компьютера (в этом случае в них допустимо задавать символы шаблонов, например ./dir/*.txt). Включаемые в архив разделы получат имена, совпадающие с именем включенного файла
- если заданные URI являются абсолютными, они рассматриваются как ссылки на внешние ресурсы. Включаемые в архив разделы получат имена, совпадающие с последним компонентом части **PATH** формата URI.

В качестве имени схемы в абсолютных URI допустимо употреблять схему **fsys**, поддерживаемую библиотекой PureLibrary.

Совместно с параметром **-append** можно задавать параметр **-join PART**. При его задании все указанные в **-append** параметры будут добавлены не в индивидуальные разделы архива, а в один общий раздел с именем, заданным в параметре **PART**. Естественно, сливаемые таким образом данные должны в принципе позволять такое слияние (например, формат **NTRIPLES** такое слияние позволяет, а формат **RDFXML** - нет).

### Утилита da.xmlcrowler

Эта утилита может употребляться только в качестве промежуточного элемента конвейера. Она позволяет выполнить часто встречающий на практике краулинг данных в формате XML. 

Параметры запуска утилиты следующие:

> java -jar da.xmlcrowler.jar <стандартные аргументы> -rules RULES

Утилита подерживает весь набор стандартных аргументов. Параметр **-rules RULES** задает URI ресурса, где описаны правила преобразования XML-модели в RDF-формат. Пример правил преобразования
можно найти в модуле da.xmlcrowler (файл rules.example). Краулер *заменяет* в разделе архива содержимое XML на раскрауленные данные.

Утилита использует потоковый обработчик (SAX-парсер) при обработке XML, поэтому не имеет каких-либо ограничений на размер обрабатываемого XML.

### Утилита da.htmlcrawler

Эта утилита может употребляться только в качестве промежуточного элемента конвейера. Она позволяет выполнить часто встречающий на практике краулинг данных с сайта в формате HTML. Как правило, ее используют совместно
с утилитой **da.xmlcrowler**, потому что данные с сайта в HTML-формате утилита при выгрузке просто *преобразует* в данные формата XML. Преобразование же самого XML в RDF-представление удобно производить уже утилитой **da.xmlcrowler**.

Параметры запуска утилиты следующие:

> java -jar da.htmlcrawler.jar URI <стандартные аргументы> \[-headers HEADERS] \[-robots ROBOTS] \[-sitemap SITEMAP] \[-depth DEPTH]

Из набора стандартных аргументов утилита не поддерживает параметр **-pass PATTERN**, а параметр **-process PATTERN** рассматривается ею не как имена разделов архива, а как имена страниц сайта, подлежащих обработке.
Параметр **URI** представляет собой адрес сайта, подлежащего обработке. Параметр **-headers HEADERS** позволяет задавать заголовки запроса к сайту. Сам параметр **HEADERS** указывает на любой источник, содержащий
данные в формате класса *java.util.Properties*. Параметр **-robots ROBOTS** позволяет сымитировать содержимое файла *robots.txt* на сайте (например, если таковой отсутствует). Сам параметр **ROBOTS** указывает на
любой источник, содержащий данные в формате файла [robots.txt](https://developers.google.com/search/docs/crawling-indexing/robots/create-robots-txt). Параметр **-sitemap SITEMAP** позволяет аналогичным образом
сымитировать содержимое файла *sitemap.xml* или *sitemap_index.xml* на сайте. Сам параметр **SITEMAP** указывает на любой источник, содержащий данные в формате файла 
[sitemap.xml либо sitemap_index.xml](https://www.sitemaps.org/ru/protocol.html). Какой именно формат данных используется в том или ином случае, утилита определит самостоятельно. Параметр **-depth DEPTH** ограничивает глубину рекурсивного обхода по ссылкам, содержащимся в HTML-страницах. Его значение по умолчанию равно нулю (рекурсивный обход не производится). Если данные с сайта на являются HTML-страницей (например,
изображения), но они подходят под шаблон имени, заданный в параметре **-process PATTERN**, они в этом случае также будут включены в выходной архив под своими полными именами (часть *path* из URI, 
ссылающегося на эти данные). 

Утилита использует потоковый обработчик (SAX-парсер) при формировании XML, поэтому не имеет каких-либо ограничений на размер обрабатываемого HTML.

Для взаимодействия с сервером, из соображения совместимости с Java 9, используется проект [Apache HTTP-клиент](https://hc.apache.org/httpcomponents-client-5.2.x/). Для преобразования формата HTML в формат XML используется проект [Validator.nu HTML Parser](https://about.validator.nu/htmlparser/).

### Утилита da.converter

Эта утилита может употребляться только в качестве промежуточного элемента конвейера. Она позволяет преобразовывать форму представления RDF-данных в разделах архива из одного формата в другой.

Параметры запуска утилиты следующие:

> java -jar da.converter.jar <стандартные аргументы> \[-if RDF] -of RDF

Утилита подерживает весь набор стандартных аргументов. Параметр **-if RDF** задает исходный формат данных в разделах архива в формате нумерации *chav1961.da.util.interfaces.ContentFormat*. Если этот параметр не задан, для обработки будет использовано значение ключа **contentType** из раздела **ticket.txt**. Параметр **-of RDF** задает новый формат данных в разделах архива в 
формате нумерации *chav1961.da.util.interfaces.ContentFormat*. Его значение будет занесено в ключ **contentType** раздела **ticket.txt**. Утилита *заменяет* в разделе архива данные прежнего формата на данные нового формата.

Для преобразования формата данных используется проект [RDF4J](https://rdf4j.org/).

### Утилита da.infer

Эта утилита может употребляться только в качестве промежуточного элемента конвейера. Она позволяет выполнить предварительный инференсинг данных, чтобы не нагружать этим процессом основную 
базу. Областью инференсинга всегда является содержимое *только текущего раздела* архива. Как показала практика, это весьма эффективный способ повышения производительности системы в целом.

Параметры запуска утилиты следующие:

> java -jar da.infer.jar TYPE <стандартные аргументы> -ontology ONTOLOGY \[-customRules RULES]

Утилита подерживает весь набор стандартных аргументов. Параметр TYPE задает тип онтологии, по правилам которой надлежит произвести инференсинг. Допустимые значения параметра см. 
нумерацию *chav1961.da.util.interfaces.OntologyType* в данном проекте. Параметр **-ontology ONTOLOGY** задает URI источника, где описана онтология вашего проекта. Содержимое источника,
на который указывает этот URI, должно иметь тот же формат, что и значение ключа **contentType** из раздела **ticket.txt**, кодировка онтологии - "UTF-8".

Если при запуске утилиты задан параметр **-customRules RULES**, то, помимо инференсинга, утилита выполнит над содержимым раздела дополнительные манипуляции. Параметр **RULES** - любой
URI, откуда можно взять список правил манипуляции. Кодировка источника - "UTF-8". Правила манипуляции представляют собой текстовый файл, содержащий один или несколько операторов подмножества [SPARQL](https://ru.wikipedia.org/wiki/SPARQL). Допустимыми операторами являются только CONSTRUCT, UPDATE и DELETE. Один оператор в тексте отделяется от другого строкой "---". Их применение позволяет выполнить дополнительную обработку содержимого раздела (типичный пример - удалить часть излишне выведенных "капитанских" триплов).

Утилита *заменяет* в разделе архива прежние данные на новые (с дополнительно выведенными в процессе инференсинга сущностями). Формат данных в архиве при этом всегда преобразуется в **NTRIPLES**. 

Для выполнения инференсинга используется проект [JENA](https://jena.apache.org/).

### Утилита da.sender

Эта утилита может употребляться только в качестве промежуточного элемента конвейера. Она позволяет отправить выбранные разделы архива либо на удаленный сервер по различным протоколам, либо выгрузить их в файловую систему компьютера.

Параметры запуска утилиты следующие:

> java -jar da.sender.jar target <стандартные аргументы> \[-headers HEADERS] \[-mf]

Утилита подерживает весь набор стандартных аргументов. Параметр **target** задает либо URI сервера, куда следует отправить данные, либо URI файловой системы, куда их необходимо выгрузить. Сервер определяется типом схемы (а именно, http:, https:, tcp: или tcps:). Допустимо также употреблять схему **fsys**, поддерживаемую библиотекой PureLibrary. Параметр **-headers HEADERS** имеет смысл задавать только при работе по протоколу http/https - он указывает, какие заголовки нужно добавить в POST-запрос при передаче данных. Параметр **HEADERS** при этом указывает любой допустимый URI с данными в формате класса *java.util.Properties*, откуда взять заголовки. Параметр **-mf** имеет смысл задавать только при работе по протоколу http/https - он указывает, что вместо индивидуальной передачи каждого раздела архива необходимо собрать их в один запрос с MIME типом "multipart/form-data".

Для взаимодействия с сервером, из соображения совместимости с Java 9, используется проект [Apache HTTP-клиент](https://hc.apache.org/httpcomponents-client-5.2.x/).

### Коды завершения

Все утилиты данного проекта поддерживают три кода завершения:

- код **0** - успешная обработка
- код **128** - ошибка в параметрах командной строки
- код **129** - программная ошибка при работе утилиты

### Информация разработчику

ЕСли возникнет необходимость вклиниться в процесс работы конвейера со своим собственным обработчиком, в модуле da.util имеется класс *chav1961.da.util.AbstractZipProcessor*, от которого рекомендуется сделать "дочку", чтобы поддержать стандартную функциональность всех утилит. Помимо метода **processPart(...)**, который вам в любом случае придется реализовать, можно также переопределить в вашем классе методы **processTicket(...)** (гарантированно вызывается **до** всех вызовов метода processPart()) и **processApending(...)**  (гарантированно вызывается **после** всех вызовов метода processPart()). Подробности
 использования данного класса см Javadoc. Желательно также придерживаться соглашения по кодам завершения, описанным в предыдущем разделе.
