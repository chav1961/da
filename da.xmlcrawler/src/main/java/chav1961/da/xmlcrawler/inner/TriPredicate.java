package chav1961.da.xmlcrawler.inner;

@FunctionalInterface
public interface TriPredicate<T, U, V> {
	String[] NULL_NAMES = new String[0];
	
    boolean test(T t, U u, V v);

    default String[] getLocalVarDefinitions() {
    	return NULL_NAMES;
    }
    
    default boolean charContentRequired() {
    	return false;
    }

    default boolean subtreeContentRequired() {
    	return false;
    }
    
    default String getContentVarName() {
    	return null;
    }
}