package mil.army.usace.hec.vortex.math;

import mil.army.usace.hec.vortex.Options;
import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.io.DataReader;
import mil.army.usace.hec.vortex.io.DataWriter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CalculatableUnit {
    private final DataReader reader;
    private final float multiplyValue;
    private final float divideValue;
    private final float addValue;
    private final float subtractValue;
    private final Path destination;
    private final Options writeOptions;

    private CalculatableUnit(Builder builder) {
        reader = builder.reader;
        multiplyValue = builder.multiplyValue;
        divideValue = builder.divideValue;
        addValue = builder.addValue;
        subtractValue = builder.subtractValue;
        destination = builder.destination;
        writeOptions = builder.writeOptions;
    }

    public static class Builder {
        private DataReader reader;
        private float multiplyValue = Float.NaN;
        private float divideValue = Float.NaN;
        private float addValue = Float.NaN;
        private float subtractValue = Float.NaN;
        private Path destination;
        private Options writeOptions;

        public Builder reader(DataReader reader) {
            this.reader = reader;
            return this;
        }

        public Builder multiplyValue(float multiplyValue) {
            this.multiplyValue = multiplyValue;
            return this;
        }

        public Builder divideValue(float divideValue) {
            this.divideValue = divideValue;
            return this;
        }

        public Builder addValue(float addValue) {
            this.addValue = addValue;
            return this;
        }

        public Builder subtractValue(float subtractValue) {
            this.subtractValue = subtractValue;
            return this;
        }

        public Builder destination(Path destination) {
            this.destination = destination;
            return this;
        }

        public Builder writeOptions(Options writeOptions) {
            this.writeOptions = writeOptions;
            return this;
        }

        public CalculatableUnit build() {
            if (reader == null) {
                throw new IllegalArgumentException("DataReader must be provided");
            }
            if (destination == null) {
                throw new IllegalArgumentException("Destination must be provided");
            }
            return new CalculatableUnit(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public void process() {
        List<VortexGrid> grids = reader.getDtos().stream().map(grid -> (VortexGrid) grid).collect(Collectors.toList());

        grids.forEach(grid -> {
            Calculator calculator = Calculator.builder()
                    .inputGrid(grid)
                    .multiplyValue(multiplyValue)
                    .divideValue(divideValue)
                    .addValue(addValue)
                    .subtractValue(subtractValue)
                    .build();

            VortexGrid calculated = calculator.calculate();

            List<VortexData> data = new ArrayList<>();
            data.add(calculated);

            DataWriter writer = DataWriter.builder()
                    .data(data)
                    .destination(destination)
                    .options(writeOptions)
                    .build();

            writer.write();
        });
    }
}
