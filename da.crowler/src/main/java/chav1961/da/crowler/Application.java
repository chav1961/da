package chav1961.da.crowler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import chav1961.purelib.basic.ArgParser;
import chav1961.purelib.basic.PureLibSettings;
import chav1961.purelib.basic.exceptions.CommandLineParametersException;
import chav1961.purelib.basic.exceptions.SyntaxException;

public class Application {
	public static final String		ARG_SOURCE_DIR = "sd";
	public static final String		ARG_RULES_FILE = "r";
	public static final String		ARG_OUTPUT_FILE = "o";
	public static final String		ARG_DEBUG = "d";

	public static final String		KEY_HEAD = "@head";
	public static final String		KEY_BODY = "@body";
	public static final String		KEY_TAIL = "@tail";
	
	@FunctionalInterface
	public interface TriPredicate<T, U, V> {
	    boolean test(T t, U u, V v);
	}	
	
	public static enum Parts {
		HEAD(KEY_HEAD),
		BODY(KEY_BODY),
		TAIL(KEY_TAIL);
		
		private final String	partLabel;
		
		private Parts(final String partLabel) {
			this.partLabel = partLabel;
		}
		
		public String getPartLabel() {
			return partLabel;
		}
	}
	
	private static final Pattern	SUBST = Pattern.compile("\\$\\{(\\w+)\\}");
	private static final Pattern	ATTRS = Pattern.compile("(\\w+)\\[(.+)\\]");
	private static final Pattern	ATTR = Pattern.compile("@(\\w+)=\\$\\{(\\w+)\\}");
	private static final char[]		NULL_ARRAY = "".toCharArray();

	public static void main(final String[] args) {
		final ArgParser				parserTemplate = new ApplicationArgParser();
		final Map<String,char[]>	variables = new HashMap<>();
		
		try{final ArgParser		parser = parserTemplate.parse(args);
			final RuleSet		rules;

			fillVariables(variables);
			if (parser.isTyped(ARG_RULES_FILE)) {
				try(final InputStream	is = new FileInputStream(parser.getValue(ARG_RULES_FILE, File.class))) {
					rules = loadRules(is, variables);
				}
			}
			else {
				rules = loadRules(System.in, variables);
			}
			
			final long 	startTime = System.currentTimeMillis();
			final int	count;
			
			if (parser.isTyped(ARG_OUTPUT_FILE)) {
				try(final OutputStream	os = new FileOutputStream(parser.getValue(ARG_OUTPUT_FILE, File.class))) {
					count = uploadModel(parser.getValue(ARG_SOURCE_DIR, File.class), rules, os, parser.getValue(ARG_DEBUG, boolean.class));
				}
			}
			else {
				count = uploadModel(parser.getValue(ARG_SOURCE_DIR, File.class), rules, System.out, parser.getValue(ARG_DEBUG, boolean.class));
			}
			
			System.err.println("Total files processed: "+count+", duration="+(System.currentTimeMillis()-startTime)+"msec");
		} catch (IOException | SyntaxException e) {
			e.printStackTrace();
			System.exit(129);
		} catch (CommandLineParametersException e) {
			System.err.println(e.getLocalizedMessage());
			System.err.println(parserTemplate.getUsage("crowl"));
			System.exit(128);
		}
	}

	private static void fillVariables(final Map<String, char[]> variables) {
		for( Entry<String, String> item : System.getenv().entrySet()) {
			variables.put(item.getKey(), item.getValue().toCharArray());
		}
		for( Entry<Object, Object> item : System.getProperties().entrySet()) {
			variables.put(item.getKey().toString(), item.getValue().toString().toCharArray());
		}
		variables.put("timestamp", new Date(System.currentTimeMillis()).toString().toCharArray());
	}

