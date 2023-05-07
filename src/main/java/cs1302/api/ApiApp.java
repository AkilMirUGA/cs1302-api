package cs1302.api;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.text.Text;
import javafx.geometry.Pos;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.scene.layout.Priority;

import javafx.scene.control.TextArea;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.util.Properties;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.lang.Runnable;
import java.lang.Thread;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

/**
 * Represents an App that takes in the title of a book and returns a list of media associated
 * with the work's author.
 */
public class ApiApp extends Application {

    private Stage stage;
    private Scene scene;
    private VBox root;
    private HBox interact;
    private VBox startup;
    private VBox loaded;
    private HBox imageLoad1;
    private HBox imageLoad2;
    private TextField search;
    private Button load;
    private String author;
    private String authorId;
    private Image starterIconImage;
    private ImageView starterIcon;
    private Label welcomeLabel;
    private Label instructions;
    private String[] titles;
    private String[] years;
    private String[] images;
    private int numResults;
    private Label bestKnownFor;
    private Label otherWorks;
    private Text authorText;
    private Text worksList;
    private String placeholder;
    private ImageView imageview1;
    private ImageView imageview2;
    private ImageView imageview3;
    private ImageView imageview4;
    private Image[] pics;
    private Text media1;
    private Text media2;
    private Text media3;
    private Text media4;
    private Label note;
    private Label api;
    private HBox message;

    /**
     * Constructs an {@code ApiApp} object. This default (i.e., no argument)
     * constructor is executed in Step 2 of the JavaFX Application Life-Cycle.
     */
    public ApiApp() {
        root = new VBox();
        interact = new HBox();
        startup = new VBox();
        search = new TextField("Search...");
        load = new Button("Load");
        starterIconImage = new Image("file:resources/MovieBook.png");
        starterIcon = new ImageView(starterIconImage);
        welcomeLabel = new Label("Welcome to Media Find!");
        instructions = new Label("Enter a book of your liking to receive a list of media" +
        " pertaining to its author, ranging anywhere from movies to games to other novels!!");
        titles = new String[15];
        years = new String[15];
        images = new String[4];
        numResults = 0;
        bestKnownFor = new Label("Best Known For:");
        otherWorks = new Label("Some Other Works:");
        loaded = new VBox();
        imageLoad1 = new HBox();
        imageLoad2 = new HBox();
        authorText = new Text(author);
        worksList = new Text();
        imageview1 = new ImageView();
        imageview2 = new ImageView();
        imageview3 = new ImageView();
        imageview4 = new ImageView();
        pics = new Image[4];
        media1 = new Text();
        media2 = new Text();
        media3 = new Text();
        media4 = new Text();
        note = new Label("NOTE: Some results may be inaccurate and/or absent due to fallacies"
        + " in the Open Library database");
        api = new Label("Results generated by the Open Library and IMDb APIs");
        message = new HBox();

    } // ApiApp

    /** {@inheritDoc} */
    @Override
    public void start(Stage stage) {

        this.stage = stage;

        // setup scene
        scene = new Scene(root);

        // setup stage
        stage.setTitle("ApiApp!");
        stage.setScene(scene);
        stage.setMaxWidth(1280);
        stage.setMaxHeight(720);
        stage.setOnCloseRequest(event -> Platform.exit());
        stage.sizeToScene();
        stage.show();

    } // start

