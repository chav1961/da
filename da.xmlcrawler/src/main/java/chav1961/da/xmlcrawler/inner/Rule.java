package chav1961.da.xmlcrawler.inner;

import java.util.function.Supplier;

import org.w3c.dom.Attr;

public class Rule {
	public final TriPredicate<String, Attr, Object>[] predicate;
	public final Supplier<char[]>[] format;

	public Rule(final TriPredicate<String, Attr, Object>[] predicate, final Supplier<char[]>[] format) {
		this.predicate = predicate;
		this.format = format;
	}
}
