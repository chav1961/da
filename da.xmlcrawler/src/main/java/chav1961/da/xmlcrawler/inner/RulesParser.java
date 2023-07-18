package chav1961.da.xmlcrawler.inner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Attr;

import chav1961.purelib.basic.CharUtils;
import chav1961.purelib.basic.MathUtils;
import chav1961.purelib.basic.PureLibSettings;
import chav1961.purelib.basic.Utils;
import chav1961.purelib.basic.exceptions.SyntaxException;
import chav1961.purelib.basic.growablearrays.GrowableCharArray;
import chav1961.purelib.cdb.SyntaxNode;

public class RulesParser {
	private static final Pattern	SUBST = Pattern.compile("\\$\\{(\\w+)\\}");
	private static final Pattern	ATTRS = Pattern.compile("(\\w+)\\[(.+)\\]");
	private static final Pattern	NAME_AND_ATTRS = Pattern.compile("\\$\\{(\\w+)\\}\\[(.+)\\]");
	private static final Pattern	ATTR = Pattern.compile("@(\\w+)=\\$\\{(\\w+)\\}");
	private static final Pattern	ATTR_OPT = Pattern.compile("@@(\\w+)=\\$\\{(\\w+)\\}");
	private static final Pattern	ATTR_VALUE = Pattern.compile("@(\\w+)=\"(.*)\"");
	private static final Pattern	ATTR_OPT_VALUE = Pattern.compile("@@(\\w+)=\"(.*)\"");
	private static final Pattern	PREPROC = Pattern.compile("(\\Q.if \\E)([^\\n]*)|(\\Q.else\\E)([^\\n]*)|(\\Q.endif\\E)([^\\n]*)");

	private static final String		KEY_HEAD = "@head";
	private static final String		KEY_BODY = "@body";
	private static final String		KEY_TAIL = "@tail";
	private static final String		KEY_COMMENT = "//";
	private static final String		CONTINUATION = "\\";

	private final Map<String,String>	variables = new HashMap<>();
	private final Rule[]				rulesParsed;
	private final Function<Map<String,String>, char[]>[]	beforeContent;
	private final Function<Map<String,String>, char[]>[]	afterContent;

	private static enum LexType {
		NAME,
		VALUE,
		OPEN,
		CLOSE,
		OR,
		AND,
		NOT,
		COMPARISON,
		EOF
	}

	private static enum DepthLevel {
		OR,
		AND,
		NOT,
		COMPARISON,
		TERM
	}
	
	private static enum Operation {
		LOADNAME,
		LOADSTRING,
		EXISTS,
		EQ,
		NE,
		GT, 
		GE,
		LT,
		LE,
		MATCHES,
		NOT,
		AND,
		OR
	}
	
	public RulesParser(final InputStream is) throws IOException, SyntaxException, IllegalArgumentException, NullPointerException {
		if (is == null) {
			throw new NullPointerException("Input stream can't be null");
		}
		else {
			final StringBuilder		before = new StringBuilder(); 
			final List<Rule>		rules = new ArrayList<>();
			final StringBuilder		after = new StringBuilder();
			int						startHead = 0, startTail = 0;

			variables.putAll(System.getenv());
			for(Entry<Object, Object> item : System.getProperties().entrySet()) {
				variables.put(item.getKey().toString(), item.getValue().toString());
			}
			variables.put("timestamp", new Date(System.currentTimeMillis()).toString());
			
			try(final Reader			rdr = new InputStreamReader(is, PureLibSettings.DEFAULT_CONTENT_ENCODING);
				final BufferedReader	brdr = new BufferedReader(rdr)) {
				final StringBuilder		sb = new StringBuilder();
				Parts	currentPart = Parts.BODY;
				String 	line;
				int		lineNo = 1;
			
				while ((line = brdr.readLine()) != null) {
					String	trimmed = line.trim();
					
					if (!trimmed.isEmpty() && !trimmed.startsWith(KEY_COMMENT)) {
						switch (trimmed.toLowerCase()) {
							case KEY_HEAD	:
								currentPart = Parts.HEAD;
								startHead = lineNo;
								break;
							case KEY_BODY	:
								currentPart = Parts.BODY;
								break;
							case KEY_TAIL	:
								currentPart = Parts.TAIL;
								startTail = lineNo;
								break;
							default :
								switch (currentPart) {
									case HEAD	:
										before.append(trimmed).append(System.lineSeparator());
										break;
									case BODY	:
										if (trimmed.endsWith(CONTINUATION)) {	// Continuation to the next line
											sb.append(trimmed, 0, trimmed.length() - 1).append(' ');
										}
										else {
											if (!sb.isEmpty()) {		// Multiline string is finished
												sb.append(trimmed);
											}
											trimmed = sb.toString();
											sb.setLength(0);
											rules.add(parseRule(lineNo, trimmed, variables));
										}
										break;
									case TAIL	:
										after.append(trimmed).append(System.lineSeparator());
										break;
									default:
										throw new UnsupportedOperationException("Part type ["+currentPart+"] is not supported yet"); 
								}
								break;
						}
						
					}
					lineNo++;
				}
			}
			this.beforeContent = buildSupplier(startHead, before.toString(), variables);
			this.afterContent = buildSupplier(startTail, after.toString(), variables);
			this.rulesParsed = rules.toArray(new Rule[rules.size()]);
		}
	}