    /** {@inheritDoc} */
    public void init() {
        System.out.println("init() called");


        startup.setPrefWidth(600);
        startup.setPrefHeight(1000);
        starterIcon.setFitHeight(150);
        starterIcon.setPreserveRatio(true);
        root.getChildren().addAll(interact, startup);


        welcomeLabel.setFont(new Font("Times New Roman", 20));
        instructions.setFont(new Font("Times New Roman", 12));
        api.setFont(new Font("Times New Roman", 12));
        instructions.setWrapText(true);
        api.setWrapText(true);
        instructions.setTextAlignment(TextAlignment.CENTER);
        api.setTextAlignment(TextAlignment.CENTER);
        interact.getChildren().addAll(search, load);
        interact.setHgrow(search, Priority.ALWAYS);
        interact.setSpacing(2);
        note.setFont(new Font("Times New Roman", 10));
        startup.getChildren().addAll(starterIcon, welcomeLabel, instructions, api, note);
        startup.setAlignment(Pos.CENTER);
        startup.setSpacing(10);

        Runnable newRun = () -> {
            retrieveBooks(search.getText());
            retrieveMedia1();
            retrieveMedia2();
            generateResults();
        };

        load.setOnAction(e -> runNow(newRun));

        loaded.setPrefWidth(600);
        loaded.setPrefHeight(1000);
        loaded.getChildren().addAll(authorText, bestKnownFor, imageLoad1, imageLoad2,
            otherWorks, worksList);
        loaded.setSpacing(5);
        authorText.setFont(new Font(20));
        bestKnownFor.setFont(new Font(15));
        otherWorks.setFont(new Font(15));

    } // init

