package chav1961.da.converter.input;

import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import chav1961.da.converter.interfaces.ContentWriter;
import chav1961.da.converter.interfaces.InputConverterInterface;
import chav1961.da.util.interfaces.DAContentFormat;
import chav1961.purelib.basic.CharUtils;
import chav1961.purelib.basic.URIUtils;
import chav1961.purelib.basic.exceptions.EnvironmentException;
import chav1961.purelib.basic.exceptions.SyntaxException;
import chav1961.purelib.basic.interfaces.LoggerFacade;
import chav1961.purelib.basic.interfaces.SyntaxTreeInterface;

public class TurtleReader extends NTripleReader {
	private static final URI		SERVE_URI = URI.create(CONV_SCHEMA+":"+DAContentFormat.TURTLE.getSchema()+":/");
	private static final char[]		BASE_DIR = "@base".toCharArray();
	private static final char[]		PREFIX_DIR = "@prefix".toCharArray();
	private static final String[]	AVAILABLE_DIRECTIVES = {"@base", "@prefix"};
	private static final char[]		A_PREDICATE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type".toCharArray();
	
	private long				currentBase = -1;
	private Map<Long, Prefix>	prefixes = new HashMap<>();
	private long[]				oldContent = new long[tempLong.length];
	private boolean				skipSubject = false, skipPredicate = false;  
	
	public TurtleReader() {
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
			return new TurtleReader();
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
			switch (data[from]) {
				case '@' :
					processDirective(lineNo, data, from, tree);
					return;
				case '#' : case '\r' : case '\n' :
					return;
				default :
					if (!skipSubject) {
						from = parseSubject(lineNo, data, start, from, tree);
					}
					else {
						tempLong[ContentWriter.SUBJ_INDEX] = oldContent[ContentWriter.SUBJ_INDEX];
					}
			}

			if (!skipPredicate) {
				from = parsePredicate(lineNo, data, start, from, tree);
			}
			else {
				tempLong[ContentWriter.PRED_INDEX] = oldContent[ContentWriter.PRED_INDEX];
			}

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
				case ':' :
					from = processPrefixedValue(lineNo, data, start, from, ContentWriter.OBJ_INDEX, tree);
					break;
				case '#' : case '\r' : case '\n' :
					throw new SyntaxException(lineNo, from-start, "Missing object");
				default :
					if (Character.isJavaIdentifierStart(data[from])) {
						from = processPrefixedValue(lineNo, data, start, from, ContentWriter.OBJ_INDEX, tree);
					}
					else {
						throw new SyntaxException(lineNo, from-start, "Illegal char ('_', '<', are available only)"); 
					}
			}