	public Map<String,String> getVariables() {
		return variables;
	}
	
	public Function<Map<String,String>, char[]>[] getHeadContent() {
		return beforeContent;
	}

	public Rule[] getRules() {
		return rulesParsed;
	}
	
	public Function<Map<String,String>, char[]>[] getTailContent() {
		return afterContent;
	}

	static Rule parseRule(final int lineNo, final String content, final Map<String,String> variables) throws SyntaxException {
		final int 		seqIndex = content.indexOf("->");
		
		if (seqIndex < 0) {
			throw new SyntaxException(lineNo, 0, "Missing '->' in the rule"); 
		}
		else {
			final int					templateLine = lineNo, formatLine = lineNo + SyntaxException.toRow(content, seqIndex);
			final String				template = content.substring(0, seqIndex).trim(), format = content.substring(seqIndex + 2).trim();
			final Map<String, String>	varClone = new HashMap<String, String>(variables);
			
			final TriPredicate<String, Function<String,String>, Map<String, String>>[]	pred = parseTemplate(templateLine, template, varClone);
			final Function<Map<String,String>, char[]>[]	supp = buildPreprocessedSupplier(lineNo, format.replace("\\n", "\n"), varClone);
			
			return new Rule(pred, supp);
		}
	}
	
	static TriPredicate<String, Function<String,String>, Map<String, String>>[] parseTemplate(final int lineNo, final String template, final Map<String, String> variables) throws SyntaxException {
		final List<TriPredicate<String, Function<String,String>, Map<String, String>>>	result = new ArrayList<>();
		Set<String>	namesDefined = null;
		String theSameLastName = null;
		String theSameLastComment = "";
		boolean theSameLast = false;
		boolean asteriskDetected = false;
		
		for(String item : template.split("/")) {
			namesDefined = new HashSet<>();
			
			theSameLast = false;
			Matcher	m = NAME_AND_ATTRS.matcher(item);
			
			if (m.find()) {
				final String varName = m.group(1);
				final String tags = m.group(2);
				final TriPredicate<String, Function<String,String>, Map<String, String>>	tpA = Utils.checkEmptyOrNullString(tags) 
																									? (t,f,v)->true 
																									: parseAttributes(lineNo, tags, variables, namesDefined);

				if (variables.containsKey(varName)) {
					final String	tagName = variables.get(varName);
					
					result.add(
							new TriPredicateImpl(namesDefined, item) {
								@Override
								public boolean test(final String tag, final Function<String,String> func, final Map<String,String> vars) {
									return tag.equals(tagName) && tpA.test(tag, func, vars);
								}
							}
					);
				}
				else {
					namesDefined.add(varName);
					result.add(
							new TriPredicateImpl(namesDefined, item) {
								@Override
								public boolean test(final String tag, final Function<String,String> func, final Map<String,String> vars) {
									if (tpA.test(tag, func, vars)) {
										vars.put(varName, tag);
										return true;
									}
									else {
										return false;
									}
								}
							}
					);
				}
			}
			else {
				m = ATTRS.matcher(item);
				
				if (m.find()) {
					final String tagName = m.group(1);
					final String tags = m.group(2);
					final TriPredicate<String, Function<String,String>, Map<String, String>>	tpA = Utils.checkEmptyOrNullString(tags) 
																										? (t,f,v)->true 
																										: parseAttributes(lineNo, tags, variables, namesDefined);
					
					result.add(new TriPredicateImpl(namesDefined, item) {
							@Override
							public boolean test(final String tag, final Function<String, String> func, final Map<String, String> vars) {
								return tagName.equals(tag) && tpA.test(tag, func, vars);
							}
						}
					);
				}
				else {
					m = SUBST.matcher(item);
					
					if (m.find()) {
						final String varName = m.group(1);
						
						if (variables.containsKey(varName)) {
							final String	val = variables.get(varName);
							
							if (val != null) {
								result.add(new TriPredicateImpl(namesDefined, item) {
										@Override
										public boolean test(final String tag, final Function<String, String> func, final Map<String, String> vars) {
											return val.equals(tag);
										}
									}
								);
							}
							else {
								result.add(new TriPredicateImpl(namesDefined, item) {
										@Override
										public boolean test(final String tag, final Function<String, String> func, final Map<String, String> vars) {
											return Objects.equals(vars.get(varName),tag);
										}
									}
								);
							}
						}
						else {
							namesDefined.add(varName);
							result.add(
									new TriPredicateImpl(namesDefined, item) {
										@Override
										public boolean test(final String tag, final Function<String, String> func, final Map<String, String> vars) {
											return true;
										}
									}
							);
							theSameLastName = varName;
							theSameLastComment = item;
							theSameLast = true;
							asteriskDetected = item.endsWith("*");
						}
					}
					else {
						final String	tagName = item;
						
						result.add(new TriPredicateImpl(namesDefined, item) {
								@Override
								public boolean test(final String tag, final Function<String, String> func, final Map<String, String> vars) {
									return tagName.equals(tag);
								}
							}
						);
					}
				}
			}
		}
		if (theSameLast) {
			final boolean 	scr = asteriskDetected;
			final String	varName = theSameLastName;
			
			result.set(result.size() - 1, new TriPredicateImpl(new HashSet<>(Arrays.asList(varName)), theSameLastComment){
				@Override
				public boolean test(String t, Function<String, String> u, Map<String, String> v) {
					return true;
				}

				@Override
				public String[] getLocalVarDefinitions() {
					return NULL_NAMES;
				};
				
				@Override
				public boolean charContentRequired() {
					return !scr;
				};
				
				@Override
				public boolean subtreeContentRequired() {
					return scr;
				};
				
				@Override
				public String getContentVarName() {
					return varName;
				};
			});
			variables.put(varName, null);
		}
		return result.toArray(new TriPredicate[result.size()]);
	}

