package fr.ac_versailles.crdp.apiscol.meta.searchEngine;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SolrRecordsSyntaxAnalyser {
	private final static String dynamicFacetGroupSchema = "(.*)-taxon$";
	private final static String dynamicFacetSchema = "^(.*)!_!(.*)\\(__(.*)__\\)$";
	private static Pattern dynamicFacetGroupPattern = Pattern
			.compile(dynamicFacetGroupSchema);
	private static Pattern dynamicFacetPattern = Pattern
			.compile(dynamicFacetSchema);

	public static String detectDynamicFacetGroupName(String facetGroupName) {
		Matcher matcher = dynamicFacetGroupPattern.matcher(facetGroupName);
		if (matcher.find())
			return matcher.group(1);
		return "";
	}

	public static ArrayList<String> dynamicFacetEntrySegments(
			String concatenedString) {
		dynamicFacetPattern = Pattern.compile(dynamicFacetSchema);
		ArrayList<String> list = new ArrayList<String>();
		Matcher matcher = dynamicFacetPattern.matcher(concatenedString);
		if (matcher.find()) {
			list.add(matcher.group(1));
			list.add(matcher.group(2));
			list.add(matcher.group(3));
		}
		return list;
	}
}
