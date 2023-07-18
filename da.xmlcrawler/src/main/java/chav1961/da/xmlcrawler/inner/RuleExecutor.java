package chav1961.da.xmlcrawler.inner;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class RuleExecutor {
	private static final long[]	MASKS = new long[64];
	
	static {
		long mask = 0;
		
		for(int index = 0; index < MASKS.length; index++) {
			MASKS[index] = mask |= (1L << index);
		}
	}
	
	private final TriPredicate<String, Function<String, String>, Map<String, String>>[]	template;
	private final Function<Map<String, String>, char[]>[]	format;
	private final Map<String,String> 	vars = new HashMap<>();
	private long	mask = 1L;
	private int 	depth = 0;

	public RuleExecutor(final Rule rule, final Map<String, String> vars) {
		if (rule == null) {
			throw new NullPointerException("Rule can't be null");
		}
		else if (vars == null) {
			throw new NullPointerException("Vars can't be null");
		}
		else {
			this.template = rule.predicate;
			this.format = rule.format;
			this.vars.putAll(vars);
		}
	}
	
	public RuleExecutor(final TriPredicate<String, Function<String, String>, Map<String, String>>[] template, final Function<Map<String, String>, char[]>[] format, final Map<String, String> vars) {
		if (template == null) {
			throw new NullPointerException("Template can't be null");
		}
		else if (format == null) {
			throw new NullPointerException("Format can't be null");
		}
		else if (vars == null) {
			throw new NullPointerException("Vars can't be null");
		}
		else {
			this.template = template;
			this.format = format;
			this.vars.putAll(vars);
		}
	}

	public void push(final String tag, final Function<String, String> attrs) {
		if (depth < template.length && template[depth].test(tag, attrs, vars)) {
			depth++;
			mask |= (1L << depth);
		}
		else {
			depth++;
			mask &= ~(1L << depth);
		}
	}
	
	public boolean canServe() {
		return depth == template.length - 1 && (mask & MASKS[depth]) == MASKS[depth];
	}

	public boolean collectingRequired() {
		if (canServe()) {
			return template[depth].charContentRequired() || template[depth].subtreeContentRequired();
		}
		else {
			return false;
		}
	}
	
	public void setVar(final String name, final String value) {
		vars.putIfAbsent(name, value);
	}
	
	public void print(final Writer wr) throws IOException {
		final StringBuilder	sb = new StringBuilder();
		
		for(Function<Map<String, String>, char[]> item : format) {
			sb.append(item.apply(vars));
		}
		wr.write(sb.toString());
	}
	
	public void pop() {
		if (depth < 0) {
			throw new IllegalStateException("Stack exhausted");
		}
		else {
			mask &= ~(1L << depth);
			if (depth < template.length) {
				for(String item : template[depth].getLocalVarDefinitions()) {
					vars.remove(item);
				}
			}
			depth--;
		}
	}
	
	public boolean charContentRequired() {
		if (canServe() && depth < template.length) {
			return template[depth].charContentRequired();
		}
		else {
			return false;
		}
	}
	
	public String getContentVarName() {
		if (canServe() && depth < template.length) {
			return template[depth].getContentVarName();
		}
		else {
			throw new IllegalStateException("Content var name is not available in the current state");
		}
	}
}