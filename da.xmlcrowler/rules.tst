// 
// Общее замечание - все правила должны быть записаны в одну строку. Если строка слишком длинная,
// можно в конце строки поставить символ '\' и продолжить данные на следующия строках.
// Для обозначения перевода строки, который должен попасть в выходной поток, в строках можно 
// использовать комбинацию '\n'
//

//
// Секция @head - необязательная
// Текст, который может быть добавлен в начало файла с выгруженными данными
// В тексте допустимо использование подстановочный переменных вида ${name}
// В секции @head и @tail доступны только имена из переменных окружения ОС,
// из параметров -Dимя=значение из командной строки java, а также предопределенное имя
// ${timestamp} в формате даты/времени (время запуска приложения)
// 

@head
#
# User 		: ${USERNAME}
# Uploaded	: ${timestamp}  
#  

//
// Секция @body - обязательная
// Если секций @head и @tail в файле нет, строку @body можно не писать
//
// Формат задания правил следуюший:
//      шаблон пути файла : шаблон пути XML -> выходной шаблон
//
// Все ${name}, упомянутые в шаблонах пути файла и пути XML, наряду с общими подстановочными переменными, можно использовать для подстановки в выходном шаблоне
// 

@body
/ads/${module}/locale/*/*.xml : AdsDefinition/AdsLocalizingBundleDefinition/String[@Id=${id}]/Value[@Language=${lang}]/${content} -> \ 
	<http://radixware.org/${id}> <http://radixware.org/TextContent> "${content}"@${lang}. \
	<http://radixware.org/${id}> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://radixware.org/MultiLangString>.

/dds/${module}/locale/*.xml	  : AdsDefinition/AdsLocalizingBundleDefinition/String[@Id=${id}]/Value[@Language=${lang}]/${content} -> \
	<http://radixware.org/${id}> <http://radixware.org/TextContent> "${content}"@${lang}. \
	<http://radixware.org/${id}> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://radixware.org/MultiLangString>.

//	
// Секция @tail - необязательная
// Текст, который может быть добавлен в конец файла с выгруженными данными
//

@tail
# the end
