package re.jok.vaccinetracker;

import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.ocpsoft.prettytime.PrettyTime;
import org.ocpsoft.prettytime.units.JustNow;
import org.ocpsoft.prettytime.units.Millisecond;
import spark.ModelAndView;
import spark.Service;
import spark.template.velocity.VelocityTemplateEngine;

public class CovidVaccineTracker {

	private static final DecimalFormat vaccineNumberFormat = new DecimalFormat("#,###");
	private static final DateTimeFormatter releaseDateFormat = DateTimeFormatter.ofPattern("dd/MM/yy");
	private static final PrettyTime prettyTime = new PrettyTime();

	public static void main(String[] args) {
		// we don't want to format durations with "moments ago" or millis
		prettyTime.removeUnit(JustNow.class);
		prettyTime.removeUnit(Millisecond.class);

		VaccineCheckerThread checkerThread = new VaccineCheckerThread();
		checkerThread.start();

		VelocityTemplateEngine velocityTemplateEngine = new VelocityTemplateEngine();
		Service service = Service.ignite()
				.port(80)
				.staticFileLocation("/static");

//		service.before(((request, response) -> {
//			final String url = request.url();
//			if (url.startsWith("http://")) {
//				final String[] split = url.split("http://");
//				response.redirect("https://" + split[1]);
//			}
//		}));

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
			map.put("firstDoseDiffFormatted", checkerThread.getSecondDoseDiff() != -1 ?
					(checkerThread.getFirstDoseDiff() >= 0 ? "+" : "") + vaccineNumberFormat.format(checkerThread.getFirstDoseDiff()) : "Unknown");
			map.put("secondDoseDiffFormatted", checkerThread.getSecondDoseDiff() != -1 ?
					(checkerThread.getSecondDoseDiff() >= 0 ? "+" : "") + vaccineNumberFormat.format(checkerThread.getSecondDoseDiff()) : "Unknown");
			map.put("dataFromDateFormatted", checkerThread.getDataUptoDate() != null ?
					releaseDateFormat.format(checkerThread.getDataUptoDate()) : "Never");
			map.put("dataFromDateMinus1Formatted", checkerThread.getDataUptoDate() != null ?
					releaseDateFormat.format(checkerThread.getDataUptoDate().minus(1, ChronoUnit.DAYS)) : "Never");
			map.put("lastCheckFormatted", checkerThread.getLastCheck() != null ?
					prettyTime.format(Date.from(checkerThread.getLastCheck().toInstant())) : "Never");

			return new ModelAndView(map, "/templates/index.vm");
		}, velocityTemplateEngine);
	}

}