	static TriPredicate<String, Function<String, String>, Map<String, String>> parseAttributes(final int lineNo, final String attributes, final Map<String, String> variables, final Set<String> newNames) throws SyntaxException {
		final List<TriPredicate<String, Function<String, String>, Map<String, String>>>	temp = new ArrayList<>();
		
		for (String item : attributes.split(",")) {
			final String	name;
			final String	value;
			final boolean	rightIsString;
			final boolean	leftIsOptional;
			Matcher	m = ATTR_OPT.matcher(item);
			
			if (m.find()) {
				name = m.group(1);
				value = m.group(2);
				rightIsString = false;
				leftIsOptional = true;
			}
			else {
				m = ATTR.matcher(item);
				
				if (m.find()) {
					name = m.group(1);
					value = m.group(2);
					rightIsString = false;
					leftIsOptional = false;
				}
				else {
					m = ATTR_OPT_VALUE.matcher(item);
					
					if (m.find()) {
						name = m.group(1);
						value = m.group(2);
						rightIsString = true;
						leftIsOptional = true;
					}
					else {
						m = ATTR_VALUE.matcher(item);
						
						if (m.find()) {
							name = m.group(1);
							value = m.group(2);
							rightIsString = true;
							leftIsOptional = false;
						}
						else {
							throw new SyntaxException(lineNo, lineNo, "Illegal attr");
						}
					}
				}
			}
			if (rightIsString) {
				if (leftIsOptional) {
					temp.add((t, f, v)->{
						final String	val = f.apply(name);
						
						if (val != null) {
							return Objects.equals(val, value);
						}
						else {
							return true;
						}
					});
				}
				else {
					temp.add((t, f, v)->{
						return Objects.equals(f.apply(name), value);
					});
				}
			}
			else {
				if (variables.containsKey(value)) {
					if (leftIsOptional) {
						temp.add((t, f, v)->{
							final String	val = f.apply(name);
							
							if (val != null) {
								return Objects.equals(v.get(value), val);
							}
							else {
								return true;
							}
						});
					}
					else {
						temp.add((t, f, v)->{
							return Objects.equals(v.get(value), f.apply(name));
						});
					}
				}
				else {
					variables.put(value, null);
					newNames.add(value);
					
					if (leftIsOptional) {
						temp.add((t, f, v)->{
							v.put(value, f.apply(name));
							return true;
						});
					}
					else {
						temp.add((t, f, v)->{
							final String	val = f.apply(name);
							
							if (val == null) {
								return false;
							}
							else {
								v.put(value, val);
								return true;
							}
						});
					}
				}
			}
		}
		final TriPredicate<String, Function<String, String>, Map<String, String>>[]	result = temp.toArray(new TriPredicate[temp.size()]);
		
		return (t, f, v) -> {
			final Map<String, String>	vTemp = new HashMap<>(v);
			
			for(TriPredicate<String, Function<String, String>, Map<String, String>> item : result) {
				if (!item.test(t, f, vTemp)) {
					return false;
				}
			}
			v.putAll(vTemp);
			return true;
		};
	}