			from = CharUtils.skipBlank(data, from, true);
			switch (data[from]) {
				case '.' : 
					wr.process(flags, tempLong, tempChar);
					skipSubject = false;
					skipPredicate = false;  
					break;
				case ',' : 
					wr.process(flags, tempLong, tempChar);
					System.arraycopy(tempLong, 0, oldContent, 0, oldContent.length);
					skipSubject = true;
					skipPredicate = true;  
					break;
				case ';' : 
					wr.process(flags, tempLong, tempChar);
					System.arraycopy(tempLong, 0, oldContent, 0, oldContent.length);
					skipSubject = true;
					skipPredicate = false;  
					break;
				default :
					throw new SyntaxException(lineNo, from-start, "Missing '.', ',' or ';' "); 
			}
		} catch (IllegalArgumentException exc) {
			throw new SyntaxException(lineNo, from-start, exc.getLocalizedMessage(), exc); 
		}
	}

	private void processDirective(final int lineNo, final char[] data, int from,final SyntaxTreeInterface<char[]> tree) throws SyntaxException {
		if (CharUtils.compare(data, from, PREFIX_DIR)) {
			final long	nameId;
			
			from = CharUtils.skipBlank(data, from + PREFIX_DIR.length, true);
			if (Character.isJavaIdentifierStart(data[from])) {
				from = CharUtils.parseName(data, from, tempInt);
				nameId = tree.placeOrChangeName(data, tempInt[0], tempInt[1]+1, null);
				from = CharUtils.skipBlank(data, from, true);
			}
			else {
				nameId = Long.MIN_VALUE;
			}
			
			if (data[from] == ':') {
				from = CharUtils.skipBlank(data, from + 1, true);
				
				if (data[from] == '<') {
					from = CharUtils.skipBlank(data, parseURI(lineNo, data, from, tree, ContentWriter.CONTEXT_INDEX), true);
					
					if (data[from] != '.') {
						throw new SyntaxException(lineNo, SyntaxException.toCol(data, from), "Missing '.' "); 
					}
					else {
						prefixes.put(nameId, new Prefix(nameId, tempLong[ContentWriter.CONTEXT_INDEX], tree.getName(tempLong[ContentWriter.CONTEXT_INDEX]).toCharArray()));
					}
				}
				else {
					throw new SyntaxException(lineNo, SyntaxException.toCol(data, from), "Missing URI "); 
				}
			}
			else {
				throw new SyntaxException(lineNo, SyntaxException.toCol(data, from), "Missing name or ':'"); 
			}
		}
		else if (CharUtils.compare(data, from, BASE_DIR)) {
			from = CharUtils.skipBlank(data, from + BASE_DIR.length, true);
			if (data[from] == '<') {
				from = CharUtils.skipBlank(data, parseURI(lineNo, data, from, tree, ContentWriter.CONTEXT_INDEX), true);
				
				if (data[from] != '.') {
					throw new SyntaxException(lineNo, SyntaxException.toCol(data, from), "Missing '.' "); 
				}
				else {
					currentBase = tempLong[ContentWriter.CONTEXT_INDEX];
				}
			}
			else {
				throw new SyntaxException(lineNo, SyntaxException.toCol(data, from), "Missing URI "); 
			}
		}
		else {
			throw new SyntaxException(lineNo, SyntaxException.toCol(data, from), "Unsupported directive. Only "+Arrays.toString(AVAILABLE_DIRECTIVES)+" are available"); 
		}
	}

	@Override
	protected int parseSubject(final int lineNo, final char[] data, final int start, int from, final SyntaxTreeInterface<char[]> tree) throws SyntaxException, IllegalArgumentException, NullPointerException {
		if (Character.isJavaIdentifierStart(data[from])) {
			return processPrefixedValue(lineNo, data, start, from, ContentWriter.SUBJ_INDEX, tree);
		}
		else if (data[from] == ':') {
			return processPrefixedValue(lineNo, data, start, from, ContentWriter.SUBJ_INDEX, tree);
		}
		else {
			return super.parseSubject(lineNo, data, start, from, tree);
		}
	}	

	@Override
	protected int parsePredicate(final int lineNo, final char[] data, final int start, int from, final SyntaxTreeInterface<char[]> tree) throws SyntaxException, IllegalArgumentException, NullPointerException {
		if (Character.isJavaIdentifierStart(data[from])) {
			if (data[from] == 'a' && !Character.isJavaIdentifierPart(data[from + 1]) && data[from + 1] != ':') {	// rdf:type
				tempLong[ContentWriter.PRED_INDEX] = tree.placeOrChangeName(data, from, from+1, A_PREDICATE);
				return CharUtils.skipBlank(data, from + 1, true);
			}
			else {
				return processPrefixedValue(lineNo, data, start, from, ContentWriter.PRED_INDEX, tree);
			}
		}
		else if (data[from] == ':') {
			return processPrefixedValue(lineNo, data, start, from, ContentWriter.PRED_INDEX, tree);
		}
		else {
			return super.parsePredicate(lineNo, data, start, from, tree);
		}
	}	
	
	private int processPrefixedValue(final int lineNo, final char[] data, final int start, int from, final int resultIndex, final SyntaxTreeInterface<char[]> tree) throws SyntaxException, IllegalArgumentException, NullPointerException {
		final int	startName = from;
		final long 	id;
		
		if (Character.isJavaIdentifierStart(data[from])) {
			from = CharUtils.parseName(data, from, tempInt);
			id = tree.seekName(data, tempInt[0], tempInt[1] + 1);
		}
		else {
			id = Long.MIN_VALUE;
		}
		
		if (id < 0 && id != Long.MIN_VALUE) {
			throw new SyntaxException(lineNo, SyntaxException.toCol(data, from), "Unknown prefix ["+new String(data, tempInt[0], tempInt[1]-tempInt[0]+1)+"]");
		}
		else {
			final Prefix	pref = prefixes.get(id);
			
			from = CharUtils.skipBlank(data, from, true);
			if (data[from] == ':') {
				from = CharUtils.parseName(data, CharUtils.skipBlank(data, from + 1, true), tempInt);
				
				final char[]	result = new char[pref.prefixURI.length + tempInt[1] - tempInt[0] + 1];
				
				System.arraycopy(pref.prefixURI, 0, result, 0, pref.prefixURI.length);
				System.arraycopy(data, tempInt[0], result, pref.prefixURI.length, tempInt[1] - tempInt[0] + 1);
				
				tempLong[resultIndex] = tree.placeOrChangeName(data, startName, tempInt[1] + 1, result);
				return CharUtils.skipBlank(data, from, true);
			}
			else {
				throw new SyntaxException(lineNo, SyntaxException.toCol(data, from), "Missing ':'");
			}
		}
	}
	
	private static class Prefix {
		final long		prefixId;
		final long		prefixValueId;
		final char[]	prefixURI;
		
		private Prefix(long prefixId, long prefixValueId, char[] prefixURI) {
			this.prefixId = prefixId;
			this.prefixValueId = prefixValueId;
			this.prefixURI = prefixURI;
		}

		@Override
		public String toString() {
			return "Prefix [prefixId=" + prefixId + ", prefixValueId=" + prefixValueId + ", prefixURI=" + Arrays.toString(prefixURI) + "]";
		}
	}
}