	private static RuleSet loadRules(final InputStream in, final Map<String,char[]> variables) throws IOException, SyntaxException {
		final List<Template>		rules = new ArrayList<>();
		final StringBuilder			before = new StringBuilder(), after = new StringBuilder();
		
		try(final Reader			rdr = new InputStreamReader(in, PureLibSettings.DEFAULT_CONTENT_ENCODING);
			final BufferedReader	brdr = new BufferedReader(rdr)) {
			final StringBuilder		sb = new StringBuilder();
			Parts	currentPart = Parts.BODY;
			String	line;
			int		lineNo = 1;
			
			while ((line = brdr.readLine()) != null) {
				String	trimmed = line.trim();
				
				if (!trimmed.isEmpty() && !trimmed.startsWith("//")) {
					switch (trimmed.toLowerCase()) {
						case KEY_HEAD	:
							currentPart = Parts.HEAD;
							break;
						case KEY_BODY	:
							currentPart = Parts.BODY;
							break;
						case KEY_TAIL	:
							currentPart = Parts.TAIL;
							break;
						default :
							if (trimmed.endsWith("\\")) {	// Continuation to the next line
								sb.append(trimmed, 0, trimmed.length() - 1).append("\\n");
							}
							else {
								if (!sb.isEmpty()) {		// Multiline string is finished
									sb.append(trimmed);
									trimmed = sb.toString();
									sb.setLength(0);
								}
								switch (currentPart) {
									case HEAD	:
										before.append(trimmed).append(System.lineSeparator());
										break;
									case BODY	:
										rules.add(parseLine(lineNo, trimmed, variables));
										break;
									case TAIL	:
										after.append(trimmed).append(System.lineSeparator());
										break;
									default:
										throw new UnsupportedOperationException("Part type ["+currentPart+"] is not supported yet"); 
								}
							}
							break;
					}
					
				}
				lineNo++;
			}
		}
		final Map<String, List<Template>>	groups = new HashMap<>();
		
		for(Template item : rules) {
			if (!groups.containsKey(item.filePattern.pattern())) {
				groups.put(item.filePattern.pattern(), new ArrayList<>());
			}
			groups.get(item.filePattern.pattern()).add(item);
		}
		
		final List<Rule>	result = new ArrayList<>();
		
		for(Entry<String, List<Template>> item : groups.entrySet()) {
			result.add(new Rule(item.getValue().get(0).filePattern, item.getValue().toArray(new Template[item.getValue().size()])));
		}
		return new RuleSet(parseSection(escapeString(before.toString()), variables), 
						result.toArray(new Rule[result.size()]), 
						parseSection(escapeString(after.toString()), variables));
	}

	private static String escapeString(final String source) {
		return source.replace("\\n", "\n");
	}