	static Function<Map<String,String>, char[]>[] buildPreprocessedSupplier(final int lineNo, final String format, final Map<String,String> variables) throws SyntaxException, IllegalArgumentException, NullPointerException {
		final Matcher m = PREPROC.matcher(format);
		final List<IfPreprocessor> stack = new ArrayList<>();
		List<Function<Map<String,String>, char[]>> current;
		long nesting = 1L;	// up to 64 .if depth level!
		int from = 0, depth = 0;
		
		stack.add(0, new IfPreprocessor((t)->true));
		current = stack.get(0).trueBranch;
		while (m.find(from)) {
			if (".if ".equals(m.group(1))) {
				final Predicate<Map<String,String>>	pred = buildPredicate(lineNo, m.group(2), variables);
				
				current.addAll(Arrays.asList(buildSupplier(lineNo, format.substring(from, m.start()), variables)));
				stack.add(0, new IfPreprocessor(pred));
				current = stack.get(0).trueBranch;
				depth++;
				nesting |= 1L << depth;
			}
			else if (".else".equals(m.group(3))) {
				current.addAll(Arrays.asList(buildSupplier(lineNo, format.substring(from, m.start()), variables)));
				current = stack.get(0).falseBranch;
				nesting &= ~(1L << depth);
			}
			else if (".endif".equals(m.group(5))) {
				if (depth <= 0) {
					throw new SyntaxException(lineNo, 0, "Nesting error"); 
				}
				else {
					current.addAll(Arrays.asList(buildSupplier(lineNo, format.substring(from, m.start()), variables)));
					final IfPreprocessor temp = stack.remove(0);
					
					nesting &= ~(1L << depth);
					depth--;
					if ((nesting & (1L << depth)) != 0) {
						current = stack.get(0).trueBranch;
					}
					else {
						current = stack.get(0).falseBranch;
					}
					current.add(temp);
				}
			}
			from = m.end() + 1;
		}
		current.addAll(Arrays.asList(buildSupplier(lineNo, format.substring(from), variables)));
		return current.toArray(new Function[current.size()]);
	}
	
