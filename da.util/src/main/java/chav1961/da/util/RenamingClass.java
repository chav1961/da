package chav1961.da.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import chav1961.da.util.interfaces.RenamingInterface;
import chav1961.purelib.basic.CharUtils;
import chav1961.purelib.basic.exceptions.SyntaxException;

public class RenamingClass implements RenamingInterface {
	private final String	expression;
	private final Pair[]	pairs;
	
	public RenamingClass(final String expression) throws IllegalArgumentException, SyntaxException {
		if (expression == null || expression.isEmpty()) {
			throw new IllegalArgumentException("Expression can't be null or empty");
		}
		else {
			final char[]		content = CharUtils.terminateAndConvert2CharArray(expression, '\n');
			final List<Pair>	list = new ArrayList<>();
			int					from = 0, start = 0;
			String				left = null, right = null;
			
			while (content[from] != '\n') {
				if (content[from] == '-' && content[from+1] == '>') {
					if (left != null) {
						throw new SyntaxException(0, from, "Missing ';' before '->'");
					}
					else {
						left = new String(content, start, from-start);
						checkPattern(left, start);
						start = from += 2;
					}
				}
				else if (content[from] == ';' && (from == 0 || content[from-1] != '\\')) {
					if (left == null) {
						throw new SyntaxException(0, from, "Missing '->'");
					}
					else {
						right = new String(content, start, from-start);
						list.add(new Pair(Pattern.compile(left), right));
						left = right = null;
						start = ++from;
					}
				}
				else {
					from++;
				}
			}
			if (left != null) {
				right = new String(content, start, from-start);
				list.add(new Pair(Pattern.compile(left), right));
			}
			this.expression = expression;
			this.pairs = list.toArray(new Pair[list.size()]);
		}
	}

	@Override
	public String renameEntry(final String partName) {
		if (partName == null || partName.isEmpty()) {
			return partName;
		}
		else {
			for (Pair p : pairs) {
				final Matcher	m = p.pattern.matcher(partName);
				
				if (m.matches()) {
					return m.replaceAll(p.replacement);
				}
			}
			return partName;
		}
	}

	private void checkPattern(final String pattern, final int start) throws SyntaxException {
		try{Pattern.compile(pattern);			
		} catch (PatternSyntaxException exc) {
			throw new SyntaxException(0, start + exc.getIndex(), exc.getDescription());
		}
	}
	
	private static class Pair {
		final Pattern	pattern;
		final String	replacement;
		
		private Pair(final Pattern pattern, final String replacement) {
			this.pattern = pattern;
			this.replacement = replacement;
		}
	}

	@Override
	public String toString() {
		return "RenamingImpl [expression=" + expression + "]";
	}
}
