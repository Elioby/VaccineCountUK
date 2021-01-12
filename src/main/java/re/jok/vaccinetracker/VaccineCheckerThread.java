package re.jok.vaccinetracker;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import lombok.Getter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@Getter
public class VaccineCheckerThread extends Thread {

	private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMMM-yyyy");

	private volatile double vaccinations = -1;
	private volatile double vaccinationsDiff = -1;
	private volatile double firstDose = -1;
	private volatile double secondDose = -1;
	private volatile LocalDate dataFromDate = null;
	private volatile ZonedDateTime lastCheck = null;

	@Override
	public void run() {
		while (true) {
			try {
				boolean updateSuccess = false;
				try {
					updateSuccess = tryUpdateNumbers(LocalDate.now());
				} catch (FileNotFoundException ignored) {

				} finally {
					if (!updateSuccess) {
						System.err.println("Error getting new data... trying to find yesterdays!");
						try {
							tryUpdateNumbers(LocalDate.now().minus(1, ChronoUnit.DAYS));
						} catch (FileNotFoundException fileNotFoundException) {
							System.err.println("Failed to find any data from yesterday either!");
						}
					}
				}

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

	private boolean tryUpdateNumbers(LocalDate date) throws FileNotFoundException {
		try {
			Sheet sheet = getVaccineSheet(date);
			Cell cell;

			// vaccinations
			cell = sheet.getRow(13).getCell(3);
			vaccinations = cell.getNumericCellValue();

			// yesterday vaccinations
			Sheet sheetYesterday = getVaccineSheet(date.minus(1, ChronoUnit.DAYS));

			cell = sheetYesterday.getRow(13).getCell(3);
			vaccinationsDiff = vaccinations - cell.getNumericCellValue();

			// first dose
			cell = sheet.getRow(14).getCell(3);
			firstDose = cell.getNumericCellValue();

			// second dose
			cell = sheet.getRow(15).getCell(3);
			secondDose = cell.getNumericCellValue();
		} catch (FileNotFoundException e) {
			throw e;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		lastCheck = ZonedDateTime.now();
		dataFromDate = date;

		return true;
	}

	private Sheet getVaccineSheet(LocalDate date) throws IOException {
		String dateString = formatter.format(date);
		String url = "https://www.england.nhs.uk/statistics/wp-content/uploads/sites/2/2021/01/COVID-19-daily-announced-vaccinations-" + dateString + ".xlsx";
		try (InputStream is = new BufferedInputStream(new URL(url).openStream())) {
			Workbook wb = new XSSFWorkbook(is);
			return wb.getSheet("Total Vaccinations");
		}
	}

}
