package chav1961.da.converter.input;

import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.util.Arrays;

import chav1961.da.converter.interfaces.ContentWriter;
import chav1961.da.converter.interfaces.InputConverterInterface;
import chav1961.da.util.interfaces.DAContentFormat;
import chav1961.purelib.basic.CharUtils;
import chav1961.purelib.basic.LineByLineProcessor;
import chav1961.purelib.basic.SubstitutableProperties;
import chav1961.purelib.basic.URIUtils;
import chav1961.purelib.basic.exceptions.EnvironmentException;
import chav1961.purelib.basic.exceptions.PreparationException;
import chav1961.purelib.basic.exceptions.SyntaxException;
import chav1961.purelib.basic.interfaces.LoggerFacade;
import chav1961.purelib.basic.interfaces.SyntaxTreeInterface;

public class NTripleReader implements InputConverterInterface {
	private static final URI						SERVE_URI = URI.create(CONV_SCHEMA+":"+DAContentFormat.N_TRIPLES.getSchema()+":/");
	private static final String						LITERAL_CACHING = "object.literal.length.caching";
	private static final char[]						DUMMY_STRING = "".toCharArray();
	private static final SubstitutableProperties	props; 

	static {
		try{props = new SubstitutableProperties(URI.create("root://"+NTripleReader.class.getName()+"/converter.properties"));
		} catch (IOException e) {
			throw new PreparationException(e.getLocalizedMessage());
		} 
	}

	private final int		literalCaching = props.getProperty(LITERAL_CACHING,int.class);
	private final long[]	tempLong = new long[6];
	private final int[]		tempInt = new int[2];
			
	
	public NTripleReader() {
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
			return new NTripleReader();
		}
		else {
			throw new EnvironmentException("Can't create instance for serving ["+resource+"]");
		}
	}

	@Override
	public void process(final Reader rdr, final SyntaxTreeInterface<char[]> tree, final ContentWriter writer, final LoggerFacade logger) throws IOException {
		// TODO Auto-generated method stub
		if (rdr == null) {
			throw new NullPointerException("Reader can't be null"); 
		}
		else if (tree == null) {
			throw new NullPointerException("Tree can't be null"); 
		}
		else if (writer == null) {
			throw new NullPointerException("Writer can't be null"); 
		}
		else if (logger == null) {
			throw new NullPointerException("Logger can't be null"); 
		}
		else {
			try(final LineByLineProcessor	lblp = new LineByLineProcessor((displacement, lineNo, data, from, length)->processLine(lineNo, data, from, from +length, tree, writer))) {
				lblp.write(rdr);
				lblp.flush();
			} catch (SyntaxException e) {
				throw new IOException(e.getLocalizedMessage(), e); 
			}
		}
		
	}
	
	private void processLine(final int lineNo, final char[] data, int from, final int to, final SyntaxTreeInterface<char[]> tree, final ContentWriter wr) throws IOException, SyntaxException {
		final int	start = from;
		char[]		tempChar = null;
		int			begin, flags = (1 << ContentWriter.SUBJ_INDEX) | (1 << ContentWriter.PRED_INDEX);
		
		from = CharUtils.skipBlank(data, from, true);
		try {
			switch (data[from]) {
				case '_' :
					from = parseAnon(lineNo, data, from, tree, ContentWriter.SUBJ_INDEX);
					break;
				case '<' :
					from = parseURI(lineNo, data, from, tree, ContentWriter.SUBJ_INDEX);
					break;
				case '#' : case '\r' : case '\n' :
					return;
				default :
					throw new SyntaxException(lineNo, from-start, "Illegal char ('_', '<', '#' are available only)"); 
			}
			
			switch (data[from]) {
				case '_' :
					from = parseAnon(lineNo, data, from, tree, ContentWriter.PRED_INDEX);
					break;
				case '<' :
					from = parseURI(lineNo, data, from, tree, ContentWriter.PRED_INDEX);
					break;
				case '#' : case '\r' : case '\n' :
					throw new SyntaxException(lineNo, from-start, "Missing predicate"); 
				default :
					throw new SyntaxException(lineNo, from-start, "Illegal char ('_', '<', are available only)"); 
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
				case '#' : case '\r' : case '\n' :
					throw new SyntaxException(lineNo, from-start, "Missing predicate"); 
				default :
					throw new SyntaxException(lineNo, from-start, "Illegal char ('_', '<', are available only)"); 
			}
			
			from = CharUtils.skipBlank(data, from, true);
			if (data[from] == '.') {
				wr.process(flags, tempLong, tempChar);
			}
			else {
				throw new SyntaxException(lineNo, from-start, "Missing '.'"); 
			}
		} catch (IllegalArgumentException exc) {
			throw new SyntaxException(lineNo, from-start, exc.getLocalizedMessage(), exc); 
		}
	}
	
	private int parseURI(final int lineNo, final char[] data, int from, final SyntaxTreeInterface<char[]> tree, final int tempLongIndex) throws SyntaxException, IllegalArgumentException, NullPointerException {
		final int 	begin = from + 1;
		
		from = CharUtils.parseString(data, begin, '>', CharUtils.NULL_APPENDABLE);
		if (from == begin + 1) {
			throw new SyntaxException(lineNo, SyntaxException.toCol(data, from), "Empty URI"); 
		}
		else {
			tempLong[tempLongIndex] = insertIntoTree(data, begin, from - 1, tree, true);
			return CharUtils.skipBlank(data, from, true);
		}
	}

	private int parseAnon(final int lineNo, final char[] data, int from, final SyntaxTreeInterface<char[]> tree, final int tempLongIndex) throws SyntaxException, IllegalArgumentException, NullPointerException {
		final int 	begin = from;
		
		from = CharUtils.parseName(data, from + 2, tempInt);
		if (from == begin + 2) {
			throw new SyntaxException(lineNo, SyntaxException.toCol(data, from), "Empty anon name"); 
		}
		else {
			tempLong[tempLongIndex] = insertIntoTree(data, begin, from, tree, false);
			return CharUtils.skipBlank(data, from, true);
		}
	}
	
	private long insertIntoTree(final char[] data, final int from, final int to, final SyntaxTreeInterface<char[]> tree, final boolean checkURI) throws SyntaxException, IllegalArgumentException, NullPointerException {
		final long	id = tree.seekName(data, from, to);
		
		if (id < 0) {
			final long	result = tree.placeName(data, from, to, Arrays.copyOfRange(data, from, to));
			
			if (checkURI) {
				final String	s = CharUtils.unescapeStringContent(tree.getName(result));
				
				try{if (!URI.create(s).isAbsolute()) {
						throw new SyntaxException(SyntaxException.toRow(data, from), SyntaxException.toCol(data, from), "URI ["+s+"] is not absolute");
					}
				} catch (IllegalArgumentException exc) {
					throw new SyntaxException(SyntaxException.toRow(data, from), SyntaxException.toCol(data, from), exc.getLocalizedMessage(), exc); 
				}
			}
			return result;
		}
		else {
			return id;
		}
	}
}
