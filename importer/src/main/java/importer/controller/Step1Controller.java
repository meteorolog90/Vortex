package importer.controller;

import com.google.inject.Inject;
import importer.WizardData;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.stage.FileChooser;
import javafx.stage.StageStyle;
import mil.army.usace.hec.vortex.io.DataReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static java.nio.file.StandardOpenOption.CREATE;

public class Step1Controller {

    private Logger log = LoggerFactory.getLogger(Step1Controller.class);

    @FXML
    ListView<String> inFiles;

    @FXML
    Button add;

    @Inject
    WizardData model;

    @FXML
    public void initialize() {
        Image addImage = new Image(getClass().getResourceAsStream("/opened-folder-16.png"));
        add.setGraphic(new ImageView(addImage));

        inFiles.itemsProperty().bindBidirectional(model.inFilesProperty());
        inFiles.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        inFiles.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DELETE) {
                ObservableList<String> selection = inFiles.getSelectionModel().getSelectedItems();
                model.removeInFiles(selection);
                inFiles.getSelectionModel().clearSelection();
            }
        });
    }

    @FXML
    private void handleAdd() {
        FileChooser fileChooser = new FileChooser();
        Optional<File> initialFilePath = Optional.ofNullable(getPersistedBrowseLocation());
        if (initialFilePath.isPresent()) {
            File filePath = initialFilePath.get();
            if (filePath.exists()) {
                fileChooser.setInitialDirectory(initialFilePath.get().getParentFile());
            }
        }

        // Set extension filters
        FileChooser.ExtensionFilter recognizedFilter = new FileChooser.ExtensionFilter(
                "All recognized files", "*.nc", "*.nc4", "*.hdf", "*.hdf5", "*.grib", "*.gb2", "*.grb2", "*.grib2", "*.grb", "*.asc", "*.bil", "*bil.zip", "*.dss");
        fileChooser.getExtensionFilters().add(recognizedFilter);
        FileChooser.ExtensionFilter ncFilter = new FileChooser.ExtensionFilter(
                "netCDF datasets", "*.nc", "*.nc4");
        fileChooser.getExtensionFilters().add(ncFilter);
        FileChooser.ExtensionFilter hdfFilter = new FileChooser.ExtensionFilter(
                "HDF datasets", "*.hdf", "*.hdf5");
        fileChooser.getExtensionFilters().add(hdfFilter);
        FileChooser.ExtensionFilter gribFilter = new FileChooser.ExtensionFilter(
                "GRIB datasets", "*.grib", "*.gb2", "*.grb2", "*.grib2", "*.grb");
        fileChooser.getExtensionFilters().add(gribFilter);

        FileChooser.ExtensionFilter ascFilter = new FileChooser.ExtensionFilter(
                "ASC datasets", "*.asc");
        fileChooser.getExtensionFilters().add(ascFilter);

        FileChooser.ExtensionFilter bilFilter = new FileChooser.ExtensionFilter(
                "BIL datasets", "*.bil", "*bil.zip");
        fileChooser.getExtensionFilters().add(bilFilter);

        FileChooser.ExtensionFilter dssFilter = new FileChooser.ExtensionFilter(
                "DSS datasets", "*.dss");
        fileChooser.getExtensionFilters().add(dssFilter);

        FileChooser.ExtensionFilter allFilesFilter = new FileChooser.ExtensionFilter(
                "All files", "*");
        fileChooser.getExtensionFilters().add(allFilesFilter);

        // Show save file dialog
        List<File> files = fileChooser.showOpenMultipleDialog(add.getScene().getWindow());

        if (files != null) {
            ObservableList<String> list = FXCollections.observableArrayList(
                    files.stream().map(File::toString).collect(Collectors.toList()));
            model.addInFiles(list);

            setPersistedBrowseLocation(files.get(0));
        }
    }

    @FXML
    private void handleRemove(){
        ObservableList<String> selection = inFiles.getSelectionModel().getSelectedItems();
        model.removeInFiles(selection);
        inFiles.getSelectionModel().clearSelection();
    }

    @Validate
    public boolean validate() {

        if( model.inFilesProperty().isEmpty() ) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText( "Missing Field" );
            alert.setContentText( "Input dataset is required." );
            alert.initStyle(StageStyle.UTILITY);
            alert.showAndWait();
            return false;
        }
        return true;
    }

    @Submit
    public void submit() {
        List<String> files = new ArrayList<>(model.inFilesProperty());

        Set<String> variables = files.stream()
                .map(DataReader::getVariables)
                .flatMap(Collection::stream)
                .collect(Collectors.toCollection(TreeSet::new));

        ObservableList<String> list = FXCollections.observableArrayList(variables);
        Platform.runLater(
                () -> {
                    model.setAvailableVariables(list);
                    model.setSelectedVariables(FXCollections.emptyObservableList());
                }
        );

        if (log.isDebugEnabled()) {
            log.debug("[SUBMIT] the user has completed step 1");
        }
    }

    private void setPersistedBrowseLocation(File file) {
        Path pathToProperties = Paths.get(System.getProperty("user.home")
                + File.separator + ".vortex" + File.separator + "importer.properties" );

        try(OutputStream output = Files.newOutputStream(pathToProperties, CREATE)){
            Properties properties = new Properties();
            properties.setProperty("inFilePath", file.getPath());
            properties.store(output,null);
        } catch (IOException e) {
            log.error(e.toString());
        }
    }

    private File getPersistedBrowseLocation() {
        Path pathToProperties = Paths.get(System.getProperty("user.home")
                + File.separator + ".vortex" + File.separator + "importer.properties" );

        if (Files.exists(pathToProperties)) {
            try (InputStream input = Files.newInputStream(pathToProperties)) {
                Properties properties = new Properties();
                properties.load(input);
                String outFilePath = properties.getProperty("inFilePath");
                if (outFilePath == null){
                    return null;
                }
                if (Files.exists(Paths.get(outFilePath))) {
                    return new File(outFilePath);
                }
                return null;
            } catch (IOException e) {
                log.error(e.toString());
                return null;
            }
        }
        return null;
    }
}
