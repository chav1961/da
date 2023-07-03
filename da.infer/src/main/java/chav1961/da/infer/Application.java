package chav1961.da.infer;


import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.update.UpdateAction;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.vocabulary.ReasonerVocabulary;

import chav1961.da.util.AbstractZipProcessor;
import chav1961.da.util.Constants;
import chav1961.da.util.DAUtils;
import chav1961.da.util.interfaces.ContentFormat;
import chav1961.da.util.interfaces.OntologyType;
import chav1961.purelib.basic.ArgParser;
import chav1961.purelib.basic.PureLibSettings;
import chav1961.purelib.basic.SubstitutableProperties;
import chav1961.purelib.basic.Utils;
import chav1961.purelib.basic.exceptions.CommandLineParametersException;
import chav1961.purelib.basic.exceptions.SyntaxException;
import chav1961.purelib.basic.interfaces.LoggerFacade;

// https://jena.apache.org/documentation/inference/
// https://github.com/apache/jena/blob/main/jena-examples/src/main/java/arq/examples/riot/ExRIOT4_StreamRDF_Filter.java

public class Application extends AbstractZipProcessor {
	public static final String	ARG_ONTOLOGY_TYPE = "ontologyType";
	public static final String	ARG_ONTOLOGY = "ontology";
	public static final String	ARG_CUSTOM_RULES = "customRules";

	private final Model			schema = prepareDatabase();
	private final String		ontology;
	private final String[]		customRules;
	private final PrintStream	err;
	private final boolean 		debug;
	private final Reasoner		reasoner;
	private ContentFormat		format = ContentFormat.NTRIPLES;
	private String				baseURI = "";
	
	protected Application(final String[] processMask, final String[] passMask, final String[] removeMask, final String[][] renameMask,
						final OntologyType ontologyType, final String ontology, final String[] customRules, final PrintStream err, final boolean debug) throws SyntaxException {
		super(processMask, passMask, removeMask, renameMask);
		this.ontology = ontology;
		this.customRules = customRules;
		this.err = err;
		this.debug = debug;
		switch (ontologyType) {
			case NONE:
				this.reasoner = ReasonerRegistry.getRDFSSimpleReasoner();
				break;
			case OWL:
				this.reasoner = ReasonerRegistry.getOWLReasoner();
				break;
			case RDF:
				this.reasoner = ReasonerRegistry.getRDFSReasoner();
				this.reasoner.setParameter(ReasonerVocabulary.PROPsetRDFSLevel, ReasonerVocabulary.RDFS_FULL);
				break;
			default:
				throw new UnsupportedOperationException("Ontology format ["+ontologyType+"] is not supported yet"); 
		}
	}

	@Override
	protected void processTicket(final SubstitutableProperties props, final LoggerFacade logger) throws IOException {
		format = props.getProperty(Constants.PART_KEY_CONTENT_TYPE, ContentFormat.class);
		if (props.containsKey(Constants.PART_KEY_BASE_URI)) {
			baseURI = props.getProperty(Constants.PART_KEY_BASE_URI, String.class);
		}
		try(final InputStream	is = new ByteArrayInputStream(ontology.getBytes(PureLibSettings.DEFAULT_CONTENT_ENCODING))) {
			downloadContent(is, format, schema);
		}
		super.processTicket(props, logger);
	}
	
	@Override
	protected void processPart(final String part, final SubstitutableProperties props, final LoggerFacade logger, final InputStream source, final OutputStream target) throws IOException {
		final Model	model = prepareDatabase(); 
		final long	startTime = System.currentTimeMillis();
		
		try{
			if (debug) {
				message(err, "Download [%1$s] ...", part);
			}
			downloadContent(source, format, model);

			if (debug) {
				message(err, "Starting inference on [%1$s] ...", part);
			}
			final Model infModel = ModelFactory.createInfModel(reasoner, schema, model);
			
			try{
				if (customRules.length > 0) {
					if (debug) {
						message(err, "Processing custom rules on [%1$s]...", part);
					}
					processCustomRules(infModel, customRules);
				}
				if (debug) {
					message(err, "Upload [%1$s]...", part);
				}
				uploadContent(infModel, target, format);
				if (debug) {
					message(err, "Processing [%1$s] completed, duration = %2$d msec", part, System.currentTimeMillis() - startTime);
				}
			} finally {
				unprepareDatabase(infModel);
			}
		} finally {
			unprepareDatabase(model);
		}
	}
	
