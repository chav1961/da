package chav1961.da.converter.input;

import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.util.Arrays;

import chav1961.da.converter.interfaces.ContentWriter;
import chav1961.da.converter.interfaces.InputConverterInterface;
import chav1961.da.util.interfaces.DAContentFormat;
import chav1961.purelib.basic.CharUtils;
import chav1961.purelib.basic.URIUtils;
import chav1961.purelib.basic.exceptions.EnvironmentException;
import chav1961.purelib.basic.exceptions.SyntaxException;
import chav1961.purelib.basic.interfaces.LoggerFacade;
import chav1961.purelib.basic.interfaces.SyntaxTreeInterface;

public class NQuadReader extends NTripleReader {
	private static final URI	SERVE_URI = URI.create(CONV_SCHEMA+":"+DAContentFormat.N_QUADS.getSchema()+":/");
	
	public NQuadReader() {
	}
	
	@Override
	public boolean canServe(final URI resource) throws NullPointerException {
		if (resource == null) {
			throw new NullPointerException("Resource to test can'tbe null");
		}
		return URIUtils.canServeURI(resource, SERVE_URI);
	}

	@Override
	public InputConverterInterface newInstance(final URI resource) throws EnvironmentException, NullPointerException, IllegalArgumentException {
		if (resource == null) {
			throw new NullPointerException("Resource to test can'tbe null");
		}
		else if (canServe(resource)) {
			return new NQuadReader();
		}
		else {
			throw new EnvironmentException("Can't create instance for serving ["+resource+"]");
		}
	}

	@Override
	protected void processLine(final int lineNo, final char[] data, int from, final int to, final SyntaxTreeInterface<char[]> tree, final ContentWriter wr) throws IOException, SyntaxException {
		final int	start = from;
		char[]		tempChar = null;
		int			begin, flags = (1 << ContentWriter.SUBJ_INDEX) | (1 << ContentWriter.PRED_INDEX);
		
		from = CharUtils.skipBlank(data, from, true);
		try {
			if ((from = parseSubject(lineNo, data, start, from, tree)) < 0) {
				return;
			}
			
			from = parsePredicate(lineNo, data, start, from, tree);

			tempLong[ContentWriter.OBJ_INDEX] = ContentWriter.DUMMY_VALUE;
			tempLong[ContentWriter.TYPE_INDEX] = ContentWriter.DUMMY_VALUE;
			tempLong[ContentWriter.LANG_INDEX] = ContentWriter.DUMMY_VALUE;
			tempLong[ContentWriter.CONTEXT_INDEX] = ContentWriter.DUMMY_VALUE;
			
			switch (data[from]) {
				case '_' :
					from = parseAnon(lineNo, data, from, tree, ContentWriter.OBJ_INDEX);
					break;
				case '<' :
					from = parseURI(lineNo, data, from, tree, ContentWriter.OBJ_INDEX);
					break;
				case '\"' :
					begin = from + 1;
					from = CharUtils.parseString(data, begin, '\"', CharUtils.NULL_APPENDABLE);
					if (from == begin + 1) {	// empty string
						tempLong[ContentWriter.OBJ_INDEX] = ContentWriter.DUMMY_VALUE;
						tempChar = DUMMY_STRING;
					}
					else if (from-begin < literalCaching) {
						tempLong[ContentWriter.OBJ_INDEX] = insertIntoTree(data, begin, from - 1, tree, false);
						flags |= 1 << ContentWriter.OBJ_INDEX;
					}
					else {
						tempLong[ContentWriter.OBJ_INDEX] = ContentWriter.DUMMY_VALUE;
						tempChar = Arrays.copyOfRange(data, begin, from - 1);
					}
					if (data[from] == '^' && data[from + 1] == '^') {
						from = CharUtils.skipBlank(data, from + 2, true);
						if (data[from] == '<') {
							from = parseURI(lineNo, data, from, tree, ContentWriter.TYPE_INDEX);
							flags |= 1 << ContentWriter.TYPE_INDEX;
						}
						else {
							throw new SyntaxException(lineNo, from-start, "Illegal char ('<' are available only)"); 
						}
					}
					else if (data[from] == '@') {
						from = CharUtils.skipBlank(data, from + 1, true);
						if (Character.isLetter(data[from])) {
							from = CharUtils.parseName(data, CharUtils.skipBlank(data, from, true), tempInt);
							
							if (data[from] == '-') {
								final int	temp = tempInt[0];
								
								from = CharUtils.parseName(data, CharUtils.skipBlank(data, from + 1, true), tempInt);
								tempInt[0] = temp;
								tempLong[ContentWriter.LANG_INDEX] = insertIntoTree(data, tempInt[0], tempInt[1] + 1, tree, false);
							}
							else {
								tempLong[ContentWriter.LANG_INDEX] = insertIntoTree(data, tempInt[0], tempInt[1] + 1, tree, false);
							}
							flags |= 1 << ContentWriter.LANG_INDEX;
						}
						else {
							throw new SyntaxException(lineNo, from-start, "Language specification is missing"); 
						}
					}
					
					break;
				case '#' : case '\r' : case '\n' :
					throw new SyntaxException(lineNo, from-start, "Missing predicate"); 
				default :
					throw new SyntaxException(lineNo, from-start, "Illegal char ('_', '<', are available only)"); 
			}

			from = CharUtils.skipBlank(data, from, true);
			from = parseContent(lineNo, data, start, from, tree);
			from = CharUtils.skipBlank(data, from, true);
			
			if (data[from] == '.') {
				wr.process(flags, tempLong, tempChar);
			}
			else {
				throw new SyntaxException(lineNo, from-start, "Missing '.'"+new String(data,start,from)); 
			}
		} catch (IllegalArgumentException exc) {
			throw new SyntaxException(lineNo, from-start, exc.getLocalizedMessage(), exc); 
		}
	}
	
	int parseContent(final int lineNo, final char[] data, final int start, int from, final SyntaxTreeInterface<char[]> tree) throws SyntaxException, IllegalArgumentException, NullPointerException {
		switch (data[from]) {
			case '_' : return parseAnon(lineNo, data, from, tree, ContentWriter.CONTEXT_INDEX);
			case '<' : return parseURI(lineNo, data, from, tree, ContentWriter.CONTEXT_INDEX);
			case '.' : return from;
			case '#' : case '\r' : case '\n' :
				throw new SyntaxException(lineNo, from-start, "Missing predicate"); 
			default :
				throw new SyntaxException(lineNo, from-start, "Illegal char ('_', '<', are available only)"); 
		}
	}
	
}
