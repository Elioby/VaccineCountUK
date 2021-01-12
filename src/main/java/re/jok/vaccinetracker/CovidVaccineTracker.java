package re.jok.vaccinetracker;

import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.ocpsoft.prettytime.PrettyTime;
import spark.ModelAndView;
import spark.Service;
import spark.Spark;
import spark.template.velocity.VelocityTemplateEngine;

public class CovidVaccineTracker {

	private static final DecimalFormat vaccineNumberFormat = new DecimalFormat("#,###");
	private static final DateTimeFormatter releaseDateFormat = DateTimeFormatter.ofPattern("dd/MM/yy");
	private static final PrettyTime prettyTime = new PrettyTime();

	public static void main(String[] args) {
		VaccineCheckerThread checkerThread = new VaccineCheckerThread();
		checkerThread.start();

		VelocityTemplateEngine velocityTemplateEngine = new VelocityTemplateEngine();
		Service service = Service.ignite()
				.port(80)
				.staticFileLocation("/static");

		service.get("/", (req, res) -> {
			Map<String, Object> map = new HashMap<>();

			map.put("vaccinationsFormatted", checkerThread.getVaccinations() != -1 ?
					vaccineNumberFormat.format(checkerThread.getVaccinations()) : "Unknown");
			map.put("vaccinationsDiffFormatted", checkerThread.getVaccinationsDiff() != -1 ?
					(checkerThread.getVaccinationsDiff() >= 0 ? "+" : "") + vaccineNumberFormat.format(checkerThread.getVaccinationsDiff()) : "Unknown");
			map.put("firstDoseFormatted", checkerThread.getSecondDose() != -1 ?
					vaccineNumberFormat.format(checkerThread.getFirstDose()) : "Unknown");
			map.put("secondDoseFormatted", checkerThread.getSecondDose() != -1 ?
					vaccineNumberFormat.format(checkerThread.getSecondDose()) : "Unknown");
			map.put("dataFromDateFormatted", checkerThread.getDataFromDate() != null ?
					releaseDateFormat.format(checkerThread.getDataFromDate()) : "Never");
			map.put("dataFromDateMinus1Formatted", checkerThread.getDataFromDate() != null ?
					releaseDateFormat.format(checkerThread.getDataFromDate().minus(1, ChronoUnit.DAYS)) : "Never");
			map.put("lastCheckFormatted", checkerThread.getLastCheck() != null ?
					prettyTime.format(Date.from(checkerThread.getLastCheck().toInstant())) : "Never");

			return new ModelAndView(map, "/templates/index.vm");
		}, velocityTemplateEngine);
	}

}
