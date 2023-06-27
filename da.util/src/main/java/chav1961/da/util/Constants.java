package chav1961.da.util;

/**
 * <p>This class contains well-known constants to use them in the Data Acquisition package</p> 
 * @author Alexander Chernomyrdin aka chav1961
 * @since 0.0.1
 */
public class Constants {
	/**
	 * <p>Command line: turn on debug trace</p> 
	 */
	public static final String		ARG_DEBUG = "d";
	
	/**
	 * <p>Command line: process ZIP parts to the given name mask</p>
	 */
	public static final String		ARG_PROCESS = "process";  
	
	/**
	 * <p>Command line: skip ZIP parts to the given name mask without processing</p>
	 */
	public static final String		ARG_PASS = "pass";
	
	/**
	 * <p>Command line: rename processed/skipped ZIP parts with the given template.</p>
	 * @see DAUtils#parseRenameArgument(String)
	 */
	public static final String		ARG_RENAME = "rename";
	
	/**
	 * <p>Command line: remove processed/skipped ZIP parts from ZIP parts.</p>
	 */
	public static final String		ARG_REMOVE = "remove";  
	
	/**
	 * <p>Data Acquisition pipe content: Name of the same first part of the ZIP stream.</p>
	 */
	public static final String		PART_TICKET = "ticket.txt";
	
	/**
	 * <p>Data Acquisition pipe content: Name of the same last part of the ZIP stream.</p>
	 */
	public static final String		PART_LOG = "log.txt";
	
	/**
	 * <p>Data Acquisition pipe content: Name of the content type key in the same first part of the ZIP stream.</p>
	 * @see #PART_TICKET
	 */
	public static final String		PART_KEY_CONTENT_TYPE = "contentType"; 

	/**
	 * <p>Data Acquisition pipe content: Base URI for entities.</p>
	 * @see #PART_TICKET
	 */
	public static final String		PART_KEY_BASE_URI = "baseUri"; 
	
	/**
	 * <p>Pattern for part name mask, that always returns false</p> 
	 */
	public static final String[]	MASK_NONE = new String[0];
	
	/**
	 * <p>Pattern for part name mask, that always returns true</p> 
	 */
	public static final String[]	MASK_ANY = {".+"};
}