	static Predicate<Map<String,String>> buildPredicate(final int lineNo, final String predicate, final Map<String,String> variables) throws SyntaxException {
		final char[] 		pred = CharUtils.terminateAndConvert2CharArray(predicate, '\uFFFF');
		final int[]			bounds = new int[2];
		final List<Lexema>	lexemas = new ArrayList<>();
		final StringBuilder	sb = new StringBuilder();
		int from = 0;
		
loop:	for(;;) {
			from = CharUtils.skipBlank(pred, from, true);
			switch (pred[from]) {
				case '\uFFFF' :
					lexemas.add(new Lexema(lineNo, from, LexType.EOF));
					from++;
					break loop;
				case '(' :
					lexemas.add(new Lexema(lineNo, from, LexType.OPEN));
					from++;
					break;
				case ')' :
					lexemas.add(new Lexema(lineNo, from, LexType.CLOSE));
					from++;
					break;
				case '|' :
					lexemas.add(new Lexema(lineNo, from, LexType.OR));
					from++;
					break;
				case '&' :
					lexemas.add(new Lexema(lineNo, from, LexType.AND));
					from++;
					break;
				case '~' :
					lexemas.add(new Lexema(lineNo, from, LexType.NOT));
					from++;
					break;
				case '=' :
					if (pred[from+1] == '*') {
						lexemas.add(new Lexema(lineNo, from, LexType.COMPARISON, "=*"));
						from += 2;
					}
					else {
						lexemas.add(new Lexema(lineNo, from, LexType.COMPARISON, "="));
						from++;
					}
					break;
				case '?' :
					lexemas.add(new Lexema(lineNo, from, LexType.COMPARISON, "?"));
					from++;
					break;
				case '>' :
					if (pred[from+1] == '=') {
						lexemas.add(new Lexema(lineNo, from, LexType.COMPARISON, ">="));
						from += 2;
					}
					else {
						lexemas.add(new Lexema(lineNo, from, LexType.COMPARISON, ">"));
						from++;
					}
					break;
				case '<' :
					if (pred[from+1] == '=') {
						lexemas.add(new Lexema(lineNo, from, LexType.COMPARISON, "<="));
						from += 2;
					}
					else if (pred[from+1] == '>') {
						lexemas.add(new Lexema(lineNo, from, LexType.COMPARISON, "<>"));
						from += 2;
					}
					else {
						lexemas.add(new Lexema(lineNo, from, LexType.COMPARISON, "<"));
						from++;
					}
					break;
				case '\"' :
					from = CharUtils.parseStringExtended(pred, from + 1, '\"', sb);
					if (pred[from - 1] == '\"') {
						lexemas.add(new Lexema(lineNo, from, LexType.VALUE, sb.toString()));
						sb.setLength(0);
					}
					else {
						throw new SyntaxException(lineNo, from, "Unclosed string constant");
					}
					break;
				default :
					if (Character.isJavaIdentifierStart(pred[from])) {
						final int	start = from;
						
						from = CharUtils.parseName(pred, from, bounds);
						final String	name = new String(pred, start, from - start);
						
						if (variables.containsKey(name)) {
							lexemas.add(new Lexema(lineNo, from, LexType.NAME, name));
						}
						else {
							throw new SyntaxException(lineNo, from, "Undefined name ["+name+"]");
						}
					}
					else {
						throw new SyntaxException(lineNo, from, "Unknown lexema");
					}
			}
		}
		final SyntaxNode<Operation,SyntaxNode<?,?>>	root = new SyntaxNode(0, 0, Operation.OR, 0, sb);
		final int	atEOF = buildPredicate(DepthLevel.OR, lexemas, 0,  variables, root);
		
		if (lexemas.get(atEOF).type != LexType.EOF) {
			throw new SyntaxException(lineNo, atEOF, "Unparsed tail");
		}
		return (t)->calculate(t, root);
	}
	