    private static HttpClient BOOK_HTTP_CLIENT = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_2)
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();

    private static Gson BOOK_GSON = new GsonBuilder()
        .setPrettyPrinting()
        .create();

    /**
     * Constructs a JSON using the inputted text, parses it to a GSON and saves
     * the intended response in a {@code author} variable.
     *
     * @param searchText the input into the {@code search} search bar
     */
    private void retrieveBooks(String searchText) {
        try {
            String query = URLEncoder.encode(searchText, StandardCharsets.UTF_8);
            String uri = "https://openlibrary.org/search.json?title=" + query;
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .build();
            HttpResponse<String> response = BOOK_HTTP_CLIENT
                .send(request, BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IOException(response.toString());
            } // if
            String jsonString = response.body();
            BookResponse bookResponse = BOOK_GSON
                .fromJson(jsonString, BookResponse.class);
            if (bookResponse.docs.length >= 1) {
                BookResult result = bookResponse.docs[0];
                author = result.authorName[0];
            } else {
                throw new ArrayIndexOutOfBoundsException("Sorry, there are no such books.");
            } // if
        } catch (IOException | InterruptedException | ArrayIndexOutOfBoundsException e) {
            alertError(e);
        } // try
    } // retrieveBooks

    private static HttpClient MOVIE_HTTP_CLIENT1 = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_2)
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();

    private static Gson MOVIE_GSON1 = new GsonBuilder()
        .setPrettyPrinting()
        .create();

    private static HttpClient MOVIE_HTTP_CLIENT2 = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_2)
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();

    private static Gson MOVIE_GSON2 = new GsonBuilder()
        .setPrettyPrinting()
        .create();

    /**
     * Constructs a JSON using the {@code author} variable, parses it to a GSON and saves
     * the intended response in a {@code authorId} variable.
     */
    private void retrieveMedia1() {
        try {
            if (author == null) {
                throw new NullPointerException("No author found.");
            } // if
            authorId = null;
            String query1 = author.replace(" ", "");
            query1 = query1.replace(".", "");
            query1 = query1.replace(",", "");
            query1 = query1.replace("-", "");
            query1 = query1.replace("'", "");
            String uri1 = "https://imdb-api.com/en/API/SearchName/" + key() + "/" + query1;
            HttpRequest request1 = HttpRequest.newBuilder()
                .uri(URI.create(uri1))
                .build();
            HttpResponse<String> response1 = MOVIE_HTTP_CLIENT1
                .send(request1, BodyHandlers.ofString());
            if (response1.statusCode() != 200) {
                throw new IOException(response1.toString());
            } // if
            String jsonString1 = response1.body();
            IMDbNameResponse nameResponse = MOVIE_GSON1
                .fromJson(jsonString1, IMDbNameResponse.class);

            for (int i = 0; i < nameResponse.results.length; i++) {
                String name = nameResponse.results[i].title;
                name = name.replace(" ", "");
                name = name.replace(".", "");
                name = name.replace(",", "");
                name = name.replace("-", "");
                name = name.replace("'", "");
                if (query1.equalsIgnoreCase(name)) {
                    authorId = nameResponse.results[i].id;
                    break;
                } // if
            } // for

            if (authorId == null) {
                throw new NullPointerException("No author found.");
            } // if

        } catch (IOException | InterruptedException | NullPointerException e) {
            alertError(e);
        } // try
    } // retrieveMedia1

    /**
     * Constructs a JSON using the {@code authorId} variable, parses it to a GSON and saves
     * the intended responses in a set of arrays.
     */
    private void retrieveMedia2() {
        try {
            if (authorId == null) {
                author = placeholder;
                throw new NullPointerException("No author found.");
            } // if
            String uri = "https://imdb-api.com/en/API/Name/" + key() + "/" + authorId;
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri)).build();
            HttpResponse<String> response = MOVIE_HTTP_CLIENT2
                .send(request, BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IOException(response.toString());
            } // if
            IMDbIDResponse idResponse = MOVIE_GSON2
                .fromJson(response.body(), IMDbIDResponse.class);
            for (int i = 0; i < 15; i++) {
                titles[i] = null;
                years[i] = null;
                if (i < 4) {
                    images[i] = null;
                } // if
            } // for
            numResults = 0;
            if (idResponse.knownFor.length >= 1 && idResponse.castMovies.length >= 1) {
                for (int i = 0; i < idResponse.knownFor.length; i++) {
                    IMDbIDResult result = idResponse.knownFor[i];
                    if (result.role.equalsIgnoreCase("writer")) {
                        titles[i] = result.title;
                        images[i] = result.image;
                        if (result.year == null || result.year.equals("")) {
                            years[i] = "(no year listed)";
                        } else {
                            years[i] = "(" + result.year + ")";
                        } // if
                        numResults++;
                    } // if
                } // for
                for (int i = 0, j = 4; i < idResponse.castMovies.length; i++) {
                    IMDbIDResult result = idResponse.castMovies[i];
                    if (result.role.equalsIgnoreCase("writer")) {
                        titles[j] = result.title;
                        if (result.year == null || result.year.equals("")) {
                            years[j] = "(no year listed)";
                        } else {
                            years[j] = "(" + result.year + ")";
                        } // if
                        j++;
                        numResults++;
                        if (j == titles.length) {
                            break;
                        } // if
                    }
                } // for
            } // if
            placeholder = author;
        } catch (IOException | InterruptedException | NullPointerException e) {
            alertError(e);
        } // try
    } // retrieveMedia2


    /**
     * Adds all the results from the arrays to the scene graph to display
     * the list of adaptations.
     */
    private void generateResults() {
        try {
            if (titles[0] == null) {
                throw new NullPointerException("Sorry, there are no results available :(");
            } // if
            authorText.setText(author);
            Platform.runLater(() -> root.getChildren().clear());
            Platform.runLater(() -> root.getChildren().addAll(interact, loaded));
            String list = "";

            for (int i = 4; i < numResults; i++) {
                list = list + titles[i] + " " + years[i] + "\n";
            } // for
            worksList.setText(list);
            for (int i = 0; i < 4; i++) {
                pics[i] = new Image(images[i]);
            } // for
            imageview1.setImage(pics[0]);
            imageview2.setImage(pics[1]);
            imageview3.setImage(pics[2]);
            imageview4.setImage(pics[3]);
            imageview1.setFitHeight(200);
            imageview1.setFitWidth(150);
            imageview2.setFitHeight(200);
            imageview2.setFitWidth(150);
            imageview3.setFitHeight(200);
            imageview3.setFitWidth(150);
            imageview4.setFitHeight(200);
            imageview4.setFitWidth(150);
            media1.setText(titles[0] + " " + years[0]);
            media2.setText(titles[1] + " " + years[1]);
            media3.setText(titles[2] + " " + years[2]);
            media4.setText(titles[3] + " " + years[3]);
            Platform.runLater(() -> imageLoad1.getChildren().clear());
            Platform.runLater(() -> imageLoad2.getChildren().clear());
            Platform.runLater(() ->
                imageLoad1.getChildren().addAll(imageview1, media1, imageview2, media2));
            imageLoad1.setHgrow(imageview1, Priority.ALWAYS);
            imageLoad1.setHgrow(imageview2, Priority.ALWAYS);
            imageLoad1.setHgrow(media1, Priority.ALWAYS);
            imageLoad1.setHgrow(media2, Priority.ALWAYS);
            Platform.runLater(() ->
                imageLoad2.getChildren().addAll(imageview3, media3, imageview4, media4));
            imageLoad2.setHgrow(imageview3, Priority.ALWAYS);
            imageLoad2.setHgrow(imageview4, Priority.ALWAYS);
            imageLoad2.setHgrow(media3, Priority.ALWAYS);
            imageLoad2.setHgrow(media4, Priority.ALWAYS);
            media1.setWrappingWidth(150);
            media2.setWrappingWidth(150);
            media3.setWrappingWidth(150);
            media4.setWrappingWidth(150);
        } catch (NullPointerException npe) {
            alertError(npe);
        } // try


    } // generateResults


    /**
     * Generates the IMDb API Key.
     * @return the IMDb API Key.
     */
    public String key() {
        String configPath = "resources/config.properties";
        try (FileInputStream configFileStream = new FileInputStream(configPath)) {
            Properties config = new Properties();
            config.load(configFileStream);
            String apikey = config.getProperty("imdb.apikey");
            return apikey;
        } catch (IOException ioe) {
            System.err.println(ioe);
            ioe.printStackTrace();
            return "IOException";
        } // try
    } // key



