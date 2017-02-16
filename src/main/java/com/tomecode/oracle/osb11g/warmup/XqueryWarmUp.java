package com.tomecode.oracle.osb11g.warmup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.xml.namespace.QName;

import com.bea.wli.common.xquery.XQueryInfo;
import com.bea.wli.common.xquery.iterators.NullIterator;
import com.bea.wli.config.ChangeDescriptor;
import com.bea.wli.config.ChangeDescriptor.Create;
import com.bea.wli.config.ChangeDescriptor.Delete;
import com.bea.wli.config.ChangeDescriptor.IdentityChange;
import com.bea.wli.config.ChangeDescriptor.Update;
import com.bea.wli.config.ChangeDescriptor.UpdateAndIdentityChange;
import com.bea.wli.config.Ref;
import com.bea.wli.config.spi.ResourceLifecycleListener;
import com.bea.wli.sb.resources.config.XqueryEntryDocument;
import com.bea.wli.sb.resources.xquery.XqueryExecutor;
import com.bea.wli.sb.resources.xquery.XqueryRepository;
import com.bea.wli.sb.stages.transform.ExecuteOptions;

import weblogic.logging.NonCatalogLogger;
import weblogic.xml.query.exceptions.XQueryException;
import weblogic.xml.query.xdbc.XQType;

/**
 * This class is a listener for OSB resources - XQuery, is activated when
 * something has changed in XQuery or when the server was started or when XQuery
 * was deployed.
 * 
 * 
 * Listener, create at least one new thread which call/execute/warm-up all
 * changed/deployed XQuery functions/files with dummy arguments.
 * 
 * @author Tome
 *
 */
public final class XqueryWarmUp extends ResourceLifecycleListener {

	private static final NonCatalogLogger logger = new NonCatalogLogger("XqueryWarmUp");
	private static int CHUNK_SIZE = Integer.parseInt(System.getProperty("OSb.XqueryWarmUp.initChunkSize", "200"));

	public XqueryWarmUp() {
		super(true, (Set<String>) new HashSet<String>(Arrays.asList(new String[] { "Xquery", })), true);
	}

	@Override
	public final void beginChangeNotification(String arg0, @SuppressWarnings("rawtypes") Map arg1) throws Exception {

	}

	@Override
	public final void beginLoadNotification(String arg0, @SuppressWarnings("rawtypes") Map arg1) {

	}

	@Override
	public final void changesCommitted(String arg0, @SuppressWarnings("rawtypes") Map map) {
		deploymentWarmUp(map);
	}

	@Override
	public final void changesRolledback(String arg0, @SuppressWarnings("rawtypes") Map arg1) {

	}

	@Override
	public final void endChangeNotification(String arg0, @SuppressWarnings("rawtypes") Map arg1) throws Exception {

	}

	@Override
	public final void endLoadNotification(String arg0, @SuppressWarnings("rawtypes") Map arg1) {
		logger.info("Warm-up...");
		Ref[] xqRefs = filterRefs();
		wampUp(xqRefs);
	}

	@Override
	public final void onCreate(String arg0, Create arg1, @SuppressWarnings("rawtypes") Map arg2) throws Exception {

	}

	@Override
	public final void onDelete(String arg0, Delete arg1, @SuppressWarnings("rawtypes") Map arg2) throws Exception {

	}

	@Override
	public final void onIdentityChange(String arg0, IdentityChange arg1, @SuppressWarnings("rawtypes") Map arg2) throws Exception {

	}

	@Override
	public final void onLoad(String arg0, Create arg1, @SuppressWarnings("rawtypes") Map arg2) {

	}

	@Override
	public final void onUpdate(String arg0, Update arg1, @SuppressWarnings({ "rawtypes" }) Map arg2) throws Exception {

	}

	@Override
	public final void onUpdateAndIdentityChange(String arg0, UpdateAndIdentityChange arg1, @SuppressWarnings("rawtypes") Map arg2) throws Exception {

	}

	private final Ref[] filterRefs() {
		Set<Ref> xqRefs = XqueryRepository.get().getRefs(null);
		Ref[] refs = new Ref[xqRefs.size()];
		xqRefs.toArray(refs);
		return refs;
	}