	private static int buildPredicate(final DepthLevel level, final List<Lexema> lexemas, int from, final Map<String, String> variables, final SyntaxNode<Operation,SyntaxNode<?,?>> root) throws SyntaxException {
		switch (level) {
			case OR			:
				from = buildPredicate(DepthLevel.AND, lexemas, from, variables, root);
				if (lexemas.get(from).type == LexType.OR) {
					final List<SyntaxNode<Operation,SyntaxNode<?,?>>>	temp = new ArrayList<>(Arrays.asList((SyntaxNode<Operation,SyntaxNode<?,?>>)root.clone()));
					
					do {
						final SyntaxNode<Operation,SyntaxNode<?,?>>		node = (SyntaxNode<Operation, SyntaxNode<?, ?>>) root.clone();
						
						from = buildPredicate(DepthLevel.AND, lexemas, from + 1, variables, node);
						temp.add(node);
					} while (lexemas.get(from).type == LexType.OR);
					root.type = Operation.OR;
					root.children = temp.toArray(new SyntaxNode[temp.size()]);
				}
				break;
			case AND		:
				from = buildPredicate(DepthLevel.NOT, lexemas, from, variables, root);
				if (lexemas.get(from).type == LexType.AND) {
					final List<SyntaxNode<Operation,SyntaxNode<?,?>>>	temp = new ArrayList<>(Arrays.asList((SyntaxNode<Operation,SyntaxNode<?,?>>)root.clone()));
					
					do {
						final SyntaxNode<Operation,SyntaxNode<?,?>>		node = (SyntaxNode<Operation, SyntaxNode<?, ?>>) root.clone();
						
						from = buildPredicate(DepthLevel.NOT, lexemas, from + 1, variables, node);
						temp.add(node);
					} while (lexemas.get(from).type == LexType.AND);
					root.type = Operation.AND;
					root.children = temp.toArray(new SyntaxNode[temp.size()]);
				}
				break;
			case NOT		:
				if (lexemas.get(from).type == LexType.NOT) {
					final SyntaxNode<Operation,SyntaxNode<?,?>>		node = (SyntaxNode<Operation, SyntaxNode<?, ?>>) root.clone();
					
					from = buildPredicate(DepthLevel.COMPARISON, lexemas, from + 1, variables, node);
					root.type = Operation.NOT;
					root.children = new SyntaxNode[]{node};
				}
				else {
					from = buildPredicate(DepthLevel.COMPARISON, lexemas, from, variables, root);
				}
				break;
			case COMPARISON	:
				from = buildPredicate(DepthLevel.TERM, lexemas, from, variables, root);
				if (lexemas.get(from).type == LexType.COMPARISON) {
					final SyntaxNode<Operation,SyntaxNode<?,?>>		node = (SyntaxNode<Operation, SyntaxNode<?, ?>>) root.clone();
					final SyntaxNode<Operation,SyntaxNode<?,?>>		right = (SyntaxNode<Operation, SyntaxNode<?, ?>>) root.clone();
					
					switch (lexemas.get(from).value) {
						case "="	:
							from = buildPredicate(DepthLevel.TERM, lexemas, from + 1, variables, right);
							root.type = Operation.EQ;
							root.children = new SyntaxNode[] {node, right};
							break;
						case ">="	:
							from = buildPredicate(DepthLevel.TERM, lexemas, from + 1, variables, right);
							root.type = Operation.GE;
							root.children = new SyntaxNode[] {node, right};
							break;
						case "<="	:
							from = buildPredicate(DepthLevel.TERM, lexemas, from + 1, variables, right);
							root.type = Operation.LE;
							root.children = new SyntaxNode[] {node, right};
							break;
						case "<>"	:
							from = buildPredicate(DepthLevel.TERM, lexemas, from + 1, variables, right);
							root.type = Operation.NE;
							root.children = new SyntaxNode[] {node, right};
							break;
						case ">"	:
							from = buildPredicate(DepthLevel.TERM, lexemas, from + 1, variables, right);
							root.type = Operation.GT;
							root.children = new SyntaxNode[] {node, right};
							break;
						case "<"	:
							from = buildPredicate(DepthLevel.TERM, lexemas, from + 1, variables, right);
							root.type = Operation.LT;
							root.children = new SyntaxNode[] {node, right};
							break;
						case "=*"	:
							from = buildPredicate(DepthLevel.TERM, lexemas, from + 1, variables, right);
							root.type = Operation.MATCHES;
							root.children = new SyntaxNode[] {node, right};
							break;
						case "?"	:
							root.type = Operation.EXISTS;
							root.children = new SyntaxNode[] {node};
							from++;
							break;
						default : 
							throw new UnsupportedOperationException("Comparison operator ["+lexemas.get(from).value+"] is not supported yet");
					}
				}
				else {
					throw new SyntaxException(0, 0, "Comparison operator is missing");
				}
				break;
			case TERM		:
				switch (lexemas.get(from).type) {
					case NAME	:
						root.type = Operation.LOADNAME;
						root.cargo = lexemas.get(from).value;
						break;
					case OPEN	:
						from = buildPredicate(DepthLevel.OR, lexemas, from + 1, variables, root);
						if (lexemas.get(from).type != LexType.CLOSE) {
							throw new SyntaxException(0, 0, "Missing ')'");
						}
						break;
					case VALUE	:
						root.type = Operation.LOADSTRING;
						root.cargo = lexemas.get(from).value; 
						break;
					default:
						throw new SyntaxException(0, 0, "Missing term");
				}
				from++;
				break;
			default:
				throw new UnsupportedOperationException("Depth level ["+level+"] is not supported yet");
		}
		return from;
	}