	private Model prepareDatabase() {
		 return ModelFactory.createDefaultModel();
	}

	private void downloadContent(final InputStream source, final ContentFormat format, final Model model) throws IOException {
		switch (format) {
			case JSONLD:
				model.read(source, baseURI, format.getNativeName());
				break;
			case N3:
				model.read(source, baseURI, format.getNativeName());
				break;
			case NQUADS:
				model.read(source, baseURI, format.getNativeName());
				break;
			case NTRIPLES:
				model.read(source, baseURI, format.getNativeName());
				break;
			case RDFXML:
				model.read(source, baseURI, format.getNativeName());
				break;
			case TRIG:
				model.read(source, baseURI, format.getNativeName());
				break;
			case TRIX:
				model.read(source, baseURI, format.getNativeName());
				break;
			case TURTLE:
				model.read(source, baseURI, format.getNativeName());
				break;
			case RDFA: case BINARY:
			default:
				throw new UnsupportedOperationException("Content format ["+format+"] is not supported yet"); 
		}
	}

	private void processCustomRules(final Model model, final String[] customRules) throws IOException {
		final File	temp = File.createTempFile("tmp", ".ntriples");
		
		try{
			model.begin();
			try(final OutputStream	os = new FileOutputStream(temp)) {
				for (String rule : customRules) {
					if (rule.toUpperCase().contains("CONSTRUCT ")) {
						final Query query = QueryFactory.create(rule) ;
						final QueryExecution qexec = QueryExecutionFactory.create(query, model);
						final Model resultModel = qexec.execConstruct();
						
						RDFDataMgr.write(os, resultModel, Lang.NTRIPLES);
						resultModel.close();
						qexec.close() ;
					}
					else if (rule.toUpperCase().contains("UPDATE ") || rule.toUpperCase().contains("DELETE ")) {
						final UpdateRequest req = UpdateFactory.create(rule);
						UpdateAction.execute(req, model);
					}
				}
				os.flush();
			}
			try(final InputStream	is = new FileInputStream(temp)) {
				model.read(is, baseURI, "NTRIPLES");
			}
			model.commit();
		} finally {
			temp.delete();
		}
	}
	
	private void uploadContent(final Model model, final OutputStream target, final ContentFormat format) throws IOException {
		switch (format) {
			case JSONLD:
				RDFDataMgr.write(target, model, Lang.JSONLD);
				break;
			case N3:
				RDFDataMgr.write(target, model, Lang.NT);
				break;
			case NQUADS:
				RDFDataMgr.write(target, model, Lang.NQUADS);
				break;
			case NTRIPLES:
				RDFDataMgr.write(target, model, Lang.NTRIPLES);
				break;
			case RDFXML:
				RDFDataMgr.write(target, model, Lang.RDFXML);
				break;
			case TRIG:
				RDFDataMgr.write(target, model, Lang.TRIG);
				break;
			case TRIX:
				RDFDataMgr.write(target, model, Lang.TRIX);
				break;
			case TURTLE:
				RDFDataMgr.write(target, model, Lang.TURTLE);
				break;
			case RDFA: case BINARY:
			default:
				throw new UnsupportedOperationException("Content format ["+format+"] is not supported yet"); 
		}
		target.flush();
	}

	private void unprepareDatabase(final Model model) {
		model.close();
	}

	public static void main(final String[] args) {
		System.exit(main(System.in, args, System.out, System.err));
	}