	// <file path template>:<xml path template> -> <format>
	// ${name} can be used inside any part
	// xml path can contains /name[@item=${name},...] and can terminate with ${name}
	private static Template parseLine(final int lineNo, final String line, final Map<String,char[]> variables) throws SyntaxException {
		final int	colonIndex = line.indexOf(':'), seqIndex = line.indexOf("->");
		
		if (colonIndex < 0) {
			throw new SyntaxException(lineNo, 0, "Missing ':' in the rule"); 
		}
		else if (seqIndex < 0) {
			throw new SyntaxException(lineNo, 0, "Missing '->' in the rule"); 
		}
		else {
			final String		pathTemplate = line.substring(0, colonIndex).trim(); 
			final String		xmlTemplate = line.substring(colonIndex + 1, seqIndex).trim(); 
			final String		format = line.substring(seqIndex + 2).trim().replace("\\n", System.lineSeparator());
			final Map<String, Integer>	names = new HashMap<>();			
			int					varNumber = 0;
			
			// Parse file path template
			final StringBuilder	sbPT = new StringBuilder();
			int					extracts = 0, depth = 0; 
			
			for(String item : pathTemplate.split("/")) {
				final Matcher	m = SUBST.matcher(item);
				
				if (m.find()) {
					final String	name = m.group(1);
					
					sbPT.append("/.+");
					if (names.containsKey(name)) {
						throw new SyntaxException(lineNo, 0, "Duplicate substitution name ["+name+"] in the file path template"); 
					}
					else {
						names.put(name, varNumber++);
						extracts |= (1 << depth);
					}
				}
				else if (item.indexOf('.') >= 0 || item.indexOf('*') >= 0 || item.indexOf('?') >= 0) {
					sbPT.append('/').append(item.replace(".", "\\.").replace("?", ".").replace("*", ".+"));
				}
				else {
					sbPT.append('/').append(item);
				}
				depth++;
			}
			
			// Parse xml path template
			final StringBuilder					sbXT = new StringBuilder();
			final List<String>					tags = new ArrayList<>();
			final List<Map<String, Integer>>	tagAttrs = new ArrayList<>();
			boolean								textNodePresents = false;
			
			for(String item : xmlTemplate.split("/")) {
				final Matcher	m = ATTRS.matcher(item);
				
				if (m.find()) {
					final Map<String, Integer>	attrNames = new HashMap<>();
					
					for(String attr : m.group(2).split(",")) {
						final Matcher	m1 = ATTR.matcher(attr);
						
						if (m1.find()) {
							final String	substName = m1.group(2);
							final String	attrName = m1.group(1);
							
							if (names.containsKey(substName)) {
								throw new SyntaxException(lineNo, 0, "Duplicate substitution name ["+substName+"] in the tag ["+m.group(1)+"] from the XML path");
							}
							else {
								names.put(substName, varNumber++);
							}
							if (attrNames.containsKey(attrName)) {
								throw new SyntaxException(lineNo, 0, "Duplicate attribute reference ["+attrName+"] for tag ["+m.group(1)+"] in the XML path");
							}
							else {
								attrNames.put(attrName, names.get(substName));
							}
						}
						else {
							throw new SyntaxException(lineNo, 0, "Illegal attribute descriptor for tag ["+m.group(1)+"] in the XML path");
						}
					}
					tags.add(m.group(1));
					tagAttrs.add(attrNames);
				}
				else {
					final Matcher	m1 = SUBST.matcher(item);
					
					if (m1.find()) {
						if (!textNodePresents) {
							names.put(m1.group(1), varNumber++);
							textNodePresents = true;
						}
						else {
							throw new SyntaxException(lineNo, 0, "Substitution ${"+m1.group(1)+"} in the XML path can't be more than once"); 
						}
					}
					else {
						tags.add(item);
						tagAttrs.add(null);
					}
				}
			}
			
			// Parse format
			final List<Function<char[][], char[]>>	supp = new ArrayList<>();
			final Matcher		m = SUBST.matcher(format);
			int		from = 0;
			
			while (m.find()) {
				final String	name = m.group(1);
				final String	text = format.substring(from, m.start());
				final char[]	charText = text.toCharArray();
				
				supp.add((s)->charText);
				if (!names.containsKey(name)) {
					if (variables.containsKey(name)) {
						final char[]	varValue = variables.get(name);
						
						supp.add((s)->varValue);
					}
					else {
						throw new SyntaxException(lineNo, 0, "Unknown substitution ${"+name+"} in the format"); 
					}
				}
				else {
					final int	varIndex = names.get(name);
					
					supp.add((s)->s[varIndex]);
				}
				from = m.end();
			}
			final char[]		tail = (format.substring(from)+System.lineSeparator()).toCharArray();
			
			supp.add((s)->tail);
			
			final List<TriPredicate<String, Attributes,Template>>	patterns = new ArrayList<>();
			for(int index = 0; index < tags.size(); index++) {
				final String	value = tags.get(index);
				final int		valueHash = value.hashCode();
				
				if (tagAttrs.get(index) != null) {
					final Map<String, Integer>	pairs = tagAttrs.get(index);
					
					patterns.add((str, attrs, templ)->str.hashCode() == valueHash && value.equals(str) && extractAttrs(attrs, pairs, templ));
				}
				else {
					patterns.add((str, attrs, templ)->str.hashCode() == valueHash && value.equals(str));
				}
			}
			
			return new Template(Pattern.compile(sbPT.substring(1)), extracts, patterns.toArray(new TriPredicate[patterns.size()]), 
					new char[varNumber][], textNodePresents, supp.toArray(new Function[supp.size()]));
		}
	}

	private static Supplier<char[]>[] parseSection(final String section, final Map<String,char[]> variables) throws SyntaxException {
		final List<Supplier<char[]>>	supp = new ArrayList<>();
		final Matcher					m = SUBST.matcher(section);
		int		from = 0;
		
		while (m.find()) {
			final String	name = m.group(1);
			final String	text = section.substring(from, m.start());
			final char[]	charText = text.toCharArray();
			
			supp.add(()->charText);
			if (variables.containsKey(name)) {
				final char[]	varValue = variables.get(name);
				
				supp.add(()->varValue);
			}
			else {
				throw new SyntaxException(0, 0, "Unknown substitution ${"+name+"} in the format"); 
			}
			from = m.end();
		}
		final char[]		tail = (section.substring(from)+System.lineSeparator()).toCharArray();
		
		supp.add(()->tail);
		return supp.toArray(new Supplier[supp.size()]);
	}
	
