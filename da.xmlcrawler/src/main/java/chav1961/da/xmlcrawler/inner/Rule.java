package chav1961.da.xmlcrawler.inner;

import java.util.Map;
import java.util.function.Function;

public class Rule {
	public final TriPredicate<String, Function<String, String>, Map<String, String>>[] predicate;
	public final Function<Map<String, String>, char[]>[]	format;

	public Rule(final TriPredicate<String, Function<String, String>, Map<String, String>>[] predicate, final Function<Map<String, String>, char[]>[] format) {
		this.predicate = predicate;
		this.format = format;
	}
}
