package edu.brown.cs.student.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import com.google.common.collect.ImmutableMap;

import freemarker.template.Configuration;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import spark.ExceptionHandler;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.Spark;
import spark.TemplateViewRoute;
import spark.template.freemarker.FreeMarkerEngine;

/**
 * The Main class of our project. This is where execution begins.
 */
public final class Main {

  // use port 4567 by default when running server
  private static final int DEFAULT_PORT = 4567;

  /**
   * The initial method called when execution begins.
   *
   * @param args An array of command line arguments
   */
  public static void main(String[] args) {
    new Main(args).run();
  }

  private String[] args;

  private Main(String[] args) {
    this.args = args;
  }

  private void run() {
    // set up parsing of command line flags
    OptionParser parser = new OptionParser();

    // "./run --gui" will start a web server
    parser.accepts("gui");

    // use "--port <n>" to specify what port on which the server runs
    parser.accepts("port").withRequiredArg().ofType(Integer.class)
        .defaultsTo(DEFAULT_PORT);

    OptionSet options = parser.parse(args);
    if (options.has("gui")) {
      runSparkServer((int) options.valueOf("port"));
    }

    List<Star> stars = new ArrayList<>();
    try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
      String input;
      boolean fileLoaded = false;
      while ((input = br.readLine()) != null) {
        try {
          input = input.trim();
          String[] arguments = input.split(" ");
          if ((arguments[0].equals("add") || arguments[0].equals("subtract")) && arguments.length == 3) {
          try {
                MathBot mathBot = new MathBot();
                int val1 = Integer.parseInt(arguments[1]);
                int val2 = Integer.parseInt(arguments[2]);
                double output;
                if (arguments[0].equals("add")) {
                  output = mathBot.add(val1, val2);
                } else { // arguments[0] must be 'subtract'
                  output = mathBot.subtract(val1, val2);
                }
                System.out.println(output);
          } catch (Exception e) {
            System.out.println(arguments[0]);
          }
          } else if (arguments[0].equals("stars") && arguments.length == 2) {
            try {
              stars = parseFile(arguments[1]);
              fileLoaded = true;
            } catch (Exception e) {
              System.out.println("ERROR Reading File. Ensure File is in appropriate Path.");
            }
          } else if ((arguments[0]).equals("naive_neighbors") && arguments.length == 5 && fileLoaded) {
              try {
                // create "Star" of which we will compare to.
                List<String> mainStarArgs = new ArrayList<>();
                mainStarArgs.add("0");
                mainStarArgs.add("MainStart");
                mainStarArgs.addAll(Arrays.asList(arguments).subList(2, arguments.length)); // adds x y z
                Star mainStar = createStar(mainStarArgs);
                List<Star> naiveKnnOutput = naive_knn(stars, mainStar, Integer.parseInt(arguments[1]));
                for (Star star : naiveKnnOutput) {
                  String message = "Here are the " + arguments[1] + " Stars that are closest to the Coordinates " +
                          "(" + arguments[2] + ", " + arguments[3] + ", " +  arguments[4] + "):";
                  System.out.println(message);
                  System.out.println("Star Name: " + star.name + ". Distance to inputted coordinates: " +
                          star.euclideanDistance);
                }

              } catch (Exception e) {
                System.out.println(arguments[0]);
            }
          } else if ((arguments[0]).equals("naive_neighbors") && arguments.length == 3 && fileLoaded) {
              try {
                Star starFromName = findStarByName(stars, arguments[2]);
                List<Star> naiveKnnOutput = naive_knn(stars, starFromName, Integer.parseInt(arguments[1]));
                for (Star star : naiveKnnOutput) {
                  String message = "Here are the " + arguments[1] + " Stars that are closest to the Star named " +
                          arguments[2];
                  System.out.println(message);
                  System.out.println("Star Name: " + star.name + ". Distance to " + arguments[2] +
                          star.euclideanDistance);
                }
              } catch (Exception e) {
                System.out.println(arguments[0]);
            }
          } else { // default behaviour of the REPL
              System.out.println(arguments[0]);
            }
        } catch (Exception e) {
          // e.printStackTrace();
          System.out.println("ERROR: We couldn't process your input");
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("ERROR: Invalid input for REPL");
    }
  }

  /**
   * Finds a star by inputted name
   * @return Star (of Null if name does not exist in starList)
   */
  private static Star findStarByName(List<Star> starList, String name) {
    for (Star star : starList) {
      if (star.name.equals(name)) {
        starList.remove(star);
        return star;
      }
    }
    return null;
  }

  /**
   * Naive KNN process that returns a List of k Stars closest to the inputted mainStar
   * @return List of Star
   */
  private static List<Star> naive_knn(List<Star> starList, Star mainStar, int k) {
    // first, calculate the euclidean distance of each star to the mainStar (star inputted by user)
    for (Star star : starList) {
      Double dis = euclideanDistance(mainStar.x, mainStar.y, mainStar.z, star.x, star.y, star.z);
      star.setEuclideanDistance(dis);
    }

    // Second, sort this Star List by Euclidean Distance
    starList.sort(Comparator.comparing(star -> star.euclideanDistance));

    // Finally, based on how many k nearest neighbors we are trying to find, traverse the list up and down
    // from the kth position to see if there are any ties and randomly choose the proper number of Stars
    // from those ties so that a total of k neighbors are chosen

    // list of indexes with equal euclideanDistances to mainStar
    List<Integer> intsLessThanK = new ArrayList<>();
    double epsilon = 0.00001d;
    for (int i = k - 1; i > 0; i--) {
      if (Math.abs(starList.get(i).euclideanDistance - starList.get(k).euclideanDistance) < epsilon) {
        intsLessThanK.add(i);
      } else {
        break;
      }
    }
    List<Integer> intsGreaterThanK = new ArrayList<>();
    for (int i = k + 1; i < starList.size(); i++) {
      if (Math.abs(starList.get(i).euclideanDistance - starList.get(k).euclideanDistance) < epsilon) {
        intsGreaterThanK.add(i);
      } else {
        break;
      }
    }
    Random rand = new Random();

    if (intsLessThanK.size() == 0 && intsGreaterThanK.size() == 0) {
      return starList.subList(0, k);
    } else {
      List<Star> firstPartOfList = starList.subList(0, (k - 1) - intsLessThanK.size());
      List<Integer> starsToRandomlyChooseFrom = new ArrayList<>();
      starsToRandomlyChooseFrom.addAll(intsLessThanK);
      starsToRandomlyChooseFrom.addAll(intsGreaterThanK);
      while (firstPartOfList.size() < k) {
        int randomIndex = rand.nextInt(starsToRandomlyChooseFrom.size());
        int index1 = starsToRandomlyChooseFrom.get(randomIndex);
        firstPartOfList.add(starList.get(index1));
        starsToRandomlyChooseFrom.remove(randomIndex);
      }
      return firstPartOfList;
    }
  }

  /**
   * Calculates the Euclidean distance between two 3-Dimensional points.
   * @return Double
   */
  private static Double euclideanDistance(Double x1, Double y1, Double z1, Double x2, Double y2, Double z2) {
      return Math.sqrt(Math.pow((x2 - x1), 2) + Math.pow((y2 - y1), 2) + Math.pow((z2 - z1), 2));
  }

  /**
   * Function to parse contents of CSV file
   * @return List of Star
   */
  private static List<Star> parseFile(String filePath) throws IOException {
    Path path = Paths.get(filePath);
    List<Star> output = new ArrayList<>();
    try {
      BufferedReader br = Files.newBufferedReader(path);

      String line = br.readLine();

      while (line != null) {
        List<String> starAttributes = Arrays.asList(line.split(","));

        Star star = createStar(starAttributes);
        output.add(star);

        line = br.readLine();
      }
    } catch (IOException ieo) {
      ieo.printStackTrace();
    }
    return output;
  }

  /**
   * Creates a Star from inputted data. Process used in CSV processing.
   * @return new Star
   */
  private static Star createStar(List<String> data) {
    String starID = data.get(0);
    String name = data.get(1);
    Double x = Double.parseDouble(data.get(2));
    Double y = Double.parseDouble(data.get(3));
    Double z = Double.parseDouble(data.get(4));

    return new Star(starID, name, x, y, z);
  }

  /**
   * Class to represent a Star.
   */
  private static class Star {
    String starID;
    String name;
    Double x;
    Double y;
    Double z;
    Double euclideanDistance = null;

    private Star(String starID, String name, Double x, Double y, Double z) {
      this.starID = starID;
      this.name = name;
      this.x = x;
      this.y = y;
      this.z = z;
    }

    private void setEuclideanDistance(Double dis) {
      this.euclideanDistance = dis;
    }
  }

  private static FreeMarkerEngine createEngine() {
    Configuration config = new Configuration(Configuration.VERSION_2_3_0);

    // this is the directory where FreeMarker templates are placed
    File templates = new File("src/main/resources/spark/template/freemarker");
    try {
      config.setDirectoryForTemplateLoading(templates);
    } catch (IOException ioe) {
      System.out.printf("ERROR: Unable use %s for template loading.%n",
          templates);
      System.exit(1);
    }
    return new FreeMarkerEngine(config);
  }

  private void runSparkServer(int port) {
    // set port to run the server on
    Spark.port(port);

    // specify location of static resources (HTML, CSS, JS, images, etc.)
    Spark.externalStaticFileLocation("src/main/resources/static");

    // when there's a server error, use ExceptionPrinter to display error on GUI
    Spark.exception(Exception.class, new ExceptionPrinter());

    // initialize FreeMarker template engine (converts .ftl templates to HTML)
    FreeMarkerEngine freeMarker = createEngine();

    // setup Spark Routes
    Spark.get("/", new MainHandler(), freeMarker);
  }

  /**
   * Display an error page when an exception occurs in the server.
   */
  private static class ExceptionPrinter implements ExceptionHandler<Exception> {
    @Override
    public void handle(Exception e, Request req, Response res) {
      // status 500 generally means there was an internal server error
      res.status(500);

      // write stack trace to GUI
      StringWriter stacktrace = new StringWriter();
      try (PrintWriter pw = new PrintWriter(stacktrace)) {
        pw.println("<pre>");
        e.printStackTrace(pw);
        pw.println("</pre>");
      }
      res.body(stacktrace.toString());
    }
  }

  /**
   * A handler to serve the site's main page.
   *
   * @return ModelAndView to render.
   * (main.ftl).
   */
  private static class MainHandler implements TemplateViewRoute {
    @Override
    public ModelAndView handle(Request req, Response res) {
      // this is a map of variables that are used in the FreeMarker template
      Map<String, Object> variables = ImmutableMap.of("title",
          "Go go GUI");

      return new ModelAndView(variables, "main.ftl");
    }
  }
}
