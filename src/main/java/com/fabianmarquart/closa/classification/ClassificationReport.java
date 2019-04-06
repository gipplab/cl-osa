package com.fabianmarquart.closa.classification;

import java.util.List;

/**
 * Inner class that is used for deserializing the JSON result.
 *
 * @deprecated used for external classifier.
 */
@Deprecated
class ClassificationReport {
    private double textCoverage;
    private List<Classification> classification;

    public ClassificationReport(double textCoverage) {
        this.textCoverage = textCoverage;
    }

    @Override
    public String toString() {
        return "ClassificationReport{" +
                "textCoverage=" + textCoverage +
                ", classification=" + classification +
                '}';
    }

    public double getTextCoverage() {

        return textCoverage;
    }

    public void setTextCoverage(double textCoverage) {
        this.textCoverage = textCoverage;
    }

    public List<Classification> getClassification() {
        return classification;
    }

    public void setClassification(List<Classification> classification) {
        this.classification = classification;
    }

    class Classification {
        private String className;
        private double p;

        public Classification(String className) {
            this.className = className;
        }

        public String getClassName() {

            return className;
        }

        public void setClassName(String className) {
            this.className = className;
        }

        @Override
        public String toString() {
            return "Classification{" +
                    "className='" + className + '\'' +
                    ", p=" + p +
                    '}';
        }

        public double getP() {
            return p;
        }

        public void setP(double p) {
            this.p = p;
        }
    }
}