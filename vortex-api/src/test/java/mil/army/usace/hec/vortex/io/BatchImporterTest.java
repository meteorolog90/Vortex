package mil.army.usace.hec.vortex.io;

import hec.heclib.grid.GridData;
import hec.heclib.grid.GridInfo;
import hec.heclib.grid.GridUtilities;
import mil.army.usace.hec.vortex.Options;
import mil.army.usace.hec.vortex.geo.WktFactory;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class BatchImporterTest {

    @Test
    void MrmsPrecipPassesRegression() {
        String inFile = new File(getClass().getResource(
                "/MRMS_GaugeCorr_QPE_01H_00.00_20170102-120000.grib2").getFile()).toString();
        List<String> inFiles = new ArrayList<>();
        inFiles.add(inFile);

        String variableName = "GaugeCorrQPE01H_altitude_above_msl";
        List<String> variables = new ArrayList<>();
        variables.add(variableName);

        Path shapefile = new File(getClass().getResource(
                "/Truckee_River_Watershed_5mi_buffer/Truckee_River_Watershed_5mi_buffer.shp").getFile()).toPath();

        Options options = Options.create();
        options.add("pathToShp", shapefile.toString());
        options.add("targetCellSize", "2000");
        options.add("targetWkt", WktFactory.shg());
        options.add("resamplingMethod", "Bilinear");

        File outFile = new File(getClass().getResource(
                "/regression/mrms/mrms.dss").getFile());

        BatchImporter importer = BatchImporter.builder()
                .inFiles(inFiles)
                .variables(variables)
                .geoOptions(options)
                .destination(outFile.toString())
                .build();

        importer.process();

        int[] status = new int[1];
        String path = "///PRECIPITATION/02JAN2017:1100/02JAN2017:1200//";
        GridData gridData = GridUtilities.retrieveGridFromDss(outFile.getPath(), path, status);

        GridInfo info = gridData.getGridInfo();
        assertEquals(996, info.getLowerLeftCellY());
        assertEquals(-1036, info.getLowerLeftCellX());

        File persisted = new File(getClass().getResource(
                "/regression/mrms/mrms_data_serialized").getFile());
        ArrayList arraylist = new ArrayList<>();
        try {
            FileInputStream fis = new FileInputStream(persisted.toString());
            ObjectInputStream ois = new ObjectInputStream(fis);
            arraylist = (ArrayList) ois.readObject();
            ois.close();
            fis.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        float[] data = gridData.getData();
        for (int i = 0; i < arraylist.size(); i++) {
            assertEquals(arraylist.get(i), data[i]);
        }

//        float[] vortex = gridData.getData();
//        List<Float> list = new ArrayList<>();
//        for (int i = 0; i < vortex.length; i++){
//            list.add(vortex[i]);
//        }
//
//        try{
//            FileOutputStream fos= new FileOutputStream("C:/Temp/mrms_data_serialized");
//            ObjectOutputStream oos= new ObjectOutputStream(fos);
//            oos.writeObject(list);
//            oos.close();
//            fos.close();
//        }catch(IOException ioe){
//            ioe.printStackTrace();
//        }

    }

    @Test
    void RtmaTemperaturePassesRegression() {
        String inFile = new File(getClass().getResource(
                "/201701021200_TMPK.grib2").getFile()).toString();
        List<String> inFiles = new ArrayList<>();
        inFiles.add(inFile);

        String variableName = "Temperature_height_above_ground";
        List<String> variables = new ArrayList<>();
        variables.add(variableName);

        Path shapefile = new File(getClass().getResource(
                "/Truckee_River_Watershed_5mi_buffer/Truckee_River_Watershed_5mi_buffer.shp").getFile()).toPath();

        Options options = Options.create();
        options.add("pathToShp", shapefile.toString());
        options.add("targetCellSize", "2000");
        options.add("targetWkt", WktFactory.shg());
        options.add("resamplingMethod", "Bilinear");

        Options writeOptions = Options.create();
        writeOptions.add("partB", "Truckee River");

        File outFile = new File(getClass().getResource(
                "/regression/rtma/rtma.dss").getFile());

        BatchImporter importer = BatchImporter.builder()
                .inFiles(inFiles)
                .variables(variables)
                .geoOptions(options)
                .destination(outFile.toString())
                .writeOptions(writeOptions)
                .build();

        importer.process();

        int[] status = new int[1];
        String path = "//TRUCKEE RIVER/TEMPERATURE/02JAN2017:1200///";
        GridData gridData = GridUtilities.retrieveGridFromDss(outFile.getPath(), path, status);

        GridInfo info = gridData.getGridInfo();
        assertEquals(996, info.getLowerLeftCellY());
        assertEquals(-1036, info.getLowerLeftCellX());

        File persisted = new File(getClass().getResource(
                "/regression/rtma/rtma_data_serialized").getFile());
        ArrayList arraylist = new ArrayList<>();
        try {
            FileInputStream fis = new FileInputStream(persisted.toString());
            ObjectInputStream ois = new ObjectInputStream(fis);
            arraylist = (ArrayList) ois.readObject();
            ois.close();
            fis.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        float[] data = gridData.getData();
        for (int i = 0; i < arraylist.size(); i++) {
            assertEquals(arraylist.get(i), data[i]);
        }

//        float[] vortex = gridData.getData();
//        List<Float> list = new ArrayList<>();
//        for (int i = 0; i < vortex.length; i++){
//            list.add(vortex[i]);
//        }
//
//        try{
//            FileOutputStream fos= new FileOutputStream("C:/Temp/rtma_data_serialized");
//            ObjectOutputStream oos= new ObjectOutputStream(fos);
//            oos.writeObject(list);
//            oos.close();
//            fos.close();
//        }catch(IOException ioe){
//            ioe.printStackTrace();
//        }
    }

    @Test
    void bilZipReadTest() {
        Path inFile = new File("C:/Users/q0hectab/Desktop/temp/Vortex/vortex-api/src/test/resources/PRISM_ppt_stable_4kmD2_20170101_20170131_bil.zip").toPath();
        List<String> inFiles = new ArrayList<>();
        inFiles.add(inFile.toString());

        String outFile = new File("C:/Temp/test_bil_zip.dss").toString();

        String variableName = "ppt";
        List<String> variables = new ArrayList<>();
        variables.add(variableName);

        BatchImporter importer = BatchImporter.builder()
                .inFiles(inFiles)
                .variables(variables)
                .destination(outFile)
                .build();

        importer.process();

    }
}