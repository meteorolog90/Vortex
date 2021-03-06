package mil.army.usace.hec.vortex.math;

import mil.army.usace.hec.vortex.VortexGrid;

public class Calculator {

    private final VortexGrid inputGrid;
    private final float multiplyValue;
    private final float divideValue;
    private final float addValue;
    private final float subtractValue;

    private Calculator(Builder builder){
        inputGrid = builder.input;
        multiplyValue = builder.multiplyValue;
        divideValue = builder.divideValue;
        addValue = builder.addValue;
        subtractValue = builder.subtractValue;
    }

    public static class Builder {
        private VortexGrid input;
        private float multiplyValue = Float.NaN;
        private float divideValue = Float.NaN;
        private float addValue = Float.NaN;
        private float subtractValue = Float.NaN;

        public Builder inputGrid(VortexGrid input){
            this.input = input;
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

        public Calculator build(){
            if (input == null)
                throw new IllegalStateException("Input grid must not be null");
            int count = 0;
            if (!Double.isNaN(multiplyValue))
                count++;
            if (!Double.isNaN(divideValue))
                count++;
            if (!Double.isNaN(addValue))
                count++;
            if(!Double.isNaN(subtractValue))
                count++;
            if (count != 1)
                throw new IllegalStateException("More than one operator initialized");
            return new Calculator(this);
        }
    }

    public static Builder builder(){return new Builder();}

    public VortexGrid calculate(){
        float[] data = inputGrid.data();
        int size = data.length;
        float[] resultantData = new float[size];

        if (!Double.isNaN(multiplyValue)) {
            for (int i = 0; i < size; i++) {
                resultantData[i] = data[i] * multiplyValue;
            }
        }

        if (!Double.isNaN(divideValue)) {
            for (int i = 0; i < size; i++) {
                resultantData[i] = data[i] / divideValue;
            }
        }

        if (!Double.isNaN(addValue)) {
            for (int i = 0; i < size; i++) {
                resultantData[i] = data[i] + addValue;
            }
        }

        if (!Double.isNaN(subtractValue)) {
            for (int i = 0; i < size; i++) {
                resultantData[i] = data[i] - subtractValue;
            }
        }

        return VortexGrid.builder()
                .dx(inputGrid.dx()).dy(inputGrid.dy())
                .nx(inputGrid.nx()).ny(inputGrid.ny())
                .originX(inputGrid.originX())
                .originY(inputGrid.originY())
                .wkt(inputGrid.wkt())
                .data(resultantData)
                .units(inputGrid.units())
                .fileName(inputGrid.fileName())
                .shortName(inputGrid.shortName())
                .fullName(inputGrid.fullName())
                .description(inputGrid.description())
                .startTime(inputGrid.startTime())
                .endTime(inputGrid.endTime())
                .interval(inputGrid.interval())
                .build();
    }

}