	public static int main(final InputStream is, final String[] args, final OutputStream os, final PrintStream ps) {
		final ArgParser	parserTemplate = new ApplicationArgParser();
		
		try{final ArgParser		parser = parserTemplate.parse(args);
			final boolean		debug = parser.getValue(Constants.ARG_DEBUG, boolean.class);
			final OntologyType	onto = parser.getValue(ARG_ONTOLOGY_TYPE, OntologyType.class);
			final String		ontoSource = Utils.fromResource(parser.getValue(ARG_ONTOLOGY, URI.class).toURL());
			final String[]		customRules;

			if (parser.isTyped(ARG_CUSTOM_RULES)) {
				try(final InputStream		ruleIs = parser.getValue(ARG_CUSTOM_RULES, URI.class).toURL().openStream();
					final Reader			rulesRdr = new InputStreamReader(ruleIs);
					final BufferedReader	rules = new BufferedReader(rulesRdr)) {
					
					customRules = loadCustomRules(rules);
				}
			}
			else {
				customRules = new String[0];			
			}
			
			final Application	app = new Application(
										parser.isTyped(Constants.ARG_PROCESS) ? new String[] {parser.getValue(Constants.ARG_PROCESS, String.class)} : Constants.MASK_NONE ,
										parser.isTyped(Constants.ARG_PASS) ? new String[] {parser.getValue(Constants.ARG_PASS, String.class)} : Constants.MASK_NONE ,
										parser.isTyped(Constants.ARG_REMOVE) ? new String[] {parser.getValue(Constants.ARG_REMOVE, String.class)} : Constants.MASK_NONE ,
										parser.isTyped(Constants.ARG_RENAME) ? DAUtils.parseRenameArgument(parser.getValue(Constants.ARG_RENAME, String.class)) : new String[0][],
										onto,
										ontoSource,
										customRules,
										ps, 
										debug);
			final long			startTime = System.currentTimeMillis();

			try(final ZipInputStream	zis = new ZipInputStream(is);
				final ZipOutputStream	zos = new ZipOutputStream(os)) {
				
				message(ps, "Process standard input...");
				app.process(zis, zos);
				zos.finish();
			}
			message(ps, "Processing terminated, duration=%1$d msec", System.currentTimeMillis() - startTime);
			return 0;
		} catch (IOException | SyntaxException exc) {
			message(ps, exc.getLocalizedMessage());
			return 129;
		} catch (CommandLineParametersException exc) {
			message(ps, exc.getLocalizedMessage());
			message(ps, parserTemplate.getUsage("da.infer"));
			return 128;
		}
	}	
	
	private static String[] loadCustomRules(final BufferedReader rules) throws IOException {
		final List<String>	result = new ArrayList<>();
		final StringBuilder	sb = new StringBuilder();
		String				line;
		
		while ((line = rules.readLine()) != null) {
			line = line.trim();
			if (!line.isEmpty() && !line.startsWith("#")) {
				if (line.startsWith("---")) {
					result.add(sb.substring(1));
					sb.setLength(0);
				}
				else {
					sb.append('\n').append(line);
				}
			}
		}
		return result.toArray(new String[result.size()]);
	}

	private static void message(final PrintStream ps, final String format, final Object... parameters) {
		ps.println("da.infer: "+String.format(format, parameters));
		ps.flush();
	}
	
	private static class ApplicationArgParser extends ArgParser {
		private static final ArgParser.AbstractArg[]	KEYS = {
			new BooleanArg(Constants.ARG_DEBUG, false, "Turn on debug trace", false),
			new PatternArg(Constants.ARG_PROCESS, false, "Process the given parts in the input *.zip. Types as pattern[,...]. If missing, all the parts will be processed. Mutually exclusive with "+Constants.ARG_PASS+" argument", ".*"),
			new PatternArg(Constants.ARG_PASS, false, "Pass the given parts in the input *.zip without processing. Types as pattern[,...]. If missing, all the parts will be processed. Mutually exclusive with "+Constants.ARG_PROCESS+" argument", ""),
			new PatternArg(Constants.ARG_REMOVE, false, false, "Remove entries from the *.zip input. Types as pattern[,...]. This option is processed AFTER processing/passing part"),
			new StringArg(Constants.ARG_RENAME, false, false, "Rename entries in the *.zip input. Types as pattern->template[;...], see Java Pattern syntax and Java Mather.replaceAll(...) description. This option is processed AFTER processing/passing part"),
			new EnumArg<OntologyType>(ARG_ONTOLOGY_TYPE, OntologyType.class, true, true, "Ontology type to inference"),
			new URIArg(ARG_ONTOLOGY, false, false, "Your ontology URI"),
			new URIArg(ARG_CUSTOM_RULES, false, false, "Custom rules for advanced database processing. Must points to text content with SPARQL splitted with '---\\n'. You can use UPDATE, DELETE and COMPOSE statements there"),
		};
		
		ApplicationArgParser() {
			super(KEYS);
		}
	}
}
