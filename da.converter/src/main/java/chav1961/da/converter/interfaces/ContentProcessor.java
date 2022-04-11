package chav1961.da.converter.interfaces;

import java.io.IOException;

@FunctionalInterface
public interface ContentProcessor {
	int		SUBJ_INDEX = 0;
	int		PRED_INDEX = 1;
	int		OBJ_INDEX = 2;
	int		TYPE_INDEX = 3;
	int		LANG_INDEX = 4;
	int		CONTEXT_INDEX = 5;
	int		DUMMY_VALUE = -1;
	
	/**
	 * <p>Process every record parsed. To reduce syntax tree content, object items either can be placed into syntax tree or can be passed thru objectContent parameter as-is. Usual criteria to choise is a length of object item (short items can be placed into tree, long can be passed as-is)</p>
	 * @param marks marks. Present bits for entity record. Bit-OR mask (see {@linkplain #SUBJ_INDEX}, {@linkplain #PRED_INDEX}, {@linkplain #OBJ_INDEX}, {@linkplain #TYPE_INDEX}, {@linkplain #LANG_INDEX}, {@linkplain #CONTEXT_INDEX})
	 * @param longContent long Id-s of parsed entities in the syntax tree. Array indices see see {@linkplain #SUBJ_INDEX}, {@linkplain #PRED_INDEX}, {@linkplain #OBJ_INDEX}, {@linkplain #TYPE_INDEX}, {@linkplain #LANG_INDEX}, {@linkplain #CONTEXT_INDEX}
	 * @param objectContent object item content. If mask contains 1 &lt;&lt;OBJ_INDEX , must be content of the object entity, otherwise must be null. 
	 * @throws IOException on any I/O errors
	 */
	void process(final int marks, final long[] longContent, final char[] objectContent) throws IOException;
}
