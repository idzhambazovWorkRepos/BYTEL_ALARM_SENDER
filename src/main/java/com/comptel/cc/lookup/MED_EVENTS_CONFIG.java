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
public class MED_EVENTS_CONFIG {
	private static final int FIRST_LOOKUP_RESULT_ITEM = 0;
	public static final String NP_LOOKUP_SERVER_NAME = "LookupServerName";
	private static final int NUMBER_OF_RETURN_COLUMNS = 8;
	private static final int NUMBER_OF_KEY_COLUMNS = 1;
	private static final int NUMBER_OF_MAX_RESULT_ROWS = 1;
	private final static Logger logger = Logger.getLogger(MED_EVENTS_CONFIG.class.getCanonicalName());
	private LookupService lookupService;
	private LookupTable lookupTable;

	public MED_EVENTS_CONFIG() {
	}

	public MED_EVENTS_CONFIG(NodeContext ctx, LookupService lookupService) throws Exception {
		this.lookupService = lookupService;
		init(ctx);
	}

	private void init(NodeContext ctx) throws Exception {
		String lookupServerName = ctx.getParameter(NP_LOOKUP_SERVER_NAME);
		String lookupTableName = "MED_EVENTS_CONFIG";

		lookupTable = lookupService.getTable(lookupServerName, lookupTableName, true);
	}

	public MedEventsConfigData getValues(String alarmid) throws Exception {
		long startTime = System.nanoTime();

		List<NormalLookupResultItem> results = lookupTable.lookup(NUMBER_OF_RETURN_COLUMNS, NUMBER_OF_KEY_COLUMNS, NUMBER_OF_MAX_RESULT_ROWS, alarmid);

		MedEventsConfigData medEventsConfigData = new MedEventsConfigData();
		if (results.size() > 0) {
			NormalLookupResultItem result = results.get(FIRST_LOOKUP_RESULT_ITEM);
			// Loop for each column and get values to an array.
			// 1- Stream
			// 2- Node
			// 3- Message
			// 4- Threshold
			// 5- Severity_regex
			// 6- Severity_match
			// 7- Severity_value
			medEventsConfigData.stream = result.getReturnValues().get(0);
			medEventsConfigData.node = result.getReturnValues().get(1);
			medEventsConfigData.message = result.getReturnValues().get(2);
			medEventsConfigData.threshold = result.getReturnValues().get(3);
			medEventsConfigData.severityRegexp = result.getReturnValues().get(4);
			medEventsConfigData.severityMatch = result.getReturnValues().get(5);
			medEventsConfigData.severityValue = result.getReturnValues().get(6);
			medEventsConfigData.defaultSeverity = result.getReturnValues().get(7);
			medEventsConfigData.match = true;
		} else {
			medEventsConfigData.match = false;
		}

		long endTime = System.nanoTime();
		double duration = (endTime - startTime) / 1e6;
		if (logger.isLoggable(Level.FINE)) {
			logger.log(Level.FINE, "LookupDuration:" + duration + " ms " + getLsTableFullName() + " with keys [" + alarmid + "] returned  " + results.size() + "results - using result[] = "
					+ medEventsConfigData.toString());
		}

		return medEventsConfigData;
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
