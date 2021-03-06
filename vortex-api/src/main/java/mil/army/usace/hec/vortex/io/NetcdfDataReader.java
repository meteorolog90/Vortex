package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.geo.Grid;
import mil.army.usace.hec.vortex.geo.ReferenceUtils;
import mil.army.usace.hec.vortex.geo.WktFactory;
import ucar.ma2.Array;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.*;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.time.CalendarDate;
import ucar.unidata.geoloc.Projection;
import ucar.unidata.geoloc.ProjectionImpl;

import javax.measure.IncommensurableException;
import javax.measure.Unit;
import javax.measure.UnitConverter;
import java.io.File;
import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import static javax.measure.MetricPrefix.KILO;
import static systems.uom.common.USCustomary.DEGREE_ANGLE;
import static tech.units.indriya.AbstractUnit.ONE;
import static tech.units.indriya.unit.Units.METRE;

public class NetcdfDataReader extends DataReader {

    private static final Logger logger = Logger.getLogger(NetcdfDataReader.class.getName());

    NetcdfDataReader(DataReaderBuilder builder) {
        super(builder);
    }

    @Override
    public List<VortexData> getDtos() {
        try (NetcdfDataset ncd = NetcdfDatasets.openDataset(path);  Formatter errlog = new Formatter()) {
            FeatureDataset dataset = FeatureDatasetFactoryManager.wrap(FeatureType.GRID, ncd, null, errlog);
            if (dataset != null) {
                FeatureType ftype = dataset.getFeatureType();
                if (ftype == FeatureType.GRID) {
                    assert (dataset instanceof GridDataset);
                    GridDataset gridDataset = (GridDataset) dataset;
                    return getData(gridDataset, variableName);
                }
            } else {
                List<Variable> variables = ncd.getVariables();
                for (Variable variable : variables) {
                    if (variable.getShortName().equals(variableName) && variable instanceof VariableDS) {
                        VariableDS variableDS = (VariableDS) variable;
                        int count = getDtoCount(variableDS);

                        VariableDsReader reader = VariableDsReader.builder()
                                .setNetcdfFile(ncd)
                                .setVariableName(variableName)
                                .build();

                        List<VortexData> dataList = new ArrayList<>();
                        for (int i = 0; i < count; i++) {
                            VortexData data = reader.read(i);
                            dataList.add(data);
                        }
                        return dataList;
                    }
                }
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, e, e::getMessage);
            return Collections.emptyList();
        }
        return Collections.emptyList();
    }

    @Override
    public VortexData getDto(int idx) {
        try (NetcdfDataset ncd = NetcdfDatasets.openDataset(path);  Formatter errlog = new Formatter()) {
            FeatureDataset dataset = FeatureDatasetFactoryManager.wrap(FeatureType.GRID, ncd, null, errlog);
            if (dataset != null) {
                FeatureType ftype = dataset.getFeatureType();
                if (ftype == FeatureType.GRID) {
                    assert (dataset instanceof GridDataset);
                    GridDataset gridDataset = (GridDataset) dataset;
                    return getData(gridDataset, variableName, idx);
                }
            } else {
                List<Variable> variables = ncd.getVariables();
                for (Variable variable : variables){
                    if (variable.getShortName().equals(variableName) && variable instanceof VariableDS) {
                        VariableDS variableDS = (VariableDS) variable;
                        List<CoordinateSystem> coordinateSystems = variableDS.getCoordinateSystems();
                        if (!coordinateSystems.isEmpty()) {
                            VariableDsReader reader = VariableDsReader.builder()
                                    .setNetcdfFile(ncd)
                                    .setVariableName(variableName)
                                    .build();

                            return reader.read(idx);
                        }
                    }
                }
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, e, e::getMessage);
        }

        return null;
    }

    @Override
    public int getDtoCount() {
        try (NetcdfDataset ncd = NetcdfDatasets.openDataset(path);  Formatter errlog = new Formatter()) {
            FeatureDataset dataset = FeatureDatasetFactoryManager.wrap(FeatureType.GRID, ncd, null, errlog);
            if (dataset != null) {
                FeatureType ftype = dataset.getFeatureType();
                if (ftype == FeatureType.GRID) {
                    assert (dataset instanceof GridDataset);
                    GridDataset gridDataset = (GridDataset) dataset;
                    return getDtoCount(gridDataset, variableName);
                }
            } else {
                List<Variable> variables = ncd.getVariables();
                for (Variable variable : variables){
                    if (variable.getShortName().equals(variableName) && variable instanceof VariableDS) {
                        VariableDS variableDS = (VariableDS) variable;
                        List<CoordinateSystem> coordinateSystems = variableDS.getCoordinateSystems();
                        if (!coordinateSystems.isEmpty()) {
                            return getDtoCount(variableDS);
                        }
                    }
                }
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, e, e::getMessage);
        }

        return 0;
    }

