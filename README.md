# Employee Salary Management System

A small Java Swing desktop application with no external libraries or build tool. Sources use Java 8-compatible APIs; JDK 17 or later is recommended.

## Run in IntelliJ IDEA

1. Open this project folder.
2. Under **File > Project Structure > Project**, select a JDK (17 is fine). Download a JDK there if needed.
3. Right-click `src` and select **Mark Directory as > Sources Root** if it is not already marked.
4. Open `src/ems/ui/MainFrame.java` and run its `main` method using the green arrow.
5. Set the run configuration's **Working directory** to this project folder so the same data file is used each time.

The original console app is still available by running `src/ems/Main.java`. The console version keeps employees in memory only and does not use the UI's CSV file.

## Features

- Add employees with required-field, duplicate-ID and positive finite salary checks.
- View employees in a read-only table; click column headings to sort.
- Search by ID, ignoring case and surrounding spaces.
- Generate a salary slip, export it as UTF-8 `.txt`, or print through the system print dialog.
- Automatically load and save employees in `employees.csv` in the working directory. The window footer shows the full path.

Only one application instance should edit the data file at a time. Back up the file before editing it manually. A malformed CSV file produces an error and disables adding employees for that session, rather than overwriting existing data. CSV fields support commas and escaped quotes, but not embedded newlines.

## Demonstration salary rules

All values are monthly and in the same currency unit. Rates are project examples, **not real statutory tax or PF rules**.

- HRA: 20%, DA: 10%, TA: 5% of basic salary.
- Gross salary = basic + allowances.
- PF: 12% of basic salary.
- Demonstration tax: 0% of gross up to 25,000; 5% of the **entire gross** above 25,000 through 50,000; 10% of the **entire gross** above 50,000. This is not progressive taxation.
- Net salary = gross - PF - tax.

For basic salary 40,000: gross = 54,000, PF = 4,800, tax = 5,400, net = 43,800.

The existing calculator uses `double` with two-decimal display formatting. It is suitable for this learning project, not production financial accounting.

## Compile and test from a terminal with a JDK on PATH

From this project folder:

```text

Run it from the aana folder, because the Java server looks for the frontend in a relative frontend folder.

1. Open PowerShell and enter the project folder:

cd E:\College\anna\aana

1. Compile the Java sources (you only need to do this after source changes, or if build has no compiled classes):

New-Item -ItemType Directory -Force build
javac -encoding UTF-8 -d build src\ems\*.java src\ems\ui\*.java tests\ems\*.java

1. Start the web server:

java -cp build ems.Main

Keep that window open, then visit http://localhost:8080 in your browser. The HTML frontend is served by the Java server; ems.ui.MainFrame is a separate desktop app and isn't needed. Press
Ctrl+C in PowerShell to stop the server. If it is already running on port 8080, just open the URL rather than starting another copy.
```

The tests are plain Java programs, with no JUnit dependency. They cover CSV persistence and error handling, salary calculation, and headless Swing panel interactions. Native window appearance and printer/file-picker dialogs should also be checked manually in IntelliJ.
