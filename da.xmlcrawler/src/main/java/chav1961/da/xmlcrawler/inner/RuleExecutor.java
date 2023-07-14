package chav1961.da.xmlcrawler.inner;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class RuleExecutor {
	private final TriPredicate<String, Function<String, String>, Map<String, String>>[]	template;
	private final Function<Map<String, String>, char[]>[]	format;
	private final Map<String,String> 	vars = new HashMap<>();
	private long	mask = 1L;
	private int 	depth = 0;

	public RuleExecutor(final TriPredicate<String, Function<String, String>, Map<String, String>>[] template, final Function<Map<String, String>, char[]>[] format, final Map<String, String> vars) {
		this.template = template;
		this.format = format;
		this.vars.putAll(vars);
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
		return (mask & (1L << depth)) != 0;
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
			for(String item : template[depth].getLocalVarDefinitions()) {
				vars.remove(item);
			}
			depth--;
		}
	}
	
	public boolean charContentRequired() {
		if (canServe()) {
			return template[depth].charContentRequired();
		}
		else {
			return false;
		}
	}
	
	public String getContentVarName() {
		if (canServe()) {
			return template[depth].getContentVarName();
		}
		else {
			throw new IllegalStateException("Content var name is not available in the current state");
		}
	}
}