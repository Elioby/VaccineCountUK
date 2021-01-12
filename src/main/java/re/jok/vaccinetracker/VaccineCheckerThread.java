package re.jok.vaccinetracker;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.zip.GZIPInputStream;
import lombok.Getter;

@Getter
public class VaccineCheckerThread extends Thread {

	private volatile double vaccinations = -1;
	private volatile double vaccinationsDiff = -1;
	private volatile double firstDose = -1;
	private volatile double secondDose = -1;
	private volatile double firstDoseDiff = -1;
	private volatile double secondDoseDiff = -1;
	private volatile LocalDate dataUptoDate = null;
	private volatile ZonedDateTime lastCheck = null;

	private static final Gson gson = new Gson();
	private static URL dataUrl;

	@Override
	public void run() {
		try {
			dataUrl = new URL("https://coronavirus.data.gov.uk/api/v1/data?filters=areaType=overview&structure=%7B%22date%22:%22date%22,%22cumPeopleVaccinatedFirstDoseByPublishDate%22:%22cumPeopleVaccinatedFirstDoseByPublishDate%22,%22cumPeopleVaccinatedSecondDoseByPublishDate%22:%22cumPeopleVaccinatedSecondDoseByPublishDate%22%7D&format=json");
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		while (true) {
			try {
				updateData();

				try {
					Thread.sleep(60000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void updateData() {
		try (InputStreamReader reader = new InputStreamReader(new GZIPInputStream(dataUrl.openConnection().getInputStream()))) {
			JsonObject json = gson.fromJson(reader, JsonObject.class);

			JsonArray data = json.get("data").getAsJsonArray();
			JsonObject mostRecentData = data.get(0).getAsJsonObject();
			JsonObject dayBeforeData = data.get(1).getAsJsonObject();
			firstDose = mostRecentData.get("cumPeopleVaccinatedFirstDoseByPublishDate").getAsInt();
			secondDose = mostRecentData.get("cumPeopleVaccinatedSecondDoseByPublishDate").getAsInt();
			vaccinations = firstDose + secondDose;

			int dayBeforeFirstDose = dayBeforeData.get("cumPeopleVaccinatedFirstDoseByPublishDate").getAsInt();
			int dayBeforeSecondDose = dayBeforeData.get("cumPeopleVaccinatedSecondDoseByPublishDate").getAsInt();
			vaccinationsDiff = vaccinations - (dayBeforeFirstDose + dayBeforeSecondDose);
			firstDoseDiff = firstDose - dayBeforeFirstDose;
			secondDoseDiff = secondDose - dayBeforeSecondDose;

			lastCheck = ZonedDateTime.now();
			dataUptoDate = LocalDate.parse(mostRecentData.get("date").getAsString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