	private static boolean extractAttrs(final Attributes attrs, final Map<String, Integer> pairs, final Template template) {
		boolean	result = true;
		
		for (Entry<String, Integer> pair : pairs.entrySet()) {
			final String	value = attrs.getValue(pair.getKey());
			
			if (value != null) {
				template.substitutions[pair.getValue()] = value.toCharArray();
			}
			else {
				result = false;
			}
		}
		return result;
	}

	private static int uploadModel(final File root, final RuleSet rules, final OutputStream os, final boolean debug) throws IOException {
		final int	count;
		
		try(final Writer	wr = new OutputStreamWriter(os, PureLibSettings.DEFAULT_CONTENT_ENCODING)) {
			
			for (Supplier<char[]> item : rules.before) {
				wr.write(item.get());
			}
			count = uploadModel(root, "", rules.rules, wr, debug);			
			for (Supplier<char[]> item : rules.after) {
				wr.write(item.get());
			}
			return count;
		}
	}

	private static int uploadModel(final File root, final String path, Rule[] rules, final Writer wr, final boolean debug) throws IOException {
		final File	current = new File(root, path);
		int count = 0;
		
		if (current.exists() && current.canRead()) {
			if (current.isDirectory()) {
				final String[]	content = current.list();
				
				if (content != null) {
					for(String item : content) {
						count += uploadModel(root, path+"/"+item, rules, wr, debug);
					}
				}
			}
			else {
				for (Rule item : rules) {
					if (item.filePattern.matcher(path).matches()) {
						if (debug) {
							System.err.println("Processing "+path+" ... ");
						}
						uploadModel(current, item, path, wr);
						count++;
					}
				}
			}
		}
		return count;
	}

	private static void uploadModel(final File file, final Rule rule, final String path, final Writer wr) throws IOException {
		final String[]	parts = path.split("/");
		
		for(int index = 0, count = 0; index < parts.length; index++) {	// Parse file path 
			if ((rule.templates[0].extractPattern & (1 << index)) != 0) {
				for(Template item : rule.templates) {
					item.substitutions[count] = parts[index].toCharArray();
				}
				count++;
			}
		}
		
		try(final InputStream		is = new FileInputStream(file);		// Parse file content
			final Reader			rdr = new InputStreamReader(is, PureLibSettings.DEFAULT_CONTENT_ENCODING)) {
			final SAXParserFactory 	factory = SAXParserFactory.newInstance();
			final SAXParser 		saxParser = factory.newSAXParser();
			final CrowlingHandler 	handler = new CrowlingHandler(rule.templates, wr);

			saxParser.parse(new InputSource(rdr), handler);
		} catch (ParserConfigurationException | SAXException e) {
			System.err.println("File ["+file.getAbsolutePath()+"]: "+e.getLocalizedMessage()); 
		}
	}

	private static class Template {
		final Pattern						filePattern;
		final int							extractPattern;
		final TriPredicate<String, Attributes, Template>[]	pathPattern;
		final boolean						hasContent;
		final char[][]						substitutions;
		final Function<char[][], char[]>[]	outputFunc;
		
		public Template(final Pattern filePattern, final int extractPattern, final TriPredicate<String, Attributes, Template>[] pathPattern, final char[][] substitutions, final boolean hasContent, final Function<char[][], char[]>[] outputFunc) {
			this.filePattern = filePattern;
			this.extractPattern = extractPattern;
			this.pathPattern = pathPattern;
			this.substitutions = substitutions;
			this.hasContent = hasContent;
			this.outputFunc = outputFunc;
		}
	}

	private static class Rule {
		public final Pattern	filePattern;
		public final Template[]	templates;
		
		public Rule(final Pattern filePattern, final Template... templates) {
			this.filePattern = filePattern;
			this.templates = templates;
		}
	}

	private static class RuleSet {
		private final Supplier<char[]>[]	before;
		private final Rule[]				rules;
		private final Supplier<char[]>[]	after;
		
		public RuleSet(final Supplier<char[]>[] before, final Rule[] rules, final Supplier<char[]>[] after) {
			this.before = before;
			this.rules = rules;
			this.after = after;
		}
	}
	