	private static boolean calculate(final Map<String, String> variables, final SyntaxNode<Operation, SyntaxNode<?, ?>> root) {
		return toBoolean(calculateInternal(variables, root));
	}

	private static Object calculateInternal(final Map<String, String> variables, final SyntaxNode<Operation, SyntaxNode<?, ?>> root) {
		switch (root.type) {
			case OR			:
				for(SyntaxNode<?, ?> item : root.children) {
					if (toBoolean(calculateInternal(variables, (SyntaxNode<Operation, SyntaxNode<?, ?>>)item))) {
						return true;
					}
				}
				return false;
			case AND		:
				for(SyntaxNode<?, ?> item : root.children) {
					if (!toBoolean(calculateInternal(variables, (SyntaxNode<Operation, SyntaxNode<?, ?>>)item))) {
						return false;
					}
				}
				return true;
			case NOT		:
				return !toBoolean(calculateInternal(variables, (SyntaxNode<Operation, SyntaxNode<?, ?>>) root.children[0]));
			case EQ			:
				return compare(calculateInternal(variables, (SyntaxNode<Operation, SyntaxNode<?, ?>>) root.children[0]), calculateInternal(variables, (SyntaxNode<Operation, SyntaxNode<?, ?>>) root.children[1])) == 0;
			case EXISTS		:
				return calculateInternal(variables, (SyntaxNode<Operation, SyntaxNode<?, ?>>) root.children[0]) != null;
			case GE			:
				return compare(calculateInternal(variables, (SyntaxNode<Operation, SyntaxNode<?, ?>>) root.children[0]), calculateInternal(variables, (SyntaxNode<Operation, SyntaxNode<?, ?>>) root.children[1])) >= 0;
			case GT			:
				return compare(calculateInternal(variables, (SyntaxNode<Operation, SyntaxNode<?, ?>>) root.children[0]), calculateInternal(variables, (SyntaxNode<Operation, SyntaxNode<?, ?>>) root.children[1])) > 0;
			case LE			:
				return compare(calculateInternal(variables, (SyntaxNode<Operation, SyntaxNode<?, ?>>) root.children[0]), calculateInternal(variables, (SyntaxNode<Operation, SyntaxNode<?, ?>>) root.children[1])) <= 0;
			case LT			:
				return compare(calculateInternal(variables, (SyntaxNode<Operation, SyntaxNode<?, ?>>) root.children[0]), calculateInternal(variables, (SyntaxNode<Operation, SyntaxNode<?, ?>>) root.children[1])) < 0;
			case NE			:
				return compare(calculateInternal(variables, (SyntaxNode<Operation, SyntaxNode<?, ?>>) root.children[0]), calculateInternal(variables, (SyntaxNode<Operation, SyntaxNode<?, ?>>) root.children[1])) != 0;
			case MATCHES	:
				final Object	left = calculateInternal(variables, (SyntaxNode<Operation, SyntaxNode<?, ?>>) root.children[0]);
				final Object	right = calculateInternal(variables, (SyntaxNode<Operation, SyntaxNode<?, ?>>) root.children[1]);
				
				if (left != null && right != null) {
					return left.toString().matches(right.toString());
				}
				else {
					return false;
				}
			case LOADNAME	:
				if (variables.containsKey(root.cargo)) {
					return variables.get(root.cargo);
				}
				else {
					return null;
				}
			case LOADSTRING	:
				return root.cargo;
			default :
				throw new UnsupportedOperationException("Operation type ["+root.type+"] is not supported yet");
		}
	}
	
