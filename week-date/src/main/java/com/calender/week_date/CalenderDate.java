package com.calender.week_date;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.threeten.extra.chrono.AccountingChronology;
import org.threeten.extra.chrono.AccountingChronologyBuilder;
import org.threeten.extra.chrono.AccountingDate;
import org.threeten.extra.chrono.AccountingYearDivision;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

/**
 * Calender date
 * Author : Somu
 * Date : 13/03/2022	
 */
public class CalenderDate implements RequestHandler<Map<String,Object> , APIGatewayProxyResponseEvent>{
	@Override
	  public APIGatewayProxyResponseEvent handleRequest(Map<String,Object> event, Context context)
	  {
		LambdaLogger logger = context.getLogger();
		logger.log("In CalenderDate-->");
		logger.log("CalenderDate EVENT 1: " + event);
		//String sYear=event.get("year").toString();
		String sYear="";
		Object query=event.get("queryStringParameters");
		logger.log("CalenderDate EVENT 3: " + query);
		if(query instanceof Map) {
			Map mYear=(Map)query;
			sYear=mYear.get("year").toString();
		}else if(query instanceof String) {
			sYear=query.toString();
		}
	    logger.log("year give: "+sYear);
	    JSONObject oCalender;
	    if(validYear(sYear)) {
			int iYear=Integer.parseInt(sYear);
			LinkedHashMap<String,HashMap<Integer,List<String>>> yearDays =createCalender(iYear);
			//System.out.println("Year calender created successfully");
			oCalender= formatCalender(yearDays,iYear);
			//System.out.print("print successful");
		}else {
			//System.out.println("Not Valid year");
			oCalender=new JSONObject();
			oCalender.put(sYear, "Not Valid year");
		}
	    // return amount of time remaining before timeout
	   // return oCalender.toString();
	    //return oCalender;
	    return new APIGatewayProxyResponseEvent()
	            .withStatusCode(200)
	            .withBody(oCalender.toString())
	            .withIsBase64Encoded(false);
	  }
	
	@SuppressWarnings("unchecked")
	private static JSONObject formatCalender(HashMap<String, HashMap<Integer, List<String>>> yearDays, int iYear) {
		JSONObject oCalender = new JSONObject();
		JSONArray monthLstJs= new JSONArray();
		JSONObject oeachMonth;
		oCalender.put("FiscalYear", iYear);
		for(String month:yearDays.keySet()) {
			oeachMonth=formatMonth(month,yearDays.get(month));
			monthLstJs.add(oeachMonth);
		}
		oCalender.put("Months", monthLstJs);
		return oCalender;
	}

	@SuppressWarnings("unchecked")
	private static JSONObject formatMonth(String month, HashMap<Integer, List<String>> monthData) {
		JSONObject oeachMonJS = new JSONObject();
		oeachMonJS.put("FiscalMonth", month);
		oeachMonJS.put("NumberOfWeeks", monthData.size());
		JSONObject oWkNoJs;
		JSONArray dayLstJs; 
		JSONArray wkLstJs= new JSONArray();
		for(Integer wkNo:monthData.keySet()) {
			oWkNoJs = new JSONObject();
			dayLstJs = new JSONArray();
			oWkNoJs.put("WeekNumber", wkNo);
			for(String day:monthData.get(wkNo)) {
				dayLstJs.add(day);
			}
			oWkNoJs.put("Days", dayLstJs);
			wkLstJs.add(oWkNoJs);
		}
		oeachMonJS.put("Weeks", wkLstJs);
		return oeachMonJS;
	}

	public static boolean validYear(String year) {
		boolean valid=false;
		if(year.length()!=4) {
			return valid;
		}
		try {
			int iYear=Integer.parseInt(year);
			if(iYear>=2000 && iYear<=2999) {
				valid=true;
			}
		}catch (Exception e) {
			return valid;
		}
		return valid;
	}
	
	public static LinkedHashMap<String,HashMap<Integer,List<String>>> createCalender(int year){
		AccountingChronology acctChrono = new AccountingChronologyBuilder()
		        .endsOn(DayOfWeek.SUNDAY)
		        .inLastWeekOf(Month.DECEMBER)
		        .withDivision(AccountingYearDivision.QUARTERS_OF_PATTERN_4_4_5_WEEKS)
		        .leapWeekInMonth(12)
		        .toChronology();
		int month=4;
		LinkedHashMap<String,HashMap<Integer,List<String>>> yearDays = new LinkedHashMap<String,HashMap<Integer,List<String>>>();
		for (int loop = 1; loop <= 12; loop++) {
		    AccountingDate start = acctChrono.date(year, month, 1);
		    AccountingDate end = start.with(TemporalAdjusters.lastDayOfMonth());
		    String monthName= Month.of(month).name().toLowerCase();
		    System.out.println("Month -> "+monthName);
		    HashMap<Integer,List<String>> monthDays= getDays(start,end);
		    yearDays.put(monthName, monthDays);
		    month++;
		    if(month==13) {
		    	year++;
		    }
		    if(month!=12) {
		    	month=month%12;
		    }
		}
		return yearDays;
	}
	
	public static HashMap<Integer, List<String>> getDays(AccountingDate startDate, AccountingDate endDate) {
		HashMap<Integer,List<String>> monthDays=new HashMap<Integer,List<String>>();
		int weekCount=0,week=1;
		List<String> days=new ArrayList<String>();
		while(!startDate.equals(endDate)) {
			if(weekCount==0) {
				//System.out.println("Week -> "+week);
				days=new ArrayList<String>();
			}
			//System.out.println(formateDate(startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)));
			days.add(formateDate(startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)));
			startDate=startDate.plus(1, ChronoUnit.DAYS);
			weekCount++;
			if(weekCount==7) {
				monthDays.put(week, days);
				week++;
				weekCount=0;
			}
		}
		//System.out.println(formateDate(startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)));
		days.add(formateDate(startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)));
		monthDays.put(week, days);
		return monthDays;
	}
	
	public static String formateDate(String inDate) {
		SimpleDateFormat oldFormat = new SimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat newFormat = new SimpleDateFormat("dd/MM/yyyy");
		String outDate="";
		try {
			outDate= newFormat.format(oldFormat.parse(inDate));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return outDate;
	}
}
