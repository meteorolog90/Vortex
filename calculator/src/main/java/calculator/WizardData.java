package calculator;

import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WizardData {

    private static final Logger logger = Logger.getLogger(WizardData.class.getName());

    private final StringProperty inFile = new SimpleStringProperty();
    private final SimpleListProperty<String> availableVariables = new SimpleListProperty<>();
    private final SimpleListProperty<String> selectedVariables = new SimpleListProperty<>();
    private final StringProperty multiplyValue = new SimpleStringProperty();
    private final StringProperty divideValue = new SimpleStringProperty();
    private final StringProperty addValue = new SimpleStringProperty();
    private final StringProperty subtractValue = new SimpleStringProperty();
    private final StringProperty destinationOut = new SimpleStringProperty();

    public StringProperty inFileProperty() {
        return inFile;
    }

    public void setInFile(String file) {
        inFile.set(file);
    }

    public String getInFile(){
        return inFile.get();
    }

    public SimpleListProperty<String> availableVariablesProperty() {
        return availableVariables;
    }

    public void setAvailableVariables(ObservableList<String> grids) {
        if(getInFile().endsWith("dss")) {
            try {
                DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                        .parseCaseInsensitive()
                        .appendPattern("ddMMMuuuu:HHmm")
                        .toFormatter();
                grids.sort(Comparator.comparing(s -> LocalDateTime.parse(s.split("/")[4], formatter)));
            } catch (Exception e) {
                logger.log(Level.SEVERE, e, e::getMessage);
            }
        }
        availableVariables.set(grids);
    }

    public SimpleListProperty<String> selectedVariablesProperty() {
        return selectedVariables;
    }

    public void setSelectedVariables(ObservableList<String> grids) {
        if(getInFile().endsWith("dss")) {
            try {
                DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                        .parseCaseInsensitive()
                        .appendPattern("ddMMMuuuu:HHmm")
                        .toFormatter();
                grids.sort(Comparator.comparing(s -> LocalDateTime.parse(s.split("/")[4], formatter)));
            } catch (Exception e) {
                logger.log(Level.SEVERE, e, e::getMessage);
            }
        }
        selectedVariables.set(grids);
    }

    public List<String> getSelectedVariables(){
        return selectedVariables;
    }

    public void removeAvailableSourceGrids(ObservableList<String> variables) {
        availableVariables.removeAll(variables);
    }

    public void removeSelectedSourceGrids(ObservableList<String> variables) {
        selectedVariables.removeAll(variables);
    }

    public StringProperty lowerThresholdProperty() {
        return multiplyValue;
    }

    public StringProperty multiplyValueProperty(){
        return multiplyValue;
    }

    public String getMultiplyValue(){
        return multiplyValue.get();
    }

    public StringProperty lowerReplacementProperty() {
        return divideValue;
    }

    public StringProperty divideValueProperty() {
        return divideValue;
    }

    public String getDivideValue(){
        return divideValue.get();
    }

    //public StringProperty upperThresholdProperty() { return addValue; }

    public StringProperty addValueProperty() {
        return addValue;
    }

    public String getAddValue() {
        return addValue.get();
    }

    public StringProperty upperThresholdProperty() { return subtractValue; }

    public StringProperty subtractValueProperty() {
        return subtractValue;
    }

    public String getSubtractValue() {
        return subtractValue.get();
    }


    public String getDestinationOut() {
        return destinationOut.get();
    }

    public StringProperty destinationOutProperty() {
        return destinationOut;
    }

}