	private static Boolean toBoolean(final Object value) {
		return (value instanceof Boolean) ? (Boolean)value : false;
	}

	private static int compare(final Object left, final Object right) {
		if (left != null && right != null) {
			return MathUtils.signum(left.toString().compareTo(right.toString()));
		}
		else {
			return -2;
		}
	}
	
	static Function<Map<String,String>, char[]>[] buildSupplier(final int lineNo, final String format, final Map<String,String> variables) throws SyntaxException, IllegalArgumentException, NullPointerException {
		final List<Function<Map<String,String>, char[]>>	result = new ArrayList<>();
		final Matcher	m = SUBST.matcher(format);
		int	from = 0;
		
		while (m.find()) {
			final int		to = m.start();
			final char[] 	val = format.substring(from, to).toCharArray();
			final String	varName = m.group(1);

			result.add((vars)->val);
			if (!variables.containsKey(varName)) {
				throw new SyntaxException(lineNo + SyntaxException.toRow(format, to), SyntaxException.toCol(format, to), "Unknown substitution name ["+varName+"]"); 
			}
			else {
				result.add((vars)->vars.get(varName).toCharArray());
			}
			from = m.end();
		}
		final char[] 	tail = format.substring(from).toCharArray();
		
		result.add((vars)->tail);
		return result.toArray(new Function[result.size()]);
	}

	private abstract static class TriPredicateImpl implements TriPredicate<String, Function<String,String>, Map<String, String>> {
		private final String[]	names; 
		private final String	comment;
		
		private TriPredicateImpl(final Set<String> localNames, final String comment) {
			this.names = localNames.toArray(new String[localNames.size()]);
			this.comment = comment;
		}

		@Override public abstract boolean test(final String tag, final Function<String, String> func, final Map<String, String> vars);
		
		@Override
	    public String[] getLocalVarDefinitions() {
	    	return names;
	    }

		@Override
		public String toString() {
			return "TriPredicateImpl [names=" + Arrays.toString(names) + ", comment=" + comment + "]";
		}
	}
	
	private static class Lexema {
		final int 		row, col;
		final LexType	type;
		final String	value;

		public Lexema(final int row, final int col, final LexType type) {
			this(row, col, type, "");
		}
		
		public Lexema(final int row, final int col, final LexType type, final String value) {
			this.row = row;
			this.col = col;
			this.type = type;
			this.value = value;
		}

		@Override
		public String toString() {
			return "Lexema [row=" + row + ", col=" + col + ", type=" + type + ", value=" + value + "]";
		}
	}
	
	private static class IfPreprocessor implements Function<Map<String,String>, char[]>{
		Predicate<Map<String,String>>				cond;
		List<Function<Map<String,String>, char[]>>	trueBranch = new ArrayList<>();
		List<Function<Map<String,String>, char[]>> 	falseBranch = new ArrayList<>();
		
		public IfPreprocessor(final Predicate<Map<String, String>> cond) {
			this.cond = cond;
		}

		@Override
		public char[] apply(final Map<String, String> t) {
			final GrowableCharArray<?>	gca = new GrowableCharArray<>(false);
			
			if (cond.test(t)) {
				for(Function<Map<String, String>, char[]> item : trueBranch) {
					gca.append(item.apply(t));
				}
			}
			else {
				for(Function<Map<String, String>, char[]> item : falseBranch) {
					gca.append(item.apply(t));
				}
			}
			return gca.extract();
		}
	}
}
