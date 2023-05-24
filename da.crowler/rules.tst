// шаблон пути файла: шаблон пути XML -> выходной шаблон
@head
# writter \
# assa  
@body
/ads/${module}/locale/*/*.xml : AdsDefinition/AdsLocalizingBundleDefinition/String[@Id=${id}]/Value[@Language=${lang}]/${content} -> \ 
	<http://radixware.org/${id}> <http://radixware.org/TextContent> "${content}"@${lang}. \
	<http://radixware.org/${id}> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://radixware.org/MultiLangString>.
/dds/${module}/locale/*.xml	  : AdsDefinition/AdsLocalizingBundleDefinition/String[@Id=${id}]/Value[@Language=${lang}]/${content} -> \
	<http://radixware.org/${id}> <http://radixware.org/TextContent> "${content}"@${lang}. \
	<http://radixware.org/${id}> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://radixware.org/MultiLangString>.
@tail
# the end	