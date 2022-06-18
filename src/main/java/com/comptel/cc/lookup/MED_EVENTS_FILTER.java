/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.comptel.cc.lookup;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.comptel.mc.node.NodeContext;
import com.comptel.mc.node.lookup.LookupService;
import com.comptel.mc.node.lookup.LookupTable;
import com.comptel.mc.node.lookup.NormalLookupResultItem;

/**
 *
 * @author cpt2sea
 */
public class MED_EVENTS_FILTER {

	private static final int FIRST_LOOKUP_RESULT_ITEM = 0;
	public static final String NP_LOOKUP_SERVER_NAME = "LookupServerName";
	private static final int NUMBER_OF_RETURN_COLUMNS = 1;
	private static final int NUMBER_OF_KEY_COLUMNS = 2;
	private static final int NUMBER_OF_MAX_RESULT_ROWS = 1;
	private final static Logger logger = Logger.getLogger(MED_EVENTS_FILTER.class.getCanonicalName());
	private LookupService lookupService;
	private LookupTable lookupTable;

	public MED_EVENTS_FILTER() {
	}

	public MED_EVENTS_FILTER(NodeContext ctx, LookupService lookupService) throws Exception {
		this.lookupService = lookupService;
		init(ctx);
	}

	private void init(NodeContext ctx) throws Exception {
		String lookupServerName = ctx.getParameter(NP_LOOKUP_SERVER_NAME);
		String lookupTableName = "MED_EVENTS_FILTER";

		lookupTable = lookupService.getTable(lookupServerName, lookupTableName, true);
	}

	public String getFilter(String stream, String node) throws Exception {
		long startTime = System.nanoTime();

		String retval = "";

		String[] keys = { stream, node };

		List<NormalLookupResultItem> results = lookupTable.lookup(NUMBER_OF_RETURN_COLUMNS, NUMBER_OF_KEY_COLUMNS, NUMBER_OF_MAX_RESULT_ROWS, keys);

		if (results.size() > 0) {
			NormalLookupResultItem result = results.get(FIRST_LOOKUP_RESULT_ITEM);
			// Loop for each column and get filter value.
			retval = result.getReturnValues().get(FIRST_LOOKUP_RESULT_ITEM);
		} else {
			results = lookupTable.lookup(NUMBER_OF_RETURN_COLUMNS, NUMBER_OF_KEY_COLUMNS, NUMBER_OF_MAX_RESULT_ROWS, stream, "*");
			if (results.size() > 0) {
				NormalLookupResultItem result = results.get(FIRST_LOOKUP_RESULT_ITEM);
				// Loop for each column and get filter value.
				retval = result.getReturnValues().get(FIRST_LOOKUP_RESULT_ITEM);
			}
		}

		long endTime = System.nanoTime();
		double duration = (endTime - startTime) / 1e6;
		if (logger.isLoggable(Level.FINE)) {
			logger.log(Level.FINE, "LookupDuration:" + duration + " ms " + getLsTableFullName() + " for stream " + stream + " and node " + node + " returned " + results.size()
					+ " results - using result[] = " + retval);
		}

		return retval;
	}

	public void reloadLookupTable() throws Exception {
		if (null != lookupTable) {
			lookupTable.reload();
			logger.log(Level.FINEST, "Lookup table ''{0}'' is reloaded.", getLsTableFullName());
		}
	}

	private String getLsTableFullName() {
		return lookupTable.getServerName() + "." + lookupTable.getName();
	}

}