    public static Set<String> getVariables(String path) {
        try (NetcdfDataset ncd = NetcdfDatasets.openDataset(path)) {
            List<Variable> variables = ncd.getVariables();
            Set<String> variableNames = new HashSet<>();
            variables.forEach(variable -> {
                if (variable instanceof VariableDS) {
                    VariableDS variableDS = (VariableDS) variable;
                    List<CoordinateSystem> coordinateSystems = variableDS.getCoordinateSystems();
                    if (!coordinateSystems.isEmpty()) {
                        variableNames.add(variable.getFullName());
                    }
                }
            });
            return variableNames;
        } catch (Exception e) {
            logger.log(Level.SEVERE, e, e::getMessage);
        }
        return Collections.emptySet();
    }

    private float[] getFloatArray(Array array) {
        float[] data = new float[(int) array.getSize()];
        for (int i = 0; i < array.getSize(); i++) {
            data[i] = array.getFloat(i);
        }
        return data;
    }

    private List<VortexData> getData(GridDataset dataset, String variable) {
        GridDatatype gridDatatype = dataset.findGridDatatype(variable);
        GridCoordSystem gcs = gridDatatype.getCoordinateSystem();

        Grid grid = getGrid(gcs);
        String wkt = getWkt(gcs.getProjection());

        List<ZonedDateTime[]> times = getTimeBounds(gcs);

        Dimension timeDim = gridDatatype.getTimeDimension();
        Dimension endDim = gridDatatype.getEnsembleDimension();
        Dimension rtDim = gridDatatype.getRunTimeDimension();

        List<VortexData> grids = new ArrayList<>();

        if (timeDim != null && endDim != null && rtDim != null) {
            IntStream.range(0, rtDim.getLength()).forEach(rtIndex ->
                    IntStream.range(0, endDim.getLength()).forEach(ensIndex ->
                            IntStream.range(0, timeDim.getLength()).forEach(timeIndex -> {
                                Array array;
                                try {
                                    array = gridDatatype.readDataSlice(rtIndex, ensIndex, timeIndex, -1, -1, -1);
                                    float[] data = getFloatArray(array);
                                    ZonedDateTime startTime = times.get(timeIndex)[0];
                                    ZonedDateTime endTime = times.get(timeIndex)[1];
                                    Duration interval = Duration.between(startTime, endTime);
                                    grids.add(VortexGrid.builder()
                                            .dx(grid.getDx()).dy(grid.getDy())
                                            .nx(grid.getNx()).ny(grid.getNy())
                                            .originX(grid.getOriginX()).originY(grid.getOriginY())
                                            .wkt(wkt)
                                            .data(data)
                                            .units(gridDatatype.getUnitsString())
                                            .fileName(dataset.getLocation())
                                            .shortName(gridDatatype.getShortName())
                                            .fullName(gridDatatype.getFullName())
                                            .description(gridDatatype.getDescription())
                                            .startTime(startTime)
                                            .endTime(endTime)
                                            .interval(interval)
                                            .build());
                                } catch (IOException e) {
                                    logger.log(Level.SEVERE, e, e::getMessage);
                                }
                            })));
        } else if (timeDim != null) {
            IntStream.range(0, times.size()).forEach(timeIndex -> {
                try {
                    Array array = gridDatatype.readDataSlice(timeIndex, -1, -1, -1);
                    float[] data = getFloatArray(array);
                    ZonedDateTime startTime = times.get(timeIndex)[0];
                    ZonedDateTime endTime = times.get(timeIndex)[1];
                    Duration interval = Duration.between(startTime, endTime);
                    grids.add(VortexGrid.builder()
                            .dx(grid.getDx()).dy(grid.getDy())
                            .nx(grid.getNx()).ny(grid.getNy())
                            .originX(grid.getOriginX()).originY(grid.getOriginY())
                            .wkt(wkt)
                            .data(data)
                            .units(gridDatatype.getUnitsString())
                            .fileName(dataset.getLocation())
                            .shortName(gridDatatype.getShortName())
                            .fullName(gridDatatype.getFullName())
                            .description(gridDatatype.getDescription())
                            .startTime(startTime)
                            .endTime(endTime)
                            .interval(interval)
                            .build());
                } catch (IOException e) {
                    logger.log(Level.SEVERE, e, e::getMessage);
                }
            });
        } else {
            try {
                Array array = gridDatatype.readDataSlice(1, -1, -1, -1);
                float[] data = getFloatArray(array);
                ZonedDateTime startTime;
                ZonedDateTime endTime;
                Duration interval;
                if (!times.isEmpty()) {
                    startTime = times.get(0)[0];
                    endTime = times.get(0)[1];
                    interval = Duration.between(startTime, endTime);
                } else if (gridDatatype.findAttributeIgnoreCase("start_date") != null
                        && gridDatatype.findAttributeIgnoreCase("stop_date") != null) {
                    String startTimeString = gridDatatype.findAttributeIgnoreCase("start_date").getStringValue();
                    startTime = ZonedDateTime.parse(startTimeString, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z"));
                    String endTimeString = gridDatatype.findAttributeIgnoreCase("stop_date").getStringValue();
                    endTime = ZonedDateTime.parse(endTimeString, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z"));
                    interval = Duration.between(startTime, endTime);
                } else {
                    startTime = null;
                    endTime = null;
                    interval = null;
                }

                grids.add(VortexGrid.builder()
                        .dx(grid.getDx()).dy(grid.getDy())
                        .nx(grid.getNx()).ny(grid.getNy())
                        .originX(grid.getOriginX()).originY(grid.getOriginY())
                        .wkt(wkt)
                        .data(data)
                        .units(gridDatatype.getUnitsString())
                        .fileName(dataset.getLocation())
                        .shortName(gridDatatype.getShortName())
                        .fullName(gridDatatype.getFullName())
                        .description(gridDatatype.getDescription())
                        .startTime(startTime)
                        .endTime(endTime)
                        .interval(interval)
                        .build());
            } catch (IOException e) {
                logger.log(Level.SEVERE, e, e::getMessage);
            }
        }
        return grids;
    }

    private static String getWkt(Projection projection) {
        return WktFactory.createWkt((ProjectionImpl) projection);
    }

    private List<ZonedDateTime[]> getTimeBounds(GridCoordSystem gcs) {
        List<ZonedDateTime[]> list = new ArrayList<>();
        if (gcs.hasTimeAxis1D()) {
            CoordinateAxis1DTime tAxis = gcs.getTimeAxis1D();
            IntStream.range(0, (int) tAxis.getSize()).forEach(time -> {
                ZonedDateTime[] zonedDateTimes = new ZonedDateTime[2];
                CalendarDate[] dates = tAxis.getCoordBoundsDate(time);

                String fileName = new File(path).getName().toLowerCase();
                if (fileName.matches(".*gaugecorr.*qpe.*01h.*grib2")
                        || fileName.matches(".*radaronly.*qpe.*01h.*grib2")) {
                    zonedDateTimes[0] = convert(dates[0]).minusHours(1);
                } else if (fileName.matches(".*hhr\\.ms\\.mrg.*hdf.*")) {
                    zonedDateTimes[0] = convert(tAxis.getCalendarDate(time));
                } else if (fileName.matches(".*aorc.*apcp.*nc4.*")) {
                    zonedDateTimes[0] = convert(tAxis.getCalendarDate(time)).minusHours(1);
                } else if (fileName.matches(".*aorc.*tmp.*nc4.*")) {
                    zonedDateTimes[0] = convert(tAxis.getCalendarDate(time));
                } else if (fileName.matches("[0-9]{2}.nc")) {
                    zonedDateTimes[0] = convert(tAxis.getCalendarDate(0));
                } else {
                    zonedDateTimes[0] = convert(dates[0]);
                }

                if (fileName.matches("hrrr.*wrfsfcf.*")){
                    zonedDateTimes[1] = zonedDateTimes[0].plusHours(1);
                } else if (fileName.matches(".*hhr\\.ms\\.mrg.*hdf.*")) {
                    zonedDateTimes[1] = zonedDateTimes[0].plusMinutes(30);
                } else if (fileName.matches(".*aorc.*apcp.*nc4.*")) {
                    zonedDateTimes[1] = convert(tAxis.getCalendarDate(time));
                } else if (fileName.matches(".*aorc.*tmp.*nc4.*")) {
                    zonedDateTimes[1] = convert(tAxis.getCalendarDate(time));
                } else if (fileName.matches("[0-9]{2}.nc")) {
                    zonedDateTimes[1] = zonedDateTimes[0].plusDays(1);
                } else {
                    zonedDateTimes[1] = convert(dates[1]);
                }

                list.add(zonedDateTimes);
            });
            return list;
        } else if (gcs.hasTimeAxis()) {
            CoordinateAxis1D tAxis = (CoordinateAxis1D) gcs.getTimeAxis();
            String units = tAxis.getUnitsString();

            IntStream.range(0, (int) tAxis.getSize()).forEach(time -> {
                String dateTimeString = (units.split(" ", 3)[2]).replaceFirst(" ", "T").split(" ")[0].replace(".", "");

                ZonedDateTime origin;

                if (dateTimeString.contains("T")) {
                    origin = ZonedDateTime.of(LocalDateTime.parse(dateTimeString, DateTimeFormatter.ISO_DATE_TIME), ZoneId.of("UTC"));
                } else {
                    origin = ZonedDateTime.of(LocalDate.parse(dateTimeString, DateTimeFormatter.ofPattern("uuuu-M-d")), LocalTime.of(0, 0), ZoneId.of("UTC"));
                }
                ZonedDateTime startTime;
                ZonedDateTime endTime;
                if (units.toLowerCase().matches("^month[s]? since.*$")) {
                    startTime = origin.plusMonths((long) tAxis.getBound1()[time]);
                    endTime = origin.plusMonths((long) tAxis.getBound2()[time]);
                } else if (units.toLowerCase().matches("^day[s]? since.*$")) {
                    startTime = origin.plusSeconds((long) tAxis.getBound1()[time] * 86400);
                    endTime = origin.plusSeconds((long) tAxis.getBound2()[time] * 86400);
                } else if (units.toLowerCase().matches("^hour[s]? since.*$")) {
                    startTime = origin.plusSeconds((long) tAxis.getBound1()[time] * 3600);
                        endTime = origin.plusSeconds((long) tAxis.getBound2()[time] * 3600);
                } else if (units.toLowerCase().matches("^minute[s]? since.*$")) {
                    endTime = origin.plusSeconds((long) tAxis.getBound2()[time] * 60);
                        startTime = origin.plusSeconds((long) tAxis.getBound1()[time] * 60);
                } else if (units.toLowerCase().matches("^second[s]? since.*$")) {
                    startTime = origin.plusSeconds((long) tAxis.getBound1()[time]);
                    endTime = origin.plusSeconds((long) tAxis.getBound2()[time]);
                } else {
                    startTime = ZonedDateTime.of(0, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
                    endTime = ZonedDateTime.of(0, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
                }
                ZonedDateTime[] zonedDateTimes = new ZonedDateTime[2];
                zonedDateTimes[0] = startTime;
                zonedDateTimes[1] = endTime;
                list.add(zonedDateTimes);
            });
            return list;
        }
        return Collections.emptyList();
    }

    private static ZonedDateTime convert(CalendarDate date) {
        return ZonedDateTime.parse(date.toString(), DateTimeFormatter.ISO_DATE_TIME);
    }

    private static Grid getGrid(GridCoordSystem coordinateSystem) {
        AtomicReference<Grid> grid = new AtomicReference<>();

        CoordinateAxis xAxis = coordinateSystem.getXHorizAxis();
        CoordinateAxis yAxis = coordinateSystem.getYHorizAxis();

        int nx = (int) xAxis.getSize();
        int ny = (int) yAxis.getSize();

        double[] edgesX = ((CoordinateAxis1D) xAxis).getCoordEdges();
        double ulx = edgesX[0];

        double urx = edgesX[edgesX.length - 1];
        double dx = (urx - ulx) / nx;

        double[] edgesY = ((CoordinateAxis1D) yAxis).getCoordEdges();
        double uly = edgesY[0];
        double lly = edgesY[edgesY.length - 1];
        double dy = (lly - uly) / ny;

        grid.set(Grid.builder()
                .nx(nx)
                .ny(ny)
                .dx(dx)
                .dy(dy)
                .originX(ulx)
                .originY(uly)
                .build());

        String wkt = getWkt(coordinateSystem.getProjection());

        String xAxisUnits = Objects.requireNonNull(xAxis).getUnitsString();

        Unit<?> cellUnits;
        switch (xAxisUnits.toLowerCase()) {
            case "m":
            case "meter":
            case "metre":
                cellUnits = METRE;
                break;
            case "km":
                cellUnits = KILO(METRE);
                break;
            case "degrees_east":
            case "degrees_north":
                cellUnits = DEGREE_ANGLE;
                break;
            default:
                cellUnits = ONE;
        }

        Unit<?> csUnits;
        switch (ReferenceUtils.getMapUnits(wkt).toLowerCase()) {
            case "m":
            case "meter":
            case "metre":
                csUnits = METRE;
                break;
            case "km":
                csUnits = KILO(METRE);
                break;
            default:
                csUnits = ONE;
        }

        if (cellUnits == DEGREE_ANGLE && (grid.get().getOriginX() == 0 || grid.get().getOriginX() > 180)) {
            grid.set(shiftGrid(grid.get()));
        }

        if (cellUnits.isCompatible(csUnits) && !cellUnits.equals(csUnits)) {
            grid.set(scaleGrid(grid.get(), cellUnits, csUnits));
        }

        return grid.get();
    }

    private static Grid shiftGrid(Grid grid){
        if (grid.getOriginX() > 180) {
            return Grid.builder()
                    .originX(grid.getOriginX() - 360)
                    .originY(grid.getOriginY())
                    .dx(grid.getDx())
                    .dy(grid.getDy())
                    .nx(grid.getNx())
                    .ny(grid.getNy())
                    .build();
        } else {
            return grid;
        }
    }

    private static Grid scaleGrid(Grid grid, Unit<?> cellUnits, Unit<?> csUnits){
        Grid scaled;
        try {
            UnitConverter converter = cellUnits.getConverterToAny(csUnits);
            scaled = Grid.builder()
                    .originX(converter.convert(grid.getOriginX()))
                    .originY(converter.convert(grid.getOriginY()))
                    .dx(converter.convert(grid.getDx()))
                    .dy(converter.convert(grid.getDy()))
                    .nx(grid.getNx())
                    .ny(grid.getNy())
                    .build();
        } catch (IncommensurableException e) {
            return null;
        }
        return scaled;
    }

    private int getDtoCount(GridDataset dataset, String variable) {
        GridDatatype gridDatatype = dataset.findGridDatatype(variable);
        Dimension timeDim = gridDatatype.getTimeDimension();
        if (timeDim != null) {
            return timeDim.getLength();
        } else {
            return 1;
        }
    }

    private int getDtoCount(VariableDS variableDS) {
        List<Dimension> dimensions = variableDS.getDimensions();
        for (Dimension dimension : dimensions){
            if (dimension.getShortName().equals("time")){
                return dimension.getLength();
            }
        }
        return 0;
    }

    private VortexData getData(GridDataset dataset, String variable, int idx) {
        GridDatatype gridDatatype = dataset.findGridDatatype(variable);
        GridCoordSystem gcs = gridDatatype.getCoordinateSystem();

        Grid grid = getGrid(gcs);
        String wkt = getWkt(gcs.getProjection());

        List<ZonedDateTime[]> times = getTimeBounds(gcs);

        Array array;
        try {
            array = gridDatatype.readDataSlice(idx, -1, -1, -1);
        } catch (IOException e) {
            logger.log(Level.SEVERE, e, e::getMessage);
            return null;
        }

        float[] data = getFloatArray(array);

        ZonedDateTime startTime;
        ZonedDateTime endTime;
        Duration interval;
        if (!times.isEmpty()) {
            startTime = times.get(idx)[0];
            endTime = times.get(idx)[1];
            interval = Duration.between(startTime, endTime);
        } else if (gridDatatype.findAttributeIgnoreCase("start_date") != null
                && gridDatatype.findAttributeIgnoreCase("stop_date") != null) {
            String startTimeString = gridDatatype.findAttributeIgnoreCase("start_date").getStringValue();
            startTime = ZonedDateTime.parse(startTimeString, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z"));
            String endTimeString = gridDatatype.findAttributeIgnoreCase("stop_date").getStringValue();
            endTime = ZonedDateTime.parse(endTimeString, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z"));
            interval = Duration.between(startTime, endTime);
        } else {
            startTime = null;
            endTime = null;
            interval = null;
        }
        return VortexGrid.builder()
                .dx(grid.getDx()).dy(grid.getDy())
                .nx(grid.getNx()).ny(grid.getNy())
                .originX(grid.getOriginX()).originY(grid.getOriginY())
                .wkt(wkt)
                .data(data)
                .units(gridDatatype.getUnitsString())
                .fileName(dataset.getLocation())
                .shortName(gridDatatype.getShortName())
                .fullName(gridDatatype.getFullName())
                .description(gridDatatype.getDescription())
                .startTime(startTime)
                .endTime(endTime)
                .interval(interval)
                .build();
    }
}