	/**
	 * warm-up XQuery which was deployed or changed (activte OSB session)
	 * 
	 * @param map
	 */
	private final void deploymentWarmUp(@SuppressWarnings("rawtypes") Map map) {
		List<Ref> refs = new ArrayList<Ref>();
		if (map != null) {
			Iterator<?> ik = map.keySet().iterator();
			while (ik.hasNext()) {
				Object k = ik.next();
				Object v = map.get(k);

				if (v instanceof ArrayList) {
					ArrayList<?> va = (ArrayList<?>) v;

					for (Object ai : va) {
						if (ai instanceof ChangeDescriptor) {
							ChangeDescriptor changeDescriptor = (ChangeDescriptor) ai;

							if ("Xquery".equals(changeDescriptor.getRef().getTypeId())) {
								refs.add(changeDescriptor.getRef());
							}
						}
					}
				}
			}
		}
		// filter changed xqueries
		Ref[] xqRefs = new Ref[refs.size()];
		refs.toArray(xqRefs);
		wampUp(xqRefs);
	}

	/**
	 * warm-up XQueries when server is started
	 * 
	 * @param xqRefs
	 */
	private final void wampUp(Ref[] xqRefs) {
		// split all references to chunks and for each chunk will be created
		// dedicated thread
		final List<Ref[]> chunksForThreads = Utils.splitRefs(xqRefs, CHUNK_SIZE);

		logger.info("WarmUp XQueries - Total count: " + xqRefs.length + " WarmUp was divide into chunks: " + CHUNK_SIZE + " and threads: " + chunksForThreads.size());

		doIt(chunksForThreads);
	}

	/**
	 * 
	 * @param chunksForThreads
	 */
	private final void doIt(final List<Ref[]> chunksForThreads) {
		final XqueryRepository repository = XqueryRepository.get();

		// create warm-up thread pool
		ExecutorService executorService = Executors.newFixedThreadPool(chunksForThreads.size());
		for (int i = 0; i <= chunksForThreads.size() - 1; i++) {
			final Ref[] refs = chunksForThreads.get(i);

			executorService.submit(new Runnable() {

				@Override
				public final void run() {
					warmUpXquery(refs);
				}

				/**
				 * initialize Xqueries
				 * 
				 * @param refs
				 */
				private final void warmUpXquery(Ref[] refs) {
					long timeStart = System.currentTimeMillis();
					for (int i = 0; i <= refs.length - 1; i++) {
						Ref ref = refs[i];
						logger.info("WarmUp XQuery (" + i + "): " + ref.getFullName());

						try {

							XqueryEntryDocument xqEntiry = repository.getEntry(ref);
							// prepare arguments
							Map<QName, Object> args = dummyArguments(xqEntiry);
							// find xq executor
							XqueryExecutor xqExecutor = repository.getExecutor(ref);
							// execute xq
							xqExecutor.execute(args, new ExecuteOptions().setAttributesAsXml(true));
						} catch (Exception e) {
							logger.error("Failed to WarmUp XQuery (" + i + "): " + ref.getFullName() + " reason: " + e.getMessage(), e);
						}

					}

					long timeEnd = System.currentTimeMillis();
					long delta = timeEnd - timeStart;
					logger.info("WarmUp XQueries - Total time: " + Utils.formatDeltaTime(delta));
				}

				/**
				 * create dummy arguments for xq
				 * 
				 * @param xqEntiry
				 * @return
				 * @throws XQueryException
				 * @throws com.bea.wli.common.xquery.XQueryException
				 */
				private final Map<QName, Object> dummyArguments(XqueryEntryDocument xqEntiry) throws XQueryException, com.bea.wli.common.xquery.XQueryException {
					Map<QName, Object> args = new HashMap<QName, Object>();

					XQueryInfo xqInfo = XQueryInfo.parse(xqEntiry.getXqueryEntry().getXquery());
					for (Entry<QName, XQType> e : xqInfo.getXQTypes().entrySet()) {
						if (e.getValue().isNode()) {
							args.put(e.getKey(), Utils.DUMMY_MESSAGE);
						} else {
							String type = e.getValue().qname().toString();
							if (type.endsWith("string]")) {
								args.put(e.getKey(), "");
							} else if (type.endsWith("double]")) {
								args.put(e.getKey(), 0d);
							} else if (type.endsWith("long]")) {
								args.put(e.getKey(), 0l);
							} else if (type.endsWith("integer]")) {
								args.put(e.getKey(), 1);
							} else if (type.endsWith("boolean]")) {
								args.put(e.getKey(), true);
							} else {
								args.put(e.getKey(), new NullIterator());
							}
						}

					}

					return args;
				}
			});
		}

	}

}