/**
 * Represents a response from the Open Library API. This is used by Gson to
 * create an object from the JSON response body.
 */
    private static class BookResponse {
        int numFound;
        BookResult[] docs;
    } // BookResponse

    /**
     * Represents a result in a response from the OpenLibrary Search API. This is
     * used by Gson to create an object from the JSON response body.
     */
    private static class BookResult {
        @SerializedName("author_name") String[] authorName;
        String title;
    } // BookResult


/**
 * Represents a response from the IMDb API. This is used by Gson to
 * create an object from the JSON response body.
 */
    private static class IMDbNameResponse {
        IMDbNameResult[] results;
    } // IMDbNameResponse


/**
 * Represents a result in a response from the IMDb Search API. This is
 * used by Gson to create an object from the JSON response body.
 */
    private static class IMDbNameResult {
        String id;
        String title;
    } // IMDbNameResult


/**
 * Represents a response from the IMDb API. This is used by Gson to
 * create an object from the JSON response body.
 */
    private static class IMDbIDResponse {
        IMDbIDResult[] castMovies;
        IMDbIDResult[] knownFor;
    } // IMDbIDResponse

    /**
     * Represents a result in a response from the IMDb Search API. This is
     * used by Gson to create an object from the JSON response body.
     */
    private static class IMDbIDResult {
        String title;
        String role;
        String image;
        String year;
    } // IMDbIDResult

    /**
     * Creates and immediately starts a new daemon thread that executes {@code target.run()}.
     * This method, which may be called from any thread, will return immediately its the caller.
     * @param target the object whose {@code run} method is invoked when this thread is started.
     */
    private static void runNow(Runnable target) {
        // Creates a new thread and runs the runnable on it.
        Thread thread = new Thread(target);
        thread.setDaemon(true);
        thread.start();
    } // runNow

    /**
     * Show a modal error alert based on {@code cause}.
     * @param cause a {@link java.lang.Throwable Throwable} that caused the alert
     */
    public static void alertError(Throwable cause) {
        Runnable alertRun = () -> {
            TextArea text = new TextArea(cause.toString());
            text.setEditable(false);
            Alert alert = new Alert(AlertType.ERROR);
            alert.getDialogPane().setContent(text);
            alert.setResizable(true);
            alert.showAndWait();
        };
        Platform.runLater(alertRun);
    } // alertError
} // ApiApp