	private static class CrowlingHandler extends DefaultHandler {
		private static final long[]		MASKS = new long[64];
		private static final long[]		BITS = new long[64];
		
		static {
			long	mask = 1;
			long	bit = 1;
			
			for(int index = 0; index < 64; index++, mask = (mask << 1) | 1, bit <<= 1) {
				MASKS[index] = mask;
				BITS[index] = bit;
			}
		}
		
		private final Template[]		rules;
		private final Writer			wr;
		private final long[]			matches;
		private int						depth = -1;
		
		public CrowlingHandler(final Template[] rules, final Writer wr) {
			this.rules = rules;
			this.wr = wr;
			this.matches = new long[rules.length];
		}
		
	    @Override
	    public void characters(char[] ch, int start, int length) throws SAXException {
			for (Template rule : rules) {
		    	if (rule.hasContent) {
		    		if (rule.substitutions[rule.substitutions.length-1] == null) {
		    			rule.substitutions[rule.substitutions.length-1] = Arrays.copyOfRange(ch, start, start + length);
		    		}
		    		else {
			    		rule.substitutions[rule.substitutions.length-1] = append(rule.substitutions[rule.substitutions.length-1], ch, start, length);
		    		}
		    	}
	    	}
	    }

		@Override
	    public void startElement(String uri, String lName, String qName, Attributes attr) throws SAXException {
			depth++;
			for (int index = 0; index < rules.length; index++) {
				final Template	rule = rules[index];
				
		    	if (depth < rule.pathPattern.length) {
		    		if (rule.pathPattern[depth].test(qName, attr, rule)) {
			    		matches[index] |= BITS[depth];
		    		}
		    		else {
			    		matches[index] &= ~BITS[depth];
		    		}
		    	}
		    	else {
		    		matches[index] &= ~BITS[depth];
		    	}
		    	if (rule.hasContent) {
		    		rule.substitutions[rule.substitutions.length-1] = null;
		    	}
			}
	    }

	    @Override
	    public void endElement(String uri, String localName, String qName) throws SAXException {
			for (int index = 0; index < rules.length; index++) {
				final Template	rule = rules[index];
		    	
		    	if (depth == rule.pathPattern.length - 1 && (matches[index] & MASKS[depth]) == MASKS[depth]) {
			    	if (rule.hasContent) {
			    		if (rule.substitutions[rule.substitutions.length-1] == null) {
			    			rule.substitutions[rule.substitutions.length-1] = NULL_ARRAY;
			    		}
			    		else {
				    		rule.substitutions[rule.substitutions.length-1] = escapeString(rule.substitutions[rule.substitutions.length-1]);
			    		}
			    	}		    		
		    		
		    		try{
		    			for(Function<char[][], char[]> item : rule.outputFunc) {
		    				wr.write(item.apply(rule.substitutions));
		    			}
					} catch (IOException e) {
						throw new SAXException(e);
					}
		    	}
	    		matches[index] &= ~BITS[depth];
			}
			depth--;
	    }

	    private char[] append(final char[] source, final char[] append, int start, int length) {
	    	final char[]	result = new char[source.length + length];
			
	    	System.arraycopy(source, 0, result, 0, source.length);
	    	System.arraycopy(append, start, result, source.length, length);
	    	return result;
		}

		private char[] escapeString(final char[] source) {
			for(char item : source) {
				if (item == '\n') {
					return new String(source).trim().replace("\n", "\\n").toCharArray();
				}
			}
			return source;
			
		}
	}	

	private static class ApplicationArgParser extends ArgParser {
		private static final ArgParser.AbstractArg[]	KEYS = {
			new FileArg(ARG_SOURCE_DIR, true, "Source directory to crowl model. Default is current directory", "./"),
			new FileArg(ARG_RULES_FILE, false, "Rules file location. You can use 'crowl ... <rules.txt' instead of this parameters", "./rules.txt"),
			new FileArg(ARG_OUTPUT_FILE, false, false, "Output file location. You can use 'crowl ... >output.txt' instead of this parameters"),
			new BooleanArg(ARG_DEBUG, false, "Turn on debug trace", false),
		};
		
		ApplicationArgParser() {
			super(KEYS);
		}
	}
}
