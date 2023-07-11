package chav1961.da.xmlcrawler.inner;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class RuleExecutor implements TriPredicate<String, Function<String, String>, Map<String, String>> {
	final TriPredicate<String, Function<String, String>, Map<String, String>>[]	template;
	final Function<Map<String, String>, char[]>[]	format;
	final Map<String,String> vars = new HashMap<>();
	final StringBuilder sb = new StringBuilder();
	long	mask = 1L;
	int depth = 0;

	public RuleExecutor(final TriPredicate<String, Function<String, String>, Map<String, String>>[] template, final Function<Map<String, String>, char[]>[] format, final Map<String, String> vars) {
		this.template = template;
		this.format = format;
		this.vars.putAll(vars);
	}

	@Override
	public boolean test(final String tag, final Function<String, String> attrs, final Map<String, String> vars) {
		if (template[depth].test(tag, attrs, vars)) {
			depth++;
			mask |= (1L << depth);
			return true;
		}
		else {
			return false;
		}
	}
	
	public boolean canServe() {
		return (mask & (1L << depth)) != 0;
	}

	public boolean collectingRequired() {
		return template[depth].charContentRequired() || template[depth].subtreeContentRequired();
	}

	public void setVar(final String name, final String value) {
		vars.putIfAbsent(name, value);
	}
	
	public void pop() {
		if (depth < 0) {
			throw new IllegalStateException("Stack exhausted");
		}
		else {
			mask &= ~(1L << depth);
			depth--;
			for(String item : template[depth].getLocalVarDefinitions()) {
				vars.remove(item);
			}
		}
	}
	
	public Function<Map<String, String>, char[]>[] getFormat() {
		return format;
	}
}